package io.github.bxwbb.qcVaultPlus.storage;

import io.github.bxwbb.qcVaultPlus.QcVaultPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class StorageManger {

    private final QcVaultPlus plugin;

    private final Map<Player, Storage> storage = new HashMap<>();

    public StorageManger(QcVaultPlus plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            storage.put(onlinePlayer, new Storage(onlinePlayer));
        }
    }

    public void save() {
        for (Storage storage : storage.values()) {
            storage.save();
        }
    }

    public Storage getStorage(Player player) {
        if (!storage.containsKey(player)) {
            storage.put(player, new Storage(player));
        }
        return storage.get(player);
    }
}
