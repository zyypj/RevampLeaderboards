package me.zypj.revamp.leaderboard.commands;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final LeaderboardPlugin plugin;
    private final Map<String, ISubCommand> commands = new HashMap<>();

    public CommandManager(LeaderboardPlugin plugin, String label, ISubCommand... subs) {
        for (ISubCommand sub : subs) {
            commands.put(sub.name(), sub);
            for (String a : sub.aliases()) commands.put(a, sub);
        }
        this.plugin = plugin;
        PluginCommand cmd = plugin.getCommand(label);
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.usage").replace("{label}", label));
            return false;
        }
        ISubCommand sub = commands.get(args[0].toLowerCase());
        if (sub == null) {
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.subcommand-not-found"));
            return false;
        }
        if (!sub.allowConsole() && !(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return false;
        }
        if (!sub.hasPermission(sender)) {
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("no-permission"));
            return false;
        }
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return commands.keySet().stream()
                    .filter(k -> k.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }
        ISubCommand sub = commands.get(args[0].toLowerCase());
        if (sub == null) return Collections.emptyList();
        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
