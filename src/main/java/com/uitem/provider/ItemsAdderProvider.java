package com.uitem.provider;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
        // Try multiple method names that ItemsAdder API might expose
        String[] candidates = {"getNamespacedIds", "getAllItems", "getItems", "getItemIds"};
        for (String name : candidates) {
            Method m = tryMethodOpt(customStackClass, name).orElse(null);
            if (m != null) {
                return buildListFn(m);
            }
        }
        // Fallback: scan ItemsAdder content directory
        Function<Void, List<String>> fileFallback = buildFileFallback();
        if (fileFallback != null) return fileFallback;
        return v -> List.of();
    }

    private Function<Void, List<String>> buildListFn(Method m) {
        return v -> {
            try {
                Object result = m.invoke(null);
                if (result == null) return List.of();
                Set<String> ids = new HashSet<>();
                if (result instanceof Map<?, ?> map) {
                    for (Object k : map.keySet()) {
                        if (k != null) ids.add(stripNamespace(k.toString()));
                    }
                } else if (result instanceof Collection<?> col) {
                    for (Object o : col) {
                        if (o != null) {
                            ids.add(stripNamespace(o.toString()));
                        }
                    }
                }
                List<String> out = new ArrayList<>(ids);
                out.removeIf(String::isBlank);
                Collections.sort(out);
                return out;
            } catch (Exception e) {
                return List.of();
            }
        };
    }

    /**
     * Fallback: scan ItemsAdder's content/data directories for item definitions.
     * IA stores items in YAML files under plugins/ItemsAdder/contents/.
     */
    private Function<Void, List<String>> buildFileFallback() {
        File pluginsDir = Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder();
        if (pluginsDir == null || !pluginsDir.isDirectory()) return null;
        // Explore common subdirectories: contents/, data/items/, items/
        File[] searchDirs = {
            new File(pluginsDir, "contents"),
            new File(pluginsDir, "data" + File.separator + "items"),
            new File(pluginsDir, "items")
        };
        List<File> ymlFiles = new ArrayList<>();
        for (File dir : searchDirs) {
            if (dir.isDirectory()) {
                collectYmlFiles(dir, ymlFiles);
            }
        }
        if (ymlFiles.isEmpty()) return null;
        return v -> {
            Set<String> ids = new HashSet<>();
            for (File f : ymlFiles) {
                String name = stripExtension(f.getName());
                if (name != null && !name.isBlank()) {
                    // The parent folder is often the namespace
                    String parent = f.getParentFile().getName();
                    ids.add(name);
                }
            }
            List<String> out = new ArrayList<>(ids);
            out.removeIf(String::isBlank);
            Collections.sort(out);
            return out;
        };
    }

    private void collectYmlFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectYmlFiles(f, result);
            } else if (f.getName().endsWith(".yml") || f.getName().endsWith(".yaml")) {
                result.add(f);
            }
        }
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
    private Object resolveCustomStack(String id) throws Exception {
        Object cs = getInstance.invoke(null, id);
        if (cs != null) return cs;
        if (id.indexOf(':') < 0) {
            cs = getInstance.invoke(null, "itemsadder:" + id);
            if (cs != null) return cs;
            // Some configs use "ia:" as the namespace alias.
            cs = getInstance.invoke(null, "ia:" + id);
            if (cs != null) return cs;
        }
        return null;
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
        return listIdsFn.apply(null);
    }

    @Override
    public ItemStack buildPreview(String id) {
        return getItem(id);
    }
}
