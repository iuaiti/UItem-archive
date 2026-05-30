package com.uitem.integration;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

public class ExecutableItemsIntegration {

    private Class<?> executableItemsAPIClass;
    private Method getManagerMethod;
    private Method isValidIDMethod;
    private Method getItemByIdMethod;
    private Method getItemByStackMethod;
    private Method buildItemMethod;
    private Method getIdMethod;

    private static final String[] API_CLASS_PATHS = {
        "com.ssomar.score.api.executableitems.ExecutableItemsAPI",
        "com.ssomar.executableitems.api.ExecutableItemsAPI"
    };

    private static final String[] MANAGER_PATHS = {
        "com.ssomar.score.api.executableitems.ExecutableItemsManagerInterface",
        "com.ssomar.executableitems.api.ExecutableItemsManagerInterface"
    };

    private static final String[] ITEM_PATHS = {
        "com.ssomar.score.api.executableitems.ExecutableItemInterface",
        "com.ssomar.executableitems.api.ExecutableItemInterface"
    };

    public ExecutableItemsIntegration() throws Exception {
        this.executableItemsAPIClass = tryLoad(API_CLASS_PATHS);
        Class<?> managerInterface = tryLoad(MANAGER_PATHS);
        Class<?> itemInterface = tryLoad(ITEM_PATHS);

        this.getManagerMethod = executableItemsAPIClass.getMethod("getExecutableItemsManager");
        this.isValidIDMethod = managerInterface.getMethod("isValidID", String.class);
        this.getItemByIdMethod = managerInterface.getMethod("getExecutableItem", String.class);
        this.getItemByStackMethod = managerInterface.getMethod("getExecutableItem", ItemStack.class);
        this.buildItemMethod = itemInterface.getMethod("buildItem", int.class, Optional.class, Optional.class);
        this.getIdMethod = itemInterface.getMethod("getId");
    }

    private static Class<?> tryLoad(String[] paths) throws ClassNotFoundException {
        for (String path : paths) {
            try {
                return Class.forName(path);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException("Cannot find any of: " + String.join(", ", paths));
    }

    private Object getManager() throws Exception {
        return getManagerMethod.invoke(null);
    }

    public ItemStack getItem(String eiId) {
        if (eiId == null || eiId.isBlank()) return null;
        try {
            Object manager = getManager();
            Boolean valid = (Boolean) isValidIDMethod.invoke(manager, eiId);
            if (valid == null || !valid) return null;

            Optional<?> opt = (Optional<?>) getItemByIdMethod.invoke(manager, eiId);
            if (opt == null || opt.isEmpty()) return null;

            Object execItem = opt.get();
            Object result = buildItemMethod.invoke(execItem, 1, Optional.empty(), Optional.empty());
            if (result instanceof ItemStack itemStack) {
                return itemStack.clone();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasItem(String eiId) {
        if (eiId == null || eiId.isBlank()) return false;
        try {
            Object manager = getManager();
            Boolean valid = (Boolean) isValidIDMethod.invoke(manager, eiId);
            return valid != null && valid;
        } catch (Exception e) {
            return false;
        }
    }

    public String getExecutableItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            Object manager = getManager();
            Optional<?> opt = (Optional<?>) getItemByStackMethod.invoke(manager, item);
            if (opt == null || opt.isEmpty()) return null;

            Object execItem = opt.get();
            Object idResult = getIdMethod.invoke(execItem);
            return idResult != null ? idResult.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
