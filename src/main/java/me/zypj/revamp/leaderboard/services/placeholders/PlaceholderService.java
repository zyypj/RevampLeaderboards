package me.zypj.revamp.leaderboard.services.placeholders;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.model.CustomPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RequiredArgsConstructor
public class PlaceholderService {

    private final LeaderboardPlugin plugin;

    public String apply(OfflinePlayer player, String params) {
        if (params == null || params.isEmpty()) return "";
        String lower = params.toLowerCase(Locale.ROOT);

        if (lower.startsWith("remains_")) return handleRemains(params);

        if (lower.startsWith("position_")) return handlePosition(player, params);

        return handleEntryData(player, params);
    }

    private String handleRemains(String params) {
        String[] parts = params.split("_", 3);
        if (parts.length < 2) return "";

        String periodKey = parts[1].toLowerCase(Locale.ROOT);
        if (periodKey.equals("total")) return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-never");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset;
        try {
            nextReset = computeNextReset(periodKey);
        } catch (Exception e) {
            return "";
        }

        Duration duration = Duration.between(now, nextReset);
        return formatDuration(duration);
    }

    private LocalDateTime computeNextReset(String periodKey) {
        FileConfiguration cfg = plugin.getConfig();
        LocalDateTime now = LocalDateTime.now();

        switch (periodKey) {
            case "daily":
                LocalTime dailyTime = parseTime(cfg.getString("scheduler.reset.daily.time", "00:00"));
                LocalDateTime nextDaily = now.with(dailyTime);
                if (!nextDaily.isAfter(now)) nextDaily = nextDaily.plusDays(1);

                return nextDaily;

            case "weekly":
                DayOfWeek dow = DayOfWeek.valueOf(
                        cfg.getString("scheduler.reset.weekly.day", "SUNDAY").toUpperCase(Locale.ROOT)
                );
                LocalTime weeklyTime = parseTime(cfg.getString("scheduler.reset.weekly.time", "00:00"));
                LocalDate nextDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(dow));
                LocalDateTime nextWeekly = LocalDateTime.of(nextDate, weeklyTime);
                if (!nextWeekly.isAfter(now)) nextWeekly = nextWeekly.plusWeeks(1);

                return nextWeekly;

            case "monthly":
                int dayOfMonth = Integer.parseInt(cfg.getString("scheduler.reset.monthly.day", "1"));
                LocalTime monthlyTime = parseTime(cfg.getString("scheduler.reset.monthly.time", "00:00"));
                LocalDate today = LocalDate.now();
                int validDay = Math.min(dayOfMonth, today.lengthOfMonth());
                LocalDateTime nextMonthly = LocalDateTime.of(today.withDayOfMonth(validDay), monthlyTime);
                if (!nextMonthly.isAfter(now)) {
                    LocalDate plusMonth = today.plusMonths(1);
                    int lastDay = plusMonth.lengthOfMonth();
                    int adjDay = Math.min(dayOfMonth, lastDay);
                    nextMonthly = LocalDateTime.of(plusMonth.withDayOfMonth(adjDay), monthlyTime);
                }

                return nextMonthly;

            default:
                throw new IllegalArgumentException("Unknown period: " + periodKey);
        }
    }

    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr);
        } catch (DateTimeParseException ex) {
            return LocalTime.MIDNIGHT;
        }
    }

    private String formatDuration(Duration dur) {
        long seconds = Math.max(0, dur.getSeconds());
        long days = seconds / 86_400;
        long hours = (seconds % 86_400) / 3_600;
        long minutes = (seconds % 3_600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-days-missing");
            String unit = days == 1
                    ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-day")
                    : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-days");

            return tpl.replace("{dd}", Long.toString(days))
                    .replace("{day-meaning}", unit);
        }
        if (hours > 0) {
            String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-hours-missing");
            String unit = hours == 1
                    ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-hour")
                    : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-hours");

            return tpl.replace("{hh}", String.format("%02d", hours))
                    .replace("{mm}", String.format("%02d", minutes))
                    .replace("{hour-meaning}", unit);
        }
        if (minutes > 0) {
            String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-minutes-missing");
            String unit = minutes == 1
                    ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-minute")
                    : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-minutes");

            return tpl.replace("{mm}", String.format("%02d", minutes))
                    .replace("{ss}", String.format("%02d", secs))
                    .replace("{minute-meaning}", unit);
        }

        String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-seconds-missing");
        String unit = secs == 1
                ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-second")
                : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-seconds");

        return tpl.replace("{mm}", String.format("%02d", minutes))
                .replace("{ss}", String.format("%02d", secs))
                .replace("{second-meaning}", unit);
    }

    private String handlePosition(OfflinePlayer player, String params) {
        String[] parts = params.split("_", 3);
        if (parts.length < 3) {
            return "";
        }
        PeriodType period = parsePeriod(parts[1]);
        String boardKey = parts[2];
        List<BoardEntry> entries = plugin.getBootstrap().getBoardService().getLeaderboard(boardKey, period, 0);
        String targetUuid = player.getUniqueId().toString();

        for (int i = 0; i < entries.size(); i++)
            if (entries.get(i).getUuid().equals(targetUuid))
                return Integer.toString(i + 1);

        return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-not-load");
    }

    private PeriodType parsePeriod(String key) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "daily":
                return PeriodType.DAILY;
            case "weekly":
                return PeriodType.WEEKLY;
            case "monthly":
                return PeriodType.MONTHLY;
            default:
                return PeriodType.TOTAL;
        }
    }

    private String handleEntryData(OfflinePlayer player, String params) {
        String[] parts = params.split("_", 4);
        if (parts.length < 4) return "";

        String dataType = parts[0].toLowerCase(Locale.ROOT);
        PeriodType period = parsePeriod(parts[1]);

        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            return "";
        }
        String boardKey = parts[3];

        List<BoardEntry> list = plugin.getBootstrap().getBoardService().getLeaderboard(boardKey, period, position);
        if (position < 1 || list.size() < position) {
            if (dataType.equalsIgnoreCase("amount")) return "0";

            if (plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders().containsKey(dataType)) return "";

            return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-nobody");
        }

        BoardEntry entry = list.get(position - 1);
        switch (dataType) {
            case "playername":
                return entry.getPlayerName();
            case "uuid":
                return entry.getUuid();
            case "amount":
                return formatAmount(entry.getValue());
            default:
                return handleCustomPlaceholder(entry, dataType);
        }
    }

    private String formatAmount(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
        String str = bd.toPlainString();
        return str.contains(".")
                ? str.replace('.', ',')
                : str;
    }

    private String handleCustomPlaceholder(BoardEntry entry, String dataType) {
        Map<String, CustomPlaceholder> customMap = plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders();
        if (!customMap.containsKey(dataType)) return "";

        CustomPlaceholder cp = customMap.get(dataType);
        OfflinePlayer off = Bukkit.getOfflinePlayer(UUID.fromString(entry.getUuid()));

        String expr = cp.getPlaceholder()
                .replace("{player}", off.getName());
        String live = PlaceholderAPI.setPlaceholders(off, expr);
        if (!live.isEmpty() && !live.startsWith("%")) return live;

        String cached = plugin.getBootstrap().getCustomPlaceholderService().getValue(off.getUniqueId(), dataType);
        return cached != null ? cached : "";
    }
}
