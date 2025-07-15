package me.zypj.revamp.leaderboard.services;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

public class SchedulerService {

    private final LeaderboardPlugin plugin;
    private final BoardService boardService;
    private final FileConfiguration cfg;

    public SchedulerService(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.boardService = plugin.getBootstrap().getBoardService();
        this.cfg = plugin.getConfig();
    }

    public void scheduleAll() {
        scheduleUpdates();
        scheduleDaily();
        scheduleWeekly();
        scheduleMonthly();
    }

    private void scheduleUpdates() {
        long init = cfg.getLong("scheduler.update.initial-delay-ticks", 20L);
        long period = cfg.getLong("scheduler.update.period-ticks", 1200L);
        plugin.getServer().getScheduler()
                .runTaskTimer(plugin, boardService::updateAll, init, period);
    }

    private void scheduleDaily() {
        String timeStr = cfg.getString("scheduler.reset.daily.time", "00:00");
        try {
            LocalTime t = LocalTime.parse(timeStr);
            long period = 20L * 60 * 60 * 24;
            long delay = computeDelay(t);
            plugin.getServer().getScheduler()
                    .runTaskTimer(plugin,
                            () -> boardService.reset(PeriodType.DAILY),
                            delay, period);
        } catch (DateTimeParseException ex) {
            plugin.getLogger().warning("Invalid daily.time: " + timeStr);
        }
    }

    private void scheduleWeekly() {
        String dayStr = cfg.getString("scheduler.reset.weekly.day", "SUNDAY");
        String timeStr = cfg.getString("scheduler.reset.weekly.time", "00:00");
        try {
            DayOfWeek dow = DayOfWeek.valueOf(dayStr.toUpperCase());
            LocalTime t = LocalTime.parse(timeStr);
            long period = 20L * 60 * 60 * 24 * 7;
            long delay = computeDelay(dow, t);
            plugin.getServer().getScheduler()
                    .runTaskTimer(plugin,
                            () -> boardService.reset(PeriodType.WEEKLY),
                            delay, period);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid weekly config: " + dayStr + "@" + timeStr);
        }
    }

    private void scheduleMonthly() {
        String day = cfg.getString("scheduler.reset.monthly.day", "1");
        String time = cfg.getString("scheduler.reset.monthly.time", "00:00");
        try {
            int dom = Integer.parseInt(day);
            LocalTime t = LocalTime.parse(time);
            long delay = computeDelay(dom, t);
            plugin.getServer().getScheduler()
                    .runTaskLater(plugin, () -> {
                        try {
                            boardService.reset(PeriodType.MONTHLY);
                        } finally {
                            scheduleMonthly();
                        }
                    }, delay);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid monthly config: " + day + "@" + time);
        }
    }

    private long computeDelay(LocalTime t) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.with(t);
        if (!next.isAfter(now)) next = next.plusDays(1);
        long seconds = Duration.between(now, next).getSeconds();
        return seconds * 20L;
    }

    private long computeDelay(DayOfWeek dow, LocalTime t) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate = today.with(TemporalAdjusters.nextOrSame(dow));
        LocalDateTime next = LocalDateTime.of(nextDate, t);
        if (!next.isAfter(LocalDateTime.now())) next = next.plusWeeks(1);
        long seconds = Duration.between(LocalDateTime.now(), next).getSeconds();
        return seconds * 20L;
    }

    private long computeDelay(int dayOfMonth, LocalTime t) {
        LocalDate today = LocalDate.now();
        int lastDay = today.lengthOfMonth();
        int d = Math.min(dayOfMonth, lastDay);
        LocalDateTime next = LocalDateTime.of(today.withDayOfMonth(d), t);
        if (!next.isAfter(LocalDateTime.now())) {
            LocalDate plus = today.plusMonths(1);
            int ld = plus.lengthOfMonth();
            next = LocalDateTime.of(plus.withDayOfMonth(Math.min(dayOfMonth, ld)), t);
        }
        long seconds = Duration.between(LocalDateTime.now(), next).getSeconds();
        return seconds * 20L;
    }
}
