package io.github.bxwbb.qcVaultPlus.arrowCase;

import io.github.bxwbb.qcVaultPlus.QcVaultPlus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrowCaseManger {

    private final List<ArrowCaseConfig> arrowCaseConfigs = new ArrayList<>();
    private final Map<ItemStack, Inventory> arrowCaseInventoryMap = new HashMap<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private final QcVaultPlus plugin;

    public ArrowCaseManger(QcVaultPlus plugin) {
        this.plugin = plugin;
    }

    public void registerArrowCase(ArrowCaseConfig arrowCaseConfig) {
        arrowCaseConfigs.add(arrowCaseConfig);
    }

    public boolean containsArrowCase(String typeId) {
        for (ArrowCaseConfig arrowCaseConfig : arrowCaseConfigs) {
            if (arrowCaseConfig.typeId().equals(typeId)) {
                return true;
            }
        }
        return false;
    }

    public ArrowCaseConfig getArrowCaseConfig(String typeId) {
        for (ArrowCaseConfig arrowCaseConfig : arrowCaseConfigs) {
            if (arrowCaseConfig.typeId().equals(typeId)) {
                return arrowCaseConfig;
            }
        }
        return null;
    }

    public void clear() {
        arrowCaseConfigs.clear();
    }

    public List<ArrowCaseConfig> getArrowCaseConfigs() {
        return arrowCaseConfigs;
    }

    public Inventory getArrowCaseInventory(ItemStack itemStack) {
        if (!arrowCaseInventoryMap.containsKey(itemStack)) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            Integer amountObj = pdc.get(getKey("arrowCaseAmount"), PersistentDataType.INTEGER);
            int amount = 0;
            if (amountObj != null) {
                amount = amountObj;
            }
            amount = Math.min(54, Math.max(1, amount));
            String typeId = pdc.get(getKey("arrowCaseTypeId"), PersistentDataType.STRING);
            Inventory inventory = plugin.getServer().createInventory(null, ((amount + 8) / 9) * 9, mm.deserialize(getArrowCaseConfig(typeId).displayName()));
            int rowCount = (int) Math.ceil(amount / 9.0) * 9;
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta barrierMeta = barrier.getItemMeta();
            barrierMeta.displayName(mm.deserialize("<gray>不可使用空位"));
            barrier.setItemMeta(barrierMeta);
            for (int slot = amount; slot < rowCount; slot++) {
                inventory.setItem(slot, barrier);
            }
            arrowCaseInventoryMap.put(itemStack, inventory);
            return inventory;
        } else {
            return arrowCaseInventoryMap.get(itemStack);
        }
    }

    public ArrowCaseConfig getArrowCaseConfig(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        return getArrowCaseConfig(pdc.get(getKey("arrowCaseTypeId"), PersistentDataType.STRING));
    }

    public NamespacedKey getKey(String tagName) {
        return new NamespacedKey(plugin, tagName);
    }

    public ItemStack takeFirstArrow(ItemStack arrowCaseItem) {
        Inventory inv = getArrowCaseInventory(arrowCaseItem);
        if (arrowCaseItem == null || !arrowCaseItem.hasItemMeta()) return null;
        ItemStack target = null;
        int targetSlot = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isArrow(item)) {
                target = item.clone();
                target.setAmount(1);
                targetSlot = i;
                break;
            }
        }
        if (target == null) return null;
        inv.setItem(targetSlot, null);
        List<ItemStack> arrowList = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isArrow(item)) {
                arrowList.add(item);
                inv.setItem(i, null);
            }
        }
        for (int i = 0; i < arrowList.size(); i++) {
            inv.setItem(i, arrowList.get(i));
        }
        return target;
    }

    private boolean isArrow(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getType().name().endsWith("ARROW");
    }
}
