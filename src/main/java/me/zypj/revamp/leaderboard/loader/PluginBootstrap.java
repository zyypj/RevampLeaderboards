package me.zypj.revamp.leaderboard.loader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.ApplicationAdapter;
import me.zypj.revamp.leaderboard.adapter.BoardsConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import me.zypj.revamp.leaderboard.adapter.MessagesAdapter;
import me.zypj.revamp.leaderboard.repository.ArchiveRepository;
import me.zypj.revamp.leaderboard.repository.BoardRepository;
import me.zypj.revamp.leaderboard.repository.impl.JdbcArchiveRepository;
import me.zypj.revamp.leaderboard.repository.impl.JdbcBoardRepository;
import me.zypj.revamp.leaderboard.repository.impl.SQLiteBoardRepository;
import me.zypj.revamp.leaderboard.services.*;
import me.zypj.revamp.leaderboard.api.web.config.WebConfig;
import org.springframework.boot.SpringApplication;

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
    private ApplicationAdapter applicationAdapter;
    private BoardsConfigAdapter boardsConfigAdapter;

    private DatabaseService databaseService;
    private BoardRepository boardRepository;
    private ArchiveRepository archiveRepository;

    private BoardService boardService;
    private CustomPlaceholderService customPlaceholderService;
    private SchedulerService schedulerService;
    private HistoryService historyService;
    private ShardManager shardManager;

    public void init() {
        setupFiles();
        setupDatabase();
        setupServices();
        setupWeb();
    }

    private void setupFiles() {
        plugin.saveDefaultConfig();

        configAdapter = new ConfigAdapter(plugin);
        messagesAdapter = new MessagesAdapter(plugin);
        applicationAdapter = new ApplicationAdapter(plugin);
        boardsConfigAdapter = new BoardsConfigAdapter(plugin);

        messagesAdapter.init();
        applicationAdapter.init();
    }

    private void setupDatabase() {
        databaseService = new DatabaseService(plugin);

        ExecutorService dbExec = Executors.newFixedThreadPool(configAdapter.getDatabaseThreadPoolSize());

        boardRepository = "sqlite".equalsIgnoreCase(configAdapter.getDatabaseType())
                ? new SQLiteBoardRepository(databaseService.getDataSource(), dbExec)
                : new JdbcBoardRepository(databaseService.getDataSource(), dbExec);
        archiveRepository = new JdbcArchiveRepository(databaseService.getDataSource(), dbExec);
    }

    private void setupServices() {
        boardService = new BoardService(plugin);
        customPlaceholderService = new CustomPlaceholderService(plugin);
        schedulerService = new SchedulerService(plugin);
        historyService = new HistoryService(plugin);
        shardManager = new ShardManager(plugin);

        boardService.init();
        boardService.updateAll();

        schedulerService.scheduleAll();

        shardManager.init();
    }

    private void setupWeb() {
        if (!applicationAdapter.isEnabled()) return;

        Map<String, Object> props = new HashMap<>();
        props.put("server.port", applicationAdapter.getPort());
        props.put("server.servlet.context-path", applicationAdapter.getBasePath());

        SpringApplication app = new SpringApplication(WebConfig.class);
        app.setDefaultProperties(props);
        app.addInitializers(ctx ->
                ctx.getBeanFactory().registerSingleton("pluginBootstrap", this)
        );

        new Thread(app::run, applicationAdapter.getName()).start();
    }
}
