package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ReloadCommand implements ISubCommand {

    private final LeaderboardPlugin plugin;

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String[] aliases() {
        return new String[]{"rl"};
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public boolean hasPermission(CommandSender s) {
        return s.hasPermission("leaderboard.reload");
    }

    @Override
    public String usage() {
        return "/lb reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        plugin.getBootstrap().getBoardsConfigAdapter().reload();
        sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.reload.success"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
