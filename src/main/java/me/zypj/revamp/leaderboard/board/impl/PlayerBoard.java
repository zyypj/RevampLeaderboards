package me.zypj.revamp.leaderboard.board.impl;

import me.zypj.revamp.leaderboard.board.AbstractBoard;
import me.zypj.revamp.leaderboard.board.source.PlayerBoardSource;

import java.util.UUID;

public class PlayerBoard extends AbstractBoard<UUID> {
    public PlayerBoard(String boardKey, String placeholder) {
        super(boardKey, new PlayerBoardSource(placeholder));
    }
}
