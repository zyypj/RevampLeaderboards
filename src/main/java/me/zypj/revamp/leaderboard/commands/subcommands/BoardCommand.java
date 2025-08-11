package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import me.zypj.revamp.leaderboard.model.BoardEntry;
import org.bukkit.command.CommandSender;

import java.util.*;
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
        return "/lb board <addcomposite|remove|list|test> ...";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.usage").replace("{usage}", usage()));
            return false;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {

            case "addcomposite": {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getBootstrap()
                            .getMessagesAdapter()
                            .getMessage("commands.board.addcomposite.usage"));
                    return false;
                }

                String compositeKey = sanitize(args[1]);
                List<String> placeholders = Arrays.stream(args).skip(2)
                        .map(s -> s.startsWith("%") ? s : "%" + sanitize(s) + "%")
                        .collect(Collectors.toList());

                if (placeholders.isEmpty()) {
                    sender.sendMessage(plugin.getBootstrap()
                            .getMessagesAdapter()
                            .getMessage("commands.board.addcomposite.missing-placeholders"));
                    return false;
                }

                for (String ph : placeholders) {
                    String resolved = safeResolve(ph);
                    if (!validatePlaceholder(resolved)) {
                        sender.sendMessage(plugin.getBootstrap()
                                .getMessagesAdapter()
                                .getMessage("commands.board.addcomposite.invalid-numeric")
                                .replace("{placeholder}", ph)
                                .replace("{resolved}", resolved == null ? "null" : resolved));
                        return false;
                    }
                }

                plugin.getBootstrap().getBoardService().addCompositeBoard(compositeKey, placeholders);
                plugin.getBootstrap().getBoardService().addBoard(compositeKey);

                sender.sendMessage(plugin.getBootstrap()
                        .getMessagesAdapter()
                        .getMessage("commands.board.addcomposite.created")
                        .replace("{key}", compositeKey)
                        .replace("{count}", String.valueOf(placeholders.size())));
                break;
            }

            case "remove": {
                if (args.length < 2) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.remove.usage")
                            .replace("{usage}", "/lb board remove <key>"));
                    return false;
                }

                String key = sanitize(args[1]);
                boolean existedSimple = plugin.getBootstrap().getBoardService().getBoards().contains(key);
                boolean existedComposite = plugin.getBootstrap().getBoardService().isComposite(key);

                if (!existedSimple && !existedComposite) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.board-not-found").replace("{board}", key));
                    return false;
                }

                if (existedComposite) plugin.getBootstrap().getBoardService().removeCompositeBoard(key);

                if (existedSimple) plugin.getBootstrap().getBoardService().removeBoard(key);

                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.remove.board-del")
                        .replace("{board}", key));
                break;
            }

            case "list": {
                List<String> simple = plugin.getBootstrap().getBoardService().getBoards();
                Set<String> composite = plugin.getBootstrap().getBoardService().getCompositeKeys();

                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.list.top-message"));

                Set<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                all.addAll(simple);
                all.addAll(composite);

                for (String k : all) {
                    boolean isComp = composite.contains(k);
                    String line = plugin.getBootstrap()
                            .getMessagesAdapter()
                            .getMessage("commands.board.list.board-message")
                            .replace("{board}", k + (isComp ? " §8(§dcomposite§8)" : ""));
                    sender.sendMessage(line);
                }
                break;
            }

            case "test": {
                if (args.length < 2) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.usage")
                            .replace("{usage}", "/lb board test <key>"));
                    return false;
                }
                String key = sanitize(args[1]);

                boolean exists = plugin.getBootstrap().getBoardService().getBoards().contains(key)
                        || plugin.getBootstrap().getBoardService().isComposite(key);

                if (!exists) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.board-not-found").replace("{board}", key));
                    return false;
                }

                List<BoardEntry> top = plugin.getBootstrap().getBoardService().getLeaderboard(key, PeriodType.TOTAL, 10);
                if (top.isEmpty()) {
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.board-empty").replace("{board}", key));
                    return true;
                }

                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.top-message").replace("{board}", key));
                for (int i = 0; i < top.size(); i++) {
                    BoardEntry e = top.get(i);
                    sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.player-message")
                            .replace("{position}", String.valueOf(i + 1))
                            .replace("{player-name}", e.getPlayerName())
                            .replace("{value}", String.valueOf(e.getValue())));
                }

                String remainsPh = "%lb_remains_total_" + key + "%";
                String remaining = PlaceholderAPI.setPlaceholders(null, remainsPh);
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.test.reset-message").replace("{remaing}", remaining));
                break;
            }

            default: {
                sender.sendMessage(plugin.getBootstrap().getMessagesAdapter().getMessage("commands.board.usage").replace("{usage}", usage()));
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Stream.of("addcomposite", "remove", "list", "test")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length >= 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String last = args[args.length - 1].toLowerCase(Locale.ROOT);

            if ("remove".equals(sub) || "test".equals(sub)) {
                Set<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                all.addAll(plugin.getBootstrap().getBoardService().getBoards());
                all.addAll(plugin.getBootstrap().getBoardService().getCompositeKeys());
                return all.stream().filter(k -> k.startsWith(last)).collect(Collectors.toList());
            }

            if ("addcomposite".equals(sub)) {
                if (args.length == 2) return Collections.singletonList("<compositeKey>");
                return plugin.getBootstrap().getConfigAdapter().getCustomPlaceholders().values().stream()
                        .map(cp -> cp.getPlaceholder().replace("%", "").toLowerCase(Locale.ROOT))
                        .filter(ph -> ph.startsWith(last))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private boolean validatePlaceholder(String resolved) {
        if (resolved == null || resolved.isEmpty()) return false;
        String normalized = resolved.trim().replace(",", ".");
        try {
            Double.parseDouble(normalized);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String safeResolve(String ph) {
        try {
            return PlaceholderAPI.setPlaceholders(null, ph);
        } catch (Throwable t) {
            return null;
        }
    }

    private String sanitize(String in) {
        return in.replace("%", "").replaceAll("[^a-zA-Z0-9_]", "").toLowerCase(Locale.ROOT);
    }
}
