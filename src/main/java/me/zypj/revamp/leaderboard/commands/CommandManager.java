package me.zypj.revamp.leaderboard.commands;

import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, ISubCommand> commands = new HashMap<>();

    public CommandManager(LeaderboardPlugin plugin, String label, ISubCommand... subs) {
        for (ISubCommand sub : subs) {
            commands.put(sub.name(), sub);
            for (String a : sub.aliases()) commands.put(a, sub);
        }
        PluginCommand cmd = plugin.getCommand(label);
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /" + label + " <subcomando>");
            return false;
        }
        ISubCommand sub = commands.get(args[0].toLowerCase());
        if (sub == null) {
            sender.sendMessage("§cSubcomando não encontrado.");
            return false;
        }
        if (!sub.allowConsole() && !(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return false;
        }
        if (!sub.hasPermission(sender)) {
            sender.sendMessage("§cSem permissão.");
            return false;
        }
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        boolean ok = sub.execute(sender, subArgs);
        if (!ok) sender.sendMessage("§cUso correto: " + sub.usage());
        return ok;
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
