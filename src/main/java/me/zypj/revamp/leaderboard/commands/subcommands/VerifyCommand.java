package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class VerifyCommand implements ISubCommand {

    private final LeaderboardPlugin plugin;

    @Override
    public String name() {
        return "verify";
    }

    @Override
    public String[] aliases() {
        return new String[0];
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("leaderboard.verify");
    }

    @Override
    public String usage() {
        return "/lb verify";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        plugin.getBootstrap().getBoardsConfigAdapter().reload();
        sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.verify.reload"));

        plugin.getBootstrap().getBoardService().invalidateCache();
        sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.verify.invalidate-cache"));

        plugin.getBootstrap().getBoardService().updateAll();
        sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.verify.boards-updated"));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
