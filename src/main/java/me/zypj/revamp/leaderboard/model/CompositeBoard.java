package me.zypj.revamp.leaderboard.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CompositeBoard {
    private final String key;
    private final List<String> placeholders;
}
