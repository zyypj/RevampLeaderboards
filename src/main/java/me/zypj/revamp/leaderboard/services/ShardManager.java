package me.zypj.revamp.leaderboard.services;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.repository.BoardRepository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShardManager {

    private final LeaderboardPlugin plugin;
    private final BoardRepository repo;
    private final boolean enabled;
    private final int maxEntries;

    private final Map<String, Map<PeriodType, List<String>>> shards = new ConcurrentHashMap<>();

    public ShardManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.repo = plugin.getBootstrap().getBoardRepository();
        ConfigAdapter config = plugin.getBootstrap().getConfigAdapter();
        this.enabled = config.isShardingEnabled();
        this.maxEntries = config.getShardMaxEntries();
    }

    public void init() {
        if (!enabled) return;

        for (String raw : plugin.getBootstrap().getBoardsConfigAdapter().getBoards()) {
            String base = sanitize(raw);
            Map<PeriodType, List<String>> map = new EnumMap<>(PeriodType.class);

            for (PeriodType pt : PeriodType.values()) {
                String prefix = base + "_" + pt.name().toLowerCase();
                List<String> tbls = discoverOrCreateShards(prefix);
                map.put(pt, tbls);
            }
            shards.put(raw, map);
        }
    }

    public List<String> getShards(String raw, PeriodType period) {
        if (!enabled) return Collections.singletonList(sanitize(raw) + "_" + period.name().toLowerCase());

        return Collections.unmodifiableList(
                shards.getOrDefault(raw, Collections.emptyMap())
                        .getOrDefault(period, Collections.emptyList())
        );
    }

    public String getShardForWrite(String raw, PeriodType period) {
        if (!enabled) {
            return sanitize(raw) + "_" + period.name().toLowerCase();
        }

        List<String> tbls = shards.get(raw).get(period);
        String last = tbls.get(tbls.size() - 1);
        int cnt = repo.count(last);
        if (cnt >= maxEntries) {
            int idx = tbls.size() + 1;
            String newName = baseName(raw, period) + "_" + idx;
            repo.initTables(Collections.singletonList(newName));
            tbls.add(newName);
            plugin.getLogger().info("Created new shard table: " + newName);
            return newName;
        }
        return last;
    }

    private List<String> discoverOrCreateShards(String prefix) {
        List<String> found = new ArrayList<>();
        try (Connection c = plugin.getBootstrap().getDatabaseService()
                .getDataSource().getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            ResultSet rs = md.getTables(null, null, prefix + "%", new String[]{"TABLE"});
            while (rs.next()) {
                found.add(rs.getString("TABLE_NAME"));
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Error discovering shards for " + prefix + ": " + ex.getMessage());
        }
        Collections.sort(found);
        if (found.isEmpty()) {
            found.add(prefix);
            repo.initTables(Collections.singletonList(prefix));
        }
        return found;
    }

    private String baseName(String raw, PeriodType pt) {
        return sanitize(raw) + "_" + pt.name().toLowerCase();
    }

    private String sanitize(String in) {
        return in.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }
}
