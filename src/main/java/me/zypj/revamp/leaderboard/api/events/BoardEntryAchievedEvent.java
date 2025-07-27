package me.zypj.revamp.leaderboard.api.events;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Event fired when a player achieves or updates their score on a specific leaderboard.
 */
@Getter
public class BoardEntryAchievedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    /** The key of the leaderboard where the entry was achieved. */
    private final String boardKey;

    /** The period type of the leaderboard (e.g., DAILY, WEEKLY, MONTHLY). */
    private final PeriodType period;

    /** The unique ID of the player who achieved the entry. */
    private final UUID playerId;

    /** The new score value achieved by the player. */
    private final double newValue;

    /**
     * Constructs a new BoardEntryAchievedEvent.
     *
     * @param boardKey the unique identifier of the leaderboard
     * @param period   the period type of the leaderboard
     * @param playerId the UUID of the player who achieved the entry
     * @param newValue the new score value achieved by the player
     */
    public BoardEntryAchievedEvent(String boardKey, PeriodType period, UUID playerId, double newValue) {
        this.boardKey = boardKey;
        this.period   = period;
        this.playerId = playerId;
        this.newValue = newValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the static handler list for registration.
     *
     * @return handler list instance
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
