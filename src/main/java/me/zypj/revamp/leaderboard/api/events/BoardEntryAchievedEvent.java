package me.zypj.revamp.leaderboard.api.events;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

@Getter
public class BoardEntryAchievedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String boardKey;
    private final PeriodType period;
    private final UUID playerId;
    private final double newValue;

    public BoardEntryAchievedEvent(String boardKey, PeriodType period, UUID playerId, double newValue) {
        this.boardKey = boardKey;
        this.period = period;
        this.playerId = playerId;
        this.newValue = newValue;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
