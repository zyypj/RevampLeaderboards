package me.zypj.revamp.leaderboard.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class BoardService {

    private final LeaderboardPlugin plugin;
    private final DataSource dataSource;
    private final BoardsConfigAdapter boardsAdapter;
    private final ConfigAdapter configAdapter;

    private final LoadingCache<CacheKey, List<BoardEntry>> leaderboardCache;
    private final ExecutorService dbExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Map<String, String> sanitizedMap = new HashMap<>();
    private final Map<String, EnumMap<PeriodType, String>> tableMap = new HashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<UUID, Double>> lastValues = new ConcurrentHashMap<>();

    public BoardService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.dataSource = plugin.getBootstrap().getDatabaseService().getDataSource();
        this.boardsAdapter = plugin.getBootstrap().getBoardsConfigAdapter();
        this.configAdapter = plugin.getBootstrap().getConfigAdapter();

        for (String raw : boardsAdapter.getBoards()) {
            String san = sanitize(raw);
            sanitizedMap.put(raw, san);

            EnumMap<PeriodType, String> tbls = new EnumMap<>(PeriodType.class);
            for (PeriodType pt : PeriodType.values()) {
                tbls.put(pt, san + "_" + pt.name().toLowerCase());
            }
            tableMap.put(raw, tbls);
        }

        this.leaderboardCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build(new CacheLoader<CacheKey, List<BoardEntry>>() {
                    @Override
                    public List<BoardEntry> load(CacheKey key) {
                        return loadTopFromDb(key.raw, key.period, key.limit);
                    }
                });
    }

    public void initTables() {
        for (String raw : boardsAdapter.getBoards()) {
            for (String tbl : tableMap.get(raw).values()) {
                String ddl = "CREATE TABLE IF NOT EXISTS `" + tbl + "` ("
                        + "player_uuid VARCHAR(36) NOT NULL,"
                        + "player_name VARCHAR(16) NOT NULL,"
                        + "value DOUBLE NOT NULL,"
                        + "PRIMARY KEY (player_uuid)"
                        + ")";
                dbExecutor.submit(() -> executeUpdate(ddl));
            }
        }
    }

    public void saveOnJoin(OfflinePlayer off) {
        for (String raw : boardsAdapter.getBoards()) {
            String san = sanitizedMap.get(raw);
            for (PeriodType pt : PeriodType.values()) {
                double val = parsePlaceholder(off, raw);
                scheduleUpsert(san, off, pt, val);
            }
        }
        leaderboardCache.invalidateAll();
    }

    public void updateAll() {
        for (String raw : boardsAdapter.getBoards()) {
            String san = sanitizedMap.get(raw);
            EnumMap<PeriodType, String> tbls = tableMap.get(raw);

            for (PeriodType pt : PeriodType.values()) {
                String table = tbls.get(pt);
                ConcurrentMap<UUID, Double> map =
                        lastValues.computeIfAbsent(table, __ -> new ConcurrentHashMap<>());

                List<BatchEntry> batch = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    UUID id = p.getUniqueId();
                    double newVal = parsePlaceholder(p, raw);
                    if (newVal != map.getOrDefault(id, -1d)) {
                        map.put(id, newVal);
                        batch.add(new BatchEntry(id.toString(), p.getName(), newVal));
                    }
                }

                if (!batch.isEmpty()) {
                    batchUpsert(table, batch);
                }
            }
        }
        leaderboardCache.invalidateAll();
    }

    public List<BoardEntry> getLeaderboard(String rawPlaceholder, PeriodType period) {
        return getLeaderboard(rawPlaceholder, period, 0);
    }

    public List<BoardEntry> getLeaderboard(String rawPlaceholder, PeriodType period, int limit) {
        CacheKey key = new CacheKey(rawPlaceholder, period, limit);
        try {
            return leaderboardCache.get(key);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load leaderboard cache: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void reset(PeriodType period) {
        for (String raw : boardsAdapter.getBoards()) {
            String table = tableMap.get(raw).get(period);
            dbExecutor.submit(() -> executeUpdate("TRUNCATE TABLE `" + table + "`"));
        }
        lastValues.clear();
        leaderboardCache.invalidateAll();
    }

    public void clearDatabase() {
        for (String raw : boardsAdapter.getBoards()) {
            for (String tbl : tableMap.get(raw).values()) {
                dbExecutor.submit(() -> executeUpdate("TRUNCATE TABLE `" + tbl + "`"));
            }
        }
        lastValues.clear();
        leaderboardCache.invalidateAll();
    }

    public void addBoard(String rawPh) {
        boardsAdapter.addBoard(rawPh);
        String san = sanitize(rawPh);
        sanitizedMap.put(rawPh, san);

        EnumMap<PeriodType, String> tbls = new EnumMap<>(PeriodType.class);
        for (PeriodType pt : PeriodType.values()) {
            String tbl = san + "_" + pt.name().toLowerCase();
            tbls.put(pt, tbl);
            String ddl = "CREATE TABLE IF NOT EXISTS `" + tbl + "` ("
                    + "player_uuid VARCHAR(36) NOT NULL,"
                    + "player_name VARCHAR(16) NOT NULL,"
                    + "value DOUBLE NOT NULL,"
                    + "PRIMARY KEY (player_uuid)"
                    + ")";
            dbExecutor.submit(() -> executeUpdate(ddl));
        }
        tableMap.put(rawPh, tbls);
        leaderboardCache.invalidateAll();
    }

    public void removeBoard(String rawPh) {
        boardsAdapter.removeBoard(rawPh);
        sanitizedMap.remove(rawPh);
        tableMap.remove(rawPh);
        leaderboardCache.invalidateAll();
    }

    public List<String> getBoards() {
        return Collections.unmodifiableList(boardsAdapter.getBoards());
    }

    private List<BoardEntry> loadTopFromDb(String raw, PeriodType period, int limit) {
        List<BoardEntry> list = new ArrayList<>();
        String table = sanitizedMap.get(raw) + "_" + period.name().toLowerCase();

        String sql = "SELECT player_uuid, player_name, value FROM `" + table + "` "
                + "ORDER BY value DESC"
                + (limit > 0 ? " LIMIT ?" : "");

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (limit > 0) {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BoardEntry(
                            rs.getString("player_uuid"),
                            rs.getString("player_name"),
                            rs.getDouble("value")
                    ));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error loadTop " + table + ": " + ex.getMessage());
        }
        return list;
    }

    private void scheduleUpsert(String sanitized, OfflinePlayer off, PeriodType period, double val) {
        String table = sanitized + "_" + period.name().toLowerCase();
        String sql = "INSERT INTO `" + table + "` "
                + "(player_uuid, player_name, value) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), value = VALUES(value)";

        dbExecutor.submit(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, off.getUniqueId().toString());
                ps.setString(2, off.getName());
                ps.setDouble(3, val);
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Error upsert " + table + ": " + ex.getMessage());
            }
        });
    }

    private void batchUpsert(String table, List<BatchEntry> batch) {
        StringBuilder sb = new StringBuilder(
                "INSERT INTO `" + table + "` (player_uuid, player_name, value) VALUES "
        );
        for (int i = 0; i < batch.size(); i++) {
            sb.append("(?, ?, ?)");
            if (i < batch.size() - 1) sb.append(", ");
        }
        sb.append(" ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), value = VALUES(value)");

        dbExecutor.submit(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sb.toString())) {

                int idx = 1;
                for (BatchEntry e : batch) {
                    ps.setString(idx++, e.uuid);
                    ps.setString(idx++, e.name);
                    ps.setDouble(idx++, e.value);
                }
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Error batch upsert " + table + ": " + ex.getMessage());
            }
        });
    }

    private double parsePlaceholder(OfflinePlayer off, String rawPh) {
        try {
            String s = PlaceholderAPI.setPlaceholders(off, "%" + rawPh + "%");
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
    }

    private void executeUpdate(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error exec: " + ex.getMessage());
        }
    }

    private String sanitize(String in) {
        return in.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }

    private static class CacheKey {
        private final String raw;
        private final PeriodType period;
        private final int limit;

        CacheKey(String raw, PeriodType period, int limit) {
            this.raw = raw;
            this.period = period;
            this.limit = limit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey key = (CacheKey) o;
            return limit == key.limit
                    && raw.equals(key.raw)
                    && period == key.period;
        }

        @Override
        public int hashCode() {
            return Objects.hash(raw, period, limit);
        }
    }

    private static class BatchEntry {
        final String uuid;
        final String name;
        final double value;

        BatchEntry(String uuid, String name, double value) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }
    }
}
