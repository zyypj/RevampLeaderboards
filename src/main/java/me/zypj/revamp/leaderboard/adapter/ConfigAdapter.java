package me.zypj.revamp.leaderboard.adapter;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.model.CustomPlaceholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigAdapter {

    private final FileConfiguration config;

    public ConfigAdapter(LeaderboardPlugin plugin) {
        this.config = plugin.getConfig();
    }

    public String getDatabaseHost() {
        return config.getString("database.host");
    }

    public int getDatabasePort() {
        return config.getInt("database.port");
    }

    public String getDatabaseName() {
        return config.getString("database.database");
    }

    public String getDatabaseUser() {
        return config.getString("database.user");
    }

    public int getDatabaseThreadPoolSize() {
        return config.getInt("database.thread-pool-size",
                Runtime.getRuntime().availableProcessors());
    }

    public int getCacheTtlSeconds() {
        return config.getInt("cache.ttl-seconds", 30);
    }

    public int getCacheRefreshSeconds() {
        return config.getInt("cache.refresh-seconds", 30);
    }

    public String getDatabasePassword() {
        return config.getString("database.password");
    }

    public String getNobodyMessage() {
        return config.getString("messages.nobody");
    }

    public Map<String, CustomPlaceholder> getCustomPlaceholders() {
        Map<String, CustomPlaceholder> map = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("custom-placeholders");
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            String data = section.getString(key + ".data");
            String placeholder = section.getString(key + ".placeholder");
            boolean canBeNull = section.getBoolean(key + ".can-be-null", false);
            if (data != null && placeholder != null) {
                map.put(data, new CustomPlaceholder(data, placeholder, canBeNull));
            }
        }
        return map;
    }
}
