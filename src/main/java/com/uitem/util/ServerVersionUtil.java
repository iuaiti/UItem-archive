package com.uitem.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Runtime Minecraft / Paper version helpers for the supported range 1.21.4 – 1.26.2.
 */
public final class ServerVersionUtil {

    public static final int MIN_MAJOR = 1;
    public static final int MIN_MINOR = 21;
    public static final int MIN_PATCH = 4;

    public static final int MAX_MAJOR = 1;
    public static final int MAX_MINOR = 26;
    public static final int MAX_PATCH = 2;

    private static final int[] PARSED = parseBukkitVersion();
    private static final Boolean ENTITY_EQUIPMENT_CHANGED = detectEntityEquipmentChangedEvent();

    private ServerVersionUtil() {
    }

    public static int major() {
        return PARSED[0];
    }

    public static int minor() {
        return PARSED[1];
    }

    public static int patch() {
        return PARSED[2];
    }

    public static String displayVersion() {
        return major() + "." + minor() + "." + patch();
    }

    public static boolean isSupportedRange() {
        return compareTo(MIN_MAJOR, MIN_MINOR, MIN_PATCH) >= 0
                && compareTo(MAX_MAJOR, MAX_MINOR, MAX_PATCH) <= 0;
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        return compareTo(major, minor, patch) >= 0;
    }

    public static boolean supportsEntityEquipmentChangedEvent() {
        return Boolean.TRUE.equals(ENTITY_EQUIPMENT_CHANGED);
    }

    /**
     * Default resource pack format for the running server when config does not override.
     */
    public static int defaultPackFormat() {
        if (isAtLeast(1, 26, 0)) {
            return 64;
        }
        if (isAtLeast(1, 21, 9)) {
            return 48;
        }
        if (isAtLeast(1, 21, 6)) {
            return 46;
        }
        if (isAtLeast(1, 21, 5)) {
            return 41;
        }
        return 34;
    }

    public static void applyHideAdditionalTooltip(ItemMeta meta) {
        if (meta == null) {
            return;
        }
        try {
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        } catch (IllegalArgumentException ignored) {
            // Older API builds without this flag
        }
    }

    private static int compareTo(int major, int minor, int patch) {
        if (PARSED[0] != major) {
            return Integer.compare(PARSED[0], major);
        }
        if (PARSED[1] != minor) {
            return Integer.compare(PARSED[1], minor);
        }
        return Integer.compare(PARSED[2], patch);
    }

    private static int[] parseBukkitVersion() {
        String raw = Bukkit.getBukkitVersion();
        int dash = raw.indexOf('-');
        if (dash > 0) {
            raw = raw.substring(0, dash);
        }
        String[] parts = raw.split("\\.");
        int major = parts.length > 0 ? parseInt(parts[0], 1) : 1;
        int minor = parts.length > 1 ? parseInt(parts[1], 21) : 21;
        int patch = parts.length > 2 ? parseInt(parts[2], 0) : 0;
        return new int[]{major, minor, patch};
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Boolean detectEntityEquipmentChangedEvent() {
        try {
            Class.forName("io.papermc.paper.event.entity.EntityEquipmentChangedEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}