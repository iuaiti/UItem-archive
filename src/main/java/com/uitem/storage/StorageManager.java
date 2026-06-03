package com.uitem.storage;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Loads/saves items from external folder ({@code items/<id>.yml}) or legacy {@code items.yml}.
 */
public class StorageManager {

    private static final List<String> DEFAULT_ITEM_RESOURCES = List.of(
            "items/ruby_sword.yml",
            "items/magic_apple.yml",
            "items/fire_blade.yml",
            "items/vial_blade.yml"
    );

    private final UItemPlugin plugin;
    private ExternalStorageSettings settings;

    private final Object fileLock = new Object();
    private final AtomicBoolean asyncSaveWorkerRunning = new AtomicBoolean(false);
    private final AtomicLong requestCounter = new AtomicLong();
    private final AtomicLong minimumAllowedWriteId = new AtomicLong();
    private final AtomicReference<PendingSave> pendingSaveRef = new AtomicReference<>();
    private final AtomicLong lastDebounceSchedule = new AtomicLong();
    private static final long DEBOUNCE_MS = 2000;

    public StorageManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.settings = new ExternalStorageSettings(plugin);
    }

    public void reloadSettings() {
        this.settings = new ExternalStorageSettings(plugin);
    }

    /**
     * Copy bundled example item files into the data folder when missing.
     */
    public void ensureDefaultExternalFiles() {
        if (!settings.useItemFolder()) {
            if (!settings.itemsLegacyFile().exists()) {
                createLegacyDefaultFile();
            }
            return;
        }
        if (!settings.itemsDirectory().exists() && !settings.itemsDirectory().mkdirs()) {
            plugin.getLogger().warning("Could not create items directory: " + settings.itemsDirectory().getAbsolutePath());
        }
        for (String resourcePath : DEFAULT_ITEM_RESOURCES) {
            File target = new File(plugin.getDataFolder(), resourcePath);
            if (!target.exists()) {
                plugin.saveResource(resourcePath, false);
            }
        }
        if (settings.importLegacyOnLoad() && settings.itemsLegacyFile().exists() && isDirectoryEmpty(settings.itemsDirectory())) {
            plugin.getLogger().info("[UItem] Importing items from legacy items.yml into items/ folder...");
            importLegacyItemsFile();
        }
    }

    public void loadAll(UItemManager manager) {
        reloadSettings();
        minimumAllowedWriteId.set(requestCounter.incrementAndGet());
        pendingSaveRef.set(null);
        manager.clearAll();

        if (settings.useItemFolder()) {
            if (!settings.itemsDirectory().exists()) {
                settings.itemsDirectory().mkdirs();
            }
            loadAllFromFolder(manager);
            if (settings.importLegacyOnLoad()) {
                loadLegacyItemsMerge(manager);
            }
        } else {
            loadAllFromLegacyFile(manager);
        }
    }

    private void loadAllFromFolder(UItemManager manager) {
        File[] files = settings.itemsDirectory().listFiles((dir, name) ->
                name != null && name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("No item files in " + settings.itemsDirectory().getName() + "/ — 0 items loaded.");
            return;
        }
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File file : files) {
            String id = ItemYamlSerializer.idFromFileName(file.getName());
            if (id == null || id.isBlank()) continue;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            UItemData data = ItemYamlSerializer.load(plugin, id, yaml);
            if (data != null) {
                manager.registerItem(data);
                plugin.getLogger().info("  Loaded item: " + id + " (" + file.getName() + ")");
            }
        }
    }

    private void loadAllFromLegacyFile(UItemManager manager) {
        File legacy = settings.itemsLegacyFile();
        if (!legacy.exists()) {
            createLegacyDefaultFile();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().info("No 'items' section in items.yml. Starting with 0 items.");
            return;
        }
        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(id);
            UItemData data = ItemYamlSerializer.load(plugin, id, section);
            if (data != null) {
                manager.registerItem(data);
                plugin.getLogger().info("  Loaded item: " + id);
            }
        }
    }

    /** Load ids from legacy file that are not already present (folder wins). */
    private void loadLegacyItemsMerge(UItemManager manager) {
        File legacy = settings.itemsLegacyFile();
        if (!legacy.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String id : itemsSection.getKeys(false)) {
            if (manager.getItem(id) != null) continue;
            ConfigurationSection section = itemsSection.getConfigurationSection(id);
            UItemData data = ItemYamlSerializer.load(plugin, id, section);
            if (data != null) {
                manager.registerItem(data);
                plugin.getLogger().info("  Loaded item (legacy): " + id);
            }
        }
    }

    private void importLegacyItemsFile() {
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(settings.itemsLegacyFile());
        ConfigurationSection itemsSection = legacy.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(id);
            if (section == null) continue;
            File target = ItemYamlSerializer.fileForId(settings.itemsDirectory(), id);
            if (target.exists()) continue;
            YamlConfiguration out = new YamlConfiguration();
            UItemData data = ItemYamlSerializer.load(plugin, id, section);
            if (data == null) continue;
            ItemYamlSerializer.save(data, out);
            try {
                out.save(target);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not import item '" + id + "' to folder", e);
            }
        }
    }

    public void saveAll(UItemManager manager) {
        long requestId = requestCounter.incrementAndGet();
        pendingSaveRef.set(new PendingSave(manager.snapshotAll(), requestId));
        if (asyncSaveWorkerRunning.compareAndSet(false, true)) {
            SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
        }
    }

    public void saveAllDebounced(UItemManager manager) {
        long now = System.currentTimeMillis();
        lastDebounceSchedule.set(now);
        saveAll(manager);
        long scheduleId = now;
        SchedulerUtil.runDelayedAsync(plugin, () -> {
            if (lastDebounceSchedule.get() == scheduleId) {
                if (pendingSaveRef.get() != null && asyncSaveWorkerRunning.compareAndSet(false, true)) {
                    SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
                }
            }
        }, DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

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
                if (!pendingSaveRef.compareAndSet(pending, null)) continue;
                writeSnapshot(pending.snapshot(), pending.requestId());
            }
        } finally {
            asyncSaveWorkerRunning.set(false);
            if (pendingSaveRef.get() != null && asyncSaveWorkerRunning.compareAndSet(false, true)) {
                SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
            }
        }
    }

    private void writeSnapshot(Map<String, UItemData> snapshot, long writeId) {
        if (snapshot == null || writeId < minimumAllowedWriteId.get()) {
            return;
        }
        synchronized (fileLock) {
            if (writeId < minimumAllowedWriteId.get()) {
                return;
            }
            if (settings.useItemFolder()) {
                writeSnapshotToFolder(snapshot, writeId);
            } else {
                writeSnapshotToLegacyFile(snapshot, writeId);
            }
        }
    }

    private void writeSnapshotToFolder(Map<String, UItemData> snapshot, long writeId) {
        if (!settings.itemsDirectory().exists()) {
            settings.itemsDirectory().mkdirs();
        }
        for (Map.Entry<String, UItemData> entry : snapshot.entrySet()) {
            if (writeId < minimumAllowedWriteId.get()) return;
            String id = entry.getKey().toLowerCase(Locale.ROOT);
            YamlConfiguration yaml = new YamlConfiguration();
            ItemYamlSerializer.save(entry.getValue(), yaml);
            try {
                yaml.save(ItemYamlSerializer.fileForId(settings.itemsDirectory(), id));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save item file: " + id + ".yml", e);
            }
        }
        File[] files = settings.itemsDirectory().listFiles((dir, name) ->
                name != null && name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = ItemYamlSerializer.idFromFileName(file.getName());
                if (id != null && !snapshot.containsKey(id)) {
                    if (!file.delete()) {
                        plugin.getLogger().warning("Could not delete removed item file: " + file.getName());
                    }
                }
            }
        }
    }

    private void writeSnapshotToLegacyFile(Map<String, UItemData> snapshot, long writeId) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, UItemData> entry : snapshot.entrySet()) {
            String path = "items." + entry.getKey();
            YamlConfiguration itemYaml = new YamlConfiguration();
            ItemYamlSerializer.save(entry.getValue(), itemYaml);
            for (String key : itemYaml.getKeys(true)) {
                if (itemYaml.isConfigurationSection(key)) continue;
                config.set(path + "." + key, itemYaml.get(key));
            }
        }
        try {
            config.save(settings.itemsLegacyFile());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save items.yml!", e);
        }
    }

    private static boolean isDirectoryEmpty(File directory) {
        if (!directory.isDirectory()) return true;
        File[] files = directory.listFiles((dir, name) -> name != null && name.endsWith(".yml"));
        return files == null || files.length == 0;
    }

    private record PendingSave(Map<String, UItemData> snapshot, long requestId) {
    }

    private void createLegacyDefaultFile() {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("""
                UItem legacy items.yml — prefer storage.items.type: folder and files in items/
                """);

        UItemData ruby = new UItemData("ruby_sword");
        ruby.setMaterial(org.bukkit.Material.DIAMOND_SWORD);
        ruby.setDisplayName("<gradient:#ff0000:#ff6600><bold>Ruby Sword</bold></gradient>");
        ruby.setLore(List.of(
                "<gray>Một thanh kiếm huyền thoại",
                "<gray>được rèn từ <red>Ruby <gray>nguyên chất"
        ));
        ruby.getEnchantments().put("sharpness", 5);
        ruby.setUnbreakable(true);
        ruby.setCustomModelData(10001);

        YamlConfiguration rubyYaml = new YamlConfiguration();
        ItemYamlSerializer.save(ruby, rubyYaml);
        copyRootToPath(config, "items.ruby_sword", rubyYaml);

        try {
            config.save(settings.itemsLegacyFile());
            plugin.getLogger().info("Created sample items.yml (legacy mode).");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create items.yml!", e);
        }
    }

    private static void copyRootToPath(YamlConfiguration target, String path, YamlConfiguration source) {
        for (String key : source.getKeys(true)) {
            if (source.isConfigurationSection(key)) continue;
            target.set(path + "." + key, source.get(key));
        }
    }
}