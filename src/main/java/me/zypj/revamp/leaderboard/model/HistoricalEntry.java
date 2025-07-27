package me.zypj.revamp.leaderboard.model;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;

import java.time.LocalDateTime;

@Getter
public class HistoricalEntry {
    private final String boardKey;
    private final PeriodType period;
    private final LocalDateTime snapshotTime;
    private final String entryKey;
    private final String entryDisplay;
    private final double value;

    public HistoricalEntry(String boardKey,
                           PeriodType period,
                           LocalDateTime snapshotTime,
                           String entryKey,
                           String entryDisplay,
                           double value) {
        this.boardKey = boardKey;
        this.period = period;
        this.snapshotTime = snapshotTime;
        this.entryKey = entryKey;
        this.entryDisplay = entryDisplay;
        this.value = value;
    }
}