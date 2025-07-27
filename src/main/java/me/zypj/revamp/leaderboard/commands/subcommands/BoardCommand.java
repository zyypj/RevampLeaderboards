package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class BoardCommand implements ISubCommand {

    private final LeaderboardPlugin plugin;

    @Override
    public String name() {
        return "board";
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
        return sender.hasPermission("leaderboard.board");
    }

    @Override
    public String usage() {
        return "/lb board <add|remove|list|test> [placeholder]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.usage").replace("{usage}", usage()));
            return false;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "add":
                if (args.length < 4) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.add.usage")
                            .replace("{usage}", "/lb board add <boardKey> <key-placeholder> <value-placeholder>"));
                    return false;
                }

                String boardKey = args[1].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                String keyPh = args[2];
                String valPh = args[3];
                plugin.getBootstrap().getBoardsConfigAdapter().addStringBoard(boardKey, keyPh, valPh);
                plugin.getBootstrap().getBoardsConfigAdapter().reload();
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.add.board-added")
                        .replace("{board}", boardKey));
                break;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.remove.usage")
                            .replace("{usage}", "/lb board remove <boardKey>"));
                    return false;
                }

                String remKey = args[1].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                plugin.getBootstrap().getBoardsConfigAdapter().removeBoard(remKey);
                plugin.getBootstrap().getBoardsConfigAdapter().reload();
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.remove.board-del")
                        .replace("{board}", remKey));
                break;

            case "list":
                Set<String> boards = plugin.getBootstrap().getBoardsConfigAdapter().getBoardKeys();
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.list.top-message"));
                for (String b : boards) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.list.board-message")
                            .replace("{board}", b));
                }
                break;

            case "test":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.usage")
                            .replace("{usage}", "/lb board test <boardKey>"));
                    return false;
                }

                String testKey = args[1].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                Set<String> keys = plugin.getBootstrap().getBoardsConfigAdapter().getBoardKeys();

                if (!keys.contains(testKey)) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.board-not-found")
                            .replace("{board}", testKey));
                    return false;
                }

                List<BoardEntry> top = plugin.getBootstrap()
                        .getBoardService()
                        .getLeaderboard(testKey, PeriodType.TOTAL, 10);

                if (top.isEmpty()) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.board-empty")
                            .replace("{board}", testKey));
                    return false;
                }

                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.top-message")
                        .replace("{board}", testKey));

                for (int i = 0; i < top.size(); i++) {
                    BoardEntry e = top.get(i);
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.entry-message")
                            .replace("{position}", String.valueOf(i + 1))
                            .replace("{entry-display}", e.getDisplay())
                            .replace("{value}", String.valueOf(e.getValue()))
                    );
                }

                String remainsPh = "%lb_remains_total_" + testKey + "%";
                String remaining = PlaceholderAPI.setPlaceholders(null, remainsPh);
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.reset-message")
                        .replace("{remaing}", remaining));
                break;

            default:
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.usage").replace("{usage}", usage()));
                return false;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Stream.of("add", "remove", "list", "test")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            if (sub.equals("remove") || sub.equals("list")) {
                return plugin.getBootstrap().getBoardsConfigAdapter().getBoardKeys().stream()
                        .filter(b -> b.startsWith(prefix))
                        .collect(Collectors.toList());
            }
            if (sub.equals("add")) {
                return plugin.getBootstrap().getConfigAdapter()
                        .getCustomPlaceholders().values().stream()
                        .map(cp -> cp.getPlaceholder().replace("%", "").toLowerCase())
                        .filter(ph -> ph.startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
