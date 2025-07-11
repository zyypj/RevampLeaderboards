package me.zypj.revamp.leaderboard.services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;

public class DatabaseService {
    @Getter
    private final HikariDataSource dataSource;

    public DatabaseService(LeaderboardPlugin plugin) {
        HikariConfig hc = new HikariConfig();
        ConfigAdapter config = plugin.getBootstrap().getConfigAdapter();
        hc.setJdbcUrl("jdbc:mysql://"
                + config.getDatabaseHost() + ":"
                + config.getDatabasePort() + "/"
                + config.getDatabaseName()
                + "?useSSL=false");
        hc.setUsername(config.getDatabaseUser());
        hc.setPassword(config.getDatabasePassword());
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hc.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(hc);
    }
}
