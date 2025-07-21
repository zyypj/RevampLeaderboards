package me.zypj.revamp.leaderboard.loader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.MessagesAdapter;
import me.zypj.revamp.leaderboard.repository.BoardRepository;
import me.zypj.revamp.leaderboard.repository.impl.JdbcArchiveRepository;
import me.zypj.revamp.leaderboard.repository.impl.JdbcBoardRepository;
import me.zypj.revamp.leaderboard.repository.impl.SQLiteBoardRepository;
import me.zypj.revamp.leaderboard.services.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@RequiredArgsConstructor
public class PluginBootstrap {

    private final LeaderboardPlugin plugin;

    private ConfigAdapter configAdapter;
    private MessagesAdapter messagesAdapter;
    private BoardsConfigAdapter boardsConfigAdapter;

    private DatabaseService databaseService;
    private BoardRepository boardRepository;

    private BoardService boardService;
    private CustomPlaceholderService customPlaceholderService;
    private SchedulerService schedulerService;

    public void init() {
        setupFiles();
        setupDatabase();
        setupServices();
    }

    private void setupFiles() {
        plugin.saveDefaultConfig();

        configAdapter = new ConfigAdapter(plugin);
        messagesAdapter = new MessagesAdapter(plugin);
        boardsConfigAdapter = new BoardsConfigAdapter(plugin);

        messagesAdapter.init();
    }

    private void setupDatabase() {
        databaseService = new DatabaseService(plugin);

        ExecutorService dbExec = Executors.newFixedThreadPool(configAdapter.getDatabaseThreadPoolSize());

        boardRepository = "sqlite".equalsIgnoreCase(configAdapter.getDatabaseType())
                ? new SQLiteBoardRepository(databaseService.getDataSource(), dbExec)
                : new JdbcBoardRepository(databaseService.getDataSource(), dbExec);
    }

    private void setupServices() {
        boardService = new BoardService(plugin);
        customPlaceholderService = new CustomPlaceholderService(plugin);
        schedulerService = new SchedulerService(plugin);

        boardService.init();
        boardService.updateAll();

        schedulerService.scheduleAll();
    }
}
