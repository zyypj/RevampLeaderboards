package me.zypj.revamp.leaderboard.repository;

import me.zypj.revamp.leaderboard.model.BoardEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface BoardRepository {
    void initTables(List<String> tableNames);

    void save(String table, String entryKey, String entryDisplay, double value);

    void batchSave(String table, List<BoardBatchEntry> batch);

    void truncate(String table);

    List<BoardEntry> loadTop(String table, int limit);

    int count(String table);

    ExecutorService getExecutor();

    class BoardBatchEntry {
        public final String key;
        public final String display;
        public final double value;

        public BoardBatchEntry(String key, String display, double value) {
            this.key = key;
            this.display = display;
            this.value = value;
        }
    }
}
