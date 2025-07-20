package me.zypj.revamp.leaderboard.loader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.MessagesAdapter;
import me.zypj.revamp.leaderboard.services.BoardService;
import me.zypj.revamp.leaderboard.services.CustomPlaceholderService;
import me.zypj.revamp.leaderboard.services.DatabaseService;
import me.zypj.revamp.leaderboard.services.SchedulerService;

@Getter
@RequiredArgsConstructor
public class PluginBootstrap {

    private final LeaderboardPlugin plugin;

    private ConfigAdapter configAdapter;
    private MessagesAdapter messagesAdapter;
    private BoardsConfigAdapter boardsConfigAdapter;

    private DatabaseService databaseService;

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
    }

    private void setupDatabase() {
        databaseService = new DatabaseService(plugin);
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
