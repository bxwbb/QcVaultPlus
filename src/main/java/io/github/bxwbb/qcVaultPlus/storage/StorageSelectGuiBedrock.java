package io.github.bxwbb.qcVaultPlus.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import static io.github.bxwbb.qcVaultPlus.arrowCase.ArrowCaseConfig.plugin;

public class StorageSelectGuiBedrock implements StorageSelectGui {
    @Override
    public void open(Player player, Storage storage) {
        int playerStorageCount = storage.storageCount;
        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title("玩家仓库")
                .content("选择仓库");
        for (int i = 0; i < Storage.MAX_STORAGE_COUNT; i++) {
            if (i < playerStorageCount) {
                formBuilder.button("仓库 " + (i + 1));
            } else {
                formBuilder.button("仓库未解锁");
            }
        }
        formBuilder.resultHandler((simpleForm, response) -> {
            if (response == null) return;
            int index = Integer.parseInt(response.toString());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (index >= playerStorageCount) {
                    player.sendMessage("§c该仓库尚未解锁！");
                    return;
                }
                player.openInventory(storage.inventories[index]);
            });
        });
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }
}
