package me.zypj.revamp.leaderboard.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.MessagesAdapter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.model.CustomPlaceholder;
import me.zypj.revamp.leaderboard.services.BoardService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderBoardPlaceholderExpansion extends PlaceholderExpansion {

    private final LeaderboardPlugin plugin;
    private final BoardService boardService;

    public LeaderBoardPlaceholderExpansion(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.boardService = plugin.getBootstrap().getBoardService();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lb";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.isEmpty()) return "";

        String lower = params.toLowerCase();

        if (lower.startsWith("remains_")) {
            String[] parts = params.split("_", 3);
            if (parts.length < 3) return "";

            String period = parts[1].toLowerCase();

            MessagesAdapter msg = plugin.getBootstrap().getMessagesAdapter();
            FileConfiguration cfg = plugin.getConfig();
            LocalDateTime now = LocalDateTime.now();

            if (period.equals("total")) return msg.getMessage("meaning-never");

            LocalDateTime next;
            try {
                switch (period) {
                    case "daily": {
                        LocalTime t = LocalTime.parse(cfg.getString("scheduler.reset.daily.time", "00:00"));
                        next = now.with(t);
                        if (!next.isAfter(now)) next = next.plusDays(1);
                        break;
                    }
                    case "weekly": {
                        DayOfWeek dow = DayOfWeek.valueOf(cfg.getString("scheduler.reset.weekly.day", "SUNDAY").toUpperCase());
                        LocalTime t = LocalTime.parse(cfg.getString("scheduler.reset.weekly.time", "00:00"));
                        LocalDate today = LocalDate.now();
                        LocalDate d = today.with(TemporalAdjusters.nextOrSame(dow));
                        next = LocalDateTime.of(d, t);
                        if (!next.isAfter(now)) next = next.plusWeeks(1);
                        break;
                    }
                    case "monthly": {
                        int dom = Integer.parseInt(cfg.getString("scheduler.reset.monthly.day", "1"));
                        LocalTime t = LocalTime.parse(cfg.getString("scheduler.reset.monthly.time", "00:00"));
                        LocalDate today = LocalDate.now();
                        int day = Math.min(dom, today.lengthOfMonth());
                        next = LocalDateTime.of(today.withDayOfMonth(day), t);
                        if (!next.isAfter(now)) {
                            LocalDate plus = today.plusMonths(1);
                            int ld = plus.lengthOfMonth();
                            next = LocalDateTime.of(plus.withDayOfMonth(Math.min(dom, ld)), t);
                        }
                        break;
                    }
                    default:
                        return "";
                }
            } catch (Exception e) {
                return "";
            }

            Duration dur = Duration.between(now, next);
            long totalSec = dur.getSeconds();
            long days = totalSec / 86_400;
            long hours = (totalSec % 86_400) / 3_600;
            long minutes = (totalSec % 3_600) / 60;
            long seconds = totalSec % 60;

            if (days > 0) {
                String tpl = msg.getMessage("when-days-missing");
                String unit = days == 1 ? msg.getMessage("meaning-day") : msg.getMessage("meaning-days");
                return tpl.replace("{dd}", String.valueOf(days))
                        .replace("{day-meaning}", unit);
            } else if (hours > 0) {
                String tpl = msg.getMessage("when-hours-missing");
                String unit = hours == 1 ? msg.getMessage("meaning-hour") : msg.getMessage("meaning-hours");
                return tpl.replace("{hh}", String.format("%02d", hours))
                        .replace("{mm}", String.format("%02d", minutes))
                        .replace("{hour-meaning}", unit);
            } else if (minutes > 0) {
                String tpl = msg.getMessage("when-minutes-missing");
                String unit = minutes == 1 ? msg.getMessage("meaning-minute") : msg.getMessage("meaning-minutes");
                return tpl.replace("{mm}", String.format("%02d", minutes))
                        .replace("{ss}", String.format("%02d", seconds))
                        .replace("{minute-meaning}", unit);
            } else {
                String tpl = msg.getMessage("when-seconds-missing");
                String unit = seconds == 1 ? msg.getMessage("meaning-second") : msg.getMessage("meaning-seconds");
                return tpl.replace("{mm}", String.format("%02d", minutes))
                        .replace("{ss}", String.format("%02d", seconds))
                        .replace("{second-meaning}", unit);
            }
        }

        if (lower.startsWith("position_")) {
            String[] parts = params.split("_", 3);
            if (parts.length < 3) return "";

            PeriodType period = parsePeriod(parts[1]);
            String boardKey = parts[2];

            List<BoardEntry> full = boardService.getLeaderboard(boardKey, period, 0);
            String uuid = player.getUniqueId().toString();
            for (int i = 0; i < full.size(); i++)
                if (full.get(i).getUuid().equals(uuid)) return String.valueOf(i + 1);

            return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-not-load");
        }

        String[] parts = params.split("_", 4);
        if (parts.length < 4) return "";

        String dataType = parts[0];
        PeriodType period = parsePeriod(parts[1]);

        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            return "";
        }

        String boardKey = parts[3];
        List<BoardEntry> list = boardService.getLeaderboard(boardKey, period, position);

        if (position < 1 || list.size() < position) {
            if (dataType.equalsIgnoreCase("amount")) return "0";
            if (plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders().containsKey(dataType)) return "";

            return plugin.getBootstrap().getMessagesAdapter().getMessage("meaning-nobody");
        }

        BoardEntry entry = list.get(position - 1);
        switch (dataType.toLowerCase()) {
            case "playername":
                return entry.getPlayerName();
            case "uuid":
                return entry.getUuid();
            case "amount": {
                double value = entry.getValue();
                BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
                String formatted = bd.toPlainString();
                if (formatted.contains(".")) formatted = formatted.replace('.', ',');

                return formatted;
            }
            default:
                Map<String, CustomPlaceholder> customMap = plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders();
                if (!customMap.containsKey(dataType)) return "";

                CustomPlaceholder cp = customMap.get(dataType);
                OfflinePlayer off = Bukkit.getOfflinePlayer(UUID.fromString(entry.getUuid()));
                String ph = cp.getPlaceholder().replace("{player}", off.getName());
                String live = PlaceholderAPI.setPlaceholders(off, ph);
                if (!live.isEmpty() && !live.startsWith("%")) return live;

                String cached = plugin.getBootstrap().getCustomPlaceholderService().getValue(off.getUniqueId(), dataType);
                return cached == null ? "" : cached;
        }
    }

    private PeriodType parsePeriod(String key) {
        switch (key.toLowerCase()) {
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
}
