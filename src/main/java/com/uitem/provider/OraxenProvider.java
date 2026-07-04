package com.uitem.provider;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Oraxen provider — reflection-based optional hook.
 */
public class OraxenProvider implements ItemProvider, BrowseableProvider {

    public static final String NAMESPACE = "oraxen";

    private final Class<?> oraxenItemsClass;
    private final Method existsMethod;
    private final Method getItemById;
    private final Function<ItemStack, String> idFromStack;
    private final Function<Void, List<String>> listIdsFn;
    private final Method itemBuilderBuild;

    public OraxenProvider() throws Exception {
        this.oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
        this.existsMethod = oraxenItemsClass.getMethod("exists", String.class);
        this.getItemById = oraxenItemsClass.getMethod("getItemById", String.class);
        this.itemBuilderBuild = null; // resolved per-builder at call time

        // Id extractor
        Function<ItemStack, String> extractor;
        var getIdOpt = tryMethodOpt(oraxenItemsClass, "getId", ItemStack.class);
        if (getIdOpt.isPresent()) {
            Method getId = getIdOpt.get();
            extractor = stack -> {
                try {
                    Object r = getId.invoke(null, stack);
                    return r == null ? null : r.toString();
                } catch (Exception e) { return null; }
            };
        } else {
            extractor = stack -> null;
        }
        this.idFromStack = extractor;
        this.listIdsFn = buildListIdsFn();
    }

    @SuppressWarnings("unchecked")
    private Function<Void, List<String>> buildListIdsFn() {
        // Try a few candidate methods for listing Oraxen items.
        var m = tryMethodOpt(oraxenItemsClass, "getNamespacedIds");
        var entrySetOpt = tryMethodOpt(oraxenItemsClass, "entrySet");
        var valuesOpt = tryMethodOpt(oraxenItemsClass, "values");
        Method listMethod = m.isPresent() ? m.get()
                : (entrySetOpt.isPresent() ? entrySetOpt.get()
                : (valuesOpt.isPresent() ? valuesOpt.get() : null));
        if (listMethod == null) return v -> List.of();
        return v -> {
            try {
                Object result = listMethod.invoke(null);
                if (result instanceof java.util.Map<?, ?> map) {
                    List<String> ids = new ArrayList<>();
                    for (Object k : map.keySet()) ids.add(k == null ? null : k.toString());
                    return ids;
                }
                if (result instanceof java.util.Collection<?> col) {
                    List<String> ids = new ArrayList<>();
                    for (Object o : col) ids.add(o == null ? null : o.toString());
                    return ids;
                }
                return List.of();
            } catch (Exception e) {
                return List.of();
            }
        };
    }

    @Override public String getNamespace() { return NAMESPACE; }
    @Override public String getDisplayName() { return "Oraxen"; }
    @Override public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("Oraxen") != null;
    }

    @Override
    public boolean exists(String id) {
        try {
            return Boolean.TRUE.equals(existsMethod.invoke(null, id));
        } catch (Exception e) { return false; }
    }

    @Override
    public ItemStack getItem(String id) {
        try {
            Object builder = getItemById.invoke(null, id);
            if (builder == null) return null;
            Method build = builder.getClass().getMethod("build");
            Object result = build.invoke(builder);
            return result instanceof ItemStack is ? is.clone() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractLocalId(ItemStack stack) {
        if (stack == null) return null;
        String full = idFromStack.apply(stack);
        return ItemsAdderProvider.stripNamespace(full);
    }

    @Override
    public List<String> listIds() {
        return listIdsFn.apply(null);
    }

    @Override
    public ItemStack buildPreview(String id) {
        return getItem(id);
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
}
