package com.uitem.potion;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PotionEffectTypes {

    private static List<String> cachedKeys;

    private PotionEffectTypes() {
    }

    public static List<String> allKeys() {
        if (cachedKeys != null) {
            return cachedKeys;
        }
        List<String> keys = new ArrayList<>();
        for (PotionEffectType type : Registry.POTION_EFFECT_TYPE) {
            if (type == null) continue;
            NamespacedKey key = type.getKey();
            if (key != null) {
                keys.add(key.getKey().toUpperCase(Locale.ROOT));
            }
        }
        keys.sort(Comparator.naturalOrder());
        cachedKeys = List.copyOf(keys);
        return cachedKeys;
    }

    public static PotionEffectType resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        PotionEffectType fromKey = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(normalized));
        if (fromKey != null) {
            return fromKey;
        }
        try {
            return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(
                    raw.trim().toUpperCase(Locale.ROOT)));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String displayName(String typeKey) {
        if (typeKey == null) {
            return "?";
        }
        return typeKey.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}