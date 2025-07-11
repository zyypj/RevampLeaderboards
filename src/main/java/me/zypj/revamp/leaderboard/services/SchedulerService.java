package me.zypj.revamp.leaderboard.services;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class SchedulerService {
    private final JavaPlugin plugin;
    private final BoardService boardService;
    private final FileConfiguration cfg;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public SchedulerService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.boardService = plugin.getBootstrap().getBoardService();
        this.cfg = plugin.getConfig();
    }

    public void scheduleResets() {
        scheduleDaily();
        scheduleWeekly();
        scheduleMonthly();
    }

    public void scheduleUpdates() {
        long initialDelay = cfg.getLong("scheduler.update.initial-delay-ticks", 20L);
        long period = cfg.getLong("scheduler.update.period-ticks", 1200L);

        plugin.getServer().getScheduler()
                .runTaskTimer(plugin, boardService::updateAll, initialDelay, period);
    }

    private void scheduleDaily() {
        String timeStr = cfg.getString("scheduler.reset.daily.time", "00:00");

        LocalTime time = LocalTime.parse(timeStr, timeFormatter);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.with(time);

        if (!next.isAfter(now)) next = next.plusDays(1);
        long delay = computeDelay(next);

        plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> {
                    boardService.reset(PeriodType.DAILY);
                    scheduleDaily();
                }, delay);
    }

    private void scheduleWeekly() {
        String dayStr = cfg.getString("scheduler.reset.weekly.day", "SUNDAY");
        DayOfWeek day = DayOfWeek.valueOf(dayStr.toUpperCase());
        String timeStr = cfg.getString("scheduler.reset.weekly.time", "00:00");

        LocalTime time = LocalTime.parse(timeStr, timeFormatter);
        LocalDateTime now = LocalDateTime.now();
        LocalDate nextDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day));
        LocalDateTime next = LocalDateTime.of(nextDate, time);

        if (!next.isAfter(now)) next = next.plusWeeks(1);
        long delay = computeDelay(next);


        plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> {
                    boardService.reset(PeriodType.WEEKLY);
                    scheduleWeekly();
                }, delay);
    }

    private void scheduleMonthly() {
        int dayOfMonth = cfg.getInt("scheduler.reset.monthly.day", 1);
        String timeStr = cfg.getString("scheduler.reset.monthly.time", "00:00");

        LocalTime time = LocalTime.parse(timeStr, timeFormatter);
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();

        int lastDay = date.lengthOfMonth();
        dayOfMonth = Math.min(dayOfMonth, lastDay);
        LocalDateTime next = LocalDateTime.of(date.withDayOfMonth(dayOfMonth), time);

        if (!next.isAfter(now)) {
            date = date.plusMonths(1);
            lastDay = date.lengthOfMonth();
            dayOfMonth = Math.min(cfg.getInt("scheduler.reset.monthly.day", 1), lastDay);
            next = LocalDateTime.of(date.withDayOfMonth(dayOfMonth), time);
        }

        long delay = computeDelay(next);

        plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> {
                    boardService.reset(PeriodType.MONTHLY);
                    scheduleMonthly();
                }, delay);
    }

    private long computeDelay(LocalDateTime target) {
        long seconds = LocalDateTime.now().until(target, ChronoUnit.SECONDS);
        return seconds * 20L;
    }
}
