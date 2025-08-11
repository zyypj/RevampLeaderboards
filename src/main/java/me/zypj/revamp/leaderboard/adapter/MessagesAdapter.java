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
        String fullPath = "messages." + path;
        String msg = yaml.getString(fullPath, true);

        if (msg == null) {
            String warn = "Message not found for path: " + fullPath;
            plugin.getLogger().warning(warn);
            return warn;
        }

        return msg;
    }
}
