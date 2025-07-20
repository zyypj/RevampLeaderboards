package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SensiveCommand implements ISubCommand {

    private final LeaderboardPlugin plugin;

    @Override
    public String name() {
        return "sensive";
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
        return sender.isOp();
    }

    @Override
    public String usage() {
        return "/lb sensive resetDatabase [board]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("resetDatabase")) {
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.sensive.usage").replace("{usage}", usage()));
            return false;
        }

        if (args.length == 1) {
            plugin.getBootstrap().getBoardService().clearDatabase();
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.sensive.boards-del"));
            return true;
        }

        String raw = args[1]
                .replace("%", "")
                .replaceAll("[^a-zA-Z0-9_]", "")
                .toLowerCase();
        try {
            plugin.getBootstrap().getBoardService().clearBoard(raw);
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.sensive.board-del").replace("{board}", raw));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.sensive.board-not-found").replace("{board}", raw));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("resetDatabase").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "resetDatabase".equalsIgnoreCase(args[0])) {
            return plugin.getBootstrap().getBoardService()
                    .getBoards().stream()
                    .filter(b -> b.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
