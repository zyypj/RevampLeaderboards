package me.zypj.revamp.leaderboard.repository.impl;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.repository.BoardRepository;

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
        String ddlTemplate = "CREATE TABLE IF NOT EXISTS `%s` (" +
                "entry_key VARCHAR(64) NOT NULL," +
                "entry_display VARCHAR(64) NOT NULL," +
                "value DOUBLE NOT NULL," +
                "PRIMARY KEY (entry_key)" +
                ")";
        for (String table : rawBoards) {
            String ddl = String.format(ddlTemplate, table);
            executor.submit(() -> executeUpdate(ddl));
        }
    }

    @Override
    public void save(String table, String entryKey, String entryDisplay, double value) {
        String sql = "INSERT INTO `" + table + "`(entry_key, entry_display, value) VALUES(?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE entry_display=VALUES(entry_display), value=VALUES(value)";
        executor.submit(() -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, entryKey);
                ps.setString(2, entryDisplay);
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

        StringBuilder sb = new StringBuilder("INSERT INTO `")
                .append(table)
                .append("`(entry_key, entry_display, value) VALUES ");
        for (int i = 0; i < batch.size(); i++) {
            sb.append("(?, ?, ?)");
            if (i < batch.size() - 1) sb.append(',');
        }
        sb.append(" ON DUPLICATE KEY UPDATE entry_display=VALUES(entry_display), value=VALUES(value)");
        String sql = sb.toString();

        executor.submit(() -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                int idx = 1;
                for (BoardBatchEntry e : batch) {
                    ps.setString(idx++, e.key);
                    ps.setString(idx++, e.display);
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
        executor.submit(() -> executeUpdate("TRUNCATE TABLE `" + table + "`"));
    }

    @Override
    public List<BoardEntry> loadTop(String table, int limit) {
        List<BoardEntry> list = new ArrayList<>();
        String sql = "SELECT entry_key, entry_display, value "
                + "FROM `" + table + "` "
                + "ORDER BY value DESC, entry_display ASC"
                + (limit > 0 ? " LIMIT ?" : "");

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (limit > 0) {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BoardEntry(
                            rs.getString("entry_key"),
                            rs.getString("entry_display"),
                            rs.getDouble("value")
                    ));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error loading top from table " + table, ex);
        }
        return list;
    }

    @Override
    public int count(String table) {
        String sql = "SELECT COUNT(*) FROM `" + table + "`";
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error counting table " + table, ex);
        }
        return 0;
    }

    private void executeUpdate(String sql) {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error executing SQL: " + sql, ex);
        }
    }
}
