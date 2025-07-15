package me.zypj.revamp.leaderboard.services.sort;

import me.zypj.revamp.leaderboard.model.BoardEntry;
import java.util.Comparator;

public interface BoardSortingStrategy {
    Comparator<BoardEntry> comparator();
}
