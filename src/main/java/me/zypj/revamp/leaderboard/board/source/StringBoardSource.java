package me.zypj.revamp.leaderboard.board.source;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.board.BoardSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class StringBoardSource implements BoardSource<String> {

    private final String keyPlaceholder;
    private final String valuePlaceholder;

    @Override
    public Map<String, Double> fetchCurrentValues() {
        Map<String, Double> aggregated = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String key = PlaceholderAPI.setPlaceholders(player, keyPlaceholder);
            if (key.isEmpty()) continue;

            String raw = PlaceholderAPI.setPlaceholders(player, valuePlaceholder);
            double v = parseDouble(raw);

            aggregated.merge(key, v, Double::sum);
        }
        return aggregated;
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
    }
}
