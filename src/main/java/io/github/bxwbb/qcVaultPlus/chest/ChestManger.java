package io.github.bxwbb.qcVaultPlus.chest;

import io.github.bxwbb.qcVaultPlus.QcVaultPlus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChestManger {
    private final QcVaultPlus plugin;
    private final BukkitScheduler scheduler;
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private final LootManger lootManager;

    private final List<ChestConfig> chestConfigs = new ArrayList<>();
    private final Map<Location, Integer> chestCooldown = new ConcurrentHashMap<>();
    public final Map<Location, TextDisplay> textDisplays = new ConcurrentHashMap<>();
    // 标记：本次冷却是否已经刷新物资
    private final Map<Location, Boolean> alreadyRefreshed = new ConcurrentHashMap<>();

    public ChestManger(QcVaultPlus plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
        this.lootManager = new LootManger(plugin);
        startTickTask();
    }

    public LootManger getLootManager() {
        return lootManager;
    }

    private void startTickTask() {
        scheduler.scheduleSyncRepeatingTask(plugin, this::tickLoop, 0, 20);
    }

    // 判断箱子是否空
    private boolean isChestEmpty(Location loc) {
        Block b = loc.getBlock();
        if (b.getType() != Material.CHEST) return false;
        BlockState state = b.getState();
        if (!(state instanceof Chest chest)) return false;
        Inventory inv = chest.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private void tickLoop() {
        List<ChestConfig> cfgSnap = new ArrayList<>(chestConfigs);
        int online = Bukkit.getOnlinePlayers().size();

        for (ChestConfig cfg : cfgSnap) {
            List<Location> locSnap = new ArrayList<>(cfg.locations());
            int limit = cfg.playerSwitchLimit();
            int cooldownFull = cfg.highTime();
            int cooldownCrowd = cfg.lowTime();
            String lootPoolId = cfg.lootTable();
            String formatStr = cfg.timeString();
            int targetCd = online > limit ? cooldownCrowd : cooldownFull;

            for (Location loc : locSnap) {
                chestCooldown.putIfAbsent(loc, targetCd);
                alreadyRefreshed.putIfAbsent(loc, false);
                int remain = chestCooldown.get(loc);
                boolean refreshed = alreadyRefreshed.get(loc);
                boolean empty = isChestEmpty(loc);

                // 箱子有物资，不计时、不刷新、隐藏文字
                if (!empty) {
                    removeTextDisplay(loc);
                    continue;
                }

                // 箱子是空的，走冷却逻辑
                if (!refreshed) {
                    boolean newCd = remain == targetCd;
                    if (newCd) {
                        createTextDisplay(loc, remain, formatStr);
                    } else {
                        refreshTextDisplay(loc, remain, formatStr);
                    }

                    TextDisplay display = textDisplays.get(loc);
                    if (display != null && !display.isDead()) {
                        faceNearPlayer(display, loc);
                    }

                    // 冷却走完，仅刷新一次物资
                    if (remain <= 0) {
                        refreshChestLoot(loc, lootPoolId);
                        alreadyRefreshed.put(loc, true);
                        removeTextDisplay(loc);
                        continue;
                    }
                    chestCooldown.put(loc, remain - 1);
                } else {
                    // 已经刷新过，等玩家拿空箱子才重置冷却
                    removeTextDisplay(loc);
                    chestCooldown.put(loc, targetCd);
                    alreadyRefreshed.put(loc, false);
                }
            }
        }
    }

    private void faceNearPlayer(TextDisplay display, Location chestLoc) {
        Player target = null;
        double minDist = 12;
        for (Player p : Bukkit.getOnlinePlayers()) {
            double dist = p.getLocation().distanceSquared(chestLoc);
            if (dist < minDist * minDist) {
                minDist = dist;
                target = p;
            }
        }
        if (target == null) return;
        display.setRotation(target.getLocation().getYaw(), 0);
    }

    private void refreshChestLoot(Location loc, String lootPoolId) {
        scheduler.runTask(plugin, () -> {
            Block b = loc.getBlock();
            if (b.getType() != Material.CHEST) return;

            LootManger.LootPool pool = lootManager.getLoot(lootPoolId);
            if (pool == null) {
                plugin.getLogger().warning("未找到自定义战利品池：" + lootPoolId);
                return;
            }
            lootManager.spawnLoot(b, pool);
        });
    }

    private void createTextDisplay(Location loc, int time, String format) {
        scheduler.runTask(plugin, () -> {
            removeTextDisplay(loc);
            Location spawn = loc.clone().add(0.5, 1.4, 0.5);
            TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(spawn, EntityType.TEXT_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            display.text(mm.deserialize(formatCountdownZero(time, format)));
            textDisplays.put(loc, display);
        });
    }

    private void refreshTextDisplay(Location loc, int time, String format) {
        scheduler.runTask(plugin, () -> {
            TextDisplay display = textDisplays.get(loc);
            if (display == null || display.isDead()) {
                ChestConfig cfg = findConfig(loc);
                if (cfg != null) createTextDisplay(loc, time, cfg.timeString());
                return;
            }
            display.text(mm.deserialize(formatCountdownZero(time, format)));
        });
    }

    private void removeTextDisplay(Location loc) {
        TextDisplay display = textDisplays.remove(loc);
        if (display != null && !display.isDead()) display.remove();
    }

    private ChestConfig findConfig(Location target) {
        for (ChestConfig cfg : chestConfigs) {
            if (cfg.locations().contains(target)) return cfg;
        }
        return null;
    }

    public void registerChest(ChestConfig chestConfig) {
        chestConfigs.add(chestConfig);
    }

    public void clear() {
        chestConfigs.clear();
        chestCooldown.clear();
        alreadyRefreshed.clear();
        textDisplays.values().forEach(d -> {if (!d.isDead()) d.remove();});
        textDisplays.clear();
    }

    public boolean containsChest(String typeId) {
        for (ChestConfig cfg : chestConfigs) {
            if (cfg.typeId().equals(typeId)) return true;
        }
        return false;
    }

    public ChestConfig getChestConfig(String typeId) {
        for (ChestConfig cfg : chestConfigs) {
            if (cfg.typeId().equals(typeId)) return cfg;
        }
        return null;
    }

    public List<ChestConfig> getChestConfigs() {
        return chestConfigs;
    }

    public String formatCountdownZero(int totalSecond, String template) {
        int min = totalSecond / 60;
        int sec = totalSecond % 60;
        return String.format(template, min, sec);
    }
}