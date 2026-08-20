package com.uitem.provider;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cache layer over ItemsAdder's item registry.
 * <p>
 * Two data tiers:
 * <ul>
 *   <li><b>Registry (API)</b> — {@code ItemsAdder.getAllItems()} decides which items exist.
 *       Uses reflection so the project compiles without ItemsAdder.</li>
 *   <li><b>YAML metadata</b> — pack config files only enrich entries with GUI info
 *       (category, hidden menu icons, permission). A broken YAML never removes an item.</li>
 * </ul>
 * The cache is built after {@code ItemsAdderLoadDataEvent} fires (first load and each
 * {@code /iareload}) and swapped atomically, so GUIs always read a consistent snapshot
 * and never touch the disk.
 */
public class IaRegistryService implements ExternalItemRegistry, Listener {

    private final Plugin plugin;

    // reflection handles
    private final Method getAllItems;        // static ItemsAdder.getAllItems() -> List<CustomStack>, null on old IA
    private final Method getNamespacedId;    // CustomStack#getNamespacedID() (fallback getId)
    private final boolean eventRegistered;

    private volatile boolean ready = false;
    private volatile List<ItemEntry> entries = List.of();
    private volatile Map<String, ItemEntry> byId = Map.of();

    public IaRegistryService(Plugin plugin) throws Exception {
        this.plugin = plugin;
        Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");

        Method allItems = null;
        try {
            Class<?> apiClass = Class.forName("dev.lone.itemsadder.api.ItemsAdder");
            allItems = apiClass.getMethod("getAllItems");
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // old IA — file-scan fallback will be used for enumeration
        }
        this.getAllItems = allItems;

        Method idGetter;
        try {
            idGetter = customStackClass.getMethod("getNamespacedID");
        } catch (NoSuchMethodException e) {
            idGetter = customStackClass.getMethod("getId");
        }
        this.getNamespacedId = idGetter;

        this.eventRegistered = registerLoadDataListener();
        if (!eventRegistered) {
            // No load event available (very old IA): build once, lazily, on first read.
            if (ItemsAdderProvider.isDebug()) {
                plugin.getLogger().warning("[UItem] IA ItemsAdderLoadDataEvent not found — cache will build lazily");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean registerLoadDataListener() {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class
                    .forName("dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent");
            Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR,
                    (listener, event) -> {
                        if (eventClass.isInstance(event)) {
                            if (ItemsAdderProvider.isDebug()) {
                                plugin.getLogger().info("[UItem] ItemsAdderLoadDataEvent received — rebuilding IA cache");
                            }
                            rebuild();
                        }
                    }, plugin);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public List<ItemEntry> entries() {
        if (!ready && !eventRegistered) {
            rebuild(); // lazy path for old IA without the load event
        }
        return entries;
    }

    public ItemEntry entry(String namespacedId) {
        return namespacedId == null ? null : byId.get(namespacedId);
    }

    @Override
    public synchronized void rebuild() {
        try {
            List<String> ids = enumerateIds();
            YamlMetadata meta = scanYamlMetadata();

            Map<String, ItemEntry> map = new LinkedHashMap<>();
            for (String id : ids) {
                String ns = namespaceOf(id);
                if (ns.startsWith("_")) continue; // internal IA items (_iainternal menu icons etc.)
                String category = meta.categoryByItem.getOrDefault(id, ItemEntry.DEFAULT_CATEGORY);
                boolean hidden = meta.iconIds.contains(id);
                int order = meta.orderByItem.getOrDefault(id, Integer.MAX_VALUE);
                String permission = meta.permissionByItem.get(id);
                map.put(id, new ItemEntry(id, category, hidden, order, permission));
            }

            List<ItemEntry> sorted = new ArrayList<>(map.values());
            sorted.sort(Comparator.comparing(ItemEntry::category)
                    .thenComparingInt(ItemEntry::order)
                    .thenComparing(ItemEntry::namespacedId));

            // atomic swap — GUIs mid-/iareload never see a half-built cache
            this.byId = Collections.unmodifiableMap(map);
            this.entries = Collections.unmodifiableList(sorted);
            this.ready = true;

            if (ItemsAdderProvider.isDebug()) {
                plugin.getLogger().info("[UItem] IA cache rebuilt: " + sorted.size() + " items ("
                        + meta.iconIds.size() + " marked hidden as menu icons)");
                for (String yamlId : meta.itemsDeclaredInYaml) {
                    if (!map.containsKey(yamlId) && !namespaceOf(yamlId).startsWith("_")) {
                        plugin.getLogger().warning("[UItem] Pack declares item '" + yamlId
                                + "' but ItemsAdder did not load it (pack error or disabled)");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[UItem] IA cache rebuild failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Tier 1: registry enumeration
    // ------------------------------------------------------------------

    private List<String> enumerateIds() throws Exception {
        if (getAllItems != null) {
            Object result = getAllItems.invoke(null);
            List<String> ids = new ArrayList<>();
            if (result instanceof Iterable<?> it) {
                for (Object customStack : it) {
                    if (customStack == null) continue;
                    Object id = getNamespacedId.invoke(customStack);
                    if (id != null && !id.toString().isBlank()) ids.add(id.toString());
                }
            }
            return ids;
        }
        // Old IA fallback: enumerate from YAML (best effort — same limits as before).
        return new ArrayList<>(scanYamlMetadata().itemsDeclaredInYaml);
    }

    private static String namespaceOf(String id) {
        int idx = id.indexOf(':');
        return idx < 0 ? "" : id.substring(0, idx).toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // Tier 2: YAML metadata (enrich only — never decides existence)
    // ------------------------------------------------------------------

    private static final class YamlMetadata {
        final Set<String> iconIds = new HashSet<>();
        final Map<String, String> categoryByItem = new HashMap<>();
        final Map<String, Integer> orderByItem = new HashMap<>();
        final Map<String, String> permissionByItem = new HashMap<>();
        final Set<String> itemsDeclaredInYaml = new HashSet<>();
    }

    private YamlMetadata scanYamlMetadata() {
        YamlMetadata meta = new YamlMetadata();
        Plugin ia = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (ia == null) return meta;
        File dataFolder = ia.getDataFolder();
        for (String sub : new String[]{"contents", "data" + File.separator + "items_packs", "data" + File.separator + "items", "items"}) {
            File dir = new File(dataFolder, sub);
            if (dir.isDirectory()) scanDir(dir, readNamespace(dir, dir.getName()), meta);
        }
        return meta;
    }

    private void scanDir(File dir, String ns, YamlMetadata meta) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDir(f, readNamespace(f, ns), meta);
            } else if (f.getName().endsWith(".yml") || f.getName().endsWith(".yaml")) {
                parseYaml(f, ns, meta);
            }
        }
    }

    /** Namespace from the folder's _config.yml / config.yml ({@code info.namespace}); inherit parent's otherwise. */
    private String readNamespace(File folder, String inherited) {
        for (String name : new String[]{"_config.yml", "config.yml", "_config.yaml", "config.yaml"}) {
            File cfg = new File(folder, name);
            if (!cfg.isFile()) continue;
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(cfg);
                String ns = yaml.getString("info.namespace");
                if (ns != null && !ns.isBlank()) return ns.trim();
            } catch (Exception ignored) {
            }
        }
        return inherited;
    }

    private void parseYaml(File file, String fallbackNs, YamlMetadata meta) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (Exception e) {
            return; // broken metadata never removes items — just skip the file
        }
        String ns = yaml.getString("info.namespace", fallbackNs);
        if (ns == null || ns.isBlank()) ns = fallbackNs;

        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null || item.getKeys(false).isEmpty()) continue;
                String id = ns + ":" + key;
                meta.itemsDeclaredInYaml.add(id);
                String perm = item.getString("permission");
                if (perm != null && !perm.isBlank()) meta.permissionByItem.put(id, perm.trim());
            }
        }

        ConfigurationSection categories = yaml.getConfigurationSection("categories");
        if (categories != null) {
            for (String catKey : categories.getKeys(false)) {
                ConfigurationSection cat = categories.getConfigurationSection(catKey);
                if (cat == null) continue;
                String catName = cat.getString("name", catKey);
                String icon = cat.getString("icon");
                if (icon != null && !icon.isBlank()) {
                    meta.iconIds.add(qualify(icon.trim(), ns));
                }
                List<String> catItems = cat.getStringList("items");
                for (int i = 0; i < catItems.size(); i++) {
                    String raw = catItems.get(i);
                    if (raw == null || raw.isBlank()) continue;
                    String id = qualify(raw.trim(), ns);
                    meta.categoryByItem.putIfAbsent(id, catName);
                    meta.orderByItem.putIfAbsent(id, i);
                }
            }
        }
    }

    /** Prefix bare ids with the file's namespace so they match registry ids. */
    private static String qualify(String id, String ns) {
        return id.indexOf(':') >= 0 ? id : ns + ":" + id;
    }
}
