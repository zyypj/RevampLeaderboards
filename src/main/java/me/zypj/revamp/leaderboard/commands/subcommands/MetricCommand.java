package me.zypj.revamp.leaderboard.commands.subcommands;

import lombok.RequiredArgsConstructor;
import me.zypj.revamp.leaderboard.LeaderboardPlugin;
import me.zypj.revamp.leaderboard.commands.ISubCommand;
import me.zypj.revamp.leaderboard.enums.PeriodType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class MetricCommand implements ISubCommand {

    private final LeaderboardPlugin plugin;

    @Override
    public String name() {
        return "metric";
    }

    @Override
    public String[] aliases() {
        return new String[]{"metrics", "stats"};
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("leaderboard.metrics");
    }

    @Override
    public String usage() {
        return "/lb metric";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "RevampLeaderboards » Metrics");

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        String uptime = formatDuration(Duration.ofMillis(rb.getUptime()));
        sender.sendMessage(col("&bUptime: &f" + uptime));

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;

        sender.sendMessage(col("&bMemory: &f" +
                humanBytes(used) + " &7/ " + humanBytes(total) + " &7(used/allocated), max " + humanBytes(max)));

        int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        sender.sendMessage(col("&bThreads: &f" + threads));

        sender.sendMessage(col("&bJava: &f" + System.getProperty("java.version")));
        sender.sendMessage(col("&bServer: &f" + Bukkit.getVersion()));

        int online = Bukkit.getOnlinePlayers().size();
        sender.sendMessage(col("&bOnline Players: &f" + online));

        int boards = plugin.getBootstrap().getBoardService().getBoards().size();
        sender.sendMessage(col("&bRegistered Boards: &f" + boards));

        String periods = String.join(", ",
                java.util.Arrays.stream(PeriodType.values())
                        .map(Enum::name).toArray(String[]::new));
        sender.sendMessage(col("&bAvailable Periods: &f" + periods));

        boolean papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        sender.sendMessage(col("&bPlaceholderAPI: &f" + (papi ? "enabled" : "disabled")));

        sender.sendMessage(col("&7Use &f/lb board list &7para ver os boards cadastrados."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    private String col(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format(java.util.Locale.ROOT, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String formatDuration(Duration d) {
        long days = d.toDays();
        d = d.minusDays(days);

        long hours = d.toHours();
        d = d.minusHours(hours);

        long minutes = d.toMinutes();
        d = d.minusMinutes(minutes);

        long seconds = d.getSeconds();
        StringBuilder sb = new StringBuilder();

        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");

        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
