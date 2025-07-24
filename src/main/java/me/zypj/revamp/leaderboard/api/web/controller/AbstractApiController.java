package me.zypj.revamp.leaderboard.api.web.controller;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;

@RequiredArgsConstructor
public abstract class AbstractApiController {
    protected final LeaderboardPlugin plugin;
}
