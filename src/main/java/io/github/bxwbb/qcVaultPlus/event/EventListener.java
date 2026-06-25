package io.github.bxwbb.qcVaultPlus.event;

import io.github.bxwbb.qcVaultPlus.QcVaultPlus;
import io.github.bxwbb.qcVaultPlus.arrowCase.ArrowCaseConfig;
import io.github.bxwbb.qcVaultPlus.storage.Storage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventListener implements Listener {

    private final QcVaultPlus plugin;
    private final NamespacedKey keyTypeId;
    private final NamespacedKey keyAmount;
    private final List<Player> playerOpenArrowCase = new ArrayList<>();
    private final Map<Player, ItemStack> offHandItem = new HashMap<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public EventListener(QcVaultPlus plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.keyTypeId = new NamespacedKey(plugin, "arrowCaseTypeId");
        this.keyAmount = new NamespacedKey(plugin, "arrowCaseAmount");
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && player.isSneaking() && event.getAction().isRightClick()) {
            ItemMeta itemMeta = item.getItemMeta();
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (pdc.has(getKey("arrowCaseTypeId"), PersistentDataType.STRING)) {
                player.openInventory(plugin.arrowCaseManger.getArrowCaseInventory(item));
                playerOpenArrowCase.add(player);
            }
        }
    }

    @EventHandler
    public void onPlayerClickInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory guiInv = event.getInventory();
        Inventory clickInv = event.getClickedInventory();
        ItemStack clickItem = event.getCurrentItem();
        ItemStack offHandQuiver = player.getInventory().getItemInMainHand();
        Component title = event.getView().title();
        int clickSlot = event.getSlot();


        if (title.equals(mm.deserialize("<bold><blue>私人保险箱"))) {
            int unlockSlots = 1;
            if (clickItem != null && clickItem.getType() == Material.BARRIER && clickSlot >= unlockSlots) {
                event.setCancelled(true);
                return;
            }
        }

        if (offHandQuiver.hasItemMeta()) {
            PersistentDataContainer quiverPdc = offHandQuiver.getItemMeta().getPersistentDataContainer();
            if (quiverPdc.has(keyTypeId, PersistentDataType.STRING)) {
                if (!playerOpenArrowCase.contains(player)) return;

                int maxSlot = getQuiverMaxSlot(offHandQuiver);
                int usedSlot = getQuiverUsedSlot(guiInv);

                event.setCancelled(true);

                if (clickInv == guiInv) {
                    if (clickItem == null || clickItem.getType().isAir()) return;
                    if (!isArrowItem(clickItem)) return;

                    ItemStack takeArrow = clickItem.clone();
                    takeArrow.setAmount(1);

                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage("§c你的背包没有空位，无法取出箭矢！");
                        return;
                    }

                    guiInv.setItem(event.getSlot(), null);

                    List<ItemStack> arrowList = new ArrayList<>();
                    int totalSize = guiInv.getSize();
                    for (int s = 0; s < totalSize; s++) {
                        ItemStack item = guiInv.getItem(s);
                        if (isArrowItem(item)) {
                            arrowList.add(item);
                            guiInv.setItem(s, null);
                        }
                    }
                    for (int s = 0; s < arrowList.size(); s++) {
                        guiInv.setItem(s, arrowList.get(s));
                    }

                    player.getInventory().addItem(takeArrow);
                    return;
                }

                if (clickInv == player.getInventory()) {
                    if (clickItem == null || clickItem.getType().isAir()) return;
                    if (!isArrowItem(clickItem)) return;

                    if (usedSlot >= maxSlot) {
                        player.sendMessage("§c箭袋容量已满，无法存入更多箭矢！");
                        return;
                    }

                    ItemStack storeArrow = clickItem.clone();
                    storeArrow.setAmount(1);
                    clickItem.setAmount(clickItem.getAmount() - 1);
                    if (clickItem.getAmount() <= 0) {
                        clickInv.setItem(event.getSlot(), null);
                    }

                    for (int slot = 0; slot < guiInv.getSize(); slot++) {
                        ItemStack slotItem = guiInv.getItem(slot);
                        if (slotItem == null || slotItem.getType().isAir()) {
                            guiInv.setItem(slot, storeArrow);
                            break;
                        }
                    }
                }
            }
        }
        if (title.equals(mm.deserialize("<bold><blue>仓库选择界面"))) {
            int slot = event.getSlot();
            Storage storage = plugin.storageManger.getStorage(player);
            if (slot < storage.storageCount) {
                player.closeInventory();
                player.openInventory(storage.inventories[slot]);
            } else {
                player.sendMessage(Component.text("该仓库未解锁"));
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCloseInventory(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        playerOpenArrowCase.remove(player);
    }

    public NamespacedKey getKey(String tagName) {
        return new NamespacedKey(plugin, tagName);
    }

    public static boolean isArrowItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String matName = item.getType().name();
        return matName.endsWith("ARROW");
    }

    private int getQuiverMaxSlot(ItemStack quiverItem) {
        if (!quiverItem.hasItemMeta()) return 1;
        PersistentDataContainer pdc = quiverItem.getItemMeta().getPersistentDataContainer();
        Integer amountObj = pdc.get(keyAmount, PersistentDataType.INTEGER);
        int raw = amountObj == null ? 1 : amountObj;
        return Math.min(54, Math.max(1, raw));
    }

    private int getQuiverUsedSlot(Inventory quiverGui) {
        int used = 0;
        for (ItemStack item : quiverGui.getContents()) {
            if (isArrowItem(item)) used++;
        }
        return used;
    }

    @EventHandler
    public void onPlayerPullBow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        Action act = event.getAction();
        if ((act == Action.RIGHT_CLICK_AIR || act == Action.RIGHT_CLICK_BLOCK) && handItem.getType() == Material.BOW) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand.getType().equals(Material.AIR) || !offHand.hasItemMeta()) return;
            ArrowCaseConfig arrowCaseConfig = plugin.arrowCaseManger.getArrowCaseConfig(offHand);
            if (arrowCaseConfig != null) {
                ItemStack arrow = plugin.arrowCaseManger.takeFirstArrow(offHand);
                if (arrow == null) {
                    player.sendMessage("§c你的箭袋已空！");
                } else {
                    offHandItem.put(player, offHand);
                    player.getInventory().setItemInOffHand(arrow);
                }
            }
        }
    }

    @EventHandler
    public void onArrowLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (offHandItem.containsKey(player)) {
            ItemStack offHand = offHandItem.get(player);
            player.getInventory().setItemInOffHand(offHand);
            offHandItem.remove(player);
        }
    }

    @EventHandler
    public void onArrowHitEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        Entity hitEntity = event.getEntity();
        double damage = event.getDamage();
        String entityName = hitEntity.getName();
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1, 1.2f);
        Component actionBarText = mm.deserialize("<green>命中目标：<white>" + entityName + " <green>伤害：<white>" + String.format("%.1f", damage));
        shooter.sendActionBar(actionBarText);
        Title title = Title.title(
                mm.deserialize("<green>↓ ↑"),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(750), Duration.ofMillis(250))
        );
        shooter.showTitle(title);
    }

    @EventHandler
    public void onArrowHitBlock(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (event.getHitEntity() == null) {
            shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 0.6f, 0.9f);
        }
    }
}