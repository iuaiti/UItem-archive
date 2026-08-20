package com.uitem.provider;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * ItemsAdder provider. Uses reflection so the project compiles without ItemsAdder on the
 * classpath. Optional hook — fails quietly when ItemsAdder is missing.
 * <p>
 * Enumeration comes from the injected {@link IaRegistryService} cache (built from
 * ItemsAdder's own registry after {@code ItemsAdderLoadDataEvent}); this class only
 * resolves individual items via {@code CustomStack}.
 */
public class ItemsAdderProvider implements ItemProvider, BrowseableProvider {

    public static final String NAMESPACE = "ia";

    private static boolean debug = false;

    public static void setDebug(boolean v) { debug = v; }
    public static boolean isDebug() { return debug; }

    private final IaRegistryService registry;
    private final Method getInstance;
    private final Method getItemStack;
    private final Function<ItemStack, String> idFromStack;

    public ItemsAdderProvider(IaRegistryService registry) throws Exception {
        this.registry = registry;
        Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
        this.getInstance = customStackClass.getMethod("getInstance", String.class);
        this.getItemStack = customStackClass.getMethod("getItemStack");

        // Extractor from ItemStack
        Function<ItemStack, String> extractor;
        var getByItemStackOpt = tryMethodOpt(customStackClass, "getByItemStack", ItemStack.class);
        if (getByItemStackOpt.isPresent()) {
            Method getByItemStack = getByItemStackOpt.get();
            Method namespacedIdGetter = tryMethodOpt(customStackClass, "getNamespacedID")
                    .or(() -> tryMethodOpt(customStackClass, "getId"))
                    .or(() -> tryMethodOpt(customStackClass, "getName"))
                    .orElse(null);
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
    }

    private java.util.Optional<Method> tryMethodOpt(Class<?> c, String name, Class<?>... params) {
        try { return java.util.Optional.of(c.getMethod(name, params)); } catch (NoSuchMethodException e) {
            return java.util.Optional.empty();
        }
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
        if (cs != null) return cs;

        if (iaNativeId.indexOf(':') < 0) {
            cs = getInstance.invoke(null, "itemsadder:" + iaNativeId);
            if (cs != null) return cs;
            cs = getInstance.invoke(null, "ia:" + iaNativeId);
            if (cs != null) return cs;
        }
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
        if (!registry.isReady()) {
            if (debug) Bukkit.getLogger().info("[UItem] IA listIds: registry cache not ready yet");
        }
        List<String> ids = new ArrayList<>();
        for (ItemEntry entry : registry.entries()) {
            if (!entry.hidden()) ids.add(entry.namespacedId());
        }
        if (debug) Bukkit.getLogger().info("[UItem] IA listIds: " + ids.size() + " items");
        return ids;
    }

    @Override
    public ItemStack buildPreview(String id) {
        return getItem(id);
    }
}
