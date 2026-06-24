package io.github.bxwbb.qcVaultPlus.arrowCase;

import io.github.bxwbb.qcVaultPlus.QcVaultPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public record ArrowCaseConfig(
        Material itemType,
        String displayName,
        List<String> lore,
        int amount,
        int customModelData,
        String typeId
) {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    public static QcVaultPlus plugin;

    public ItemStack getToItem() {
        ItemStack itemStack = new ItemStack(itemType);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(mm.deserialize(displayName));
        List<Component> lore = new ArrayList<>();
        for (String s : this.lore) {
            lore.add(mm.deserialize(s));
        }
        itemMeta.lore(lore);
        itemMeta.setCustomModelData(customModelData);
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        pdc.set(getKey("arrowCaseAmount"), PersistentDataType.INTEGER, amount);
        pdc.set(getKey("arrowCaseTypeId"), PersistentDataType.STRING, typeId);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public NamespacedKey getKey(String tagName) {
        return new NamespacedKey(plugin, tagName);
    }
}
