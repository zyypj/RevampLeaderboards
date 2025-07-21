package me.zypj.revamp.leaderboard.model;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class HistoricalEntry {
    private final String boardKey;
    private final PeriodType period;
    private final LocalDateTime snapshotTime;
    private final UUID playerUuid;
    private final String playerName;
    private final double value;

    public HistoricalEntry(String boardKey, PeriodType period,
                           LocalDateTime snapshotTime,
                           UUID playerUuid, String playerName, double value) {
        this.boardKey = boardKey;
        this.period = period;
        this.snapshotTime = snapshotTime;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.value = value;
    }
}