// src/main/java/me/zypj/revamp/leaderboard/repository/ArchiveRepository.java
package me.zypj.revamp.leaderboard.repository;

import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.model.HistoricalEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ArchiveRepository {
    void initHistoryTable();

    void saveSnapshot(String boardKey, PeriodType period,
                      LocalDateTime snapshotTime,
                      List<BoardEntry> entries);

    List<HistoricalEntry> getHistory(String boardKey, PeriodType period,
                                     LocalDateTime from, LocalDateTime to);

    List<HistoricalEntry> getPlayerHistory(UUID playerUuid,
                                           String boardKey, PeriodType period,
                                           LocalDateTime from, LocalDateTime to);
}
