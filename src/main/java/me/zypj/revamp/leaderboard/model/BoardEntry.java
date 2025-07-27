package me.zypj.revamp.leaderboard.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BoardEntry {
    private final String key;
    private final String display;
    private final double value;
}
