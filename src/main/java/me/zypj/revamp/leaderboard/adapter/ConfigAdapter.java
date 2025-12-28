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

    public String getDatabaseType() {
        return config.getString("database.type");
    }

    public String getDatabaseHost() {
        return config.getString("database.mysql.host");
    }

    public int getDatabasePort() {
        return config.getInt("database.mysql.port");
    }

    public String getDatabaseName() {
        return config.getString("database.mysql.database");
    }

    public String getDatabaseUser() {
        return config.getString("database.mysql.user");
    }

    public int getDatabaseThreadPoolSize() {
        return config.getInt("database.mysql.thread-pool-size",
                Runtime.getRuntime().availableProcessors());
    }

    public int getCacheTtlSeconds() {
        return config.getInt("cache.ttl-seconds", 30);
    }

    public int getCacheRefreshSeconds() {
        return config.getInt("cache.refresh-seconds", 30);
    }

    public int getCacheMaxEntries() {
        return config.getInt("cache.max-entries", 0);
    }

    public int getCacheMaxEntriesPerBoard() {
        return config.getInt("cache.max-entries-per-board", 0);
    }

    public boolean isCacheStatsEnabled() {
        return config.getBoolean("cache.record-stats", false);
    }

    public String getDatabasePassword() {
        return config.getString("database.mysql.password");
    }

    public boolean isShardingEnabled() {
        return config.getBoolean("sharding.enabled", false);
    }

    public int getShardMaxEntries() {
        return config.getInt("sharding.max-entries-per-shard", 10000);
    }

    public int getShardCountCacheSeconds() {
        return config.getInt("sharding.count-cache-seconds", 10);
    }

    public int getCustomPlaceholderRefreshSeconds() {
        return config.getInt("custom-placeholders-settings.refresh-seconds", 10);
    }

    public int getCustomPlaceholderCacheMaxPlayers() {
        return config.getInt("custom-placeholders-settings.cache.max-players", 5000);
    }

    public int getCustomPlaceholderCacheTtlSeconds() {
        return config.getInt("custom-placeholders-settings.cache.expire-after-access-seconds", 1800);
    }

    public boolean isHistoryJsonEnabled() {
        return config.getBoolean("history.file.enabled", true);
    }

    public String getHistoryJsonFolder() {
        return config.getString("history.file.folder", "historic");
    }

    public int getHistoryJsonRetentionDays() {
        return config.getInt("history.file.retention-days", 30);
    }

    public boolean isHistoryDailyEnabled() {
        return config.getBoolean("history.schedule.daily.enabled", true);
    }

    public boolean isHistoryWeeklyEnabled() {
        return config.getBoolean("history.schedule.weekly.enabled", true);
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
