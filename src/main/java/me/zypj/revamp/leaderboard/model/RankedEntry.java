package me.zypj.revamp.leaderboard.model;

import lombok.Getter;

@Getter
public class RankedEntry {
    private final BoardEntry entry;
    private final int rank;

    public RankedEntry(BoardEntry entry, int rank) {
        this.entry = entry;
        this.rank = rank;
    }

    public String getUuid() {
        return entry.getUuid();
    }

    public String getName() {
        return entry.getPlayerName();
    }

    public double getValue() {
        return entry.getValue();
    }
}
