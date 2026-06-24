package io.github.bxwbb.qcVaultPlus.storage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class StorageSelectGuiJava implements StorageSelectGui {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void open(Player player, Storage storage) {
        Inventory inventory = Bukkit.createInventory(null, 3 * 9, mm.deserialize("<bold><blue>仓库选择界面"));
        for (int i = 0; i < Storage.MAX_STORAGE_COUNT; i++) {
            ItemStack itemStack;
            if (i < storage.storageCount) {
                itemStack = new ItemStack(Material.CHEST);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(mm.deserialize("<yellow>仓库 " + (i + 1)));
                itemMeta.lore(List.of(mm.deserialize("<gray>点击打开仓库")));
                itemStack.setItemMeta(itemMeta);
            } else {
                itemStack = new ItemStack(Material.STRUCTURE_VOID);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(mm.deserialize("<gray>仓库未解锁"));
                itemStack.setItemMeta(itemMeta);
            }
            inventory.setItem(i, itemStack);
        }
        player.openInventory(inventory);
    }
}
