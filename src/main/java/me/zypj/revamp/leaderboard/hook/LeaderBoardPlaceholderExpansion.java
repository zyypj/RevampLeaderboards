package me.zypj.revamp.leaderboard.hook;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class LeaderBoardPlaceholderExpansion extends PlaceholderExpansion {

    private final LeaderboardPlugin plugin;

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
        return plugin.getBootstrap().getPlaceholderService().apply(player, params);
    }
}
