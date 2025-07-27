package me.zypj.revamp.leaderboard.api.impl;

import me.zypj.revamp.leaderboard.api.LeaderboardApi;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.loader.PluginBootstrap;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LeaderboardApiImpl implements LeaderboardApi {

    private final PluginBootstrap bootstrap;

    public LeaderboardApiImpl(PluginBootstrap bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @Override
    public List<String> getBoardKeys() {
        return Collections.unmodifiableList(
                bootstrap.getBoardsConfigAdapter().getBoards()
        );
    }

    @Override
    public List<BoardEntry> getLeaderboard(String boardKey, PeriodType period, int limit) {
        return bootstrap.getBoardService().getLeaderboard(boardKey, period, limit);
    }

    @Override
    public int getPosition(String boardKey, PeriodType period, @NotNull UUID playerUuid) {
        List<BoardEntry> list = bootstrap.getBoardService().getLeaderboard(boardKey, period, 0);
        String uuid = playerUuid.toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public void addBoard(String boardKey) {
        bootstrap.getBoardService().addBoard(boardKey);
    }

    @Override
    public void removeBoard(String boardKey) {
        bootstrap.getBoardService().removeBoard(boardKey);
    }

    @Override
    public void reset(PeriodType period) {
        bootstrap.getBoardService().reset(period);
    }

    @Override
    public void clearBoard(String boardKey) {
        bootstrap.getBoardService().clearBoard(boardKey);
    }

    @Override
    public void clearAllBoards() {
        bootstrap.getBoardService().clearDatabase();
    }
}
