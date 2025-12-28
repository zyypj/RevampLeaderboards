package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MaintenanceCommand implements ISubCommand {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final LeaderboardPlugin plugin;

    @Override
    public String name() {
        return "maintenance";
    }

    @Override
    public String[] aliases() {
        return new String[]{"maint", "data"};
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("leaderboard.maintenance");
    }

    @Override
    public String usage() {
        return "/lb maintenance <backup|export|import> [name]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(msg("commands.maintenance.usage").replace("{usage}", usage()));
            return false;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "backup":
                return handleBackup(sender, args);
            case "export":
                return handleExport(sender, args);
            case "import":
                return handleImport(sender, args);
            default:
                sender.sendMessage(msg("commands.maintenance.usage").replace("{usage}", usage()));
                return false;
        }
    }

    private boolean handleBackup(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("maintenance.backup.enabled", true)) {
            sender.sendMessage(msg("commands.maintenance.disabled"));
            return false;
        }
        String name = args.length > 1 ? sanitizeName(args[1]) : TS.format(LocalDateTime.now());
        String folderName = plugin.getConfig().getString("maintenance.backup.folder", "backups");
        File targetDir = new File(plugin.getDataFolder(), folderName + "/" + name);

        List<String> copied = copyFromDataFolder(targetDir, true,
                "maintenance.backup.include");
        if (copied.isEmpty()) {
            sender.sendMessage(msg("commands.maintenance.empty"));
            return false;
        }
        pruneBackups(new File(plugin.getDataFolder(), folderName),
                plugin.getConfig().getInt("maintenance.backup.keep-last", 10));

        sender.sendMessage(msg("commands.maintenance.backup.created")
                .replace("{path}", targetDir.getPath())
                .replace("{count}", String.valueOf(copied.size())));
        return true;
    }

    private boolean handleExport(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("maintenance.export.enabled", true)) {
            sender.sendMessage(msg("commands.maintenance.disabled"));
            return false;
        }
        String name = args.length > 1 ? sanitizeName(args[1]) : TS.format(LocalDateTime.now());
        String folderName = plugin.getConfig().getString("maintenance.export.folder", "exports");
        File targetDir = new File(plugin.getDataFolder(), folderName + "/" + name);

        List<String> copied = copyFromDataFolder(targetDir, true,
                "maintenance.export.include");
        if (copied.isEmpty()) {
            sender.sendMessage(msg("commands.maintenance.empty"));
            return false;
        }

        sender.sendMessage(msg("commands.maintenance.export.created")
                .replace("{path}", targetDir.getPath())
                .replace("{count}", String.valueOf(copied.size())));
        return true;
    }

    private boolean handleImport(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("maintenance.import.enabled", true)) {
            sender.sendMessage(msg("commands.maintenance.disabled"));
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("commands.maintenance.import.usage")
                    .replace("{usage}", "/lb maintenance import <name>"));
            return false;
        }
        String name = sanitizeName(args[1]);
        String folderName = plugin.getConfig().getString("maintenance.import.folder", "exports");
        File sourceDir = new File(plugin.getDataFolder(), folderName + "/" + name);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            sender.sendMessage(msg("commands.maintenance.import.not-found").replace("{name}", name));
            return false;
        }

        boolean overwrite = plugin.getConfig().getBoolean("maintenance.import.allow-overwrite", false);
        List<String> imported = copyToDataFolder(sourceDir, overwrite);
        if (imported.isEmpty()) {
            sender.sendMessage(msg("commands.maintenance.empty"));
            return false;
        }

        sender.sendMessage(msg("commands.maintenance.import.success")
                .replace("{count}", String.valueOf(imported.size())));

        if (plugin.getConfig().getBoolean("maintenance.import.auto-reload", true)) {
            plugin.getBootstrap().reload();
            sender.sendMessage(msg("commands.maintenance.import.reloaded"));
        }
        return true;
    }

    private List<String> copyFromDataFolder(File targetDir, boolean overwrite, String includePath) {
        Map<String, File> sources = getSourceFiles(includePath);
        return copyFiles(sources, targetDir, overwrite);
    }

    private List<String> copyToDataFolder(File sourceDir, boolean overwrite) {
        Map<String, File> targets = getTargetFiles();
        List<String> copied = new ArrayList<>();
        for (Map.Entry<String, File> entry : targets.entrySet()) {
            File src = new File(sourceDir, entry.getValue().getName());
            if (!src.exists()) continue;
            File dest = entry.getValue();
            if (dest.exists() && !overwrite) continue;
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied.add(dest.getName());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to import " + dest.getName() + ": " + e.getMessage());
            }
        }
        return copied;
    }

    private List<String> copyFiles(Map<String, File> sources, File targetDir, boolean overwrite) {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            plugin.getLogger().warning("Could not create folder: " + targetDir.getPath());
            return Collections.emptyList();
        }
        List<String> copied = new ArrayList<>();
        for (Map.Entry<String, File> entry : sources.entrySet()) {
            File src = entry.getValue();
            if (!src.exists()) continue;
            File dest = new File(targetDir, src.getName());
            if (dest.exists() && !overwrite) continue;
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied.add(dest.getName());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to copy " + src.getName() + ": " + e.getMessage());
            }
        }
        return copied;
    }

    private Map<String, File> getSourceFiles(String includePath) {
        Map<String, File> files = getTargetFiles();
        files.entrySet().removeIf(e -> !plugin.getConfig().getBoolean(includePath + "." + e.getKey(), true));
        return files;
    }

    private Map<String, File> getTargetFiles() {
        File base = plugin.getDataFolder();
        Map<String, File> files = new java.util.LinkedHashMap<>();
        files.put("config-yml", new File(base, "config.yml"));
        files.put("boards-yml", new File(base, "boards.yml"));
        files.put("messages-yml", new File(base, "messages.yml"));
        files.put("application-yml", new File(base, "application.yml"));
        return files;
    }

    private void pruneBackups(File backupFolder, int keepLast) {
        if (keepLast <= 0 || !backupFolder.exists()) return;
        File[] dirs = backupFolder.listFiles(File::isDirectory);
        if (dirs == null || dirs.length <= keepLast) return;

        List<File> sorted = new ArrayList<>();
        Collections.addAll(sorted, dirs);
        sorted.sort(Comparator.comparingLong(File::lastModified).reversed());

        for (int i = keepLast; i < sorted.size(); i++) {
            deleteRecursive(sorted.get(i));
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    private String sanitizeName(String in) {
        return in.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase(Locale.ROOT);
    }

    private String msg(String path) {
        return plugin.getBootstrap().getMessagesAdapter().getMessage(path);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("backup", "export", "import").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "import".equalsIgnoreCase(args[0])) {
            String folderName = plugin.getConfig().getString("maintenance.import.folder", "exports");
            File folder = new File(plugin.getDataFolder(), folderName);
            File[] dirs = folder.listFiles(File::isDirectory);
            if (dirs == null) return Collections.emptyList();
            return java.util.Arrays.stream(dirs)
                    .map(File::getName)
                    .filter(n -> n.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
