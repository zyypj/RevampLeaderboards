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
        if (lower.startsWith("amount_")) return handleAmount(params);
        if (lower.startsWith("display_")) return handleDisplay(params);

        return handleCustom(player, params);
    }

    private String handleRemains(String params) {
        String[] parts = params.split("_", 3);
        String periodKey = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "";
        if (periodKey.equals("total")) return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-never");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset = computeNextReset(periodKey);

        return formatDuration(Duration.between(now, nextReset));
    }

    private String handlePosition(OfflinePlayer viewer, String params) {
        String[] parts = params.split("_", 3);
        if (parts.length != 3 || viewer == null) return "";

        PeriodType period = parsePeriod(parts[1]);
        String boardKey = parts[2];

        List<BoardEntry> list = plugin.getBootstrap()
                .getBoardService()
                .getLeaderboard(boardKey, period, 0);

        String uuid = viewer.getUniqueId().toString();
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).getKey().equals(uuid))
                return Integer.toString(i + 1);

        return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-not-load");
    }

    private String handleAmount(String params) {
        String[] parts = params.split("_", 4);
        if (parts.length < 4) return "0";

        PeriodType period = parsePeriod(parts[1]);
        String boardKey = parts[2];
        String targetKey = parts[3];

        List<BoardEntry> list = plugin.getBootstrap()
                .getBoardService()
                .getLeaderboard(boardKey, period, 0);

        for (BoardEntry e : list)
            if (e.getKey().equals(targetKey))
                return formatAmount(e.getValue());

        return "0";
    }

    private String handleDisplay(String p) {
        String[] parts = p.split("_", 4);
        if (parts.length < 4) return "";

        PeriodType period = parsePeriod(parts[1]);
        int pos;
        try {
            pos = Integer.parseInt(parts[2]);
        } catch (Exception e) {
            return "";
        }
        String board = parts[3];

        List<BoardEntry> list = plugin.getBootstrap()
                .getBoardService()
                .getLeaderboard(board, period, 0);

        if (pos < 1 || pos > list.size())
            return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-nobody");

        return list.get(pos - 1).getDisplay();
    }

    private String handleCustom(OfflinePlayer viewer, String params) {
        String[] tokens = params.split("_");
        if (tokens.length < 3) return "";

        String dataType = tokens[0].toLowerCase(Locale.ROOT);
        PeriodType period = parsePeriod(tokens[1]);

        Map<String, CustomPlaceholder> customMap =
                plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders();
        CustomPlaceholder cp = customMap.get(dataType);
        if (cp == null) return "";

        Integer position = null;
        String boardKey;

        if (tokens.length >= 4 && tokens[2].matches("\\d+")) {
            position = Integer.parseInt(tokens[2]);
            boardKey = String.join("_", Arrays.copyOfRange(tokens, 3, tokens.length));
        } else {
            boardKey = String.join("_", Arrays.copyOfRange(tokens, 2, tokens.length));
        }

        if (position == null) {
            String live = PlaceholderAPI.setPlaceholders(viewer, cp.getPlaceholder());
            if (!live.isEmpty() && !live.startsWith("%")) return live;

            return plugin.getBootstrap()
                    .getCustomPlaceholderService()
                    .getValue(viewer.getUniqueId(), dataType);
        }

        List<BoardEntry> list = plugin.getBootstrap()
                .getBoardService()
                .getLeaderboard(boardKey, period, 0);
        if (position < 1 || position > list.size()) return "";

        OfflinePlayer target = Bukkit.getOfflinePlayer(
                UUID.fromString(list.get(position - 1).getKey())
        );
        String live = PlaceholderAPI.setPlaceholders(target, cp.getPlaceholder());
        if (!live.isEmpty() && !live.startsWith("%")) return live;

        return plugin.getBootstrap()
                .getCustomPlaceholderService()
                .getValue(target.getUniqueId(), dataType);
    }

    private PeriodType parsePeriod(String key) {
        try {
            return PeriodType.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return PeriodType.TOTAL;
        }
    }

    private String formatAmount(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
        String s = bd.toPlainString();
        return s.contains(".") ? s.replace('.', ',') : s;
    }

    private LocalDateTime computeNextReset(String periodKey) {
        FileConfiguration cfg = plugin.getConfig();
        LocalDateTime now = LocalDateTime.now();
        switch (periodKey) {
            case "daily":
                LocalTime daily = parseTime(cfg.getString("scheduler.reset.daily.time", "00:00"));
                LocalDateTime nextDaily = now.with(daily);
                if (!nextDaily.isAfter(now)) nextDaily = nextDaily.plusDays(1);

                return nextDaily;
            case "weekly":
                DayOfWeek dow = DayOfWeek.valueOf(
                        cfg.getString("scheduler.reset.weekly.day", "SUNDAY").toUpperCase());
                LocalTime weekly = parseTime(cfg.getString("scheduler.reset.weekly.time", "00:00"));
                LocalDate d = LocalDate.now().with(TemporalAdjusters.nextOrSame(dow));
                LocalDateTime nextWeekly = LocalDateTime.of(d, weekly);

                if (!nextWeekly.isAfter(now)) nextWeekly = nextWeekly.plusWeeks(1);

                return nextWeekly;
            case "monthly":
                int dom = Integer.parseInt(cfg.getString("scheduler.reset.monthly.day", "1"));
                LocalTime monthly = parseTime(cfg.getString("scheduler.reset.monthly.time", "00:00"));
                LocalDate today = LocalDate.now();
                int valid = Math.min(dom, today.lengthOfMonth());
                LocalDateTime nextMonthly = LocalDateTime.of(today.withDayOfMonth(valid), monthly);

                if (!nextMonthly.isAfter(now)) {
                    LocalDate m = today.plusMonths(1);
                    int dmax = m.lengthOfMonth();
                    nextMonthly = LocalDateTime.of(m.withDayOfMonth(Math.min(dom, dmax)), monthly);
                }

                return nextMonthly;
            default:
                return now;
        }
    }

    private LocalTime parseTime(String s) {
        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException ex) {
            return LocalTime.MIDNIGHT;
        }
    }

    private String formatDuration(Duration dur) {
        long secs = Math.max(0, dur.getSeconds());
        long days = secs / 86_400;
        long hrs = (secs % 86_400) / 3_600;
        long mins = (secs % 3_600) / 60;
        long ss = secs % 60;

        if (days > 0) {
            String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-days-missing");
            String u = days == 1
                    ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-day")
                    : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-days");
            return tpl.replace("{dd}", Long.toString(days))
                    .replace("{day-meaning}", u);
        }
        if (hrs > 0) {
            String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-hours-missing");
            String u = hrs == 1
                    ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-hour")
                    : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-hours");
            return tpl.replace("{hh}", String.format("%02d", hrs))
                    .replace("{mm}", String.format("%02d", mins))
                    .replace("{hour-meaning}", u);
        }
        if (mins > 0) {
            String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-minutes-missing");
            String u = mins == 1
                    ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-minute")
                    : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-minutes");
            return tpl.replace("{mm}", String.format("%02d", mins))
                    .replace("{ss}", String.format("%02d", ss))
                    .replace("{minute-meaning}", u);
        }
        String tpl = plugin.getBootstrap().getMessagesAdapter().getMessage("when-seconds-missing");
        String u = ss == 1
                ? plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-second")
                : plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-seconds");
        return tpl.replace("{mm}", String.format("%02d", mins))
                .replace("{ss}", String.format("%02d", ss))
                .replace("{second-meaning}", u);
    }
}
