package me.zypj.revamp.leaderboard.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface ISubCommand {
    String name();

    String[] aliases();

    boolean allowConsole();

    boolean hasPermission(CommandSender sender);

    String usage();

    boolean execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);
}
