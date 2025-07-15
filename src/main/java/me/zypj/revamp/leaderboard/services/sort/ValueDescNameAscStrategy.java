package me.zypj.revamp.leaderboard.services.sort;

import me.zypj.revamp.leaderboard.model.BoardEntry;
import java.util.Comparator;

public class ValueDescNameAscStrategy implements BoardSortingStrategy {
    @Override
    public Comparator<BoardEntry> comparator() {
        return Comparator
                .comparingDouble(BoardEntry::getValue).reversed()
                .thenComparing(BoardEntry::getPlayerName)
                .thenComparing(BoardEntry::getUuid);
    }
}
