package me.zypj.revamp.leaderboard.api.impl;

import me.zypj.revamp.leaderboard.api.LeaderboardApi;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.loader.PluginBootstrap;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardApiImpl implements LeaderboardApi {

    private final PluginBootstrap bootstrap;

    public LeaderboardApiImpl(PluginBootstrap bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @Override
    public List<String> getBoardKeys() {
        return Collections.unmodifiableList(
                new ArrayList<>(bootstrap.getBoardsConfigAdapter().getBoardKeys())
        );
    }

    @Override
    public List<BoardEntry> getLeaderboard(String boardKey, PeriodType period, int limit) {
        return bootstrap.getBoardService().getLeaderboard(boardKey, period, limit);
    }

    @Override
    public int getPosition(String boardKey, PeriodType period, @NotNull UUID playerUuid) {
        String key = playerUuid.toString();
        List<BoardEntry> list = bootstrap.getBoardService().getLeaderboard(boardKey, period, 0);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getKey().equals(key)) {
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public void addBoard(String boardKey) {
        bootstrap.getBoardsConfigAdapter().addStringBoard(boardKey, "", "");
    }

    @Override
    public void removeBoard(String boardKey) {
        bootstrap.getBoardsConfigAdapter().removeBoard(boardKey);
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

    @Override
    public String getCustomPlaceholder(UUID playerUuid, String type) {
        return bootstrap.getCustomPlaceholderService().getValue(playerUuid, type);
    }

    @Override
    public void refreshCustomPlaceholders() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            bootstrap.getCustomPlaceholderService().updatePlayer(p);
        }
    }

    @Override
    public List<String> getShards(String boardKey, PeriodType period) {
        return Collections.unmodifiableList(
                bootstrap.getShardManager().getShards(boardKey, period)
        );
    }

    @Override
    public String getWriteShard(String boardKey, PeriodType period) {
        return bootstrap.getShardManager().getShardForWrite(boardKey, period);
    }

    @Override
    public void snapshotHistory(PeriodType period) {
        bootstrap.getHistoryService().takeSnapshot(period);
    }
}
