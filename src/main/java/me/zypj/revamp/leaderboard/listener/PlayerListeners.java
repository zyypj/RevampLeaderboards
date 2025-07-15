package me.zypj.revamp.leaderboard.listener;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class PlayerListeners implements Listener {
    private final LeaderboardPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getBootstrap().getBoardService().saveOnJoin(e.getPlayer()), 10L);
    }
}