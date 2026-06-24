package io.github.bxwbb.qcVaultPlus.vault;

import io.github.bxwbb.qcVaultPlus.QcVaultPlus;
import io.github.bxwbb.qcVaultPlus.util.DataUtil;
import io.github.bxwbb.qcVaultPlus.util.InventoryUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

public class VaultManger {
    private final QcVaultPlus plugin;
    // 基础延迟秒数
    public static final double BASE_DELAY_SEC = 3.0;
    // 每多一格增加0.25秒
    public static final double DELAY_PER_SLOT = 0.25;
    // 基础初始等级（1格）
    public static final int BASE_LEVEL = 1;
    private static final ItemStack BARRIER_ITEM = new ItemStack(Material.BARRIER);
    private static final File INV_DATA_FOLDER = new File("plugins/QcVaultPlus/data/inventory");
    private static final MiniMessage mm = MiniMessage.miniMessage();
    // 玩家保险箱等级存储键
    private static final String VAULT_LEVEL_KEY = "vault_level";

    public VaultManger(QcVaultPlus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        String dataId = "vault_" + uuid;

        // 读取玩家保险箱等级，等级=可用格子
        int usableSlots = (int) DataUtil.get(player, VAULT_LEVEL_KEY, BASE_LEVEL);
        // 动态计算总开启延迟：基础3秒 + 每格0.25秒
        double totalDelaySecond = BASE_DELAY_SEC + (usableSlots - 1) * DELAY_PER_SLOT;
        int totalTick = (int) Math.ceil(totalDelaySecond * 20);

        double originX = player.getX();
        double originY = player.getY();
        double originZ = player.getZ();
        String originWorld = player.getWorld().getName();

        new BukkitRunnable() {
            int tickCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                boolean moved = !player.getWorld().getName().equals(originWorld)
                        || player.getX() != originX
                        || player.getY() != originY
                        || player.getZ() != originZ;
                if (moved) {
                    player.sendMessage("§c移动取消保险箱开启");
                    this.cancel();
                    return;
                }

                tickCount++;
                double leftSecRaw = (totalTick - tickCount) / 20.0;
                int leftSec = (int) Math.ceil(leftSecRaw);
                // 每秒刷新标题，显示容量与倒计时
                if (tickCount % 20 == 0 || tickCount == 1) {
                    Title title = Title.title(
                            Component.text("正在解锁保险箱"),
                            Component.text("容量：" + usableSlots + " 格 | 剩余 " + leftSec + " 秒"),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                    );
                    player.showTitle(title);
                }

                if (tickCount < totalTick) {
                    return;
                }
                this.cancel();
                player.clearTitle();

                // 1. 加载后台持久存储的真实背包（原始存档，无屏障）
                Inventory sourceStorageInv;
                if (InventoryUtil.hasInventory(dataId)) {
                    sourceStorageInv = InventoryUtil.loadInventory(dataId);
                } else {
                    sourceStorageInv = Bukkit.createInventory(null, 54, mm.deserialize("<bold><blue>私人保险箱"));
                }

                // 2. 创建给玩家显示的临时界面背包，复制原始存档所有物品
                Inventory displayGuiInv = Bukkit.createInventory(null, 54, mm.deserialize("<bold><blue>私人保险箱"));
                if (sourceStorageInv != null) {
                    displayGuiInv.setContents(sourceStorageInv.getContents());
                }

                // 3. 未解锁槽位填充屏障，玩家只能操作前 usableSlots 格
                for (int slot = 0; slot < 54; slot++) {
                    if (slot >= usableSlots) {
                        displayGuiInv.setItem(slot, BARRIER_ITEM);
                    }
                }

                // 打开界面
                player.openInventory(displayGuiInv);

                // 4. 关闭界面时只同步解锁范围内格子回原始存档，屏障区域丢弃不保存
                Inventory finalSourceInv = sourceStorageInv;
                plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
                        if (!event.getInventory().equals(displayGuiInv)) return;

                        // 仅同步前N个解锁格子，覆盖原始存档
                        for (int slot = 0; slot < usableSlots; slot++) {
                            if (finalSourceInv != null) {
                                finalSourceInv.setItem(slot, displayGuiInv.getItem(slot));
                            }
                        }
                        // 超出解锁范围的格子不处理、不写入存档，永久保留原始存档数据

                        // 保存原始真实背包到文件
                        InventoryUtil.saveInventory(finalSourceInv, dataId);
                    }
                }, plugin);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void load() {
        if (!INV_DATA_FOLDER.exists()) {
            return;
        }
        File[] files = INV_DATA_FOLDER.listFiles((dir, name) -> name.startsWith("vault_") && name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            String id = fileName.substring(0, fileName.lastIndexOf("."));
            plugin.getLogger().info("[Vault] 加载保险箱存档：" + id);
        }
    }
}