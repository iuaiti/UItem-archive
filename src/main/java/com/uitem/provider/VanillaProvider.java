package com.uitem.provider;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Vanilla provider — always available. Maps {@code vanilla:<material_name>}
 * to a plain {@link ItemStack}.
 */
public class VanillaProvider implements ItemProvider {

    public static final String NAMESPACE = "vanilla";

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getDisplayName() {
        return "Vanilla";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean exists(String id) {
        if (id == null || id.isBlank()) return false;
        return parseMaterial(id) != null;
    }

    @Override
    public ItemStack getItem(String id) {
        Material mat = parseMaterial(id);
        if (mat == null || !mat.isItem()) return null;
        return new ItemStack(mat);
    }

    private Material parseMaterial(String id) {
        String name = id.trim().toUpperCase(Locale.ROOT);
        try {
            Material m = Material.valueOf(name);
            return m.isItem() ? m : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
