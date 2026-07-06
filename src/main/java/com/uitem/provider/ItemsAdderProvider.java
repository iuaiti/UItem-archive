package com.uitem.provider;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * ItemsAdder provider. Uses reflection so the project compiles without ItemsAdder on the
 * classpath. Optional hook — fails quietly when ItemsAdder is missing.
 */
public class ItemsAdderProvider implements ItemProvider, BrowseableProvider {

    public static final String NAMESPACE = "ia";

    private final Class<?> customStackClass;
    private final Method getInstance;
    private final Method getItemStack;
    private final Method getName;
    private final Function<ItemStack, String> idFromStack;
    private final Function<Void, List<String>> listIdsFn;

    public ItemsAdderProvider() throws Exception {
        this.customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
        this.getInstance = customStackClass.getMethod("getInstance", String.class);
        this.getItemStack = customStackClass.getMethod("getItemStack");
        this.getName = tryMethodOpt(customStackClass, "getName").orElse(null);

        // Extractor from ItemStack
        Function<ItemStack, String> extractor;
        var getByItemStackOpt = tryMethodOpt(customStackClass, "getByItemStack", ItemStack.class);
        if (getByItemStackOpt.isPresent()) {
            Method getByItemStack = getByItemStackOpt.get();
            Method namespacedIdGetter = tryMethodOpt(customStackClass, "getNamespacedID")
                    .or(() -> tryMethodOpt(customStackClass, "getId"))
                    .orElse(getName);
            extractor = stack -> {
                try {
                    Object cs = getByItemStack.invoke(null, stack);
                    if (cs == null) return null;
                    Object r = namespacedIdGetter != null ? namespacedIdGetter.invoke(cs) : null;
                    return r == null ? null : r.toString();
                } catch (Exception e) {
                    return null;
                }
            };
        } else {
            extractor = stack -> null;
        }
        this.idFromStack = extractor;
        this.listIdsFn = buildListIdsFn();
    }

    private java.util.Optional<Method> tryMethodOpt(Class<?> c, String name) {
        try { return java.util.Optional.of(c.getMethod(name)); } catch (NoSuchMethodException e) {
            return java.util.Optional.empty();
        }
    }
    private java.util.Optional<Method> tryMethodOpt(Class<?> c, String name, Class<?> param) {
        try { return java.util.Optional.of(c.getMethod(name, param)); } catch (NoSuchMethodException e) {
            return java.util.Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Function<Void, List<String>> buildListIdsFn() {
        Function<Void, List<String>> fileFallback = buildFileFallback();
        if (fileFallback != null) {
            Bukkit.getLogger().info("[UItem] ItemsAdder listIds: using file fallback (directory scan)");
            return v -> {
                List<String> raw = fileFallback.apply(null);
                List<String> out = new ArrayList<>();
                for (String id : raw) {
                    try {
                        if (getInstance.invoke(null, id) != null) out.add(id);
                    } catch (Exception e) {
                        // skip unresolvable
                    }
                }
                int removed = raw.size() - out.size();
                if (removed > 0) {
                    Bukkit.getLogger().info("[UItem] IA file fallback: filtered " + removed + " unresolvable items, kept " + out.size());
                }
                return out;
            };
        }
        Bukkit.getLogger().warning("[UItem] ItemsAdder listIds: no files detected. Item list will be empty.");
        return v -> List.of();
    }

    private Function<Void, List<String>> buildListFn(Method m) {
        final String methodName = m.getName();
        return v -> {
            try {
                Object result = m.invoke(null);
                if (result == null) return List.of();
                Set<String> ids = new HashSet<>();
                if (result instanceof Map<?, ?> map) {
                    for (Object k : map.keySet()) {
                        if (k != null) {
                            String s = k.toString();
                            if (!s.isBlank()) ids.add(s);
                        }
                    }
                } else if (result instanceof Collection<?> col) {
                    for (Object o : col) {
                        if (o != null) {
                            String s = o.toString();
                            if (!s.isBlank()) ids.add(s);
                        }
                    }
                }
                List<String> out = new ArrayList<>(ids);
                out.removeIf(String::isBlank);
                Collections.sort(out);
                Bukkit.getLogger().info("[UItem] ItemsAdder " + methodName + " → " + out.size() + " items");
                return out;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[UItem] ItemsAdder " + methodName + " failed: " + e.getMessage());
                return List.of();
            }
        };
    }

    /**
     * Fallback: scan ItemsAdder's content/data directories for item definitions.
     * IA stores items in YAML files under plugins/ItemsAdder/contents/.
     * <p>
     * Changed: Previously it stripped file names to only the local id; now it
     * keeps the full namespace:id pair so that {@code getItem()} can use the correct
     * namespace when calling {@code CustomStack.getInstance}.
     */
    private Function<Void, List<String>> buildFileFallback() {
        File pluginsDir = Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder();
        if (pluginsDir == null || !pluginsDir.isDirectory()) return null;
        File[] searchDirs = {
            new File(pluginsDir, "contents"),
            new File(pluginsDir, "data" + File.separator + "items"),
            new File(pluginsDir, "items")
        };
        // Map of namespace (folder / config) → set of local id
        Map<String, Set<String>> nsToIds = new LinkedHashMap<>();
        for (File dir : searchDirs) {
            if (dir.isDirectory()) {
                scanNamespaceDir(dir, nsToIds);
            }
        }
        if (nsToIds.isEmpty()) return null;

        // Flatten to string "namespace:localId"
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : nsToIds.entrySet()) {
            String ns = entry.getKey();
            for (String local : entry.getValue()) {
                ids.add(ns + ":" + local);
            }
        }
        Collections.sort(ids);
        return v -> ids;
    }

    /**
     * Walk a contents directory to collect item YAML files.
     * For each file we check the parent-folder's info namespace (from an
     * adjacent _config.yml or config.yml) so items are resolved with the
     * correct namespace. Fallback namespace = the subfolder name.
     */
    private void scanNamespaceDir(File dir, Map<String, Set<String>> nsToIds) {
        File[] entries = dir.listFiles();
        if (entries == null) return;
        String folderNamespace = readNamespaceFromConfig(dir);
        for (File entry : entries) {
            if (entry.isDirectory()) {
                // Each subfolder may have its own namespace
                String subNs = readNamespaceFromConfig(entry);
                collectItemIds(entry, nsToIds, subNs);
            } else if (isItemDefinition(entry)) {
                String name = stripExtension(entry.getName());
                if (name != null && !name.isBlank()) {
                    nsToIds.computeIfAbsent(folderNamespace, k -> new HashSet<>()).add(name);
                }
            }
        }
    }

    private void collectItemIds(File dir, Map<String, Set<String>> nsToIds, String ns) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String subNs = readNamespaceFromConfig(f);
                collectItemIds(f, nsToIds, subNs);
            } else if (isItemDefinition(f)) {
                String name = stripExtension(f.getName());
                if (name != null && !name.isBlank()) {
                    nsToIds.computeIfAbsent(ns, k -> new HashSet<>()).add(name);
                }
            }
        }
    }

    /**
     * Read the {code namespace} from a _config.yml or config.yml in the folder.
     * IA puts a {@code info.namespace} key in the folder's _config.yml.
     */
    private String readNamespaceFromConfig(File folder) {
        if (folder == null || !folder.isDirectory()) return folderNamespaceFallback(folder);
        for (String name : new String[]{"_config.yml", "config.yml", "_config.yaml", "config.yaml"}) {
            File cfgFile = new File(folder, name);
            if (!cfgFile.isFile()) continue;
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(cfgFile);
                String ns = yaml.getString("info.namespace");
                if (ns != null && !ns.isBlank()) return ns.trim();
            } catch (Exception e) {
            }
        }
        return folderNamespaceFallback(folder);
    }

    private String folderNamespaceFallback(File folder) {
        return folder == null ? "itemsadder" : folder.getName();
    }

    private boolean isItemDefinition(File file) {
        String name = file.getName();
        return (name.endsWith(".yml") || name.endsWith(".yaml"))
                && !name.startsWith("_")
                && !name.equals("config.yml")
                && !name.equals("config.yaml");
    }

    private String stripExtension(String name) {
        if (name == null) return null;
        int idx = name.lastIndexOf('.');
        return idx < 0 ? name : name.substring(0, idx);
    }

    @Override public String getNamespace() { return NAMESPACE; }
    @Override public String getDisplayName() { return "ItemsAdder"; }
    @Override public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
    }

    @Override
    public boolean exists(String id) {
        if (id == null || id.isBlank()) return false;
        try {
            return resolveCustomStack(id) != null;
        } catch (Exception e) { return false; }
    }

    @Override
    public ItemStack getItem(String id) {
        try {
            Object cs = resolveCustomStack(id);
            if (cs == null) return null;
            Object result = getItemStack.invoke(cs);
            return result instanceof ItemStack is ? is.clone() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolve a CustomStack by id. ItemsAdder's {@code CustomStack.getInstance(String)}
     * expects a full namespaced id ({@code itemsadder:ruby_sword} or a custom namespace
     * configured in itemsadder config). When the user only passes the local id
     * ({@code ruby_sword}), we retry with the default {@code itemsadder:} prefix so that
     * the correct texture/model is loaded instead of falling back to vanilla.
     */
    private Object resolveCustomStack(String storeId) throws Exception {
        String iaNativeId = stripProviderPrefix(storeId);
        if (iaNativeId == null || iaNativeId.isBlank()) return null;

        Object cs = getInstance.invoke(null, iaNativeId);
        if (cs != null) {
            Bukkit.getLogger().info("[UItem] IA resolve OK: \"" + storeId + "\" → \"" + iaNativeId + "\"");
            return cs;
        }

        if (iaNativeId.indexOf(':') < 0) {
            cs = getInstance.invoke(null, "itemsadder:" + iaNativeId);
            if (cs != null) {
                Bukkit.getLogger().info("[UItem] IA resolve OK (retry itemsadder:): \"" + storeId + "\"");
                return cs;
            }
            cs = getInstance.invoke(null, "ia:" + iaNativeId);
            if (cs != null) {
                Bukkit.getLogger().info("[UItem] IA resolve OK (retry ia:): \"" + storeId + "\"");
                return cs;
            }
        }
        Bukkit.getLogger().warning("[UItem] IA resolve FAIL: \"" + storeId + "\" → \"" + iaNativeId + "\"");
        return null;
    }

    /**
     * Strip our own provider prefix ("ia" or "itemsadder") from a storage id
     * so that {@code CustomStack.getInstance} receives only the IA-native
     * namespace:id pair. Examples:
     * <ul>
     *   <li>{@code ia:myitem:rubysword} → {@code myitem:rubysword}
     *   <li>{@code ia:itemsadder:rubysword} → {@code itemsadder:rubysword}
     *   <li>{@code ia:rubysword} → {@code rubysword} (legacy single-segment)
     *   <li>{@code itemsadder:rubysword} → {@code rubysword}
     * </ul>
     */
    private String stripProviderPrefix(String storeId) {
        if (storeId == null || storeId.isBlank()) return null;
        int idx = storeId.indexOf(':');
        if (idx < 0) return storeId;
        String prefix = storeId.substring(0, idx).toLowerCase(Locale.ROOT);
        if (prefix.equals("ia") || prefix.equals("itemsadder")) {
            return storeId.substring(idx + 1);
        }
        return storeId;
    }

    public String extractLocalId(ItemStack stack) {
        if (stack == null) return null;
        String full = idFromStack.apply(stack);
        return stripNamespace(full);
    }

    /**
     * Return the FULL namespaced id (e.g. {@code itemsadder:ruby_sword}) returned by
     * ItemsAdder for the given stack, WITHOUT stripping the namespace. This is needed
     * because {@code CustomStack.getInstance(String)} expects the full namespaced id,
     * and stripping a custom namespace would break lookups.
     */
    public String extractFullId(ItemStack stack) {
        if (stack == null) return null;
        return idFromStack.apply(stack);
    }

    static String stripNamespace(String full) {
        if (full == null) return null;
        int idx = full.indexOf(':');
        return idx < 0 ? full : full.substring(idx + 1);
    }

    @Override
    public List<String> listIds() {
        try {
            List<String> result = listIdsFn.apply(null);
            int show = Math.min(result.size(), 10);
            Bukkit.getLogger().info("[UItem] IA listIds → " + result.size() + " items, first " + show + ": " + result.subList(0, show));
            return result;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[UItem] IA listIds failed: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public ItemStack buildPreview(String id) {
        Bukkit.getLogger().info("[UItem] IA buildPreview(\"" + id + "\")");
        return getItem(id);
    }
}
