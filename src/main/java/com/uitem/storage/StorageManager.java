package com.uitem.storage;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Quản lý đọc/ghi item config.
 *
 * <h3>Cấu trúc thư mục được hỗ trợ:</h3>
 * <pre>
 * plugins/UItem/
 * ├── items.yml              ← file đơn (backward-compat)
 * └── items/                 ← thư mục ưu tiên hơn nếu tồn tại
 *     ├── weapons.yml
 *     ├── armor.yml
 *     └── boss/
 *         └── mythic.yml     ← lồng thư mục tùy ý
 * </pre>
 *
 * <p><b>Quy tắc:</b>
 * <ul>
 *   <li>Nếu thư mục {@code items/} tồn tại → quét TẤT CẢ file .yml bên trong (đệ quy).</li>
 *   <li>Nếu không → đọc {@code items.yml} như cũ.</li>
 *   <li>Reload đọc lại từ disk, không dùng cache.</li>
 *   <li>Save từ GUI/lệnh chỉ ghi vào {@code items.yml} (hoặc file gốc của item đó nếu được track).</li>
 *   <li>Không ghi đè file người dùng khi reload.</li>
 *   <li>Mọi lỗi config đều được log rõ ràng — không bao giờ im lặng.</li>
 * </ul>
 */
public class StorageManager {

    // ── Tên file / thư mục ───────────────────────────────────────────────────
    private static final String LEGACY_FILE   = "items.yml";
    private static final String ITEMS_DIR     = "items";
    private static final String ITEMS_SECTION = "items";

    // ── Async-save constants ──────────────────────────────────────────────────
    private static final long DEBOUNCE_MS = 2000;

    private final UItemPlugin plugin;

    /** File ghi khi save qua GUI (luôn là items.yml để không đụng file user). */
    private final File legacyFile;

    /** Thư mục items/ (có thể chưa tồn tại). */
    private final File itemsDir;

    // ── Async-save state ─────────────────────────────────────────────────────
    private final Object           fileLock              = new Object();
    private final AtomicBoolean    asyncWorkerRunning    = new AtomicBoolean(false);
    private final AtomicLong       requestCounter        = new AtomicLong();
    private final AtomicLong       minAllowedWriteId     = new AtomicLong();
    private final AtomicReference<PendingSave> pendingRef = new AtomicReference<>();
    private final AtomicLong       lastDebounceSchedule  = new AtomicLong(0);

    public StorageManager(UItemPlugin plugin) {
        this.plugin    = plugin;
        this.legacyFile = new File(plugin.getDataFolder(), LEGACY_FILE);
        this.itemsDir   = new File(plugin.getDataFolder(), ITEMS_DIR);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — LOAD
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Load tất cả items từ disk vào manager.
     * Ưu tiên thư mục {@code items/}; fallback về {@code items.yml}.
     * Đọc lại từ disk mỗi lần — không dùng cache.
     */
    public void loadAll(UItemManager manager) {
        // Huỷ pending-save cũ để tránh ghi đè lên dữ liệu sắp load
        minAllowedWriteId.set(requestCounter.incrementAndGet());
        pendingRef.set(null);

        manager.clearAll();

        if (itemsDir.exists() && itemsDir.isDirectory()) {
            loadFromDirectory(manager);
        } else {
            loadFromLegacyFile(manager);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Load: thư mục items/
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFromDirectory(UItemManager manager) {
        List<File> yamlFiles = collectYamlFiles(itemsDir);
        if (yamlFiles.isEmpty()) {
            plugin.getLogger().info("[UItem] items/ folder is empty — no items loaded.");
            return;
        }

        plugin.getLogger().info("[UItem] Loading items from " + yamlFiles.size()
                + " file(s) in items/ ...");

        int totalLoaded = 0;
        int totalSkipped = 0;

        for (File file : yamlFiles) {
            String label = relativeToDataFolder(file); // ví dụ: "items/weapons.yml"
            FileLoadResult r = loadSingleYamlFile(file, label, manager);
            totalLoaded  += r.loaded();
            totalSkipped += r.skipped();
        }

        plugin.getLogger().info(String.format(
                "[UItem] Done — %d item(s) loaded, %d skipped.", totalLoaded, totalSkipped));
    }

    /** Thu thập đệ quy tất cả .yml trong thư mục. */
    private List<File> collectYamlFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) return result;
        Arrays.sort(children, Comparator.comparing(File::getName)); // thứ tự ổn định
        for (File f : children) {
            if (f.isDirectory()) {
                result.addAll(collectYamlFiles(f));
            } else if (f.isFile() && f.getName().endsWith(".yml")) {
                result.add(f);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Load: items.yml (legacy / fallback)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFromLegacyFile(UItemManager manager) {
        if (!legacyFile.exists()) {
            createDefaultLegacyFile();
        }
        FileLoadResult r = loadSingleYamlFile(legacyFile, LEGACY_FILE, manager);
        plugin.getLogger().info(String.format(
                "[UItem] %s loaded: %d item(s) OK, %d skipped.",
                LEGACY_FILE, r.loaded(), r.skipped()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Đọc một file .yml
    // ─────────────────────────────────────────────────────────────────────────

    private FileLoadResult loadSingleYamlFile(File file, String label, UItemManager manager) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException e) {
            logError("[%s] Cannot read file: %s", label, e.getMessage());
            return FileLoadResult.EMPTY;
        } catch (InvalidConfigurationException e) {
            logError("[%s] YAML syntax error — %s", label, yamlErrorDetail(e));
            logError("[%s] Fix the error and run /uitem reload.", label);
            return FileLoadResult.EMPTY;
        }

        // Support both:
        //  - Wrapped: items: { id: {...} }   (standard + legacy)
        //  - Flat (copy-paste friendly for external configs): id: { material: ..., ... } directly at root of the yml file
        // This makes it easy for users to drop single-item configs copied from elsewhere into items/ folder.
        ConfigurationSection itemsSection = null;
        boolean flatMode = false;

        if (config.isConfigurationSection(ITEMS_SECTION)) {
            itemsSection = config.getConfigurationSection(ITEMS_SECTION);
        } else {
            // Flat mode: scan root keys, only treat keys that look like item definitions (have 'material')
            itemsSection = config;
            flatMode = true;
        }

        if (itemsSection == null) {
            plugin.getLogger().warning("[UItem] [" + label + "] No item definitions found (neither '" + ITEMS_SECTION + "' section nor flat item entries).");
            return FileLoadResult.EMPTY;
        }

        int loaded = 0, skipped = 0;
        for (String id : itemsSection.getKeys(false)) {
            // In flat mode, skip keys that are clearly not items (e.g. comments or other top level)
            if (flatMode) {
                ConfigurationSection candidate = itemsSection.getConfigurationSection(id);
                if (candidate == null || !candidate.contains("material")) {
                    continue; // not an item definition
                }
            }

            LoadResult r = parseItem(id, itemsSection, label);
            if (r.data() != null) {
                // Kiểm tra trùng ID giữa các file
                if (manager.hasItem(id)) {
                    logError("[%s] Duplicate item id '%s' — already registered from another file. Skipped.",
                            label, id);
                    skipped++;
                } else {
                    manager.registerItem(r.data());
                    loaded++;
                }
            } else {
                skipped++;
            }
        }
        return new FileLoadResult(loaded, skipped);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Parse + validate từng item
    // ═════════════════════════════════════════════════════════════════════════

    private LoadResult parseItem(String id, ConfigurationSection itemsSection, String fileName) {

        // ── ID không rỗng ─────────────────────────────────────────────────────
        if (id == null || id.isBlank()) {
            logError("[%s] Blank item key — skipped.", fileName);
            return LoadResult.SKIP;
        }

        // ── Section phải là mapping ───────────────────────────────────────────
        if (!itemsSection.isConfigurationSection(id)) {
            logError("[%s] Invalid item '%s': not a YAML mapping (check indentation).", fileName, id);
            return LoadResult.SKIP;
        }

        ConfigurationSection s = itemsSection.getConfigurationSection(id);
        if (s == null) {
            logError("[%s] Invalid item '%s': null section.", fileName, id);
            return LoadResult.SKIP;
        }

        UItemData data = new UItemData(id);
        boolean hasWarning = false;

        // ── material ──────────────────────────────────────────────────────────
        String matStr = s.getString("material");
        if (matStr == null || matStr.isBlank()) {
            logError("[%s] Invalid item '%s' in items.yml: missing 'material' field.", fileName, id);
            data.setMaterial(Material.STONE);
            hasWarning = true;
        } else {
            Material mat = parseMaterial(matStr);
            if (mat == null) {
                logError("[%s] Invalid item '%s' in items.yml: material '%s' not found.", fileName, id, matStr);
                return LoadResult.SKIP;
            }
            if (!mat.isItem()) {
                logError("[%s] Invalid item '%s' in items.yml: material '%s' is not an item (e.g. AIR, WATER).",
                        fileName, id, matStr);
                return LoadResult.SKIP;
            }
            data.setMaterial(mat);
        }

        // ── display-name ──────────────────────────────────────────────────────
        data.setDisplayName(s.getString("display-name", "<white>" + id));

        // ── lore ──────────────────────────────────────────────────────────────
        data.setLore(s.getStringList("lore"));

        // ── enchantments ──────────────────────────────────────────────────────
        ConfigurationSection enchSection = s.getConfigurationSection("enchantments");
        if (enchSection != null) {
            Map<String, Integer> enchants = new LinkedHashMap<>();
            for (String enchKey : enchSection.getKeys(false)) {
                if (!enchSection.isInt(enchKey) && !enchSection.isLong(enchKey)) {
                    logError("[%s] Item '%s': enchantment '%s' has non-integer level — skipped.",
                            fileName, id, enchKey);
                    hasWarning = true;
                    continue;
                }
                int lvl = enchSection.getInt(enchKey);
                if (lvl < 1) {
                    logError("[%s] Item '%s': enchantment '%s' level must be >= 1 (got %d) — skipped.",
                            fileName, id, enchKey, lvl);
                    hasWarning = true;
                    continue;
                }
                enchants.put(enchKey.toLowerCase(Locale.ROOT), lvl);
            }
            data.setEnchantments(enchants);
        }

        // ── flags ─────────────────────────────────────────────────────────────
        Set<String> validFlags = new LinkedHashSet<>();
        for (String flagName : s.getStringList("flags")) {
            try {
                org.bukkit.inventory.ItemFlag.valueOf(flagName.toUpperCase(Locale.ROOT));
                validFlags.add(flagName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logError("[%s] Item '%s': unknown ItemFlag '%s' — skipped.", fileName, id, flagName);
                hasWarning = true;
            }
        }
        data.setFlags(validFlags);

        // ── custom-model-data ─────────────────────────────────────────────────
        if (s.contains("custom-model-data") && !s.isInt("custom-model-data")) {
            logError("[%s] Item '%s': 'custom-model-data' must be an integer — defaulting to -1.", fileName, id);
            hasWarning = true;
            data.setCustomModelData(-1);
        } else {
            data.setCustomModelData(s.getInt("custom-model-data", -1));
        }

        // ── unbreakable / glowing ─────────────────────────────────────────────
        data.setUnbreakable(s.getBoolean("unbreakable", false));
        data.setGlowing(s.getBoolean("glowing", false));

        // ── max-durability ────────────────────────────────────────────────────
        if (s.contains("max-durability") && !s.isInt("max-durability")) {
            logError("[%s] Item '%s': 'max-durability' must be an integer — defaulting to -1.", fileName, id);
            hasWarning = true;
            data.setMaxDurability(-1);
        } else {
            data.setMaxDurability(s.getInt("max-durability", -1));
        }

        // ── attributes ────────────────────────────────────────────────────────
        ConfigurationSection attrSection = s.getConfigurationSection("attributes");
        if (attrSection != null) {
            List<UItemData.AttributeData> attrs = new ArrayList<>();
            for (String attrKey : attrSection.getKeys(false)) {
                ConfigurationSection a = attrSection.getConfigurationSection(attrKey);
                if (a == null) {
                    logError("[%s] Item '%s': attribute '%s' is not a mapping — skipped.", fileName, id, attrKey);
                    hasWarning = true;
                    continue;
                }
                String attrName = a.getString("attribute");
                if (attrName == null || attrName.isBlank()) {
                    logError("[%s] Item '%s': attribute '%s' missing 'attribute' field — skipped.",
                            fileName, id, attrKey);
                    hasWarning = true;
                    continue;
                }
                String op = a.getString("operation", "ADD_NUMBER");
                if (!isValidOperation(op)) {
                    logError("[%s] Item '%s': attribute '%s' invalid operation '%s' " +
                                    "(valid: ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1) — skipped.",
                            fileName, id, attrKey, op);
                    hasWarning = true;
                    continue;
                }
                attrs.add(new UItemData.AttributeData(attrName, a.getDouble("amount", 0),
                        op, a.getString("slot", "ANY")));
            }
            data.setAttributes(attrs);
        }

        // ── rarity ────────────────────────────────────────────────────────────
        String rarity = s.getString("rarity", null);
        if (rarity != null && !rarity.isBlank()) {
            String up = rarity.toUpperCase(Locale.ROOT);
            if (!RarityConfig.getKeys().contains(up)) {
                logError("[%s] Item '%s': unknown rarity '%s' (known: %s) — rarity cleared.",
                        fileName, id, rarity, String.join(", ", RarityConfig.getKeys()));
                hasWarning = true;
            } else {
                data.setRarity(up);
            }
        }

        // ── integration IDs ───────────────────────────────────────────────────
        data.setOraxenId(s.getString("oraxen-id", null));
        data.setItemsAdderId(s.getString("itemsadder-id", null));
        data.setExecutableItemsId(s.getString("executableitems-id", null));

        // ── model (custom model for resource pack, Oraxen/ItemsAdder style) ──
        String modelRef = s.getString("model");
        if (modelRef != null && !modelRef.isBlank()) {
            data.setModel(modelRef);
        }

        // ── poison-on-hit ─────────────────────────────────────────────────────
        ConfigurationSection poisonSection = s.getConfigurationSection("poison-on-hit");
        if (poisonSection != null) {
            int    dur      = poisonSection.getInt("duration-ticks",  com.uitem.poison.PoisonEffect.DEFAULT_DURATION_TICKS);
            int    amp      = poisonSection.getInt("amplifier",        com.uitem.poison.PoisonEffect.DEFAULT_AMPLIFIER);
            int    maxStack = poisonSection.getInt("max-stacks",       com.uitem.poison.PoisonEffect.DEFAULT_MAX_STACKS);
            int    interval = poisonSection.getInt("tick-interval",    com.uitem.poison.PoisonEffect.DEFAULT_TICK_INTERVAL);
            double dmg      = poisonSection.getDouble("damage-per-tick", com.uitem.poison.PoisonEffect.DEFAULT_DAMAGE_PER_TICK);

            if (dur < 1)      { logError("[%s] Item '%s': poison duration-ticks < 1, defaulting to %d.", fileName, id, com.uitem.poison.PoisonEffect.DEFAULT_DURATION_TICKS); dur = com.uitem.poison.PoisonEffect.DEFAULT_DURATION_TICKS; hasWarning = true; }
            if (amp < 1)      { logError("[%s] Item '%s': poison amplifier < 1, defaulting to %d.", fileName, id, com.uitem.poison.PoisonEffect.DEFAULT_AMPLIFIER); amp = com.uitem.poison.PoisonEffect.DEFAULT_AMPLIFIER; hasWarning = true; }
            if (maxStack < 1) { logError("[%s] Item '%s': poison max-stacks < 1, defaulting to %d.", fileName, id, com.uitem.poison.PoisonEffect.DEFAULT_MAX_STACKS); maxStack = com.uitem.poison.PoisonEffect.DEFAULT_MAX_STACKS; hasWarning = true; }
            if (interval < 1) { logError("[%s] Item '%s': poison tick-interval < 1, defaulting to %d.", fileName, id, com.uitem.poison.PoisonEffect.DEFAULT_TICK_INTERVAL); interval = com.uitem.poison.PoisonEffect.DEFAULT_TICK_INTERVAL; hasWarning = true; }
            if (dmg < 0)      { logError("[%s] Item '%s': poison damage-per-tick < 0, defaulting to %.1f.", fileName, id, com.uitem.poison.PoisonEffect.DEFAULT_DAMAGE_PER_TICK); dmg = com.uitem.poison.PoisonEffect.DEFAULT_DAMAGE_PER_TICK; hasWarning = true; }

            data.setPoisonOnHit(new UItemData.PoisonConfig(dur, amp, maxStack, interval, dmg));
        }

        // effects: general potion/effect system (HIT, DAMAGE, EQUIP, FULL_SET etc)
        ConfigurationSection effectsSec = s.getConfigurationSection("effects");
        if (effectsSec != null) {
            List<UItemData.ItemEffect> effs = new ArrayList<>();
            for (String k : effectsSec.getKeys(false)) {
                ConfigurationSection es = effectsSec.getConfigurationSection(k);
                if (es == null) continue;
                UItemData.ItemEffect eff = new UItemData.ItemEffect();
                eff.setTrigger(es.getString("trigger", "HIT"));
                eff.setType(es.getString("type", "POTION"));
                eff.setPotion(es.getString("potion"));
                eff.setAmplifier(es.getInt("amplifier", 0));
                eff.setDurationTicks(es.getInt("duration-ticks", 60));
                eff.setChance(es.getDouble("chance", 100.0));
                eff.setValue(es.getDouble("value", 0.0));
                effs.add(eff);
            }
            data.setEffects(effs);
        }

        if (hasWarning) {
            plugin.getLogger().warning(
                    "[UItem] Item '" + id + "' loaded with warnings (see [ERROR] lines above).");
        }

        return new LoadResult(data);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — SAVE  (GUI / lệnh → luôn ghi vào items.yml)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Save async, gom nhiều lần gọi thành snapshot mới nhất.
     * Luôn ghi vào {@code items.yml} — không đụng file người dùng trong {@code items/}.
     */
    public void saveAll(UItemManager manager) {
        long id = requestCounter.incrementAndGet();
        pendingRef.set(new PendingSave(manager.snapshotAll(), id));
        if (asyncWorkerRunning.compareAndSet(false, true)) {
            SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
        }
    }

    /** Debounced save — dành cho UI-driven changes để tránh I/O spam. */
    public void saveAllDebounced(UItemManager manager) {
        long now = System.currentTimeMillis();
        lastDebounceSchedule.set(now);
        saveAll(manager);
        SchedulerUtil.runDelayedAsync(plugin, () -> {
            if (lastDebounceSchedule.get() == now) {
                if (pendingRef.get() != null && asyncWorkerRunning.compareAndSet(false, true)) {
                    SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
                }
            }
        }, DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /** Ghi ngay lập tức — dùng trước khi reload hoặc khi disable. */
    public void saveAllNow(UItemManager manager) {
        long syncId = requestCounter.incrementAndGet();
        minAllowedWriteId.set(syncId);
        pendingRef.set(null);
        writeSnapshot(manager.snapshotAll(), syncId);
    }

    public void saveItem(UItemManager manager, UItemData data) {
        saveAll(manager);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal flush / write
    // ─────────────────────────────────────────────────────────────────────────

    private void flushPendingSaves() {
        try {
            while (true) {
                PendingSave p = pendingRef.get();
                if (p == null) break;
                if (!pendingRef.compareAndSet(p, null)) continue;
                writeSnapshot(p.snapshot(), p.requestId());
            }
        } finally {
            asyncWorkerRunning.set(false);
            if (pendingRef.get() != null && asyncWorkerRunning.compareAndSet(false, true)) {
                SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
            }
        }
    }

    private void writeSnapshot(Map<String, UItemData> snapshot, long writeId) {
        if (snapshot == null || writeId < minAllowedWriteId.get()) return;

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, UItemData> entry : snapshot.entrySet()) {
            String id   = entry.getKey();
            UItemData d = entry.getValue();
            String path = ITEMS_SECTION + "." + id;

            Material mat = d.getMaterial() != null ? d.getMaterial() : Material.STONE;
            config.set(path + ".material", mat.name());
            config.set(path + ".display-name", d.getDisplayName());
            config.set(path + ".lore", d.getLore());

            if (d.getEnchantments() != null && !d.getEnchantments().isEmpty()) {
                d.getEnchantments().forEach((k, v) ->
                        config.set(path + ".enchantments." + k, v));
            }

            config.set(path + ".flags",
                    d.getFlags() != null ? new ArrayList<>(d.getFlags()) : List.of());
            config.set(path + ".custom-model-data", d.getCustomModelData());
            config.set(path + ".unbreakable", d.isUnbreakable());
            config.set(path + ".glowing", d.isGlowing());
            config.set(path + ".max-durability", d.getMaxDurability());

            if (d.getAttributes() != null && !d.getAttributes().isEmpty()) {
                for (int i = 0; i < d.getAttributes().size(); i++) {
                    UItemData.AttributeData a = d.getAttributes().get(i);
                    String ap = path + ".attributes.attr_" + i;
                    config.set(ap + ".attribute", a.getAttribute());
                    config.set(ap + ".amount",    a.getAmount());
                    config.set(ap + ".operation", a.getOperation());
                    config.set(ap + ".slot",      a.getSlot());
                }
            }

            if (d.getOraxenId()          != null) config.set(path + ".oraxen-id",           d.getOraxenId());
            if (d.getItemsAdderId()       != null) config.set(path + ".itemsadder-id",        d.getItemsAdderId());
            if (d.getExecutableItemsId()  != null) config.set(path + ".executableitems-id",   d.getExecutableItemsId());
            if (d.getRarity()             != null) config.set(path + ".rarity",               d.getRarity());
            if (d.getModel()              != null) config.set(path + ".model",                d.getModel());

            // Poison on-hit
            UItemData.PoisonConfig pc = d.getPoisonOnHit();
            if (pc != null) {
                String pp = path + ".poison-on-hit";
                config.set(pp + ".duration-ticks",   pc.getDurationTicks());
                config.set(pp + ".amplifier",         pc.getAmplifier());
                config.set(pp + ".max-stacks",        pc.getMaxStacks());
                config.set(pp + ".tick-interval",     pc.getTickInterval());
                config.set(pp + ".damage-per-tick",   pc.getDamagePerTick());
            }

            // Effects (attachable potions + actions)
            if (d.getEffects() != null && !d.getEffects().isEmpty()) {
                for (int i = 0; i < d.getEffects().size(); i++) {
                    UItemData.ItemEffect e = d.getEffects().get(i);
                    String ep = path + ".effects.e" + i;
                    if (e.getTrigger() != null) config.set(ep + ".trigger", e.getTrigger());
                    if (e.getType() != null) config.set(ep + ".type", e.getType());
                    if (e.getPotion() != null) config.set(ep + ".potion", e.getPotion());
                    config.set(ep + ".amplifier", e.getAmplifier());
                    config.set(ep + ".duration-ticks", e.getDurationTicks());
                    config.set(ep + ".chance", e.getChance());
                    if (e.getValue() != 0) config.set(ep + ".value", e.getValue());
                }
            }
        }

        synchronized (fileLock) {
            if (writeId < minAllowedWriteId.get()) return;
            try {
                config.save(legacyFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[UItem] Could not save " + LEGACY_FILE + ": " + e.getMessage(), e);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Default file (chỉ tạo khi chưa tồn tại)
    // ═════════════════════════════════════════════════════════════════════════

    private void createDefaultLegacyFile() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.options().header("""
                ╔══════════════════════════════════════════╗
                ║         UItem - items.yml (legacy)       ║
                ║                                          ║
                ║  RECOMMENDED: Use external folder config ║
                ║  for easy copy-paste of items!           ║
                ║                                          ║
                ║  Drop your item configs into:            ║
                ║    plugins/UItem/items/                  ║
                ║                                          ║
                ║  You can use either style:               ║
                ║    - Wrapped: items: { id: {..} }        ║
                ║    - Flat (easy copy-paste):             ║
                ║        my_item:                          ║
                ║          material: DIAMOND_SWORD         ║
                ║          ...                             ║
                ║                                          ║
                ║  Multiple files supported (weapons.yml,  ║
                ║  armor.yml, boss/mythic.yml ...).        ║
                ╚══════════════════════════════════════════╝
                """);

        cfg.set("items.ruby_sword.material", "DIAMOND_SWORD");
        cfg.set("items.ruby_sword.display-name",
                "<gradient:#ff0000:#ff6600><bold>Ruby Sword</bold></gradient>");
        cfg.set("items.ruby_sword.lore", List.of(
                "<gray>Một thanh kiếm huyền thoại",
                "<gray>được rèn từ <red>Ruby <gray>nguyên chất"));
        cfg.set("items.ruby_sword.enchantments.sharpness",  5);
        cfg.set("items.ruby_sword.enchantments.unbreaking", 3);
        cfg.set("items.ruby_sword.flags", List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        cfg.set("items.ruby_sword.custom-model-data", 10001);
        cfg.set("items.ruby_sword.unbreakable", true);
        cfg.set("items.ruby_sword.glowing", false);

        cfg.set("items.magic_apple.material", "GOLDEN_APPLE");
        cfg.set("items.magic_apple.display-name",
                "<light_purple><bold>Magic Apple</bold></light_purple>");
        cfg.set("items.magic_apple.lore", List.of("<gray>Một quả táo ma thuật",
                "<green>Hồi phục tất cả HP"));
        cfg.set("items.magic_apple.unbreakable", false);
        cfg.set("items.magic_apple.glowing", true);

        synchronized (fileLock) {
            try {
                cfg.save(legacyFile);
                plugin.getLogger().info("[UItem] Created sample " + LEGACY_FILE + ".");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[UItem] Could not create " + LEGACY_FILE + ": " + e.getMessage(), e);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private static Material parseMaterial(String name) {
        try { return Material.valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static boolean isValidOperation(String op) {
        if (op == null) return false;
        return switch (op.toUpperCase(Locale.ROOT)) {
            case "ADD_NUMBER", "ADD_SCALAR", "MULTIPLY_SCALAR_1" -> true;
            default -> false;
        };
    }

    private static String yamlErrorDetail(InvalidConfigurationException e) {
        String m = e.getMessage();
        if (m == null && e.getCause() != null) m = e.getCause().getMessage();
        return m != null ? m.trim() : "unknown YAML error";
    }

    private String relativeToDataFolder(File file) {
        String base = plugin.getDataFolder().getAbsolutePath();
        String full = file.getAbsolutePath();
        return full.startsWith(base) ? full.substring(base.length() + 1) : full;
    }

    private void logError(String fmt, Object... args) {
        plugin.getLogger().warning("[ERROR] " + String.format(fmt, args));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Records
    // ═════════════════════════════════════════════════════════════════════════

    private record PendingSave(Map<String, UItemData> snapshot, long requestId) {}

    private record LoadResult(UItemData data) {
        static final LoadResult SKIP = new LoadResult(null);
    }

    private record FileLoadResult(int loaded, int skipped) {
        static final FileLoadResult EMPTY = new FileLoadResult(0, 0);
    }
}
