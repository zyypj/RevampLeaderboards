package me.zypj.revamp.leaderboard.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class GenericBoardEntry<T> {

    private final T key;
    private final double value;
}
