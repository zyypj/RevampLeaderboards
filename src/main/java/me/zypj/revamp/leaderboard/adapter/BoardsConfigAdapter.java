package me.zypj.revamp.leaderboard.adapter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BoardsConfigAdapter {

    public enum BoardType {PLAYER, STRING}

    @Getter
    @RequiredArgsConstructor
    public static class BoardConfig {
        private final BoardType type;
        private final String placeholder;
        private final String keyPlaceholder;
        private final String valuePlaceholder;
    }

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

    public Set<String> getBoardKeys() {
        ConfigurationSection sec = configuration.getConfigurationSection("boards");
        return sec == null ? Collections.emptySet() : sec.getKeys(false);
    }

    public BoardConfig getBoardConfig(String boardKey) {
        String path = "boards." + boardKey + ".";
        String typeStr = configuration.getString(path + "type", "player").toUpperCase(Locale.ROOT);
        BoardType type = BoardType.valueOf(typeStr);

        if (type == BoardType.PLAYER) {
            String ph = configuration.getString(path + "placeholder");
            return new BoardConfig(type, ph, null, null);
        } else {
            String keyPh = configuration.getString(path + "key-placeholder");
            String valPh = configuration.getString(path + "value-placeholder");
            return new BoardConfig(type, null, keyPh, valPh);
        }
    }

    public void save() {
        try {
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addStringBoard(String boardKey, String keyPh, String valuePh) {
        String base = "boards." + boardKey + ".";
        configuration.set(base + "type", "string");
        configuration.set(base + "key-placeholder", keyPh);
        configuration.set(base + "value-placeholder", valuePh);
        save();
    }

    public void removeBoard(String boardKey) {
        configuration.set("boards." + boardKey, null);
        save();
    }
}
