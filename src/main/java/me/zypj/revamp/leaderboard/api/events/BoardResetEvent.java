package me.zypj.revamp.leaderboard.api.events;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class BoardResetEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final PeriodType period;

    public BoardResetEvent(PeriodType period) {
        this.period = period;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}