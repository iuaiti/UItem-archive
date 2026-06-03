package com.uitem.integration;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Tích hợp Nexo (fork Oraxen-style) qua reflection.
 */
public class NexoIntegration {

    private final Class<?> nexoItemsClass;

    public NexoIntegration() throws ClassNotFoundException {
        this.nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
    }

    public ItemStack getItem(String nexoId) {
        if (nexoId == null || nexoId.isBlank()) {
            return null;
        }
        try {
            Method existsMethod = nexoItemsClass.getMethod("exists", String.class);
            boolean exists = Boolean.TRUE.equals(existsMethod.invoke(null, nexoId));
            if (!exists) {
                return null;
            }

            Method itemFromId = nexoItemsClass.getMethod("itemFromId", String.class);
            Object itemBuilder = itemFromId.invoke(null, nexoId);
            if (itemBuilder == null) {
                return null;
            }

            Method buildMethod = itemBuilder.getClass().getMethod("build");
            Object result = buildMethod.invoke(itemBuilder);
            if (result instanceof ItemStack itemStack) {
                return itemStack.clone();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getIdFromItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        try {
            Method idFromItem = nexoItemsClass.getMethod("idFromItem", ItemStack.class);
            Object id = idFromItem.invoke(null, item);
            if (id instanceof String s && !s.isBlank()) {
                return s;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}