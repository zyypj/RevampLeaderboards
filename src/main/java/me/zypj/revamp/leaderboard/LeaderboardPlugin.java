package me.zypj.revamp.leaderboard;

import com.google.common.base.Stopwatch;
import lombok.Getter;
import me.zypj.revamp.leaderboard.api.LeaderboardApi;
import me.zypj.revamp.leaderboard.api.impl.LeaderboardApiImpl;
import me.zypj.revamp.leaderboard.commands.CommandManager;
import me.zypj.revamp.leaderboard.commands.subcommands.*;
import me.zypj.revamp.leaderboard.hook.LeaderBoardPlaceholderExpansion;
import me.zypj.revamp.leaderboard.listener.PlayerListeners;
import me.zypj.revamp.leaderboard.loader.PluginBootstrap;
import me.zypj.revamp.leaderboard.shared.Metrics;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class LeaderboardPlugin extends JavaPlugin {

    private PluginBootstrap bootstrap;
    private Metrics metrics;

    @Override
    public void onEnable() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        getLogger().info("");
        getLogger().info("Starting plugin...");

        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        metrics = new Metrics(this, 26576);

        loadBootstrap();

        new LeaderBoardPlaceholderExpansion(this).register();

        new CommandManager(
                this,
                "lb",
                new BoardCommand(this),
                new ReloadCommand(this),
                new SensiveCommand(this),
                new VerifyCommand(this),
                new MetricCommand(this),
                new MaintenanceCommand(this)
        );

        getServer().getPluginManager().registerEvents(new PlayerListeners(this), this);

        registerApi();

        getLogger().info("Plugin started in " + stopwatch.stop() + "!");
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) bootstrap.shutdown();
        if (metrics != null) metrics.shutdown();
    }

    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI not founded! Shutting down...");
            return false;
        }

        return true;
    }

    private void loadBootstrap() {
        bootstrap = new PluginBootstrap(this);
        bootstrap.init();
    }

    private void registerApi() {
        LeaderboardApi api = new LeaderboardApiImpl(bootstrap);
        getServer().getServicesManager()
                .register(LeaderboardApi.class, api, this, ServicePriority.Normal);
    }
}
