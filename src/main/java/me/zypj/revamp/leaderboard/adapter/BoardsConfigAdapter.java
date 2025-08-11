package me.zypj.revamp.leaderboard.adapter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BoardsConfigAdapter {
    private final File file;
    private YamlConfiguration configuration;

    public BoardsConfigAdapter(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "boards.yml");
        if (!file.exists()) plugin.saveResource("boards.yml", false);

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

    public Map<String, List<String>> getCompositeBoards() {
        ConfigurationSection sec = configuration.getConfigurationSection("composite-boards");
        if (sec == null) return Collections.emptyMap();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            List<String> phs = sec.getStringList(key);
            out.put(key, phs == null ? Collections.emptyList() : new ArrayList<>(phs));
        }
        return out;
    }

    public void addCompositeBoard(String key, List<String> placeholders) {
        String path = "composite-boards." + key;
        configuration.set(path, placeholders == null ? Collections.emptyList() : new ArrayList<>(placeholders));
        save();
    }

    public void removeCompositeBoard(String key) {
        String path = "composite-boards." + key;
        if (configuration.contains(path)) {
            configuration.set(path, null);
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
