package com.uitem.storage;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Quản lý đọc/ghi file items.yml.
 * Save được gom lại và ghi async để tránh block tick thread của Paper/Folia.
 */
public class StorageManager {

    private final UItemPlugin plugin;
    private final File itemsFile;

    private final Object fileLock = new Object();
    private final AtomicBoolean asyncSaveWorkerRunning = new AtomicBoolean(false);
    private final AtomicLong requestCounter = new AtomicLong();
    private final AtomicLong minimumAllowedWriteId = new AtomicLong();

    // AtomicReference để "lấy + xóa" pendingSave là atomic, tránh race condition
    // giữa worker đọc/xóa và main thread ghi save mới vào cùng lúc.
    private final AtomicReference<PendingSave> pendingSaveRef = new AtomicReference<>();

    public StorageManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.itemsFile = new File(plugin.getDataFolder(), "items.yml");
    }

    /**
     * Load tất cả items từ items.yml vào UItemManager.
     */
    public void loadAll(UItemManager manager) {
        minimumAllowedWriteId.set(requestCounter.incrementAndGet());
        pendingSaveRef.set(null);

        manager.clearAll();

        if (!itemsFile.exists()) {
            createDefaultFile();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");

        if (itemsSection == null) {
            plugin.getLogger().info("No 'items' section found in items.yml. Starting with 0 items.");
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            try {
                ConfigurationSection section = itemsSection.getConfigurationSection(id);
                if (section == null) continue;

                UItemData data = new UItemData(id);

                String materialStr = section.getString("material", "STONE");
                try {
                    data.setMaterial(Material.valueOf(materialStr.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material '" + materialStr + "' for item '" + id + "'. Using STONE.");
                    data.setMaterial(Material.STONE);
                }

                data.setDisplayName(section.getString("display-name", "<white>" + id));
                data.setLore(section.getStringList("lore"));

                ConfigurationSection enchSection = section.getConfigurationSection("enchantments");
                if (enchSection != null) {
                    Map<String, Integer> enchants = new LinkedHashMap<>();
                    for (String enchKey : enchSection.getKeys(false)) {
                        enchants.put(enchKey, enchSection.getInt(enchKey));
                    }
                    data.setEnchantments(enchants);
                }

                List<String> flagList = section.getStringList("flags");
                data.setFlags(new LinkedHashSet<>(flagList));
                data.setCustomModelData(section.getInt("custom-model-data", -1));
                data.setUnbreakable(section.getBoolean("unbreakable", false));
                data.setGlowing(section.getBoolean("glowing", false));
                data.setMaxDurability(section.getInt("max-durability", -1));

                ConfigurationSection attrSection = section.getConfigurationSection("attributes");
                if (attrSection != null) {
                    List<UItemData.AttributeData> attributes = new ArrayList<>();
                    for (String attrKey : attrSection.getKeys(false)) {
                        ConfigurationSection attr = attrSection.getConfigurationSection(attrKey);
                        if (attr == null) continue;
                        attributes.add(new UItemData.AttributeData(
                                attr.getString("attribute", ""),
                                attr.getDouble("amount", 0),
                                attr.getString("operation", "ADD_NUMBER"),
                                attr.getString("slot", "ANY")
                        ));
                    }
                    data.setAttributes(attributes);
                }

                data.setOraxenId(section.getString("oraxen-id", null));
                data.setItemsAdderId(section.getString("itemsadder-id", null));
                data.setExecutableItemsId(section.getString("executableitems-id", null));

                data.setRarity(section.getString("rarity", null));

                manager.registerItem(data);
                plugin.getLogger().info("  Loaded item: " + id);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading item '" + id + "'", e);
            }
        }
    }

    /**
     * Yêu cầu save async. Các lần gọi liên tiếp sẽ được gom lại thành snapshot mới nhất.
     */
    public void saveAll(UItemManager manager) {
        long requestId = requestCounter.incrementAndGet();
        // set() luôn thắng — save mới nhất sẽ được dùng
        pendingSaveRef.set(new PendingSave(manager.snapshotAll(), requestId));

        if (asyncSaveWorkerRunning.compareAndSet(false, true)) {
            SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
        }
    }

    /**
     * Ghi file ngay lập tức, dùng cho reload/disable để tránh mất dữ liệu.
     */
    public void saveAllNow(UItemManager manager) {
        long syncWriteId = requestCounter.incrementAndGet();
        minimumAllowedWriteId.set(syncWriteId);
        pendingSaveRef.set(null);
        writeSnapshot(manager.snapshotAll(), syncWriteId);
    }

    public void saveItem(UItemManager manager, UItemData data) {
        saveAll(manager);
    }

    private void flushPendingSaves() {
        try {
            while (true) {
                PendingSave pending = pendingSaveRef.get();
                if (pending == null) break;

                // compareAndSet đảm bảo chỉ worker này "sở hữu" save này.
                // Nếu main thread vừa set một save mới hơn (reference khác),
                // compareAndSet thất bại → loop lại lấy cái mới nhất, không bị mất.
                if (!pendingSaveRef.compareAndSet(pending, null)) continue;

                writeSnapshot(pending.snapshot(), pending.requestId());
            }
        } finally {
            asyncSaveWorkerRunning.set(false);
            // Sau khi set false, kiểm tra xem có save mới được thêm vào trong lúc
            // worker đang tắt không — nếu có thì spawn lại worker để xử lý.
            if (pendingSaveRef.get() != null && asyncSaveWorkerRunning.compareAndSet(false, true)) {
                SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
            }
        }
    }

    private void writeSnapshot(Map<String, UItemData> snapshot, long writeId) {
        if (snapshot == null || writeId < minimumAllowedWriteId.get()) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, UItemData> entry : snapshot.entrySet()) {
            String id = entry.getKey();
            UItemData data = entry.getValue();
            String path = "items." + id;

            Material material = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
            config.set(path + ".material", material.name());
            config.set(path + ".display-name", data.getDisplayName());
            config.set(path + ".lore", data.getLore());

            if (data.getEnchantments() != null && !data.getEnchantments().isEmpty()) {
                for (Map.Entry<String, Integer> ench : data.getEnchantments().entrySet()) {
                    config.set(path + ".enchantments." + ench.getKey(), ench.getValue());
                }
            }

            if (data.getFlags() != null && !data.getFlags().isEmpty()) {
                config.set(path + ".flags", new ArrayList<>(data.getFlags()));
            } else {
                config.set(path + ".flags", new ArrayList<>());
            }

            config.set(path + ".custom-model-data", data.getCustomModelData());
            config.set(path + ".unbreakable", data.isUnbreakable());
            config.set(path + ".glowing", data.isGlowing());
            config.set(path + ".max-durability", data.getMaxDurability());

            if (data.getAttributes() != null && !data.getAttributes().isEmpty()) {
                for (int i = 0; i < data.getAttributes().size(); i++) {
                    UItemData.AttributeData attr = data.getAttributes().get(i);
                    String attrPath = path + ".attributes.attr_" + i;
                    config.set(attrPath + ".attribute", attr.getAttribute());
                    config.set(attrPath + ".amount", attr.getAmount());
                    config.set(attrPath + ".operation", attr.getOperation());
                    config.set(attrPath + ".slot", attr.getSlot());
                }
            }

            if (data.getOraxenId() != null) {
                config.set(path + ".oraxen-id", data.getOraxenId());
            }
            if (data.getItemsAdderId() != null) {
                config.set(path + ".itemsadder-id", data.getItemsAdderId());
            }

            if (data.getRarity() != null) {
                config.set(path + ".rarity", data.getRarity());
            }

            if (data.getExecutableItemsId() != null) {
                config.set(path + ".executableitems-id", data.getExecutableItemsId());
            }
        }

        synchronized (fileLock) {
            if (writeId < minimumAllowedWriteId.get()) {
                return;
            }
            try {
                config.save(itemsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save items.yml!", e);
            }
        }
    }

    private record PendingSave(Map<String, UItemData> snapshot, long requestId) {
    }

    private void createDefaultFile() {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("""
                ╔══════════════════════════════════════════╗
                ║         UItem - Configuration            ║
                ║    Custom Item Plugin for Minecraft      ║
                ╠══════════════════════════════════════════╣
                ║  Display name & lore hỗ trợ MiniMessage  ║
                ║  Ví dụ: <red>Text, <bold>Bold,           ║
                ║  <gradient:#ff0000:#0000ff>Gradient       ║
                ╚══════════════════════════════════════════╝
                """);

        config.set("items.ruby_sword.material", "DIAMOND_SWORD");
        config.set("items.ruby_sword.display-name", "<gradient:#ff0000:#ff6600><bold>Ruby Sword</bold></gradient>");
        config.set("items.ruby_sword.lore", List.of(
                "<gray>Một thanh kiếm huyền thoại",
                "<gray>được rèn từ <red>Ruby <gray>nguyên chất",
                "",
                "<dark_gray>──────────────",
                "<yellow>Damage: <white>+15",
                "<yellow>Độ bền: <white>Vô hạn",
                "<dark_gray>──────────────",
                "",
                "<gold>✦ <gradient:#ffd700:#ffaa00>Legendary</gradient>"
        ));
        config.set("items.ruby_sword.enchantments.sharpness", 5);
        config.set("items.ruby_sword.enchantments.unbreaking", 3);
        config.set("items.ruby_sword.enchantments.fire_aspect", 2);
        config.set("items.ruby_sword.flags", List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        config.set("items.ruby_sword.custom-model-data", 10001);
        config.set("items.ruby_sword.unbreakable", true);
        config.set("items.ruby_sword.glowing", false);

        config.set("items.magic_apple.material", "GOLDEN_APPLE");
        config.set("items.magic_apple.display-name", "<light_purple><bold>Magic Apple</bold></light_purple>");
        config.set("items.magic_apple.lore", List.of(
                "<gray>Một quả táo ma thuật",
                "<green>Hồi phục tất cả HP"
        ));
        config.set("items.magic_apple.custom-model-data", -1);
        config.set("items.magic_apple.unbreakable", false);
        config.set("items.magic_apple.glowing", true);

        synchronized (fileLock) {
            try {
                config.save(itemsFile);
                plugin.getLogger().info("Created sample items.yml with 2 demo items.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create items.yml!", e);
            }
        }
    }
}
