package io.github.bxwbb.qcVaultPlus.chest;

import io.github.bxwbb.qcVaultPlus.QcVaultPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class LootManger {
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private final QcVaultPlus plugin;
    private final File lootFolder;
    private final Map<String, LootPool> cache = new HashMap<>();

    public LootManger(QcVaultPlus plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        lootFolder = new File(plugin.getDataFolder(), "loot");
        if (!lootFolder.exists()) {
            lootFolder.mkdirs();
        }
    }

    public void spawnLoot(Block block, LootPool pool) {
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }

        BlockState blockState = block.getState();
        if (!(blockState instanceof Chest chest)) {
            return;
        }

        Inventory inv = chest.getInventory();
        if (inv instanceof DoubleChestInventory doubleInv) {
            inv = doubleInv;
        }

        inv.clear();
        Random random = new Random();
        int rollCount = random.nextInt(pool.maxRoll() - pool.minRoll() + 1) + pool.minRoll();
        List<LootEntry> entries = pool.entries();
        int success = 0;
        for (int i = 0; i < rollCount; i++) {
            ItemStack item = pickRandomItem(entries, random);
            if (item == null) {
                continue;
            }
            int slot;
            int loopCnt = 0;
            do {
                slot = random.nextInt(inv.getSize());
                loopCnt++;
            } while ((inv.getItem(slot) != null && inv.getItem(slot).getType() != Material.AIR) && loopCnt < 100);
            if (loopCnt >= 100) {
                continue;
            }
            inv.setItem(slot, item);
            success++;
        }
    }

    public void spawnLootNoClear(Block block, LootPool pool) {
        if (block == null || block.getType() != Material.CHEST) return;

        BlockState blockState = block.getState();
        if (!(blockState instanceof Chest chest)) return;
        Inventory inv = chest.getInventory();
        if (inv instanceof DoubleChestInventory doubleInv) inv = doubleInv;

        int invSize = inv.getSize();
        Random random = new Random();
        int totalRoll = random.nextInt(pool.maxRoll() - pool.minRoll() + 1) + pool.minRoll();
        List<LootEntry> entries = pool.entries();

        List<Integer> emptySlots = new ArrayList<>();
        for (int s = 0; s < invSize; s++) {
            ItemStack item = inv.getItem(s);
            if (item == null || item.getType() == Material.AIR) {
                emptySlots.add(s);
            }
        }
        if (emptySlots.isEmpty()) {
            return;
        }

        int success = 0;
        for (int i = 0; i < totalRoll; i++) {
            ItemStack item = pickRandomItem(entries, random);
            if (item == null) continue;
            int idx = random.nextInt(emptySlots.size());
            int slot = emptySlots.remove(idx);
            inv.setItem(slot, item);
            success++;
            if (emptySlots.isEmpty()) break;
        }
    }

    private ItemStack pickRandomItem(List<LootEntry> entries, Random random) {
        int totalWeight = 0;
        for (LootEntry e : entries) {
            totalWeight += e.weight();
        }
        if (totalWeight <= 0) {
            return null;
        }

        int target = random.nextInt(totalWeight);
        int current = 0;
        for (LootEntry entry : entries) {
            current += entry.weight();
            if (target < current) {
                return buildItem(entry, random);
            }
        }
        return buildItem(entries.get(0), random);
    }

    private ItemStack buildItem(LootEntry entry, Random random) {
        Material mat = Material.matchMaterial(entry.material());
        if (mat == null) {
            mat = Material.STONE;
        }
        int amount = random.nextInt(entry.maxAmount() - entry.minAmount() + 1) + entry.minAmount();
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        if (entry.customModelData() > 0) meta.setCustomModelData(entry.customModelData());
        if (!entry.name().isBlank()) meta.displayName(mm.deserialize(entry.name()));
        if (!entry.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : entry.lore()) lore.add(mm.deserialize(line));
            meta.lore(lore);
        }

        for (Map.Entry<String, Integer> ench : entry.enchantments().entrySet()) {
            NamespacedKey enchKey = NamespacedKey.minecraft(ench.getKey());
            Enchantment enchType = Enchantment.getByKey(enchKey);
            if (enchType != null) meta.addEnchant(enchType, ench.getValue(), true);
        }

        for (Map.Entry<String, String> pd : entry.persistentData().entrySet()) {
            NamespacedKey pdKey = NamespacedKey.fromString(pd.getKey());
            if (pdKey != null) {
                meta.getPersistentDataContainer().set(pdKey, PersistentDataType.STRING, pd.getValue());
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private FileConfiguration loadYaml(InputStream input) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(input)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    public LootPool getLoot(String name) {
        if (cache.containsKey(name)) return cache.get(name);

        File localFile = new File(lootFolder, name + ".yml");
        FileConfiguration config = null;

        if (localFile.exists()) {
            config = YamlConfiguration.loadConfiguration(localFile);
        } else {
            InputStream in = plugin.getResource("loot/" + name + ".yml");
            if (in != null) {
                try {
                    config = loadYaml(in);
                } catch (Exception ignored) {}
            }
        }
        if (config == null) return null;

        int minRoll = config.getInt("rolls.min", 1);
        int maxRoll = config.getInt("rolls.max", 3);
        List<LootEntry> entryList = new ArrayList<>();

        ConfigurationSection entrySec = config.getConfigurationSection("entries");
        if (entrySec == null) return null;
        for (String key : entrySec.getKeys(false)) {
            ConfigurationSection e = entrySec.getConfigurationSection(key);
            String mat = e.getString("material", "stone");
            int weight = e.getInt("weight", 1);
            int minA = e.getInt("amount.min", 1);
            int maxA = e.getInt("amount.max", 1);
            int cmd = e.getInt("custom-model-data", 0);
            String displayName = e.getString("name", "");
            List<String> lore = e.getStringList("lore");

            Map<String, Integer> enchantments = new HashMap<>();
            ConfigurationSection enchSec = e.getConfigurationSection("enchantments");
            if (enchSec != null) {
                for (String ek : enchSec.getKeys(false)) enchantments.put(ek, enchSec.getInt(ek));
            }

            Map<String, String> persistentData = new HashMap<>();
            ConfigurationSection dataSec = e.getConfigurationSection("persistent-data");
            if (dataSec != null) {
                for (String dk : dataSec.getKeys(false)) persistentData.put(dk, dataSec.getString(dk));
            }

            entryList.add(new LootEntry(mat, weight, minA, maxA, cmd, displayName, lore, enchantments, persistentData));
        }

        LootPool pool = new LootPool(name, minRoll, maxRoll, entryList);
        cache.put(name, pool);
        return pool;
    }

    public void reloadAll() {
        cache.clear();
        File[] files = lootFolder.listFiles((dir, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            String fileName = f.getName();
            String id = fileName.substring(0, fileName.lastIndexOf("."));
            getLoot(id);
        }
    }

    public List<String> getAllLootIds() {
        return cache.keySet().stream().sorted().toList();
    }

    public record LootPool(String id, int minRoll, int maxRoll, List<LootEntry> entries) {}

    public record LootEntry(
            String material,
            int weight,
            int minAmount,
            int maxAmount,
            int customModelData,
            String name,
            List<String> lore,
            Map<String, Integer> enchantments,
            Map<String, String> persistentData
    ) {}
}