package me.zypj.revamp.leaderboard.api;

import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;

import java.util.List;
import java.util.UUID;

public interface LeaderboardApi {

    List<String> getBoardKeys();

    List<BoardEntry> getLeaderboard(String boardKey, PeriodType period, int limit);

    default List<BoardEntry> getLeaderboard(String boardKey, PeriodType period) {
        return getLeaderboard(boardKey, period, 0);
    }

    int getPosition(String boardKey, PeriodType period, UUID playerUuid);

    void addBoard(String boardKey);

    void removeBoard(String boardKey);

    void reset(PeriodType period);

    void clearBoard(String boardKey);

    void clearAllBoards();
}
