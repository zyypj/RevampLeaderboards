package me.zypj.revamp.leaderboard.board;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter.BoardConfig;
import me.zypj.revamp.leaderboard.board.impl.PlayerBoard;
import me.zypj.revamp.leaderboard.board.impl.StringBoard;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BoardFactory {

    private final LeaderboardPlugin plugin;

    public List<AbstractBoard<?>> createAll() {
        List<AbstractBoard<?>> boards = new ArrayList<>();
        for (String key : plugin.getBootstrap().getBoardsConfigAdapter().getBoardKeys()) {
            BoardConfig bc = plugin.getBootstrap().getBoardsConfigAdapter().getBoardConfig(key);
            switch (bc.getType()) {
                case PLAYER:
                    boards.add(new PlayerBoard(key, bc.getPlaceholder()));
                    break;
                case STRING:
                    boards.add(new StringBoard(key,
                            bc.getKeyPlaceholder(),
                            bc.getValuePlaceholder()));
                    break;
            }
        }
        return boards;
    }
}
