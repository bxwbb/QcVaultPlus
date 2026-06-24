package io.github.bxwbb.qcVaultPlus;

import io.github.bxwbb.qcVaultPlus.arrowCase.ArrowCaseConfig;
import io.github.bxwbb.qcVaultPlus.chest.ChestConfig;
import io.github.bxwbb.qcVaultPlus.chest.LootManger;
import io.github.bxwbb.qcVaultPlus.chest.LootManger.LootPool;
import io.github.bxwbb.qcVaultPlus.storage.StorageSelectGui;
import io.github.bxwbb.qcVaultPlus.storage.StorageSelectGuiBedrock;
import io.github.bxwbb.qcVaultPlus.storage.StorageSelectGuiJava;
import io.github.bxwbb.qcVaultPlus.util.DataUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandManger implements TabExecutor {

    private final QcVaultPlus plugin;
    private static final String VAULT_LEVEL_KEY = "vault_level";
    private static final String STORAGE_COUNT_KEY = "storage_count";

    public CommandManger(QcVaultPlus plugin) {
        this.plugin = plugin;
        PluginCommand command = this.plugin.getCommand("qcvaultplus");
        if (command == null) {
            throw new IllegalStateException("FUCK YOU! Command qcpet is not defined in plugin.yml");
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        try {
            if (args.length > 0) {
                switch (args[0]) {
                    case "arrowCase":
                        return handleArrowCase(commandSender, args);
                    case "reload":
                        plugin.loadConfig();
                        plugin.lootManger.reloadAll();
                        commandSender.sendMessage("重载完成");
                        return true;
                    case "chest":
                        return handleChest(commandSender, args);
                    case "loot":
                        return handleLoot(commandSender, args);
                    case "storage":
                        return handleStorage(commandSender, args);
                    case "vault":
                        return handleVault(commandSender, args);
                }
            }
        } catch (CommandException e) {
            commandSender.sendMessage("§c" + e.getMessage());
        }
        return false;
    }

    private boolean handleVault(CommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new CommandException("用法：/qcvaultplus vault open | get <玩家名> | set <玩家名> <等级> | add <玩家名> <数值>");
        }
        String sub = args[1];
        if ("open".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player player)) {
                throw new CommandException("该子指令仅玩家可用");
            }
            plugin.vaultManger.open(player);
            return true;
        }
        if (!sender.isOp()) {
            throw new CommandException("你没有执行该指令的权限");
        }
        if ("get".equalsIgnoreCase(sub)) {
            if (args.length < 3) {
                throw new CommandException("用法：/qcvaultplus vault get <玩家名>");
            }
            String targetName = args[2];
            Player target = plugin.getServer().getPlayer(targetName);
            if (target == null) {
                throw new CommandException("找不到在线玩家：" + targetName);
            }
            int level = (int) DataUtil.get(target, VAULT_LEVEL_KEY, 1);
            sender.sendMessage("§a玩家 " + targetName + " 保险箱等级：" + level);
            return true;
        }
        if ("set".equalsIgnoreCase(sub)) {
            if (args.length < 4) {
                throw new CommandException("用法：/qcvaultplus vault set <玩家名> <等级>");
            }
            String targetName = args[2];
            int targetLv;
            try {
                targetLv = Integer.parseInt(args[3]);
                if (targetLv < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new CommandException("等级必须为大于等于1的整数");
            }
            Player target = plugin.getServer().getPlayer(targetName);
            if (target == null) {
                throw new CommandException("找不到在线玩家：" + targetName);
            }
            DataUtil.set(target, VAULT_LEVEL_KEY, targetLv);
            sender.sendMessage("§a成功将玩家 " + targetName + " 保险箱等级设置为 " + targetLv);
            return true;
        }
        if ("add".equalsIgnoreCase(sub)) {
            if (args.length < 4) {
                throw new CommandException("用法：/qcvaultplus vault add <玩家名> <增加数量>");
            }
            String targetName = args[2];
            int addNum;
            try {
                addNum = Integer.parseInt(args[3]);
                if (addNum < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new CommandException("增加数值必须为正整数");
            }
            Player target = plugin.getServer().getPlayer(targetName);
            if (target == null) {
                throw new CommandException("找不到在线玩家：" + targetName);
            }
            int oldLv = (int) DataUtil.get(target, VAULT_LEVEL_KEY, 1);
            int newLv = oldLv + addNum;
            DataUtil.set(target, VAULT_LEVEL_KEY, newLv);
            sender.sendMessage("§a玩家 " + targetName + " 保险箱等级从 " + oldLv + " 提升至 " + newLv);
            return true;
        }
        throw new CommandException("无效子指令：" + sub);
    }

    private boolean handleStorage(CommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new CommandException("用法：/qcvaultplus storage open | get <玩家名> | set <玩家名> <数量> | add <玩家名> <数值>");
        }
        String sub = args[1];
        if ("open".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player player)) {
                throw new CommandException("该子指令仅玩家可用");
            }
            FloodgateApi api = FloodgateApi.getInstance();
            if (api.isFloodgatePlayer(player.getUniqueId())) {
                StorageSelectGui storageSelectGui = new StorageSelectGuiBedrock();
                storageSelectGui.open(player, plugin.storageManger.getStorage(player));
            } else {
                StorageSelectGui storageSelectGui = new StorageSelectGuiJava();
                storageSelectGui.open(player, plugin.storageManger.getStorage(player));
            }
            return true;
        }
        if (!sender.isOp()) {
            throw new CommandException("你没有执行该指令的权限");
        }
        if ("get".equalsIgnoreCase(sub)) {
            sender.sendMessage("当前拥有 " + plugin.storageManger.getStorage((Player) sender).storageCount + " 个仓库");
            return true;
        }
        if ("set".equalsIgnoreCase(sub)) {
            plugin.storageManger.getStorage((Player) sender).storageCount = Integer.parseInt(args[1]);
            return true;
        }
        if ("add".equalsIgnoreCase(sub)) {
            plugin.storageManger.getStorage((Player) sender).storageCount += Integer.parseInt(args[2]);
            return true;
        }
        throw new CommandException("无效子指令：" + sub);
    }

    private boolean handleLoot(CommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof Player player)) {
            throw new CommandException("该指令仅玩家可用");
        }
        if (args.length < 2) {
            throw new CommandException("用法：/qcvaultplus loot <战利品池ID>");
        }
        String lootId = args[1];
        LootManger lootManager = plugin.lootManger;
        LootPool pool = lootManager.getLoot(lootId);
        if (pool == null) {
            throw new CommandException("不存在该战利品池：" + lootId);
        }
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock.getType() != Material.CHEST) {
            throw new CommandException("你瞄准的方块不是箱子，请对准箱子执行指令");
        }
        lootManager.spawnLoot(targetBlock, pool);
        sender.sendMessage("§a成功清空箱子并生成战利品池：" + lootId);
        return true;
    }

    private boolean handleChest(CommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            throw new CommandException("该指令仅玩家可用");
        }
        if (args[1].equalsIgnoreCase("addpos")) {
            if (args.length < 6) throw new CommandException("参数不足，用法：/qcvaultplus chest addpos 箱子ID X Y Z");
            String typeId = args[2];
            if (plugin.chestManger.containsChest(typeId)) {
                plugin.chestManger.getChestConfig(typeId).addPos(new Location(
                        player.getWorld(),
                        Double.parseDouble(args[3]),
                        Double.parseDouble(args[4]),
                        Double.parseDouble(args[5])
                ));
                sender.sendMessage("§a成功添加箱子位置");
                return true;
            } else {
                throw new CommandException(typeId + " 箱子配置不存在");
            }
        }
        return false;
    }

    private boolean handleArrowCase(CommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            throw new CommandException("该指令仅玩家可用");
        }
        if (args[1].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                throw new CommandException("参数不足，用法：/qcvaultplus arrowCase give 箭袋ID");
            }
            String typeId = args[2];
            if (plugin.arrowCaseManger.containsArrowCase(typeId)) {
                player.getInventory().addItem(plugin.arrowCaseManger.getArrowCaseConfig(typeId).getToItem());
                sender.sendMessage("§a成功给予箭袋");
            } else {
                throw new CommandException(typeId + " 箭袋配置不存在");
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return List.of(
                    "arrowCase",
                    "chest",
                    "reload",
                    "loot",
                    "storage",
                    "vault"
            );
        }
        if (args.length == 2) {
            return switch (args[0]) {
                case "arrowCase" -> List.of("give");
                case "chest" -> List.of("addpos");
                case "loot" -> plugin.lootManger.getAllLootIds();
                case "storage", "vault" -> List.of("open", "get", "set", "add");
                default -> List.of();
            };
        }
        if ((args[0].equals("vault") || args[0].equals("storage")) && (args[1].equals("get") || args[1].equals("set") || args[1].equals("add")) && args.length == 3) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if ((args[0].equals("vault") || args[0].equals("storage")) && (args[1].equals("set") || args[1].equals("add")) && args.length == 4) {
            return List.of();
        }
        if (args.length == 3 && args[0].equals("arrowCase") && args[1].equals("give")) {
            return plugin.arrowCaseManger.getArrowCaseConfigs().stream().map(ArrowCaseConfig::typeId).toList();
        }
        if (args.length == 3 && args[0].equals("chest") && args[1].equals("addpos")) {
            return plugin.chestManger.getChestConfigs().stream().map(ChestConfig::typeId).toList();
        }
        if (args.length >= 4 && args.length < 7 && args[0].equals("chest") && args[1].equals("addpos")) {
            Location targetLoc = ((Player) commandSender).getTargetBlock(null, 5).getLocation();
            return List.of(
                    String.valueOf(targetLoc.getBlockX()),
                    targetLoc.getBlockX() + " " + targetLoc.getBlockY(),
                    targetLoc.getBlockX() + " " + targetLoc.getBlockY() + " " + targetLoc.getBlockZ()
            );
        }
        return List.of();
    }
}