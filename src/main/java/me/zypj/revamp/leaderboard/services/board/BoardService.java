package me.zypj.revamp.leaderboard.services.board;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter.BoardConfig;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter.BoardType;
import me.zypj.revamp.leaderboard.board.AbstractBoard;
import me.zypj.revamp.leaderboard.board.GenericBoardEntry;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.repository.BoardRepository;
import me.zypj.revamp.leaderboard.repository.BoardRepository.BoardBatchEntry;
import me.zypj.revamp.leaderboard.services.database.ShardManager;
import me.zypj.revamp.leaderboard.api.events.LeaderboardUpdateEvent;
import me.zypj.revamp.leaderboard.api.events.BoardEntryAchievedEvent;
import me.zypj.revamp.leaderboard.api.events.BoardResetEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BoardService {

    private final LeaderboardPlugin plugin;
    private final BoardRepository boardRepository;
    private final ShardManager shardManager;

    private final List<AbstractBoard<?>> boards;

    private final LoadingCache<CacheKey, List<BoardEntry>> cache;
    private final ConcurrentMap<String, ConcurrentMap<Object, Double>> lastValues = new ConcurrentHashMap<>();

    public BoardService(LeaderboardPlugin plugin, List<AbstractBoard<?>> boards) {
        this.plugin = plugin;
        this.boards = boards;
        this.boardRepository = plugin.getBootstrap().getBoardRepository();
        this.shardManager = plugin.getBootstrap().getShardManager();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(plugin.getBootstrap().getConfigAdapter().getCacheTtlSeconds(), TimeUnit.SECONDS)
                .refreshAfterWrite(plugin.getBootstrap().getConfigAdapter().getCacheRefreshSeconds(), TimeUnit.SECONDS)
                .build(this::loadSharded);
    }

    public void init() {
        List<String> all = new ArrayList<>();
        for (AbstractBoard<?> board : boards) {
            String san = sanitize(board.getBoardKey());
            for (PeriodType pt : PeriodType.values()) {
                all.add(san + "_" + pt.name().toLowerCase());
            }
        }
        boardRepository.initTables(all);
        shardManager.init();
    }

    public void saveOnJoin(OfflinePlayer player) {
        BoardsConfigAdapter adapter = plugin.getBootstrap().getBoardsConfigAdapter();
        for (AbstractBoard<?> board : boards) {
            String raw = board.getBoardKey();
            String san = sanitize(raw);
            BoardConfig bc = adapter.getBoardConfig(raw);

            for (PeriodType pt : PeriodType.values()) {
                String table = shardManager.getShardForWrite(san, pt);

                if (bc.getType() == BoardType.PLAYER) {
                    String ph = bc.getPlaceholder();
                    String rawVal = PlaceholderAPI.setPlaceholders(player, ph);
                    double val = parseDouble(rawVal);
                    boardRepository.save(table,
                            player.getUniqueId().toString(),
                            player.getName(),
                            val);
                } else {
                    String key = PlaceholderAPI.setPlaceholders(player, bc.getKeyPlaceholder());
                    if (key.isEmpty()) continue;
                    String rawVal = PlaceholderAPI.setPlaceholders(player, bc.getValuePlaceholder());
                    double val = parseDouble(rawVal);

                    boardRepository.save(table, key, key, val);
                }
            }
        }
    }

    public void updateAll() {
        BoardsConfigAdapter adapter = plugin.getBootstrap().getBoardsConfigAdapter();

        for (AbstractBoard<?> board : boards) {
            String raw = board.getBoardKey();
            String san = sanitize(raw);
            BoardConfig bc = adapter.getBoardConfig(raw);

            for (PeriodType pt : PeriodType.values()) {
                String mapKey = san + "_" + pt.name().toLowerCase();
                ConcurrentMap<Object, Double> map = lastValues
                        .computeIfAbsent(mapKey, k -> new ConcurrentHashMap<>());

                List<? extends GenericBoardEntry<?>> entries = board.getTop(0);
                List<BoardBatchEntry> batch = new ArrayList<>();

                for (GenericBoardEntry<?> e : entries) {
                    String keyStr = e.getKey().toString();
                    double nv = e.getValue();
                    String display;
                    if (bc.getType() == BoardType.PLAYER) {
                        display = Bukkit.getOfflinePlayer(UUID.fromString(keyStr)).getName();
                    } else {
                        display = keyStr;
                    }

                    if (nv != map.getOrDefault(e.getKey(), -1d)) {
                        map.put(e.getKey(), nv);
                        batch.add(new BoardBatchEntry(keyStr, display, nv));
                    }
                }

                if (!batch.isEmpty()) {
                    String target = shardManager.getShardForWrite(san, pt);
                    boardRepository.batchSave(target, batch);

                    if (bc.getType() == BoardType.PLAYER) {
                        Map<UUID, Double> changed = batch.stream().collect(
                                Collectors.toMap(
                                        be -> UUID.fromString(be.key),
                                        be -> be.value
                                )
                        );
                        Bukkit.getPluginManager()
                                .callEvent(new LeaderboardUpdateEvent(raw, pt, changed));
                        for (BoardBatchEntry be : batch) {
                            Bukkit.getPluginManager()
                                    .callEvent(new BoardEntryAchievedEvent(raw, pt,
                                            UUID.fromString(be.key), be.value));
                        }
                    }
                }
            }
        }
    }

    public void invalidateCache() {
        cache.invalidateAll();
    }

    public List<BoardEntry> getLeaderboard(String raw, PeriodType period, int limit) {
        CacheKey key = new CacheKey(raw, period, limit);
        try {
            List<BoardEntry> list = cache.get(key);
            return list != null ? list : Collections.emptyList();
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to load leaderboard: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    public List<BoardEntry> getLeaderboard(String raw, PeriodType period) {
        return getLeaderboard(raw, period, 0);
    }

    public void reset(PeriodType period) {
        for (AbstractBoard<?> board : boards) {
            String san = sanitize(board.getBoardKey());
            for (String shard : shardManager.getShards(san, period)) {
                boardRepository.truncate(shard);
            }
        }
        lastValues.clear();
        Bukkit.getPluginManager().callEvent(new BoardResetEvent(period));
    }

    public void clearDatabase() {
        for (AbstractBoard<?> board : boards) {
            String san = sanitize(board.getBoardKey());
            for (PeriodType pt : PeriodType.values()) {
                for (String shard : shardManager.getShards(san, pt)) {
                    boardRepository.truncate(shard);
                }
            }
        }
        lastValues.clear();
    }

    public void clearBoard(String raw) {
        String san = sanitize(raw);
        for (PeriodType pt : PeriodType.values()) {
            for (String shard : shardManager.getShards(san, pt)) {
                boardRepository.truncate(shard);
            }
            lastValues.remove(san + "_" + pt.name().toLowerCase());
        }
        invalidateCache();
    }

    private List<BoardEntry> loadSharded(CacheKey key) {
        String san = sanitize(key.raw);
        List<BoardEntry> combined = new ArrayList<>();
        for (String tbl : shardManager.getShards(san, key.period)) {
            combined.addAll(boardRepository.loadTop(tbl, Math.max(key.limit, 0)));
        }
        combined.sort(Comparator
                .comparingDouble(BoardEntry::getValue).reversed()
                .thenComparing(BoardEntry::getDisplay));
        if (key.limit > 0 && combined.size() > key.limit) {
            return combined.subList(0, key.limit);
        }
        return combined;
    }

    private String sanitize(String in) {
        return in.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
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
