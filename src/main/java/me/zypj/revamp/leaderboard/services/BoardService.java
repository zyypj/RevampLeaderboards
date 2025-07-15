package me.zypj.revamp.leaderboard.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.repository.BoardRepository;
import me.zypj.revamp.leaderboard.repository.JdbcBoardRepository.BoardBatchEntry;
import me.zypj.revamp.leaderboard.repository.JdbcBoardRepository;

import me.zypj.revamp.leaderboard.services.sort.BoardSortingStrategy;
import me.zypj.revamp.leaderboard.services.sort.ValueDescNameAscStrategy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;

public class BoardService {

    private final LeaderboardPlugin plugin;
    private final BoardsConfigAdapter boardsAdapter;
    private final BoardRepository repo;

    private final LoadingCache<CacheKey, List<BoardEntry>> cache;
    private final BoardSortingStrategy sortingStrategy;

    private final Map<String, String> sanitizedMap = new HashMap<>();
    private final Map<String, EnumMap<PeriodType, String>> tableMap = new HashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<UUID, Double>> lastValues = new ConcurrentHashMap<>();

    public BoardService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.boardsAdapter = plugin.getBootstrap().getBoardsConfigAdapter();
        ConfigAdapter cfg = plugin.getBootstrap().getConfigAdapter();

        this.sortingStrategy = new ValueDescNameAscStrategy();

        int poolSize = cfg.getDatabaseThreadPoolSize();
        ExecutorService dbExec = Executors.newFixedThreadPool(poolSize);
        DataSource ds = plugin.getBootstrap().getDatabaseService().getDataSource();
        this.repo = new JdbcBoardRepository(ds, dbExec);

        for (String raw : boardsAdapter.getBoards()) {
            String san = sanitize(raw);
            sanitizedMap.put(raw, san);
            EnumMap<PeriodType, String> m = new EnumMap<>(PeriodType.class);
            for (PeriodType pt : PeriodType.values()) {
                m.put(pt, san + "_" + pt.name().toLowerCase());
            }
            tableMap.put(raw, m);
        }

        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(cfg.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .refreshAfterWrite(cfg.getCacheRefreshSeconds(), TimeUnit.SECONDS)
                .build(key -> {
                    String table = sanitizedMap.get(key.raw) + "_" + key.period.name().toLowerCase();
                    return repo.loadTop(table, key.limit);
                });
    }

    public void init() {
        List<String> all = new ArrayList<>();
        tableMap.values().forEach(m -> all.addAll(m.values()));
        repo.initTables(all);
    }

    public void saveOnJoin(OfflinePlayer off) {
        for (String raw : boardsAdapter.getBoards()) {
            EnumMap<PeriodType, String> m = tableMap.get(raw);
            for (Map.Entry<PeriodType, String> e : m.entrySet()) {
                double v = parsePlaceholder(off, raw);
                repo.save(e.getValue(), off.getUniqueId().toString(), off.getName(), v);
            }
        }
    }

    public void updateAll() {
        for (String raw : boardsAdapter.getBoards()) {
            EnumMap<PeriodType, String> m = tableMap.get(raw);
            for (String table : m.values()) {
                ConcurrentMap<UUID, Double> map = lastValues
                        .computeIfAbsent(table, t -> new ConcurrentHashMap<>());
                List<BoardBatchEntry> batch = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    UUID id = p.getUniqueId();
                    double nv = parsePlaceholder(p, raw);
                    if (nv != map.getOrDefault(id, -1d)) {
                        map.put(id, nv);
                        batch.add(new BoardBatchEntry(id.toString(), p.getName(), nv));
                    }
                }
                repo.batchSave(table, batch);
            }
        }
    }

    public void invalidateCache() {
        cache.invalidateAll();
    }

    public List<BoardEntry> getLeaderboard(String raw, PeriodType period, int limit) {
        if (!sanitizedMap.containsKey(raw))
            throw new IllegalArgumentException("Unknown board: " + raw);

        CacheKey key = new CacheKey(raw, period, limit);
        try {
            List<BoardEntry> originals = cache.get(key);
            assert originals != null;

            Map<String, BoardEntry> unique = new LinkedHashMap<>();
            for (BoardEntry entry : originals)
                unique.putIfAbsent(entry.getUuid(), entry);
            List<BoardEntry> deduped = new ArrayList<>(unique.values());

            List<BoardEntry> filtered = new ArrayList<>(deduped.size());
            for (BoardEntry entry : deduped) {
                Player player = Bukkit.getPlayer(UUID.fromString(entry.getUuid()));
                if (player != null && player.hasPermission("leaderboard.bypass")) continue;
                filtered.add(entry);
            }

            filtered.sort(sortingStrategy.comparator());

            if (limit > 0 && filtered.size() > limit) return filtered.subList(0, limit);
            return filtered;
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to load leaderboard: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    public List<BoardEntry> getLeaderboard(String raw, PeriodType period) {
        return getLeaderboard(raw, period, 0);
    }

    public void reset(PeriodType period) {
        tableMap.values().forEach(m -> repo.truncate(m.get(period)));
        lastValues.clear();
    }

    public void clearDatabase() {
        tableMap.values().forEach(m -> m.values().forEach(repo::truncate));
        lastValues.clear();
    }

    public void clearBoard(String raw) {
        if (!tableMap.containsKey(raw)) throw new IllegalArgumentException("Unknown board: " + raw);

        EnumMap<PeriodType, String> m = tableMap.get(raw);
        for (String table : m.values()) {
            repo.truncate(table);
            lastValues.remove(table);
        }

        invalidateCache();
    }

    public void addBoard(String raw) {
        boardsAdapter.addBoard(raw);
        String san = sanitize(raw);
        sanitizedMap.put(raw, san);
        EnumMap<PeriodType, String> m = new EnumMap<>(PeriodType.class);
        for (PeriodType pt : PeriodType.values()) {
            m.put(pt, san + "_" + pt.name().toLowerCase());
        }
        tableMap.put(raw, m);
        repo.initTables(new ArrayList<>(m.values()));
    }

    public void removeBoard(String raw) {
        boardsAdapter.removeBoard(raw);
        sanitizedMap.remove(raw);
        tableMap.remove(raw);
    }

    public List<String> getBoards() {
        return Collections.unmodifiableList(boardsAdapter.getBoards());
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
