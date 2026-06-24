package io.github.bxwbb.qcVaultPlus.chest;

import org.bukkit.Location;

import java.util.List;

public record ChestConfig(
        List<Location> locations,
        int lowTime,
        int highTime,
        int playerSwitchLimit,
        String typeId,
        String lootTable,
        String timeString
) {

    public void addPos(Location location) {
        locations.add(location);
    }

}
