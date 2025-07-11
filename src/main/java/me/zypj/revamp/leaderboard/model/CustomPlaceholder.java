package me.zypj.revamp.leaderboard.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomPlaceholder {
    private final String dataType;
    private final String placeholder;
    private final boolean canBeNull;
}
