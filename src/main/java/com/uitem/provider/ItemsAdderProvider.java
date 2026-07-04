package com.uitem.provider;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
        Method m = tryMethodOpt(customStackClass, "getNamespacedIds").orElse(null);
        if (m != null) {
            return v -> {
                try {
                    Object result = m.invoke(null);
                    if (result instanceof java.util.Map<?, ?> map) {
                        List<String> out = new ArrayList<>();
                        for (Object k : map.keySet()) out.add(k == null ? null : k.toString());
                        return out;
                    }
                    if (result instanceof java.util.Collection<?> col) {
                        List<String> out = new ArrayList<>();
                        for (Object o : col) {
                            if (o != null) {
                                String s = o.toString();
                                int idx = s.indexOf(':');
                                out.add(idx < 0 ? s : s.substring(idx + 1));
                            }
                        }
                        return out;
                    }
                    return List.of();
                } catch (Exception e) {
                    return List.of();
                }
            };
        }
        return v -> List.of();
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
            return getInstance.invoke(null, id) != null;
        } catch (Exception e) { return false; }
    }

    @Override
    public ItemStack getItem(String id) {
        try {
            Object cs = getInstance.invoke(null, id);
            if (cs == null) return null;
            Object result = getItemStack.invoke(cs);
            return result instanceof ItemStack is ? is.clone() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractLocalId(ItemStack stack) {
        if (stack == null) return null;
        String full = idFromStack.apply(stack);
        return stripNamespace(full);
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
