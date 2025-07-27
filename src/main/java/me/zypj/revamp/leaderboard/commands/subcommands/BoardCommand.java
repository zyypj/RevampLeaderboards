package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
                if (args.length < 2) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.add.usage")
                            .replace("{usage}", "/lb board add <placeholder>"));
                    return false;
                }
                String rawAdd = args[1].replace("%", "").replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                plugin.getBootstrap().getBoardService().addBoard(rawAdd);
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.add.board-added")
                        .replace("{board}", rawAdd));
                break;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.remove.usage")
                            .replace("{usage}", "/lb board remove <placeholder>"));
                    return false;
                }
                String rawRem = args[1].replace("%", "").replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                plugin.getBootstrap().getBoardService().removeBoard(rawRem);
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.remove.board-del")
                        .replace("{board}", rawRem));
                break;

            case "list":
                List<String> boards = plugin.getBootstrap().getBoardService().getBoards();
                sender.sendMessage(plugin.getBootstrap()
                        .getMessagesAdapter()
                        .getMessage("commands.board.list.top-message"));

                boards.forEach(b -> sender.sendMessage(
                        plugin.getBootstrap()
                                .getMessagesAdapter()
                                .getMessage("commands.board.list.board-message")
                                .replace("{board}", b)
                ));
                break;

            case "test":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.usage")
                            .replace("{usage}", "/lb board test <board>"));
                    return false;
                }
                String rawTest = args[1].replace("%", "").replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                List<String> registered = plugin.getBootstrap().getBoardService().getBoards();
                if (!registered.contains(rawTest)) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.board-not-found").replace("{board}", rawTest));
                    return false;
                }
                List<BoardEntry> top = plugin.getBootstrap()
                        .getBoardService()
                        .getLeaderboard(rawTest, PeriodType.TOTAL, 10);
                if (top.isEmpty()) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.board-empty").replace("{board}", rawTest));
                    return false;
                }
                sender.sendMessage(plugin.getBootstrap()
                        .getMessagesAdapter()
                        .getMessage("commands.board.test.top-message")
                        .replace("{board}", rawTest));

                for (int i = 0; i < top.size(); i++) {
                    BoardEntry e = top.get(i);
                    sender.sendMessage(plugin.getBootstrap()
                            .getMessagesAdapter()
                            .getMessage("commands.board.test.player-message")
                            .replace("{position}", String.valueOf(i + 1))
                            .replace("{player-name}", e.getPlayerName())
                            .replace("{value}", String.valueOf(e.getValue()))
                    );
                }

                String remainsPh = "%lb_remains_total_" + rawTest + "%";
                String remaining = PlaceholderAPI.setPlaceholders(null, remainsPh);
                sender.sendMessage(plugin.getBootstrap()
                        .getMessagesAdapter()
                        .getMessage("commands.board.test.reset-message")
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
            return Arrays.asList("add", "remove", "list", "test").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            if ("remove".equals(sub) || "test".equals(sub)) {
                return plugin.getBootstrap().getBoardService()
                        .getBoards().stream()
                        .filter(b -> b.startsWith(prefix))
                        .collect(Collectors.toList());
            }
            if ("add".equals(sub)) {
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
