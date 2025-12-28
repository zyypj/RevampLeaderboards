package me.zypj.revamp.leaderboard.api.web.controller.impl.status;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.ApplicationAdapter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.services.board.BoardService;
import me.zypj.revamp.leaderboard.services.board.history.HistoryService;
import me.zypj.revamp.leaderboard.services.database.ShardManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${app.base-path:/api}/status")
public class StatusController {
    private final LeaderboardPlugin plugin;

    public StatusController(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @GetMapping
    public Map<String, Object> getStatus(
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        ApplicationAdapter app = plugin.getBootstrap().getApplicationAdapter();
        if (!app.isStatusEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Status endpoint disabled");
        }
        String expectedToken = app.getStatusAuthToken();
        if (expectedToken != null && !expectedToken.isEmpty()) {
            if (token == null || !expectedToken.equals(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
            }
        }

        BoardService boardService = plugin.getBootstrap().getBoardService();
        HistoryService historyService = plugin.getBootstrap().getHistoryService();
        ShardManager shardManager = plugin.getBootstrap().getShardManager();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("onlinePlayers", Bukkit.getOnlinePlayers().size());

        Map<String, Object> boards = new LinkedHashMap<>();
        boards.put("simple", boardService.getBoards().size());
        boards.put("composite", boardService.getCompositeKeys().size());
        payload.put("boards", boards);

        if (app.isStatusIncludeCache()) {
            Map<String, Object> cache = new LinkedHashMap<>();
            cache.put("entries", boardService.getCacheSize());
            cache.put("maxEntriesPerBoard", boardService.getMaxEntriesPerBoard());
            cache.put("stats", cacheStats(boardService));
            payload.put("cache", cache);
        }

        if (app.isStatusIncludeShards()) {
            Map<String, Object> shardInfo = new LinkedHashMap<>();
            for (String key : boardService.getBoards()) {
                Map<String, Integer> perPeriod = new LinkedHashMap<>();
                for (PeriodType pt : PeriodType.values()) {
                    perPeriod.put(pt.name().toLowerCase(Locale.ROOT),
                            shardManager.getShards(key, pt).size());
                }
                shardInfo.put(key, perPeriod);
            }
            payload.put("shards", shardInfo);
        }

        if (app.isStatusIncludeHistory()) {
            Map<String, Object> history = new LinkedHashMap<>();
            history.put("daily", formatSnapshot(historyService.getLastSnapshot(PeriodType.DAILY)));
            history.put("weekly", formatSnapshot(historyService.getLastSnapshot(PeriodType.WEEKLY)));
            history.put("monthly", formatSnapshot(historyService.getLastSnapshot(PeriodType.MONTHLY)));
            payload.put("history", history);
        }

        if (app.isStatusIncludeWarnings()) {
            payload.put("warnings", buildWarnings());
        }

        return payload;
    }

    private Map<String, Object> cacheStats(BoardService boardService) {
        Map<String, Object> stats = new LinkedHashMap<>();
        com.github.benmanes.caffeine.cache.stats.CacheStats s = boardService.getCacheStats();
        stats.put("hitRate", s.hitRate());
        stats.put("missRate", s.missRate());
        stats.put("evictionCount", s.evictionCount());
        stats.put("loadSuccessCount", s.loadSuccessCount());
        stats.put("loadFailureCount", s.loadFailureCount());
        return stats;
    }

    private String formatSnapshot(LocalDateTime time) {
        return time != null ? time.toString() : "";
    }

    private List<String> buildWarnings() {
        FileConfiguration cfg = plugin.getConfig();
        List<String> warnings = new java.util.ArrayList<>();

        if (cfg.getBoolean("scheduler.update.enabled", true)) {
            long period = cfg.getLong("scheduler.update.period-seconds", 60L);
            if (period < 5) {
                warnings.add("scheduler.update.period-seconds is very low (" + period + "s).");
            }
        }

        if (cfg.getBoolean("scheduler.reset.daily.enabled", true)) {
            String dailyTime = cfg.getString("scheduler.reset.daily.time", "00:00");
            if (!isValidTime(dailyTime)) {
                warnings.add("scheduler.reset.daily.time is invalid: " + dailyTime);
            }
        }
        if (cfg.getBoolean("scheduler.reset.weekly.enabled", true)) {
            String weeklyTime = cfg.getString("scheduler.reset.weekly.time", "00:00");
            if (!isValidTime(weeklyTime)) {
                warnings.add("scheduler.reset.weekly.time is invalid: " + weeklyTime);
            }
        }
        if (cfg.getBoolean("scheduler.reset.monthly.enabled", true)) {
            String monthlyTime = cfg.getString("scheduler.reset.monthly.time", "00:00");
            if (!isValidTime(monthlyTime)) {
                warnings.add("scheduler.reset.monthly.time is invalid: " + monthlyTime);
            }
        }

        int refresh = plugin.getBootstrap().getConfigAdapter().getCustomPlaceholderRefreshSeconds();
        if (!plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders().isEmpty() && refresh <= 0) {
            warnings.add("custom-placeholders-settings.refresh-seconds is 0; values will not refresh.");
        }

        if (plugin.getBootstrap().getApplicationAdapter().isStatusEnabled()
                && (plugin.getBootstrap().getApplicationAdapter().getStatusAuthToken() == null
                || plugin.getBootstrap().getApplicationAdapter().getStatusAuthToken().isEmpty())) {
            warnings.add("status endpoint is enabled without an auth token.");
        }

        if (plugin.getBootstrap().getApplicationAdapter().isStatusIncludeCache()
                && !plugin.getBootstrap().getConfigAdapter().isCacheStatsEnabled()) {
            warnings.add("cache.record-stats is false; cache stats may be zeroed.");
        }

        return warnings;
    }

    private boolean isValidTime(String timeStr) {
        try {
            LocalTime.parse(timeStr);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
