package com.uitem.potion;

import org.bukkit.Material;

public enum PotionKind {
    DRINK(Material.POTION),
    SPLASH(Material.SPLASH_POTION),
    LINGERING(Material.LINGERING_POTION);

    private final Material material;

    PotionKind(Material material) {
        this.material = material;
    }

    public Material material() {
        return material;
    }

    public PotionKind next() {
        PotionKind[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static PotionKind fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return DRINK;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DRINK;
        }
    }
}