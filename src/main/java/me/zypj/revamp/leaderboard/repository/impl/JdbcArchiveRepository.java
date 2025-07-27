package me.zypj.revamp.leaderboard.repository.impl;

import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.model.HistoricalEntry;
import me.zypj.revamp.leaderboard.repository.ArchiveRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class JdbcArchiveRepository implements ArchiveRepository {
    private final DataSource ds;
    private final ExecutorService executor;

    public JdbcArchiveRepository(DataSource ds, ExecutorService executor) {
        this.ds = ds;
        this.executor = executor;
    }

    @Override
    public void initHistoryTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS `board_history` ("
                + " snapshot_time DATETIME NOT NULL,"
                + " board_key VARCHAR(64) NOT NULL,"
                + " period VARCHAR(16) NOT NULL,"
                + " entry_key VARCHAR(64) NOT NULL,"
                + " entry_display VARCHAR(64) NOT NULL,"
                + " value DOUBLE NOT NULL"
                + ")";
        executor.submit(() -> {
            try {
                executeUpdate(ddl);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void saveSnapshot(String boardKey,
                             PeriodType period,
                             LocalDateTime snapshotTime,
                             List<BoardEntry> entries) {
        String sql = "INSERT INTO `board_history`"
                + " (snapshot_time, board_key, period, entry_key, entry_display, value)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        executor.submit(() -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (BoardEntry e : entries) {
                    ps.setTimestamp(1, Timestamp.valueOf(snapshotTime));
                    ps.setString(2, boardKey);
                    ps.setString(3, period.name());
                    ps.setString(4, e.getKey());
                    ps.setString(5, e.getDisplay());
                    ps.setDouble(6, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public List<HistoricalEntry> getHistory(String boardKey,
                                            PeriodType period,
                                            LocalDateTime from,
                                            LocalDateTime to) {
        String sql = "SELECT snapshot_time, entry_key, entry_display, value "
                + "FROM `board_history` "
                + "WHERE board_key=? AND period=? "
                + "AND snapshot_time BETWEEN ? AND ? "
                + "ORDER BY snapshot_time ASC";
        List<HistoricalEntry> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, boardKey);
            ps.setString(2, period.name());
            ps.setTimestamp(3, Timestamp.valueOf(from));
            ps.setTimestamp(4, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new HistoricalEntry(
                            boardKey,
                            period,
                            rs.getTimestamp("snapshot_time").toLocalDateTime(),
                            rs.getString("entry_key"),
                            rs.getString("entry_display"),
                            rs.getDouble("value")
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    private void executeUpdate(String sql) throws SQLException {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
    }
}
