package me.zypj.revamp.leaderboard.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BoardEntry {
    private final String uuid;
    private final String playerName;
    private final double value;
}
