package com.uitem.rarity;

import com.uitem.UItemPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class RarityConfig {

    private static final Map<String, RarityEntry> rarities = new LinkedHashMap<>();

    public static void load(UItemPlugin plugin) {
        rarities.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rarity");
        if (section == null) {
            createDefaults();
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection rs = section.getConfigurationSection(key);
            if (rs == null) continue;
            String name = key.toUpperCase(Locale.ROOT);
            String color = rs.getString("color", "<white>");
            String prefix = rs.getString("prefix", "");
            List<String> lore = rs.getStringList("lore");
            rarities.put(name, new RarityEntry(name, color, prefix, lore));
        }
    }

    private static void createDefaults() {
        rarities.put("COMMON", new RarityEntry("COMMON", "<gray>", "", List.of()));
        rarities.put("UNCOMMON", new RarityEntry("UNCOMMON", "<green>", "<green>", List.of(
                "<dark_gray>──────────────",
                "<green>Uncommon"
        )));
        rarities.put("RARE", new RarityEntry("RARE", "<blue>", "<blue>", List.of(
                "<dark_gray>──────────────",
                "<blue>Rare"
        )));
        rarities.put("EPIC", new RarityEntry("EPIC", "<dark_purple>", "<dark_purple>", List.of(
                "<dark_gray>──────────────",
                "<dark_purple>Epic"
        )));
        rarities.put("LEGENDARY", new RarityEntry("LEGENDARY", "<gold>", "<gold>", List.of(
                "<dark_gray>──────────────",
                "<gold>Legendary"
        )));
        rarities.put("ULTRA", new RarityEntry("ULTRA", "<red>", "<red>", List.of(
                "<dark_gray>──────────────",
                "<red>✦ ULTRA ✦"
        )));
    }

    public static RarityEntry get(String rarity) {
        if (rarity == null) return rarities.get("COMMON");
        return rarities.get(rarity.toUpperCase(Locale.ROOT));
    }

    public static Set<String> getKeys() {
        return rarities.keySet();
    }

    public static List<String> getRarityLore(String rarity) {
        RarityEntry entry = get(rarity);
        return entry != null ? entry.lore() : List.of();
    }

    public static String getRarityColor(String rarity) {
        RarityEntry entry = get(rarity);
        return entry != null ? entry.color() : "<white>";
    }

    public static String getRarityPrefix(String rarity) {
        RarityEntry entry = get(rarity);
        return entry != null ? entry.prefix() : "";
    }

    public record RarityEntry(String name, String color, String prefix, List<String> lore) {
    }
}
