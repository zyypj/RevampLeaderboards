package me.zypj.revamp.leaderboard.services.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.adapter.ConfigAdapter;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import java.io.File;

public class DatabaseService {
    @Getter
    private final HikariDataSource dataSource;

    public DatabaseService(LeaderboardPlugin plugin) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger hikariLogger = context.getLogger("com.zaxxer.hikari");
        hikariLogger.setLevel(Level.OFF);

        HikariConfig hc = new HikariConfig();
        ConfigAdapter config = plugin.getBootstrap().getConfigAdapter();
        if (config.getDatabaseType().equalsIgnoreCase("sqlite")) {
            plugin.getDataFolder().mkdirs();
            String fileName = config.getDatabaseName();
            File dbFile = new File(plugin.getDataFolder(), fileName);
            hc.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hc.setMaximumPoolSize(1);
            hc.setConnectionTestQuery("SELECT 1");
        } else {
            hc.setJdbcUrl("jdbc:mysql://" +
                    config.getDatabaseHost() + ":" +
                    config.getDatabasePort() + "/" +
                    config.getDatabaseName() +
                    "?useSSL=false");
            hc.setUsername(config.getDatabaseUser());
            hc.setPassword(config.getDatabasePassword());
            hc.addDataSourceProperty("cachePrepStmts", "true");
            hc.addDataSourceProperty("prepStmtCacheSize", "250");
            hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hc.setMaximumPoolSize(10);
        }
        this.dataSource = new HikariDataSource(hc);
    }
}
