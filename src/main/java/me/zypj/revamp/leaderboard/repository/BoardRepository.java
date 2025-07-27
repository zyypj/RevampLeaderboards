package me.zypj.revamp.leaderboard.repository;

import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.repository.impl.JdbcBoardRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface BoardRepository {
    void initTables(List<String> rawBoards);
    void save(String table, String uuid, String name, double value);
    void batchSave(String table, List<JdbcBoardRepository.BoardBatchEntry> batch);
    void truncate(String table);
    List<BoardEntry> loadTop(String table, int limit);
    int count(String table);
    ExecutorService getExecutor();
}
