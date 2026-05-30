package com.uitem.integration;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

public class ExecutableItemsIntegration {

    private final Object manager;
    private final Method isValidID;
    private final Method getItemById;
    private final Method getItemByStack;

    private static final String[] API_CLASS_PATHS = {
        "com.ssomar.score.api.executableitems.ExecutableItemsAPI",
        "com.ssomar.executableitems.api.ExecutableItemsAPI",
        "com.ssomar.score.api.ExecutableItemsAPI",
        "com.ssomar.executableitems.ExecutableItemsAPI"
    };

    public ExecutableItemsIntegration() throws Exception {
        Class<?> apiClass = tryLoad(API_CLASS_PATHS);
        Method getManager = apiClass.getMethod("getExecutableItemsManager");
        this.manager = getManager.invoke(null);

        Class<?> mgrClass = this.manager.getClass();
        this.isValidID = findMethod(mgrClass, "isValidID", String.class);
        this.getItemById = findMethod(mgrClass, "getExecutableItem", String.class);
        this.getItemByStack = findMethod(mgrClass, "getExecutableItem", ItemStack.class);
    }

    private static Class<?> tryLoad(String[] paths) throws ClassNotFoundException {
        for (String path : paths) {
            try {
                return Class.forName(path);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException("Cannot find any of: " + String.join(", ", paths));
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) throws NoSuchMethodException {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            for (Method m : clazz.getMethods()) {
                if (!m.getName().equals(name)) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != paramTypes.length) continue;
                boolean match = true;
                for (int i = 0; i < params.length; i++) {
                    if (!params[i].isAssignableFrom(paramTypes[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) return m;
            }
            throw e;
        }
    }

    public ItemStack getItem(String eiId) {
        if (eiId == null || eiId.isBlank()) return null;
        try {
            Object execItem = getExecItem(eiId);
            if (execItem == null) return null;
            return buildItemStack(execItem);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasItem(String eiId) {
        if (eiId == null || eiId.isBlank()) return false;
        try {
            Object execItem = getExecItem(eiId);
            return execItem != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getExecutableItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            Optional<?> opt = (Optional<?>) getItemByStack.invoke(manager, item);
            if (opt == null || opt.isEmpty()) return null;
            Object execItem = opt.get();
            Method idMethod = execItem.getClass().getMethod("getId");
            Object idResult = idMethod.invoke(execItem);
            return idResult != null ? idResult.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getExecItem(String eiId) throws Exception {
        Boolean valid = (Boolean) isValidID.invoke(manager, eiId);
        if (valid == null || !valid) return null;
        Optional<?> opt = (Optional<?>) getItemById.invoke(manager, eiId);
        return (opt == null || opt.isEmpty()) ? null : opt.get();
    }

    private ItemStack buildItemStack(Object execItem) throws Exception {
        Method build = findMethod(execItem.getClass(), "buildItem", int.class, Optional.class, Optional.class);
        Object result = build.invoke(execItem, 1, Optional.empty(), Optional.empty());
        if (result instanceof ItemStack itemStack) {
            return itemStack.clone();
        }
        return null;
    }
}
