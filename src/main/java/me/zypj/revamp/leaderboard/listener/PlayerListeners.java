package me.zypj.revamp.leaderboard.listener;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PlayerListeners implements Listener {
    private final LeaderboardPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getBootstrap().getBoardService().saveOnJoin(e.getPlayer()), 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.getBootstrap().getBoardService().removePlayer(e.getPlayer().getUniqueId());
        if (plugin.getBootstrap().getCustomPlaceholderService() != null) {
            plugin.getBootstrap().getCustomPlaceholderService().evictPlayer(e.getPlayer().getUniqueId());
        }
    }
}
