package me.zypj.revamp.leaderboard.board.source;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.board.BoardSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class PlayerBoardSource implements BoardSource<UUID> {

    private final String placeholder;

    @Override
    public Map<UUID, Double> fetchCurrentValues() {
        Map<UUID, Double> map = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String raw = PlaceholderAPI.setPlaceholders(player, placeholder);
            double value = parseDouble(raw);
            map.put(player.getUniqueId(), value);
        }
        return map;
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
    }
}
