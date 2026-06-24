package io.github.bxwbb.qcVaultPlus;

import io.github.bxwbb.qcVaultPlus.arrowCase.ArrowCaseConfig;
import io.github.bxwbb.qcVaultPlus.arrowCase.ArrowCaseManger;
import io.github.bxwbb.qcVaultPlus.chest.ChestConfig;
import io.github.bxwbb.qcVaultPlus.chest.ChestManger;
import io.github.bxwbb.qcVaultPlus.chest.LootManger;
import io.github.bxwbb.qcVaultPlus.event.EventListener;
import io.github.bxwbb.qcVaultPlus.storage.StorageManger;
import io.github.bxwbb.qcVaultPlus.vault.VaultManger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class QcVaultPlus extends JavaPlugin {

    public ArrowCaseManger arrowCaseManger;
    public ChestManger chestManger;
    public LootManger lootManger;
    public StorageManger storageManger;
    public VaultManger vaultManger;

    @Override
    public void onEnable() {
        ArrowCaseConfig.plugin = this;
        arrowCaseManger = new ArrowCaseManger(this);
        chestManger = new ChestManger(this);
        lootManger = new LootManger(this);
        storageManger = new StorageManger(this);
        vaultManger = new VaultManger(this);
        new CommandManger(this);
        new EventListener(this);
        loadConfig();
        vaultManger.load();
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration configuration = getConfig();
        if (configuration.contains("arrow-case")) {
            arrowCaseManger.clear();
            ConfigurationSection arrowCases = configuration.getConfigurationSection("arrow-case");
            if (arrowCases != null) {
                for (String key : arrowCases.getKeys(false)) {
                    arrowCaseManger.registerArrowCase(
                            new ArrowCaseConfig(
                                    Material.valueOf(arrowCases.getString(key + ".item-type")),
                                    arrowCases.getString(key + ".display-name"),
                                    arrowCases.getStringList(key + ".lore"),
                                    arrowCases.getInt(key + ".amount"),
                                    arrowCases.getInt(key + ".custom-model-data"),
                                    key
                            )
                    );
                }
            }
        }

        if (configuration.contains("chest")) {
            chestManger.clear();
            ConfigurationSection chests = configuration.getConfigurationSection("chest");
            if (chests != null) {
                for (String key : chests.getKeys(false)) {
                    List<String> locations = chests.getStringList(key + ".pos");
                    chestManger.registerChest(
                            new ChestConfig(
                                    new ArrayList<>(locations.stream().map(this::stringToLoc).toList()),
                                    chests.getInt(key + ".low-time"),
                                    chests.getInt(key + ".high-time"),
                                    chests.getInt(key + ".player-switch-limit"),
                                    key,
                                    chests.getString(key + ".loot-table"),
                                    chests.getString(key + ".time-string")
                            )
                    );
                }
            }
        }
        lootManger.reloadAll();
        storageManger.reload();
    }

    @Override
    public void onDisable() {
        FileConfiguration configuration = getConfig();
        for (ChestConfig chestConfig : chestManger.getChestConfigs()) {
            configuration.set("chest." + chestConfig.typeId() + ".pos", chestConfig.locations().stream().map(this::locToString).toList());
            configuration.set("chest." + chestConfig.typeId() + ".low-time", chestConfig.lowTime());
            configuration.set("chest." + chestConfig.typeId() + ".high-time", chestConfig.highTime());
            configuration.set("chest." + chestConfig.typeId() + ".player-switch-limit", chestConfig.playerSwitchLimit());
            configuration.set("chest." + chestConfig.typeId() + ".loot-table", chestConfig.lootTable());
            configuration.set("chest." + chestConfig.typeId() + ".time-string", chestConfig.timeString());
        }
        saveConfig();
        for (TextDisplay value : chestManger.textDisplays.values()) {
            value.remove();
        }
        storageManger.save();
    }

    public Location stringToLoc(String str) {
        if(str == null || str.isBlank()) return null;
        String[] arr = str.split(";");
        if(arr.length < 6) return null;
        World world = Bukkit.getWorld(arr[0]);
        if(world == null) return null;
        double x = Double.parseDouble(arr[1]);
        double y = Double.parseDouble(arr[2]);
        double z = Double.parseDouble(arr[3]);
        float yaw = Float.parseFloat(arr[4]);
        float pitch = Float.parseFloat(arr[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String locToString(Location loc) {
        if(loc == null) return "";
        World w = loc.getWorld();
        return w.getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }
}
