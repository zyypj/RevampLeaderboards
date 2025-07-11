package me.zypj.revamp.leaderboard.adapter;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BoardsConfigAdapter {
    private final File file;
    private YamlConfiguration configuration;

    public BoardsConfigAdapter(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "boards.yml");
        if (!file.exists()) {
            plugin.saveResource("boards.yml", false);
        }
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public List<String> getBoards() {
        return configuration.getStringList("boards");
    }

    public void addBoard(String placeholder) {
        List<String> list = getBoards();
        if (!list.contains(placeholder)) {
            list.add(placeholder);
            configuration.set("boards", list);
            save();
        }
    }

    public void removeBoard(String placeholder) {
        List<String> list = getBoards();
        if (list.remove(placeholder)) {
            configuration.set("boards", list);
            save();
        }
    }

    private void save() {
        try {
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
