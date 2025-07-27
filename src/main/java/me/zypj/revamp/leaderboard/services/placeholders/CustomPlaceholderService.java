package me.zypj.revamp.leaderboard.services.placeholders;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.model.CustomPlaceholder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CustomPlaceholderService {

    private final LeaderboardPlugin plugin;
    private final DataSource dataSource;
    private final Map<String, CustomPlaceholder> placeholders;
    private final boolean isSqlite;

    private final ConcurrentMap<UUID, ConcurrentMap<String, String>> cache = new ConcurrentHashMap<>();
    private final BukkitTask refreshTask;

    public CustomPlaceholderService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.dataSource = plugin.getBootstrap().getDatabaseService().getDataSource();
        this.placeholders = plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders();
        this.isSqlite = "sqlite".equalsIgnoreCase(plugin.getBootstrap().getConfigAdapter().getDatabaseType());

        initTable();
        loadAllFromDb();

        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllOnline, 0L, 20L * 10L);
    }

    private void initTable() {
        String ddl =
                "CREATE TABLE IF NOT EXISTS custom_placeholders ("
                        + "  player_uuid VARCHAR(36) NOT NULL,"
                        + "  data_type   VARCHAR(64) NOT NULL,"
                        + "  value       TEXT NOT NULL,"
                        + "  PRIMARY KEY (player_uuid, data_type)"
                        + ")";
        executeUpdate(ddl);
    }

    private void loadAllFromDb() {
        String sql = "SELECT player_uuid, data_type, value FROM custom_placeholders";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_uuid"));
                String type = rs.getString("data_type");
                String val = rs.getString("value");
                cache.computeIfAbsent(id, k -> new ConcurrentHashMap<>())
                        .put(type, val);
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error loading custom_placeholders: " + ex.getMessage());
        }
    }

    private void refreshAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers())
            updatePlayer(p);
    }

    public void updatePlayer(Player p) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> updatePlayer(p));
            return;
        }
        UUID id = p.getUniqueId();
        ConcurrentMap<String, String> mem = cache.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        List<String> changed = new ArrayList<>();

        for (CustomPlaceholder cp : placeholders.values()) {
            String type = cp.getDataType();
            String expr = cp.getPlaceholder().replace("{player}", p.getName());
            String newVal = PlaceholderAPI.setPlaceholders(p, expr);
            boolean canBeNull = cp.isCanBeNull();

            if ((!newVal.isEmpty() || canBeNull) && !Objects.equals(mem.get(type), newVal)) {
                mem.put(type, newVal);
                changed.add(type);
            }
        }

        if (!changed.isEmpty()) {
            upsert(id, changed, mem);
        }
    }

    private void upsert(UUID id, List<String> types, Map<String, String> mem) {
        String mysqlSql =
                "INSERT INTO custom_placeholders(player_uuid, data_type, value) "
                        + "VALUES(?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE value = ?";
        String sqliteSql =
                "INSERT OR REPLACE INTO custom_placeholders(player_uuid, data_type, value) "
                        + "VALUES(?, ?, ?)";

        String sql = isSqlite ? sqliteSql : mysqlSql;

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (String type : types) {
                ps.setString(1, id.toString());
                ps.setString(2, type);
                ps.setString(3, mem.get(type));
                if (!isSqlite) ps.setString(4, mem.get(type));
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error upserting custom_placeholders: " + ex.getMessage());
        }
    }

    public String getValue(UUID id, String type) {
        return cache.getOrDefault(id, new ConcurrentHashMap<>())
                .getOrDefault(type, "");
    }

    public void shutdown() {
        if (refreshTask != null) refreshTask.cancel();
    }

    private void executeUpdate(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error executing SQL: " + ex.getMessage());
        }
    }
}
