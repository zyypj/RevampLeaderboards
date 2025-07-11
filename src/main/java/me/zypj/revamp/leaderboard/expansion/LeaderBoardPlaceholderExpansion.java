package me.zypj.revamp.leaderboard.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.services.BoardService;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

        if (params.toLowerCase().startsWith("position_")) {
            String[] parts = params.split("_", 3);
            if (parts.length < 3) return "";

            String periodKey = parts[1];
            String boardKey = parts[2];

            PeriodType period;
            switch (periodKey.toLowerCase()) {
                case "daily":
                    period = PeriodType.DAILY;
                    break;
                case "weekly":
                    period = PeriodType.WEEKLY;
                    break;
                case "monthly":
                    period = PeriodType.MONTHLY;
                    break;
                default:
                    period = PeriodType.TOTAL;
                    break;
            }

            List<BoardEntry> list = boardService.getLeaderboard(boardKey, period);
            String uuid = player.getUniqueId().toString();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUuid().equals(uuid)) {
                    return String.valueOf(i + 1);
                }
            }
            return plugin.getBootstrap().getConfigAdapter().getNobodyMessage();
        }

        String[] parts = params.split("_", 4);
        if (parts.length < 4) return "";

        String dataType = parts[0];
        String periodKey = parts[1];
        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "";
        }
        String boardKey = parts[3];

        PeriodType period;
        switch (periodKey.toLowerCase()) {
            case "daily":
                period = PeriodType.DAILY;
                break;
            case "weekly":
                period = PeriodType.WEEKLY;
                break;
            case "monthly":
                period = PeriodType.MONTHLY;
                break;
            default:
                period = PeriodType.TOTAL;
                break;
        }

        return boardService.getValue(dataType, period, position, boardKey);
    }
}
