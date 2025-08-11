package me.zypj.revamp.leaderboard.services.board;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import me.zypj.revamp.leaderboard.api.events.BoardEntryAchievedEvent;
import me.zypj.revamp.leaderboard.api.events.BoardResetEvent;
import me.zypj.revamp.leaderboard.api.events.LeaderboardUpdateEvent;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.model.CompositeBoard;
import me.zypj.revamp.leaderboard.repository.BoardRepository;
import me.zypj.revamp.leaderboard.repository.impl.JdbcBoardRepository.BoardBatchEntry;
import me.zypj.revamp.leaderboard.services.database.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BoardService {

    private final LeaderboardPlugin plugin;
    private final BoardsConfigAdapter boardsAdapter;
    private final BoardRepository boardRepository;
    private final ShardManager shardManager;

    private final LoadingCache<CacheKey, List<BoardEntry>> cache;

    private final Map<String, String> sanitizedMap = new HashMap<>();
    private final Map<String, EnumMap<PeriodType, String>> tableMap = new HashMap<>();

    private final ConcurrentMap<String, ConcurrentMap<UUID, Double>> lastValues = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, CompositeBoard> compositeBoards = new ConcurrentHashMap<>();

    public BoardService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.boardsAdapter = plugin.getBootstrap().getBoardsConfigAdapter();
        ConfigAdapter cfg = plugin.getBootstrap().getConfigAdapter();
        boardRepository = plugin.getBootstrap().getBoardRepository();
        this.shardManager = plugin.getBootstrap().getShardManager();

        for (String raw : boardsAdapter.getBoards()) {
            String san = sanitize(raw);
            sanitizedMap.put(raw, san);
            EnumMap<PeriodType, String> m = new EnumMap<>(PeriodType.class);
            for (PeriodType pt : PeriodType.values()) {
                m.put(pt, san + "_" + pt.name().toLowerCase());
            }
            tableMap.put(raw, m);
        }

        Map<String, List<String>> composites = boardsAdapter.getCompositeBoards();
        if (composites != null) {
            for (Map.Entry<String, List<String>> e : composites.entrySet()) {
                String key = sanitize(e.getKey());
                List<String> phs = e.getValue() == null ? Collections.emptyList() : e.getValue();
                compositeBoards.put(key, new CompositeBoard(key, phs));
            }
        }

        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(cfg.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .refreshAfterWrite(cfg.getCacheRefreshSeconds(), TimeUnit.SECONDS)
                .build(this::loadSharded);
    }

    public void init() {
        List<String> all = new ArrayList<>();
        tableMap.values().forEach(m -> all.addAll(m.values()));
        boardRepository.initTables(all);
        shardManager.init();
    }

    public void saveOnJoin(OfflinePlayer off) {
        for (String raw : boardsAdapter.getBoards()) {
            for (Map.Entry<PeriodType, String> e : tableMap.get(raw).entrySet()) {
                double v = parsePlaceholder(off, raw);
                String target = shardManager.getShardForWrite(raw, e.getKey());
                boardRepository.save(target, off.getUniqueId().toString(), off.getName(), v);
            }
        }
    }

    public void updateAll() {
        for (String raw : boardsAdapter.getBoards()) {
            for (PeriodType period : PeriodType.values()) {
                ConcurrentMap<UUID, Double> map = lastValues
                        .computeIfAbsent(raw + "_" + period, k -> new ConcurrentHashMap<>());

                List<BoardBatchEntry> batch = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    UUID id = p.getUniqueId();
                    double nv = parsePlaceholder(p, raw);
                    if (nv != map.getOrDefault(id, -1d)) {
                        map.put(id, nv);
                        batch.add(new BoardBatchEntry(id.toString(), p.getName(), nv));
                    }
                }

                if (!batch.isEmpty()) {
                    String target = shardManager.getShardForWrite(raw, period);
                    boardRepository.batchSave(target, batch);

                    Map<UUID, Double> changedValues = batch.stream()
                            .collect(Collectors.toMap(
                                    be -> UUID.fromString(be.uuid),
                                    be -> be.value
                            ));
                    Bukkit.getPluginManager().callEvent(
                            new LeaderboardUpdateEvent(raw, period, changedValues)
                    );

                    for (BoardBatchEntry be : batch) {
                        UUID id = UUID.fromString(be.uuid);
                        Bukkit.getPluginManager().callEvent(
                                new BoardEntryAchievedEvent(raw, period, id, be.value)
                        );
                    }
                }
            }
        }
    }

    public void invalidateCache() {
        cache.invalidateAll();
    }

    public List<BoardEntry> getLeaderboard(String raw, PeriodType period, int limit) {
        if (!sanitizedMap.containsKey(raw)) throw new IllegalArgumentException("Unknown board: " + raw);

        CacheKey key = new CacheKey(raw, period, limit);
        try {
            List<BoardEntry> originals = cache.get(key);
            assert originals != null;
            return originals;
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to load leaderboard: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    public List<BoardEntry> getLeaderboard(String raw, PeriodType period) {
        return getLeaderboard(raw, period, 0);
    }

    public void reset(PeriodType period) {
        for (String raw : boardsAdapter.getBoards()) {
            for (String shard : shardManager.getShards(raw, period)) {
                boardRepository.truncate(shard);
            }
        }
        lastValues.clear();
        Bukkit.getPluginManager().callEvent(new BoardResetEvent(period));
    }

    public void clearDatabase() {
        for (String raw : boardsAdapter.getBoards()) {
            for (PeriodType pt : PeriodType.values()) {
                for (String shard : shardManager.getShards(raw, pt)) {
                    boardRepository.truncate(shard);
                }
            }
        }
        lastValues.clear();
    }

    public void clearBoard(String raw) {
        if (!sanitizedMap.containsKey(raw)) throw new IllegalArgumentException("Unknown board: " + raw);

        for (PeriodType pt : PeriodType.values()) {
            for (String shard : shardManager.getShards(raw, pt)) {
                boardRepository.truncate(shard);
            }
            lastValues.remove(raw + "_" + pt);
        }

        invalidateCache();
    }

    public void addBoard(String raw) {
        boardsAdapter.addBoard(raw);
        String san = sanitize(raw);
        sanitizedMap.put(raw, san);
        EnumMap<PeriodType, String> m = new EnumMap<>(PeriodType.class);
        for (PeriodType pt : PeriodType.values())
            m.put(pt, san + "_" + pt.name().toLowerCase());
        tableMap.put(raw, m);
        boardRepository.initTables(new ArrayList<>(m.values()));
        shardManager.init();
    }

    public void removeBoard(String raw) {
        boardsAdapter.removeBoard(raw);
        sanitizedMap.remove(raw);
        tableMap.remove(raw);
        shardManager.init();
    }

    public List<String> getBoards() {
        return Collections.unmodifiableList(boardsAdapter.getBoards());
    }

    public void addCompositeBoard(String rawKey, List<String> placeholders) {
        String key = sanitize(rawKey);
        compositeBoards.put(key, new CompositeBoard(key, placeholders));
        boardsAdapter.addCompositeBoard(key, placeholders);
    }

    public void removeCompositeBoard(String rawKey) {
        String key = sanitize(rawKey);
        compositeBoards.remove(key);
        boardsAdapter.removeCompositeBoard(key);
    }

    public boolean isComposite(String rawKey) {
        return compositeBoards.containsKey(sanitize(rawKey));
    }

    public Set<String> getCompositeKeys() {
        return Collections.unmodifiableSet(compositeBoards.keySet());
    }

    public double resolveCompositeValue(UUID playerId, String boardKey) {
        CompositeBoard cb = compositeBoards.get(sanitize(boardKey));
        if (cb == null) return 0.0;
        double total = 0.0;
        OfflinePlayer off = Bukkit.getOfflinePlayer(playerId);
        for (String ph : cb.getPlaceholders()) {
            try {
                String raw = PlaceholderAPI.setPlaceholders(off, ph);
                total += Double.parseDouble(raw.replace(",", ".").trim());
            } catch (Exception ignored) {
            }
        }
        return total;
    }


    private List<BoardEntry> loadSharded(CacheKey key) {
        List<BoardEntry> combined = new ArrayList<>();
        for (String tbl : shardManager.getShards(key.raw, key.period)) {
            combined.addAll(boardRepository.loadTop(tbl, Math.max(key.limit, 0)));
        }
        combined.sort(Comparator
                .comparingDouble(BoardEntry::getValue).reversed()
                .thenComparing(BoardEntry::getPlayerName));
        if (key.limit > 0 && combined.size() > key.limit) {
            return combined.subList(0, key.limit);
        }
        return combined;
    }

    private double parsePlaceholder(OfflinePlayer off, String raw) {
        try {
            String s = PlaceholderAPI.setPlaceholders(off, "%" + raw + "%");
            return Double.parseDouble(s);
        } catch (Exception ex) {
            return 0d;
        }
    }

    private String sanitize(String in) {
        return in.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }

    private static class CacheKey {
        final String raw;
        final PeriodType period;
        final int limit;

        CacheKey(String raw, PeriodType period, int limit) {
            this.raw = raw;
            this.period = period;
            this.limit = limit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey k = (CacheKey) o;
            return limit == k.limit
                    && raw.equals(k.raw)
                    && period == k.period;
        }

        @Override
        public int hashCode() {
            return Objects.hash(raw, period, limit);
        }
    }
}
