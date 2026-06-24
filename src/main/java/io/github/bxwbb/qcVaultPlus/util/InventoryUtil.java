package io.github.bxwbb.qcVaultPlus.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

public class InventoryUtil {
    private static final File DATA_FOLDER = new File("plugins/QcVaultPlus/data/inventory");

    private InventoryUtil() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    public static void saveInventory(Inventory inventory, String id) {
        if (!DATA_FOLDER.exists()) {
            DATA_FOLDER.mkdirs();
        }
        File file = new File(DATA_FOLDER, id + ".yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("size", inventory.getSize());
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            yaml.set("slots." + slot, item);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Inventory loadInventory(String id) {
        File file = new File(DATA_FOLDER, id + ".yml");
        if (!file.exists()) {
            return null;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int size = yaml.getInt("size", 27);
        Inventory inv = org.bukkit.Bukkit.getServer().createInventory(null, size);
        ItemStack[] contents = new ItemStack[size];
        for (int slot = 0; slot < size; slot++) {
            contents[slot] = yaml.getItemStack("slots." + slot);
        }
        inv.setContents(contents);
        return inv;
    }

    public static boolean hasInventory(String id) {
        File file = new File(DATA_FOLDER, id + ".yml");
        return file.exists();
    }
}