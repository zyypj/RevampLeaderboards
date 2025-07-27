package me.zypj.revamp.leaderboard.adapter;

import lombok.Getter;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.shared.file.YAML;

import java.io.IOException;

@Getter
public class ApplicationAdapter {
    private final LeaderboardPlugin plugin;
    private YAML yaml;

    private boolean enabled;
    private String name;
    private int port;
    private String basePath;

    public ApplicationAdapter(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            this.yaml = new YAML("application.yml", plugin);
            yaml.saveDefaultConfig();

            this.enabled = yaml.getBoolean("server.enable", false);
            this.name = yaml.getString("server.name", "Leaderboard-Server");
            this.port = yaml.getInt("server.port", 8080);
            this.basePath = yaml.getString("app.base-path", "/api");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.yml", e);
        }
    }
}
