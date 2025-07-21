package me.zypj.revamp.leaderboard.api.events;

import lombok.Getter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;
import java.util.UUID;

@Getter
public class LeaderboardUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String boardKey;
    private final PeriodType period;
    private final Map<UUID, Double> updatedValues;

    public LeaderboardUpdateEvent(String boardKey, PeriodType period, Map<UUID, Double> updatedValues) {
        this.boardKey = boardKey;
        this.period = period;
        this.updatedValues = updatedValues;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
