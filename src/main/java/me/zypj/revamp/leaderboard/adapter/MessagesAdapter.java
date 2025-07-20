package me.zypj.revamp.leaderboard.adapter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.shared.file.YAML;

import java.io.IOException;

@Getter
@RequiredArgsConstructor
public class MessagesAdapter {

    private YAML yaml;
    private final LeaderboardPlugin plugin;

    public void init() {
        try {
            this.yaml = new YAML("messages.yml", plugin);
            yaml.saveDefaultConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMessage(String path) {
        return yaml.getString("messages." + path, true);
    }
}
