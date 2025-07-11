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
import me.zypj.revamp.leaderboard.model.CustomPlaceholder;
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

    private final LoadingCache<String, List<BoardEntry>> leaderboardCache;
    private final ExecutorService dbExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Map<String, String> sanitizedMap = new HashMap<>();
    private final Map<String, EnumMap<PeriodType, String>> tableMap = new HashMap<>();
    private final Map<String, EnumMap<PeriodType, String>> cacheKeyMap = new HashMap<>();
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
            EnumMap<PeriodType, String> cks = new EnumMap<>(PeriodType.class);
            for (PeriodType pt : PeriodType.values()) {
                String suffix = "_" + pt.name().toLowerCase();
                tbls.put(pt, san + suffix);
                cks.put(pt, san + suffix);
            }
            tableMap.put(raw, tbls);
            cacheKeyMap.put(raw, cks);
        }

        this.leaderboardCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<BoardEntry>>() {
                    @Override
                    public List<BoardEntry> load(String key) {
                        int idx = key.lastIndexOf('_');
                        String placeholder = key.substring(0, idx);
                        String periodKey = key.substring(idx + 1);
                        PeriodType period = PeriodType.valueOf(periodKey.toUpperCase());
                        return loadTopFromDb(placeholder, period);
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
                dbExecutor.submit(() -> executeUpdate(ddl, tbl));
            }
        }
    }

    public void saveOnJoin(OfflinePlayer off) {
        for (String raw : boardsAdapter.getBoards()) {
            String san = sanitizedMap.get(raw);
            EnumMap<PeriodType, String> cks = cacheKeyMap.get(raw);
            for (PeriodType pt : PeriodType.values()) {
                double val = parsePlaceholder(off, raw);
                scheduleUpsert(san, raw, pt, off, val);
                leaderboardCache.invalidate(cks.get(pt));
            }
        }
    }

    public void updateAll() {
        for (String raw : boardsAdapter.getBoards()) {
            String san = sanitizedMap.get(raw);
            EnumMap<PeriodType, String> cks = cacheKeyMap.get(raw);

            for (PeriodType pt : PeriodType.values()) {
                String ck = cks.get(pt);
                ConcurrentMap<UUID, Double> map =
                        lastValues.computeIfAbsent(ck, __ -> new ConcurrentHashMap<>());

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
                    batchUpsert(tableMap.get(raw).get(pt), batch);
                }
                leaderboardCache.invalidate(ck);
            }
        }
    }

    public List<BoardEntry> getLeaderboard(String placeholder, PeriodType period) {
        try {
            String key = sanitize(placeholder) + "_" + period.name().toLowerCase();
            return leaderboardCache.get(key);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load cache: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void reset(PeriodType period) {
        for (String raw : boardsAdapter.getBoards()) {
            String cacheKey = cacheKeyMap.get(raw).get(period);
            String table = tableMap.get(raw).get(period);

            dbExecutor.submit(() -> executeUpdate("TRUNCATE TABLE `" + table + "`", table));
            leaderboardCache.invalidate(cacheKey);
            lastValues.remove(cacheKey);
        }
    }

    public void clearDatabase() {
        for (String raw : boardsAdapter.getBoards()) {
            for (PeriodType pt : PeriodType.values()) {
                String table = tableMap.get(raw).get(pt);
                dbExecutor.submit(() -> executeUpdate("TRUNCATE TABLE `" + table + "`", table));
            }
        }
        leaderboardCache.invalidateAll();
        lastValues.clear();
    }

    public String getValue(String type,
                           PeriodType period,
                           int pos,
                           String placeholder) {
        List<BoardEntry> list = getLeaderboard(placeholder, period);
        Map<String, CustomPlaceholder> customMap = configAdapter.getCustomPlaceholders();
        boolean isCustom = customMap.containsKey(type);

        if (pos < 1 || pos > list.size()) {
            if (type.equalsIgnoreCase("amount")) return "0";
            if (isCustom) return "";

            return configAdapter.getNobodyMessage();
        }

        BoardEntry entry = list.get(pos - 1);
        switch (type.toLowerCase()) {
            case "playername":
                return entry.getPlayerName();
            case "uuid":
                return entry.getUuid();
            case "amount":
                return Double.toString(entry.getValue());
        }

        CustomPlaceholder cp = customMap.get(type);
        OfflinePlayer off = Bukkit.getOfflinePlayer(UUID.fromString(entry.getUuid()));
        String ph = cp.getPlaceholder().replace("{player}", off.getName());
        String live = PlaceholderAPI.setPlaceholders(off, ph);
        if (!live.isEmpty() && !live.startsWith("%")) return live;

        String cached = plugin.getBootstrap().getCustomPlaceholderService().getValue(off.getUniqueId(), type);
        return cached != null ? cached : "";
    }

    public void addBoard(String rawPh) {
        boardsAdapter.addBoard(rawPh);
        String san = sanitize(rawPh);
        sanitizedMap.put(rawPh, san);

        EnumMap<PeriodType, String> tbls = new EnumMap<>(PeriodType.class);
        EnumMap<PeriodType, String> cks = new EnumMap<>(PeriodType.class);
        for (PeriodType pt : PeriodType.values()) {
            String suffix = "_" + pt.name().toLowerCase();
            tbls.put(pt, san + suffix);
            cks.put(pt, san + suffix);
            String ddl = "CREATE TABLE IF NOT EXISTS `" + san + suffix + "` ("
                    + "player_uuid VARCHAR(36) NOT NULL,"
                    + "player_name VARCHAR(16) NOT NULL,"
                    + "value DOUBLE NOT NULL,"
                    + "PRIMARY KEY (player_uuid)"
                    + ")";
            dbExecutor.submit(() -> executeUpdate(ddl, san + suffix));
        }
        tableMap.put(rawPh, tbls);
        cacheKeyMap.put(rawPh, cks);
        for (String ck : cks.values()) {
            leaderboardCache.invalidate(ck);
            lastValues.remove(ck);
        }
    }

    public void removeBoard(String rawPh) {
        boardsAdapter.removeBoard(rawPh);
        sanitizedMap.remove(rawPh);
        EnumMap<PeriodType, String> cks = cacheKeyMap.remove(rawPh);
        tableMap.remove(rawPh);
        for (String ck : cks.values()) {
            leaderboardCache.invalidate(ck);
            lastValues.remove(ck);
        }
    }

    public List<String> getBoards() {
        return Collections.unmodifiableList(boardsAdapter.getBoards());
    }

    private List<BoardEntry> loadTopFromDb(String placeholder, PeriodType period) {
        List<BoardEntry> list = new ArrayList<>();
        String table = placeholder + "_" + period.name().toLowerCase();
        String sql = "SELECT player_uuid, player_name, value FROM `" + table
                + "` ORDER BY value DESC LIMIT 10";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new BoardEntry(
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getDouble("value")
                ));
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error loadTop " + table + ": " + ex.getMessage());
        }
        return list;
    }

    private void scheduleUpsert(
            String sanitized, String raw, PeriodType period, OfflinePlayer off, double val
    ) {
        dbExecutor.submit(() -> {
            String table = sanitized + "_" + period.name().toLowerCase();
            String sql = "INSERT INTO `" + table + "` "
                    + "(player_uuid, player_name, value) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), value = VALUES(value)";
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
        dbExecutor.submit(() -> {
            StringBuilder sql = new StringBuilder(
                    "INSERT INTO `" + table + "` (player_uuid, player_name, value) VALUES "
            );
            for (int i = 0; i < batch.size(); i++) {
                sql.append("(?, ?, ?)");
                if (i < batch.size() - 1) sql.append(", ");
            }
            sql.append(" ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), value = VALUES(value)");
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql.toString())) {
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

    private void executeUpdate(String sql, String context) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error exec " + context + ": " + ex.getMessage());
        }
    }

    private String sanitize(String in) {
        return in.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
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
