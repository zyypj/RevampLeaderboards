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
        long initSec = cfg.getLong("scheduler.update.initial-delay-seconds", 1L);
        long periodSec = cfg.getLong("scheduler.update.period-seconds", 60L);
        long initTicks = initSec * 20L;
        long periodTicks = periodSec * 20L;

        plugin.getServer().getScheduler()
                .runTaskTimer(plugin, boardService::updateAll, initTicks, periodTicks);
    }

    private void scheduleDaily() {
        String timeStr = cfg.getString("scheduler.reset.daily.time", "00:00");
        try {
            LocalTime t = LocalTime.parse(timeStr);
            long periodTicks = 20L * 60 * 60 * 24;
            long delayTicks = computeDelay(t) * 20L;

            plugin.getServer().getScheduler()
                    .runTaskTimer(plugin,
                            () -> boardService.reset(PeriodType.DAILY),
                            delayTicks,
                            periodTicks);
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
            long periodTicks = 20L * 60 * 60 * 24 * 7;
            long delayTicks = computeDelay(dow, t) * 20L;

            plugin.getServer().getScheduler()
                    .runTaskTimer(plugin,
                            () -> boardService.reset(PeriodType.WEEKLY),
                            delayTicks,
                            periodTicks);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid weekly config: " + dayStr + "@" + timeStr);
        }
    }

    private void scheduleMonthly() {
        String dayStr = cfg.getString("scheduler.reset.monthly.day", "1");
        String timeStr = cfg.getString("scheduler.reset.monthly.time", "00:00");
        try {
            int dom = Integer.parseInt(dayStr);
            LocalTime t = LocalTime.parse(timeStr);
            long delaySec = computeDelay(dom, t);

            plugin.getServer().getScheduler()
                    .runTaskLater(plugin, () -> {
                        try {
                            boardService.reset(PeriodType.MONTHLY);
                        } finally {
                            scheduleMonthly();
                        }
                    }, delaySec * 20L);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid monthly config: " + dayStr + "@" + timeStr);
        }
    }

    private long computeDelay(LocalTime t) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.with(t);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).getSeconds();
    }

    private long computeDelay(DayOfWeek dow, LocalTime t) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate = today.with(TemporalAdjusters.nextOrSame(dow));
        LocalDateTime next = LocalDateTime.of(nextDate, t);
        if (!next.isAfter(LocalDateTime.now())) {
            next = next.plusWeeks(1);
        }
        return Duration.between(LocalDateTime.now(), next).getSeconds();
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
        return Duration.between(LocalDateTime.now(), next).getSeconds();
    }
}
