package me.zypj.revamp.leaderboard.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.repository.ArchiveRepository;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoryService {

    private final LeaderboardPlugin plugin;
    private final ArchiveRepository archiveRepository;
    private final BoardService boardService;
    private final FileConfiguration cfg;
    private final Gson gson;

    public HistoryService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.archiveRepository = plugin.getBootstrap().getArchiveRepository();
        this.boardService = plugin.getBootstrap().getBoardService();
        this.cfg = plugin.getConfig();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        archiveRepository.initHistoryTable();
        scheduleDaily();
        scheduleWeekly();
    }

    private void scheduleDaily() {
        String timeStr = cfg.getString("history.schedule.daily.time", "00:00");
        LocalTime t;
        try {
            t = LocalTime.parse(timeStr);
        } catch (DateTimeParseException ex) {
            t = LocalTime.MIDNIGHT;
        }

        long delayTicks = computeDelay(t) * 20L;
        long periodTicks = 20L * 60 * 60 * 24;

        new BukkitRunnable() {
            @Override
            public void run() {
                snapshotAll(PeriodType.DAILY);
            }
        }.runTaskTimer(plugin, delayTicks, periodTicks);
    }

    private void scheduleWeekly() {
        String dayStr = cfg.getString("history.schedule.weekly.day", "MONDAY");
        String timeStr = cfg.getString("history.schedule.weekly.time", "00:00");
        DayOfWeek dow;
        LocalTime t;
        try {
            dow = DayOfWeek.valueOf(dayStr.toUpperCase());
        } catch (Exception ex) {
            dow = DayOfWeek.MONDAY;
        }
        try {
            t = LocalTime.parse(timeStr);
        } catch (DateTimeParseException ex) {
            t = LocalTime.MIDNIGHT;
        }

        long delayTicks = computeDelay(dow, t) * 20L;
        long periodTicks = 20L * 60 * 60 * 24 * 7;

        new BukkitRunnable() {
            @Override
            public void run() {
                snapshotAll(PeriodType.WEEKLY);
            }
        }.runTaskTimer(plugin, delayTicks, periodTicks);
    }

    private void snapshotAll(PeriodType period) {
        LocalDateTime now = LocalDateTime.now();

        for (String key : plugin.getBootstrap().getBoardsConfigAdapter().getBoards()) {
            List<BoardEntry> entries = boardService.getLeaderboard(key, period, 0);
            archiveRepository.saveSnapshot(key, period, now, entries);
        }

        Map<String, List<BoardEntry>> allSnapshots = plugin.getBootstrap()
                .getBoardsConfigAdapter()
                .getBoards()
                .stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> boardService.getLeaderboard(key, period, 0)
                ));

        saveToJsonFile(period, now.toLocalDate(), allSnapshots);
    }

    private void saveToJsonFile(PeriodType period, LocalDate date, Map<String, List<BoardEntry>> snapshots) {
        File folder = new File(plugin.getDataFolder(), "historic/" + period.name().toLowerCase());
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Could not create folder: " + folder.getPath());
            return;
        }

        String fileName = date.toString() + ".json";
        File out = new File(folder, fileName);

        try (FileWriter writer = new FileWriter(out)) {
            gson.toJson(snapshots, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Error writing history JSON: " + e.getMessage());
        }
    }

    private long computeDelay(LocalTime t) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.with(t);
        if (!next.isAfter(now)) next = next.plusDays(1);
        return Duration.between(now, next).getSeconds();
    }

    private long computeDelay(DayOfWeek dow, LocalTime t) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.with(TemporalAdjusters.nextOrSame(dow));
        LocalDateTime next = LocalDateTime.of(targetDate, t);
        if (!next.isAfter(LocalDateTime.now())) next = next.plusWeeks(1);
        return Duration.between(LocalDateTime.now(), next).getSeconds();
    }
}
