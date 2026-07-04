package com.uitem.util;

import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.Locale;

/**
 * List of all Minecraft potion effects with display name and icon.
 * Used for UI editor and quick presets.
 */
public class PotionEffectRegistry {

    public static class PotionInfo {
        public final String key;           // e.g., "STRENGTH"
        public final String displayName;   // e.g., "Strength"
        public final String description;   // e.g., "Increases damage"
        
        public PotionInfo(String key, String displayName, String description) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
        }
    }

    // List of all potion effects in Minecraft 1.21.x
    private static final Map<String, PotionInfo> POTION_MAP = new LinkedHashMap<>();

    static {
        // Supports all potion types in 1.21.x
        register("SPEED", "Speed", "Increases movement speed");
        register("SLOWNESS", "Slowness", "Decreases movement speed");
        register("HASTE", "Haste", "Increases mining speed");
        register("MINING_FATIGUE", "Mining Fatigue", "Decreases mining speed");
        register("STRENGTH", "Strength", "Increases damage");
        register("INSTANT_HEALTH", "Instant Health", "Heals instantly");
        register("INSTANT_DAMAGE", "Instant Damage", "Deals damage instantly");
        register("JUMP_BOOST", "Jump Boost", "Jump higher");
        register("NAUSEA", "Nausea", "Distorts the screen");
        register("REGENERATION", "Regeneration", "Gradually restores health");
        register("RESISTANCE", "Resistance", "Reduces damage");
        register("FIRE_RESISTANCE", "Fire Resistance", "Immune to fire");
        register("WATER_BREATHING", "Water Breathing", "Breathe underwater");
        register("INVISIBILITY", "Invisibility", "Become invisible");
        register("BLINDNESS", "Blindness", "Cannot see far");
        register("NIGHT_VISION", "Night Vision", "See clearly in the dark");
        register("HUNGER", "Hunger", "Increases hunger level");
        register("WEAKNESS", "Weakness", "Reduces damage");
        register("POISON", "Poison", "Deals damage over time");
        register("WITHER", "Wither", "Deals damage over time (no loot drop)");
        register("HEALTH_BOOST", "Health Boost", "Increases max health");
        register("ABSORPTION", "Absorption", "Creates a temporary shield");
        register("SATURATION", "Saturation", "Restores hunger instantly");
        register("GLOWING", "Glowing", "Become glowing");
        register("LEVITATION", "Levitation", "Floats in the air");
        register("LUCK", "Luck", "Increases luck when fishing/looting");
        register("UNLUCK", "Bad Luck", "Decreases luck when fishing/looting");
        register("SLOW_FALLING", "Slow Falling", "Falls slowly");
        register("CONDUIT_POWER", "Conduit Power", "Increases speed, vision underwater, water breathing");
        register("DOLPHINS_GRACE", "Dolphin's Grace", "Increases swimming speed");
        register("BAD_OMEN", "Bad Omen", "Triggers a raid");
        register("HERO_OF_THE_VILLAGE", "Hero of the Village", "Increases discounts at traders");
        register("DARKNESS", "Darkness", "Darkens the area and allows mob spawning");
        register("WIND_CHARGED", "Wind Charged", "Creates lightning when struck");
        register("WEAVING", "Weaving", "Move faster");
    }

    private static void register(String key, String displayName, String description) {
        POTION_MAP.put(key, new PotionInfo(key, displayName, description));
    }

    /**
     * Get the list of all potion effects
     */
    public static Map<String, PotionInfo> getAllEffects() {
        return new LinkedHashMap<>(POTION_MAP);
    }

    /**
     * Get potion effect info from key
     */
    public static PotionInfo getInfo(String key) {
        return POTION_MAP.get(key.toUpperCase(Locale.ROOT));
    }

    /**
     * Check whether the potion effect is valid
     */
    public static boolean isValidEffect(String key) {
        try {
            Registry.EFFECT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all potion effect keys
     */
    public static List<String> getAllEffectKeys() {
        return new ArrayList<>(POTION_MAP.keySet());
    }

    /**
     * Get potion effect by index (for GUI pagination)
     */
    public static PotionInfo getByIndex(int index) {
        List<PotionInfo> list = new ArrayList<>(POTION_MAP.values());
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    /**
     * Total number of potion effects
     */
    public static int getTotalEffects() {
        return POTION_MAP.size();
    }
}
