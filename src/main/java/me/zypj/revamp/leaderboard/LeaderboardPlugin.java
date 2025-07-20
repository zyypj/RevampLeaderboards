package me.zypj.revamp.leaderboard;

import com.google.common.base.Stopwatch;
import lombok.Getter;
import me.zypj.revamp.leaderboard.commands.CommandManager;
import me.zypj.revamp.leaderboard.commands.subcommands.BoardCommand;
import me.zypj.revamp.leaderboard.commands.subcommands.ReloadCommand;
import me.zypj.revamp.leaderboard.commands.subcommands.SensiveCommand;
import me.zypj.revamp.leaderboard.commands.subcommands.VerifyCommand;
import me.zypj.revamp.leaderboard.hook.LeaderBoardPlaceholderExpansion;
import me.zypj.revamp.leaderboard.listener.PlayerListeners;
import me.zypj.revamp.leaderboard.loader.PluginBootstrap;
import me.zypj.revamp.leaderboard.shared.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class LeaderboardPlugin extends JavaPlugin {

    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        getLogger().info("");
        getLogger().info("Iniciando plugin...");

        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new Metrics(this, 26576);

        bootstrap = new PluginBootstrap(this);
        bootstrap.init();

        new LeaderBoardPlaceholderExpansion(this).register();

        new CommandManager(
                this,
                "lb",
                new BoardCommand(this),
                new ReloadCommand(this),
                new SensiveCommand(this),
                new VerifyCommand(this)
                );

        getServer().getPluginManager().registerEvents(new PlayerListeners(this), this);

        getLogger().info("Plugin iniciado em " + stopwatch.stop() + "!");
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        if (bootstrap != null) {
            bootstrap.getCustomPlaceholderService().shutdown();
            bootstrap.getDatabaseService().getDataSource().close();
        }
    }

    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI não encontrado, desligando plugin...");
            return false;
        }

        return true;
    }
}
