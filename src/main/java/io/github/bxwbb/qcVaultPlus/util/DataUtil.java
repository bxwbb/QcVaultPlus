package io.github.bxwbb.qcVaultPlus.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class DataUtil {
    private static final File PLAYER_DATA_DIR = new File("plugins/QcVaultPlus/data/player");

    private DataUtil() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    private static File getPlayerFile(UUID uuid) {
        if (!PLAYER_DATA_DIR.exists()) {
            PLAYER_DATA_DIR.mkdirs();
        }
        return new File(PLAYER_DATA_DIR, uuid + ".yml");
    }

    public static void set(Player player, String id, Object object) {
        File file = getPlayerFile(player.getUniqueId());
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set(id, object);
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object get(Player player, String id) {
        return get(player, id, null);
    }

    public static Object get(Player player, String id, Object defaultValue) {
        File file = getPlayerFile(player.getUniqueId());
        if (!file.exists()) {
            return defaultValue;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return cfg.get(id, defaultValue);
    }

    public static boolean has(Player player, String id) {
        File file = getPlayerFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return cfg.contains(id);
    }

    public static void remove(Player player, String id) {
        File file = getPlayerFile(player.getUniqueId());
        if (!file.exists()) {
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set(id, null);
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}