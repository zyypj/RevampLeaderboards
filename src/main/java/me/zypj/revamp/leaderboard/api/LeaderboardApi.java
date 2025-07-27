package me.zypj.revamp.leaderboard.api;

import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;

import java.util.List;
import java.util.UUID;

public interface LeaderboardApi {

    /**
     * Retrieves the keys of all registered leaderboards.
     *
     * @return an unmodifiable list of all board keys
     */
    List<String> getBoardKeys();

    /**
     * Retrieves the top entries of a specific leaderboard for a given period.
     *
     * @param boardKey the unique identifier of the leaderboard
     * @param period   the period type (e.g., DAILY, WEEKLY, MONTHLY) to query
     * @param limit    the maximum number of entries to return; 0 or negative for all entries
     * @return a list of {@link BoardEntry} sorted by rank (highest first)
     */
    List<BoardEntry> getLeaderboard(String boardKey, PeriodType period, int limit);

    /**
     * Retrieves all entries of a specific leaderboard for a given period.
     * This is a convenience method equivalent to {@link #getLeaderboard(String, PeriodType, int)}
     * with no limit.
     *
     * @param boardKey the unique identifier of the leaderboard
     * @param period   the period type to query
     * @return a list of all {@link BoardEntry} sorted by rank
     */
    default List<BoardEntry> getLeaderboard(String boardKey, PeriodType period) {
        return getLeaderboard(boardKey, period, 0);
    }

    /**
     * Gets the 1-based rank position of a player on a specific leaderboard.
     *
     * @param boardKey   the unique identifier of the leaderboard
     * @param period     the period type to query
     * @param playerUuid the UUID of the player whose position is requested
     * @return the player's rank (1-based), or -1 if the player is not on the board
     */
    int getPosition(String boardKey, PeriodType period, UUID playerUuid);

    /**
     * Registers a new empty leaderboard under the given key.
     *
     * @param boardKey the unique identifier for the new leaderboard
     */
    void addBoard(String boardKey);

    /**
     * Removes an existing leaderboard and all its data.
     *
     * @param boardKey the unique identifier of the leaderboard to remove
     */
    void removeBoard(String boardKey);

    /**
     * Resets all leaderboards for the specified period type, clearing their entries.
     *
     * @param period the period type to reset (e.g., DAILY resets only daily boards)
     */
    void reset(PeriodType period);

    /**
     * Clears all entries from the specified leaderboard without removing its registration.
     *
     * @param boardKey the unique identifier of the leaderboard to clear
     */
    void clearBoard(String boardKey);

    /**
     * Clears all entries from every registered leaderboard across all periods.
     */
    void clearAllBoards();
}
