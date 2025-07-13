package me.zypj.revamp.leaderboard.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import me.zypj.revamp.leaderboard.model.CustomPlaceholder;
import me.zypj.revamp.leaderboard.services.BoardService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        if (lower.startsWith("position_")) {
            String[] parts = params.split("_", 3);
            if (parts.length < 3) return "";

            PeriodType period = parsePeriod(parts[1]);
            String boardKey = parts[2];

            List<BoardEntry> full = boardService.getLeaderboard(boardKey, period, 0);
            String uuid = player.getUniqueId().toString();
            for (int i = 0; i < full.size(); i++) {
                if (full.get(i).getUuid().equals(uuid)) return String.valueOf(i + 1);
            }
            return plugin.getBootstrap().getConfigAdapter().getNotLoadMessage();
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
            if ("amount".equalsIgnoreCase(dataType)) return "0";
            if (plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders().containsKey(dataType)) return "";

            return plugin.getBootstrap().getConfigAdapter().getNobodyMessage();
        }

        BoardEntry entry = list.get(position - 1);
        switch (dataType.toLowerCase()) {
            case "playername":
                return entry.getPlayerName();
            case "uuid":
                return entry.getUuid();
            case "amount":
                return Double.toString(entry.getValue());
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
