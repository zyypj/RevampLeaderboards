package me.zypj.revamp.leaderboard.api.events;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when all leaderboards for a specific period are reset.
 */
@Getter
public class BoardResetEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    /** The period type whose leaderboards have been reset. */
    private final PeriodType period;

    /**
     * Constructs a new BoardResetEvent.
     *
     * @param period the period type that has been reset
     */
    public BoardResetEvent(PeriodType period) {
        this.period = period;
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
