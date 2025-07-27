package me.zypj.revamp.leaderboard.api.events;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;
import java.util.UUID;

/**
 * Event fired when the leaderboard data has been updated with new values.
 */
@Getter
public class LeaderboardUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    /** The key of the leaderboard that was updated. */
    private final String boardKey;

    /** The period type of the leaderboard update. */
    private final PeriodType period;

    /** Map of player UUIDs to their updated score values. */
    private final Map<UUID, Double> updatedValues;

    /**
     * Constructs a new LeaderboardUpdateEvent.
     *
     * @param boardKey      the unique identifier of the leaderboard
     * @param period        the period type of the leaderboard
     * @param updatedValues map of player UUIDs to their new score values
     */
    public LeaderboardUpdateEvent(String boardKey, PeriodType period, Map<UUID, Double> updatedValues) {
        this.boardKey      = boardKey;
        this.period        = period;
        this.updatedValues = updatedValues;
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
