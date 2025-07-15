package me.zypj.revamp.leaderboard.repository;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.model.BoardEntry;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcBoardRepository implements BoardRepository {

    private static final Logger LOGGER = LeaderboardPlugin
            .getPlugin(LeaderboardPlugin.class)
            .getLogger();

    private final DataSource ds;
    private final ExecutorService executor;

    public JdbcBoardRepository(DataSource ds, ExecutorService executor) {
        this.ds = ds;
        this.executor = executor;
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void initTables(List<String> rawBoards) {
        for (String table : rawBoards) {
            String ddl = "CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "value DOUBLE NOT NULL," +
                    "PRIMARY KEY (player_uuid)" +
                    ")";
            executor.submit(() -> exec(ddl));
        }
    }

    @Override
    public void save(String table, String uuid, String name, double value) {
        String sql = "INSERT INTO `" + table + "`(player_uuid, player_name, value) VALUES(?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), value=VALUES(value)";
        executor.submit(() -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setString(2, name);
                ps.setDouble(3, value);
                ps.executeUpdate();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error saving to table " + table, ex);
            }
        });
    }

    @Override
    public void batchSave(String table, List<BoardBatchEntry> batch) {
        if (batch.isEmpty()) return;
        StringBuilder sb = new StringBuilder("INSERT INTO `").append(table)
                .append("`(player_uuid, player_name, value) VALUES ");
        for (int i = 0; i < batch.size(); i++) {
            sb.append("(?, ?, ?)");
            if (i < batch.size() - 1) sb.append(",");
        }
        sb.append(" ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), value=VALUES(value)");
        String sql = sb.toString();

        executor.submit(() -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                int idx = 1;
                for (BoardBatchEntry e : batch) {
                    ps.setString(idx++, e.uuid);
                    ps.setString(idx++, e.name);
                    ps.setDouble(idx++, e.value);
                }
                ps.executeUpdate();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error batch saving to table " + table, ex);
            }
        });
    }

    @Override
    public void truncate(String table) {
        executor.submit(() -> exec("TRUNCATE TABLE `" + table + "`"));
    }

    @Override
    public List<BoardEntry> loadTop(String table, int limit) {
        List<BoardEntry> list = new ArrayList<>();
        String sql = "SELECT player_uuid, player_name, value "
                + "  FROM `" + table + "` "
                + " ORDER BY value DESC, player_name ASC"
                + (limit > 0 ? " LIMIT ?" : "");

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (limit > 0) ps.setInt(1, limit);

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
            LOGGER.log(Level.SEVERE, "Error loading top entries from table " + table, ex);
        }
        return list;
    }


    private void exec(String sql) {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error executing SQL: " + sql, ex);
        }
    }

    public static class BoardBatchEntry {
        public final String uuid;
        public final String name;
        public final double value;

        public BoardBatchEntry(String uuid, String name, double value) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }
    }
}
