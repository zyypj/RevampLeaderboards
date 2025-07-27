package me.zypj.revamp.leaderboard.board;

import java.util.Map;

public interface BoardSource<T> {

    Map<T, Double> fetchCurrentValues();
}
