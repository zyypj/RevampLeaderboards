package me.zypj.revamp.leaderboard.listener;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.services.BoardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListeners implements Listener {
    private final BoardService boardService;

    public PlayerListeners(LeaderboardPlugin plugin) {
        this.boardService = plugin.getBootstrap().getBoardService();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        boardService.saveOnJoin(e.getPlayer());
    }
}