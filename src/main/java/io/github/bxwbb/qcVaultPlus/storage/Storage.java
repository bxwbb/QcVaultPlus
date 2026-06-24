package io.github.bxwbb.qcVaultPlus.storage;

import io.github.bxwbb.qcVaultPlus.util.DataUtil;
import io.github.bxwbb.qcVaultPlus.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Storage {

    public static final int MAX_STORAGE_COUNT = 16;
    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    private final Player player;

    public final Inventory[] inventories = new Inventory[MAX_STORAGE_COUNT];
    public int storageCount = 4;

    public Storage(Player player) {
        this.player = player;
        load();
    }

    public void load() {
        for (int i = 0; i < MAX_STORAGE_COUNT; i++) {
            if (InventoryUtil.hasInventory(player.getName() + "_" + i)) {
                inventories[i] = InventoryUtil.loadInventory(player.getName() + "_" + i);
            } else {
                inventories[i] = Bukkit.createInventory(null, 6 * 9);
            }
        }
        storageCount = (int) DataUtil.get(player, "storageCount", 4);
    }

    public void save() {
        for (int i = 0; i < MAX_STORAGE_COUNT; i++) {
            InventoryUtil.saveInventory(inventories[i], player.getName() + "_" + i);
        }
        DataUtil.set(player, "storageCount", storageCount);
    }
}
