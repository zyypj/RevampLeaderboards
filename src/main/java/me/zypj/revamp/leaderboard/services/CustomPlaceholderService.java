package me.zypj.revamp.leaderboard.services;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.model.CustomPlaceholder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class CustomPlaceholderService {

    private final LeaderboardPlugin plugin;
    private final DataSource dataSource;
    private final Map<String, CustomPlaceholder> placeholders;
    private final ConcurrentMap<UUID, ConcurrentMap<String, String>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CustomPlaceholderService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.dataSource = plugin.getBootstrap().getDatabaseService().getDataSource();
        this.placeholders = plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders();

        initTable();
        loadAllFromDb();

        scheduler.scheduleAtFixedRate(this::refreshAllOnline, 0, 10, TimeUnit.SECONDS);
    }

    private void initTable() {
        String ddl = ""
                + "CREATE TABLE IF NOT EXISTS custom_placeholders ("
                + "  player_uuid VARCHAR(36) NOT NULL,"
                + "  data_type VARCHAR(64) NOT NULL,"
                + "  value TEXT NOT NULL,"
                + "  PRIMARY KEY (player_uuid, data_type)"
                + ")";
        executeUpdate(ddl);
    }

    private void loadAllFromDb() {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT player_uuid, data_type, value FROM custom_placeholders");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_uuid"));
                String type = rs.getString("data_type");
                String val = rs.getString("value");
                cache
                        .computeIfAbsent(id, __ -> new ConcurrentHashMap<>())
                        .put(type, val);
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Erro carregando custom_placeholders: " + ex.getMessage());
        }
    }

    private void refreshAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p);
        }
    }

    public void updatePlayer(Player p) {
        UUID id = p.getUniqueId();
        ConcurrentMap<String, String> mem = cache.computeIfAbsent(id, __ -> new ConcurrentHashMap<>());
        List<String> changed = new ArrayList<>();

        for (CustomPlaceholder cp : placeholders.values()) {
            String type = cp.getDataType();
            String ph = cp.getPlaceholder().replace("{player}", p.getName());
            String newVal = PlaceholderAPI.setPlaceholders(p, ph);
            boolean canNull = cp.isCanBeNull();

            if ((!newVal.isEmpty() || canNull) && !Objects.equals(mem.get(type), newVal)) {
                mem.put(type, newVal);
                changed.add(type);
            }
        }

        if (!changed.isEmpty()) {
            upsert(id, changed, mem);
        }
    }

    private void upsert(UUID id, List<String> types, Map<String, String> mem) {
        String sql = ""
                + "INSERT INTO custom_placeholders(player_uuid, data_type, value) "
                + "VALUES(?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE value = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (String t : types) {
                ps.setString(1, id.toString());
                ps.setString(2, t);
                ps.setString(3, mem.get(t));
                ps.setString(4, mem.get(t));
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Erro upsert custom_placeholder: " + ex.getMessage());
        }
    }

    public String getValue(UUID id, String type) {
        return cache.getOrDefault(id, new ConcurrentHashMap<>()).getOrDefault(type, "");
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void executeUpdate(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Erro init custom_placeholders: " + ex.getMessage());
        }
    }
}
