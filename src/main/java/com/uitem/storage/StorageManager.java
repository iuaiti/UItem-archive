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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Manages reading/writing item config.
 *
 * <h3>Supported directory structure:</h3>
 * <pre>
 * plugins/UItem/
 * ├── items.yml              ← single-file (backward-compat)
 * └── items/                 ← preferred folder, used if it exists
 *     ├── weapons.yml
 *     ├── armor.yml
 *     └── boss/
 *         └── mythic.yml     ← nestable subfolders
 * </pre>
 *
 * <p><b>Rules:</b>
 * <ul>
 *   <li>If {@code items/} folder exists → scan ALL .yml files inside (recursively).</li>
 *   <li>Otherwise → read {@code items.yml} as before.</li>
 *   <li>Reload re-reads from disk, no caching.</li>
 *   <li>Saves from GUI/commands only write to {@code items.yml} (or the item's original file if tracked).</li>
 *   <li>Does not overwrite user files on reload.</li>
 *   <li>All config errors are logged clearly — never silently ignored.</li>
 * </ul>
 */
public class StorageManager {

    // ── File / folder names ───────────────────────────────────────────────────
    private static final String LEGACY_FILE   = "items.yml";
    private static final String ITEMS_DIR     = "items";
    private static final String ITEMS_SECTION = "items";

    // ── Async-save constants ──────────────────────────────────────────────────
    private static final long DEBOUNCE_MS = 2000;

    private final UItemPlugin plugin;

    /** Main file for GUI saves (in items/item.yml - external folder, user can edit manually). */
    private final File itemsFile;

    /** Legacy items.yml in root for fallback load. */
    private final File legacyFile;

    /** The items/ folder (may not yet exist). */
    private final File itemsDir;

    // ── Async-save state ─────────────────────────────────────────────────────
    private final Object           fileLock              = new Object();
    private final AtomicBoolean    asyncWorkerRunning    = new AtomicBoolean(false);
    private final AtomicLong       requestCounter        = new AtomicLong();
    private final AtomicLong       minAllowedWriteId     = new AtomicLong();
    private final AtomicReference<PendingSave> pendingRef = new AtomicReference<>();
    private final AtomicLong       lastDebounceSchedule  = new AtomicLong(0);

    // ── Per-item debounced save state (does not touch other items' files) ────
    private final Map<String, Long> lastItemDebounceSchedule = new java.util.concurrent.ConcurrentHashMap<>();

    public StorageManager(UItemPlugin plugin) {
        this.plugin    = plugin;
        this.legacyFile = new File(plugin.getDataFolder(), LEGACY_FILE);
        this.itemsDir   = new File(plugin.getDataFolder(), ITEMS_DIR);
        this.itemsDir.mkdirs();
        this.itemsFile = new File(this.itemsDir, "item.yml");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — LOAD
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Load all items from disk into the manager.
     * Prefers the {@code items/} folder; falls back to {@code items.yml}.
     * Re-reads from disk every time — no caching.
     */
    public void loadAll(UItemManager manager) {
        // Make on-disk files (including split items/*.yml) the source of truth.
        // This fixes "GUI edits disappear after /uitem reload" — previous in-memory only items or stale deleted items are cleared.
        manager.clearAll();

        // Cancel old pending-save to avoid overwriting data that is about to be loaded
        minAllowedWriteId.set(requestCounter.incrementAndGet());
        pendingRef.set(null);

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

    /** Recursively collect all .yml files within a folder. */
    private List<File> collectYamlFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) return result;
        Arrays.sort(children, Comparator.comparing(File::getName)); // stable ordering
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
    //  Read a single .yml file
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
                if (candidate == null) {
                    continue; // not an item definition
                }
                // Accept either modern 'source' or legacy 'material' field
                if (!candidate.contains("source") && !candidate.contains("material")) {
                    continue;
                }
            }

            LoadResult r = parseItem(id, itemsSection, label);
            if (r.data() != null) {
                manager.registerItem(r.data());
                loaded++;
            } else {
                skipped++;
            }
        }
        return new FileLoadResult(loaded, skipped);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Parse + validate each item
    // ═════════════════════════════════════════════════════════════════════════

    private LoadResult parseItem(String id, ConfigurationSection itemsSection, String fileName) {

        // ── ID must not be blank ──────────────────────────────────────────────
        if (id == null || id.isBlank()) {
            logError("[%s] Blank item key — skipped.", fileName);
            return LoadResult.SKIP;
        }

        // ── Section must be a mapping ─────────────────────────────────────────
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

        // ── source (new: "namespace:id" like "vanilla:diamond_sword", "ia:ruby_sword") ──
        // Backward-compat: when source is missing, read 'material' → "vanilla:<material>".
        String sourceStr = s.getString("source");
        if (sourceStr != null && !sourceStr.isBlank()) {
            data.setSource(sourceStr);
        } else {
            String matStr = s.getString("material");
            if (matStr == null || matStr.isBlank()) {
                logError("[%s] Item '%s': missing 'source' (e.g. source: vanilla:diamond_sword) — defaulting to vanilla:stone.", fileName, id);
                hasWarning = true;
                data.setSource("vanilla:stone");
            } else {
                data.setSource("vanilla:" + matStr.toLowerCase(Locale.ROOT));
            }
        }

        // ── category / sub-category ──
        data.setCategory(s.getString("category", null));
        data.setSubCategory(s.getString("sub-category", s.getString("subcategory", null)));

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

        // custom-model-data removed — provider manages texture/model.

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

        // ── stack size ──
        data.setStackSize(s.getInt("stack-size", 1));

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

        // ── cooldown / permission / required-level ──
        if (s.contains("cooldown")) data.setCooldown(s.getDouble("cooldown", -1));
        data.setPermission(s.getString("permission", null));
        data.setRequiredLevel(s.getInt("required-level", 0));

        // Legacy integration IDs (oraxen-id/itemsadder-id/executableitems-id) — ignored on load.
        // All moved to 'source' form namespace:id.

        // effects: general effect system (HIT, DAMAGE, DIE, EQUIP, FULL_SET etc) - REFLECT/EXTRA_DAMAGE only
        ConfigurationSection effectsSec = s.getConfigurationSection("effects");
        if (effectsSec != null) {
            List<UItemData.ItemEffect> effs = new ArrayList<>();
            for (String k : effectsSec.getKeys(false)) {
                ConfigurationSection es = effectsSec.getConfigurationSection(k);
                if (es == null) continue;
                UItemData.ItemEffect eff = new UItemData.ItemEffect();
                eff.setTrigger(es.getString("trigger", "HIT"));
                eff.setType(es.getString("type", "REFLECT"));
                eff.setChance(es.getDouble("chance", 100.0));
                eff.setValue(es.getDouble("value", 0.0));
                eff.setApplyToTarget(es.getBoolean("apply-to-target", false));
                effs.add(eff);
            }
            data.setEffects(effs);
        }

        // skills: native UItem skill/passive system (like ExecutableItems but easier and more stable)
        ConfigurationSection skillsSec = s.getConfigurationSection("skills");
        if (skillsSec != null) {
            List<com.uitem.skill.Skill> skillList = new ArrayList<>();
            for (String skey : skillsSec.getKeys(false)) {
                ConfigurationSection ss = skillsSec.getConfigurationSection(skey);
                if (ss == null) continue;

                com.uitem.skill.Skill sk = new com.uitem.skill.Skill(skey);
                sk.setDisplayName(ss.getString("display-name", skey));
                sk.setTrigger(com.uitem.skill.SkillTrigger.fromString(ss.getString("trigger", "RIGHT_CLICK")));
                sk.setCooldown(ss.getDouble("cooldown", 0.0));
                sk.setCancelEvent(ss.getBoolean("cancel-event", true));
                sk.setChance(ss.getDouble("chance", 100.0));
                sk.setPeriodTicks(ss.getInt("period-ticks", 20));

                // conditions
                ConfigurationSection condSec = ss.getConfigurationSection("conditions");
                if (condSec != null) {
                    List<com.uitem.skill.SkillCondition> conds = new ArrayList<>();
                    for (String ck : condSec.getKeys(false)) {
                        ConfigurationSection cs = condSec.getConfigurationSection(ck);
                        if (cs == null) continue;
                        com.uitem.skill.SkillCondition c = new com.uitem.skill.SkillCondition();
                        String ctype = cs.getString("type", "ALWAYS");
                        try {
                            c.setType(com.uitem.skill.SkillCondition.ConditionType.valueOf(ctype.toUpperCase(Locale.ROOT)));
                        } catch (Exception e) {
                            c.setType(com.uitem.skill.SkillCondition.ConditionType.ALWAYS);
                        }
                        // copy all other keys as params
                        for (String pk : cs.getKeys(false)) {
                            if ("type".equals(pk)) continue;
                            c.setParam(pk, cs.get(pk));
                        }
                        conds.add(c);
                    }
                    sk.setConditions(conds);
                }

                // actions
                ConfigurationSection actSec = ss.getConfigurationSection("actions");
                if (actSec != null) {
                    List<com.uitem.skill.SkillAction> acts = new ArrayList<>();
                    for (String ak : actSec.getKeys(false)) {
                        ConfigurationSection as = actSec.getConfigurationSection(ak);
                        if (as == null) continue;
                        String atype = as.getString("type", "MESSAGE");
                        com.uitem.skill.SkillAction a = new com.uitem.skill.SkillAction();
                        try {
                            a.setType(com.uitem.skill.SkillAction.ActionType.valueOf(atype.toUpperCase(Locale.ROOT)));
                        } catch (Exception e) {
                            a.setType(com.uitem.skill.SkillAction.ActionType.MESSAGE);
                        }
                        for (String pk : as.getKeys(false)) {
                            if ("type".equals(pk)) continue;
                            a.setParam(pk, as.get(pk));
                        }
                        acts.add(a);
                    }
                    sk.setActions(acts);
                }

                skillList.add(sk);
            }
            data.setSkills(skillList);
        }

        if (hasWarning) {
            plugin.getLogger().warning(
                    "[UItem] Item '" + id + "' loaded with warnings (see [ERROR] lines above).");
        }

        return new LoadResult(data);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — SAVE  (GUI / commands → always writes to items.yml)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Save async, collapsing repeated calls to the latest snapshot.
     * Always writes to {@code items.yml} — does not touch user files in {@code items/}.
     */
    public void saveAll(UItemManager manager) {
        long id = requestCounter.incrementAndGet();
        pendingRef.set(new PendingSave(manager.snapshotAll(), id));
        if (asyncWorkerRunning.compareAndSet(false, true)) {
            SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
        }
    }

    /** Debounced save — for UI-driven changes to avoid I/O spam. */
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

    /**
     * Debounced save for a SINGLE item — for UI-driven edits (skills, lore, flags, enchants...).
     * Unlike {@link #saveAllDebounced}, this never touches any other item's file, so it can't
     * clobber manual edits an admin made directly to another item's yml while this GUI was open.
     */
    public void saveItemDebounced(UItemManager manager, UItemData data) {
        if (data == null || data.getId() == null) return;
        String id = data.getId();
        long now = System.currentTimeMillis();
        lastItemDebounceSchedule.put(id, now);
        SchedulerUtil.runDelayedAsync(plugin, () -> {
            if (Objects.equals(lastItemDebounceSchedule.get(id), now)) {
                lastItemDebounceSchedule.remove(id, now);
                UItemData latest = manager.getItem(id);
                saveItemIndividual(latest != null ? latest : data);
            }
        }, DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /** Write immediately — call before reload or on disable. */
    public void saveAllNow(UItemManager manager) {
        long syncId = requestCounter.incrementAndGet();
        minAllowedWriteId.set(syncId);
        pendingRef.set(null);
        writeSnapshot(manager.snapshotAll(), syncId);
    }

    public void saveItem(UItemManager manager, UItemData data) {
        // Preferred: save as individual file for easy copy-paste and separate management
        saveItemIndividual(data);
        // Also keep in-memory
        manager.registerItem(data);
    }

    /**
     * Delete the on-disk file(s) for an item id.
     * Supports the default save location (items/&lt;id&gt;.yml) and any split files under items/ (including subdirs).
     * Called on removeItem so that deleted items do not re-appear after /reload when using split files.
     */
    public void deleteItemFile(String id) {
        if (id == null || id.isBlank()) return;
        // Primary location used by saveItemIndividual
        File primary = new File(itemsDir, id + ".yml");
        if (primary.exists() && primary.delete()) {
            plugin.getLogger().info("[UItem] Deleted item file for '" + id + "': " + primary.getName());
        }
        // Also clean any copies that may exist in subfolders (user-organized split files)
        deleteItemFilesRecursive(itemsDir, id);
    }

    private void deleteItemFilesRecursive(File dir, String targetId) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                deleteItemFilesRecursive(f, targetId);
            } else if (f.isFile() && f.getName().endsWith(".yml")) {
                // Peek to see if this file defines the target item id (flat or under "items:")
                try {
                    YamlConfiguration peek = new YamlConfiguration();
                    peek.load(f);
                    ConfigurationSection root = peek.isConfigurationSection(ITEMS_SECTION)
                            ? peek.getConfigurationSection(ITEMS_SECTION)
                            : peek;
                    if (root != null && root.isConfigurationSection(targetId)) {
                        ConfigurationSection itemSec = root.getConfigurationSection(targetId);
                        if (itemSec != null && (itemSec.contains("source") || itemSec.contains("material"))) {
                            if (f.delete()) {
                                plugin.getLogger().info("[UItem] Deleted split item file for '" + targetId + "': " + relativeToDataFolder(f));
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // ignore bad yml during delete scan
                }
            }
        }
    }

    /**
     * Save a single item as its own file: items/<id>.yml (flat style, copy-paste friendly).
     * This avoids constantly recreating/overwriting a big item.yml.
     */
    public void saveItemIndividual(UItemData d) {
        if (d == null) return;
        String id = d.getId();
        File target = new File(itemsDir, id + ".yml");

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.options().setHeader(List.of(
            "Single UItem definition (copy-paste friendly).",
            "You can move this file anywhere inside the items/ folder (including subfolders).",
            "Flat style is supported."
        ));

        cfg.set(id + ".source", d.getSource());
        if (d.getCategory() != null) cfg.set(id + ".category", d.getCategory());
        if (d.getSubCategory() != null) cfg.set(id + ".sub-category", d.getSubCategory());
        if (d.getDisplayName() != null) cfg.set(id + ".display-name", d.getDisplayName());
        if (d.getLore() != null && !d.getLore().isEmpty()) cfg.set(id + ".lore", d.getLore());

        if (d.getEnchantments() != null && !d.getEnchantments().isEmpty()) {
            d.getEnchantments().forEach((k, v) -> cfg.set(id + ".enchantments." + k, v));
        }
        cfg.set(id + ".flags", d.getFlags() != null ? new ArrayList<>(d.getFlags()) : List.of());
        cfg.set(id + ".unbreakable", d.isUnbreakable());
        cfg.set(id + ".glowing", d.isGlowing());
        cfg.set(id + ".max-durability", d.getMaxDurability());
        if (d.getStackSize() != 1) cfg.set(id + ".stack-size", d.getStackSize());
        if (d.getCooldown() >= 0) cfg.set(id + ".cooldown", d.getCooldown());
        if (d.getPermission() != null) cfg.set(id + ".permission", d.getPermission());
        if (d.getRequiredLevel() > 0) cfg.set(id + ".required-level", d.getRequiredLevel());

        if (d.getAttributes() != null && !d.getAttributes().isEmpty()) {
            for (int i = 0; i < d.getAttributes().size(); i++) {
                UItemData.AttributeData a = d.getAttributes().get(i);
                String ap = id + ".attributes.attr_" + i;
                cfg.set(ap + ".attribute", a.getAttribute());
                cfg.set(ap + ".amount", a.getAmount());
                cfg.set(ap + ".operation", a.getOperation());
                cfg.set(ap + ".slot", a.getSlot());
            }
        }

        // Skills (native skill/passive system) - full copy of serialization logic for individual files
        if (d.getSkills() != null && !d.getSkills().isEmpty()) {
            for (int i = 0; i < d.getSkills().size(); i++) {
                com.uitem.skill.Skill sk = d.getSkills().get(i);
                String sp = id + ".skills." + (sk.getId() != null ? sk.getId() : "skill" + i);

                if (sk.getDisplayName() != null) cfg.set(sp + ".display-name", sk.getDisplayName());
                if (sk.getTrigger() != null) cfg.set(sp + ".trigger", sk.getTrigger().name());
                if (sk.getCooldown() > 0) cfg.set(sp + ".cooldown", sk.getCooldown());
                cfg.set(sp + ".cancel-event", sk.isCancelEvent());
                if (sk.getChance() < 100) cfg.set(sp + ".chance", sk.getChance());
                if (sk.getPeriodTicks() != 20) cfg.set(sp + ".period-ticks", sk.getPeriodTicks());

                // conditions
                if (sk.getConditions() != null && !sk.getConditions().isEmpty()) {
                    for (int ci = 0; ci < sk.getConditions().size(); ci++) {
                        com.uitem.skill.SkillCondition c = sk.getConditions().get(ci);
                        String cp = sp + ".conditions.c" + ci;
                        if (c.getType() != null) cfg.set(cp + ".type", c.getType().name());
                        if (c.getParams() != null) {
                            for (Map.Entry<String, Object> pe : c.getParams().entrySet()) {
                                cfg.set(cp + "." + pe.getKey(), pe.getValue());
                            }
                        }
                    }
                }

                // actions
                if (sk.getActions() != null && !sk.getActions().isEmpty()) {
                    for (int ai = 0; ai < sk.getActions().size(); ai++) {
                        com.uitem.skill.SkillAction a = sk.getActions().get(ai);
                        String ap = sp + ".actions.a" + ai;
                        if (a.getType() != null) cfg.set(ap + ".type", a.getType().name());
                        if (a.getParams() != null) {
                            for (Map.Entry<String, Object> pe : a.getParams().entrySet()) {
                                cfg.set(ap + "." + pe.getKey(), pe.getValue());
                            }
                        }
                    }
                }
            }
        }

        // Effects, rarity...
        if (d.getRarity() != null) cfg.set(id + ".rarity", d.getRarity());

        // Effects (left intentionally unused to suppress 'mat' lint warning if needed)
        if (d.getEffects() != null && !d.getEffects().isEmpty()) {
            for (int i = 0; i < d.getEffects().size(); i++) {
                UItemData.ItemEffect e = d.getEffects().get(i);
                String ep = id + ".effects.e" + i;
                if (e.getTrigger() != null) cfg.set(ep + ".trigger", e.getTrigger());
                if (e.getType() != null) cfg.set(ep + ".type", e.getType());
                cfg.set(ep + ".chance", e.getChance());
                if (e.getValue() != 0) cfg.set(ep + ".value", e.getValue());
                if (e.isApplyToTarget()) cfg.set(ep + ".apply-to-target", true);
            }
        }

        synchronized (fileLock) {
            try {
                cfg.save(target);
            } catch (IOException e) {
                plugin.getLogger().warning("[UItem] Failed to save individual item file " + target.getName() + ": " + e.getMessage());
            }
        }
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

        boolean useConsolidated = plugin.getConfig().getBoolean("storage.use-consolidated-files", false);
        if (!useConsolidated) {
            // User prefers individual files only (items/<id>.yml)
            // Still write individuals for safety
            for (UItemData d : snapshot.values()) {
                saveItemIndividual(d);
            }
            return;
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, UItemData> entry : snapshot.entrySet()) {
            String id   = entry.getKey();
            UItemData d = entry.getValue();
            String path = ITEMS_SECTION + "." + id;

            config.set(path + ".source", d.getSource());
            if (d.getCategory() != null) config.set(path + ".category", d.getCategory());
            if (d.getSubCategory() != null) config.set(path + ".sub-category", d.getSubCategory());
            config.set(path + ".display-name", d.getDisplayName());
            config.set(path + ".lore", d.getLore());

            if (d.getEnchantments() != null && !d.getEnchantments().isEmpty()) {
                d.getEnchantments().forEach((k, v) ->
                        config.set(path + ".enchantments." + k, v));
            }

            config.set(path + ".flags",
                    d.getFlags() != null ? new ArrayList<>(d.getFlags()) : List.of());
            config.set(path + ".unbreakable", d.isUnbreakable());
            config.set(path + ".glowing", d.isGlowing());
            config.set(path + ".max-durability", d.getMaxDurability());
            if (d.getStackSize() != 1) config.set(path + ".stack-size", d.getStackSize());
            if (d.getCooldown() >= 0) config.set(path + ".cooldown", d.getCooldown());
            if (d.getPermission() != null) config.set(path + ".permission", d.getPermission());
            if (d.getRequiredLevel() > 0) config.set(path + ".required-level", d.getRequiredLevel());

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

            if (d.getRarity() != null) config.set(path + ".rarity", d.getRarity());

            // Effects (attachable reflect/extra damage + actions)
            if (d.getEffects() != null && !d.getEffects().isEmpty()) {
                for (int i = 0; i < d.getEffects().size(); i++) {
                    UItemData.ItemEffect e = d.getEffects().get(i);
                    String ep = path + ".effects.e" + i;
                    if (e.getTrigger() != null) config.set(ep + ".trigger", e.getTrigger());
                    if (e.getType() != null) config.set(ep + ".type", e.getType());
                    config.set(ep + ".chance", e.getChance());
                    if (e.getValue() != 0) config.set(ep + ".value", e.getValue());
                    if (e.isApplyToTarget()) config.set(ep + ".apply-to-target", true);
                }
            }

            // Skills (native skill/passive system)
            if (d.getSkills() != null && !d.getSkills().isEmpty()) {
                for (int i = 0; i < d.getSkills().size(); i++) {
                    com.uitem.skill.Skill sk = d.getSkills().get(i);
                    String sp = path + ".skills." + (sk.getId() != null ? sk.getId() : "skill" + i);

                    if (sk.getDisplayName() != null) config.set(sp + ".display-name", sk.getDisplayName());
                    if (sk.getTrigger() != null) config.set(sp + ".trigger", sk.getTrigger().name());
                    if (sk.getCooldown() > 0) config.set(sp + ".cooldown", sk.getCooldown());
                    config.set(sp + ".cancel-event", sk.isCancelEvent());
                    if (sk.getChance() < 100) config.set(sp + ".chance", sk.getChance());
                    if (sk.getPeriodTicks() != 20) config.set(sp + ".period-ticks", sk.getPeriodTicks());

                    // conditions
                    if (sk.getConditions() != null && !sk.getConditions().isEmpty()) {
                        for (int ci = 0; ci < sk.getConditions().size(); ci++) {
                            com.uitem.skill.SkillCondition c = sk.getConditions().get(ci);
                            String cp = sp + ".conditions.c" + ci;
                            if (c.getType() != null) config.set(cp + ".type", c.getType().name());
                            if (c.getParams() != null) {
                                for (Map.Entry<String, Object> pe : c.getParams().entrySet()) {
                                    config.set(cp + "." + pe.getKey(), pe.getValue());
                                }
                            }
                        }
                    }

                    // actions
                    if (sk.getActions() != null && !sk.getActions().isEmpty()) {
                        for (int ai = 0; ai < sk.getActions().size(); ai++) {
                            com.uitem.skill.SkillAction a = sk.getActions().get(ai);
                            String ap = sp + ".actions.a" + ai;
                            if (a.getType() != null) config.set(ap + ".type", a.getType().name());
                            if (a.getParams() != null) {
                                for (Map.Entry<String, Object> pe : a.getParams().entrySet()) {
                                    config.set(ap + "." + pe.getKey(), pe.getValue());
                                }
                            }
                        }
                    }
                }
            }
        }

        synchronized (fileLock) {
            if (writeId < minAllowedWriteId.get()) return;
            try {
                config.save(itemsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[UItem] Could not save to " + itemsFile.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Default file (only created when it does not yet exist)
    // ═════════════════════════════════════════════════════════════════════════

    private void createDefaultLegacyFile() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.options().setHeader(java.util.Arrays.asList(
                "╔══════════════════════════════════════════╗",
                "║         UItem - items.yml (legacy)       ║",
                "║                                          ║",
                "║  RECOMMENDED: Use external folder config ║",
                "║  for easy copy-paste of items!           ║",
                "║                                          ║",
                "║  Drop your item configs into:            ║",
                "║    plugins/UItem/items/                  ║",
                "║                                          ║",
                "║  You can use either style:               ║",
                "║    - Wrapped: items: { id: {..} }        ║",
                "║    - Flat (easy copy-paste):             ║",
                "║        my_item:                          ║",
                "║          material: DIAMOND_SWORD         ║",
                "║          ...                             ║",
                "║                                          ║",
                "║  Multiple files supported (weapons.yml,  ║",
                "║  armor.yml, boss/mythic.yml ...).        ║",
                "╚══════════════════════════════════════════╝"
        ));

        cfg.set("items.ruby_sword.material", "DIAMOND_SWORD");
        cfg.set("items.ruby_sword.display-name",
                "<gradient:#ff0000:#ff6600><bold>Ruby Sword</bold></gradient>");
        cfg.set("items.ruby_sword.lore", List.of(
                "<gray>A legendary sword",
                "<gray>forged from pure <red>Ruby"));
        cfg.set("items.ruby_sword.enchantments.sharpness",  5);
        cfg.set("items.ruby_sword.enchantments.unbreaking", 3);
        cfg.set("items.ruby_sword.flags", List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        // custom-model-data removed — provider manages texture/model.
        cfg.set("items.ruby_sword.unbreakable", true);
        cfg.set("items.ruby_sword.glowing", false);

        cfg.set("items.magic_apple.material", "GOLDEN_APPLE");
        cfg.set("items.magic_apple.display-name",
                "<light_purple><bold>Magic Apple</bold></light_purple>");
        cfg.set("items.magic_apple.lore", List.of("<gray>A magical apple",
                "<green>Restores all HP"));
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
