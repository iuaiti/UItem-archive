package com.uitem.gui;

import com.uitem.UItemPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads user-customizable GUI labels/icons from plugins/UItem/gui.yml. */
public final class GuiConfig {

    private static YamlConfiguration config;

    private GuiConfig() {}

    public static void init(UItemPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        reload(plugin);
    }

    public static void reload(UItemPlugin plugin) {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
    }

    public static Material material(String path, Material fallback) {
        String raw = config != null ? config.getString(path) : null;
        if (raw == null || raw.isBlank()) return fallback;
        try {
            Material material = Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return material.isItem() ? material : fallback;
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static String text(String path, String fallback) {
        return text(path, fallback, Map.of());
    }

    public static String text(String path, String fallback, Map<String, String> placeholders) {
        String value = config != null ? config.getString(path, fallback) : fallback;
        return replace(value != null ? value : fallback, placeholders);
    }

    public static int slot(String path, int fallback) {
        if (config == null || !config.isSet(path)) return fallback;
        return config.getInt(path, fallback);
    }

    public static int slot(String path, int fallback, int inventorySize) {
        int value = slot(path, fallback);
        return value >= 0 && value < inventorySize ? value : fallback;
    }

    public static List<Integer> slotList(String path, List<Integer> fallback) {
        if (config == null || !config.isSet(path)) return fallback;
        List<Integer> list = config.getIntegerList(path);
        return list.isEmpty() ? fallback : list;
    }

    public static List<String> lore(String path, List<String> fallback) {
        return lore(path, fallback, Map.of());
    }

    public static List<String> lore(String path, List<String> fallback, Map<String, String> placeholders) {
        List<String> lines = config != null ? config.getStringList(path) : List.of();
        if (lines.isEmpty()) lines = fallback;
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(replace(line, placeholders));
        }
        return out;
    }

    private static String replace(String input, Map<String, String> placeholders) {
        String out = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            out = out.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return out;
    }
}
