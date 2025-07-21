package me.zypj.revamp.leaderboard.repository.impl;

import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.model.HistoricalEntry;
import me.zypj.revamp.leaderboard.repository.ArchiveRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
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
                + " player_uuid VARCHAR(36) NOT NULL,"
                + " player_name VARCHAR(16) NOT NULL,"
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
    public void saveSnapshot(String boardKey, PeriodType period,
                             LocalDateTime snapshotTime,
                             List<BoardEntry> entries) {
        String sql = "INSERT INTO `board_history`"
                + " (snapshot_time, board_key, period, player_uuid, player_name, value)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        executor.submit(() -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (BoardEntry e : entries) {
                    ps.setTimestamp(1, Timestamp.valueOf(snapshotTime));
                    ps.setString(2, boardKey);
                    ps.setString(3, period.name());
                    ps.setString(4, e.getUuid());
                    ps.setString(5, e.getPlayerName());
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
    public List<HistoricalEntry> getHistory(String boardKey, PeriodType period,
                                            LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT snapshot_time, player_uuid, player_name, value "
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
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getDouble("value")
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    @Override
    public List<HistoricalEntry> getPlayerHistory(UUID playerUuid,
                                                  String boardKey, PeriodType period,
                                                  LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT snapshot_time, player_name, value "
                + "FROM `board_history` "
                + "WHERE board_key=? AND period=? AND player_uuid=? "
                + "AND snapshot_time BETWEEN ? AND ? "
                + "ORDER BY snapshot_time ASC";
        List<HistoricalEntry> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, boardKey);
            ps.setString(2, period.name());
            ps.setString(3, playerUuid.toString());
            ps.setTimestamp(4, Timestamp.valueOf(from));
            ps.setTimestamp(5, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new HistoricalEntry(
                            boardKey,
                            period,
                            rs.getTimestamp("snapshot_time").toLocalDateTime(),
                            playerUuid,
                            rs.getString("player_name"),
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
