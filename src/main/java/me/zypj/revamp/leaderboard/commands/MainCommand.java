package me.zypj.revamp.leaderboard.commands;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MainCommand implements CommandExecutor, TabCompleter {
    private final LeaderboardPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("leaderboard.use")) {
            sender.sendMessage("§4§lERRO! §cVocê não tem permissão para isso.");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUso: /lb <reload|board>");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                plugin.getBootstrap().getBoardsConfigAdapter().reload();
                sender.sendMessage("§aLeaderBoard recarregado.");
                break;

            case "sensive":
                if (!sender.getName().equalsIgnoreCase("tadeu")) {
                    sender.sendMessage("§4ERRO! §cVocê não pode usar esse comando.");
                    return false;
                }

                if (!args[1].equalsIgnoreCase("resetDatabase")) {
                    sender.sendMessage("§4ERRO! §cComando incorreto.");
                    return false;
                }

                plugin.getBootstrap().getBoardService().clearDatabase();
                sender.sendMessage("§aFeito!");
                return true;
            case "board":
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /lb board <add|remove|list> [placeholder]");
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "add":
                        if (args.length < 3) {
                            sender.sendMessage("§cUso: /lb board add <placeholder>");
                            break;
                        }
                        String rawAdd = args[2].replace("%", "");
                        String phAdd = rawAdd.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                        plugin.getBootstrap().getBoardService().addBoard(phAdd);
                        sender.sendMessage("§aPlacar adicionado: " + phAdd);
                        break;

                    case "remove":
                        if (args.length < 3) {
                            sender.sendMessage("§cUso: /lb board remove <placeholder>");
                            break;
                        }
                        String rawRem = args[2].replace("%", "");
                        String phRem = rawRem.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
                        plugin.getBootstrap().getBoardService().removeBoard(phRem);
                        sender.sendMessage("§aPlacar removido: " + phRem);
                        break;

                    case "list":
                        List<String> boards = plugin.getBootstrap().getBoardService().getBoards();
                        sender.sendMessage("§6Placares registrados:");
                        boards.forEach(b -> sender.sendMessage(" - " + b));
                        break;

                    default:
                        sender.sendMessage("§cUso: /lb board <add|remove|list> [placeholder]");
                        break;
                }
                break;

            default:
                sender.sendMessage("§cUso: /lb <reload|board>");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("leaderboard.use")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String sub : Arrays.asList("reload", "board")) {
                if (sub.startsWith(args[0].toLowerCase()))
                    completions.add(sub);
            }
            return completions;
        }

        if (args[0].equalsIgnoreCase("board")) {
            if (args.length == 2) {
                for (String sub : Arrays.asList("add", "remove", "list")) {
                    if (sub.startsWith(args[1].toLowerCase()))
                        completions.add(sub);
                }
                return completions;
            }

            if (args.length == 3) {
                if (args[1].equalsIgnoreCase("remove")) {
                    for (String b : plugin.getBootstrap().getBoardService().getBoards()) {
                        if (b.startsWith(args[2].toLowerCase()))
                            completions.add(b);
                    }
                    return completions;
                }
                if (args[1].equalsIgnoreCase("add")) {
                    completions.addAll(
                            plugin.getBootstrap().getConfigAdapter()
                                    .getCustomPlaceholders().values().stream()
                                    .map(cp -> cp.getPlaceholder().replace("%", "").toLowerCase())
                                    .filter(ph -> ph.startsWith(args[2].toLowerCase()))
                                    .collect(Collectors.toList())
                    );
                    return completions;
                }
            }
        }

        return Collections.emptyList();
    }
}
