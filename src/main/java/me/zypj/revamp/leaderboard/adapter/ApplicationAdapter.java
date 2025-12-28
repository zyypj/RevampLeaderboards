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
    private String address;
    private int port;
    private String basePath;
    private boolean statusEnabled;
    private boolean statusIncludeCache;
    private boolean statusIncludeShards;
    private boolean statusIncludeHistory;
    private boolean statusIncludeWarnings;
    private String statusAuthToken;

    public ApplicationAdapter(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            this.yaml = new YAML("application.yml", plugin);
            yaml.saveDefaultConfig();
            loadSettings();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.yml", e);
        }
    }

    public void reload() {
        if (yaml != null) {
            yaml.reload();
            loadSettings();
        }
    }

    private void loadSettings() {
        this.enabled = yaml.getBoolean("server.enable", false);
        this.name = yaml.getString("server.name", "Leaderboard-Server");
        this.address = yaml.getString("server.address", "");
        this.port = yaml.getInt("server.port", 8080);
        this.basePath = yaml.getString("app.base-path", "/api");
        this.statusEnabled = yaml.getBoolean("app.status.enable", true);
        this.statusIncludeCache = yaml.getBoolean("app.status.include-cache", true);
        this.statusIncludeShards = yaml.getBoolean("app.status.include-shards", true);
        this.statusIncludeHistory = yaml.getBoolean("app.status.include-history", true);
        this.statusIncludeWarnings = yaml.getBoolean("app.status.include-warnings", true);
        this.statusAuthToken = yaml.getString("app.status.auth-token", "");
    }
}
