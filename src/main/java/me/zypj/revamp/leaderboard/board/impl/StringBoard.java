package me.zypj.revamp.leaderboard.board.impl;

import me.zypj.revamp.leaderboard.board.AbstractBoard;
import me.zypj.revamp.leaderboard.board.source.StringBoardSource;

public class StringBoard extends AbstractBoard<String> {
    public StringBoard(String boardKey, String keyPlaceholder, String valuePlaceholder) {
        super(boardKey, new StringBoardSource(keyPlaceholder, valuePlaceholder));
    }
}
