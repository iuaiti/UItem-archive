package com.uitem.tooltip;

import com.uitem.UItemPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads tooltip.yml — controls how Attributes and Stats are rendered as lore
 * lines (both have no built-in vanilla tooltip once UItem overrides the item's
 * lore, so this is the only place they show up to players).
 * Follows the same dedicated-file pattern as {@link com.uitem.rarity.RarityConfig}.
 */
public final class TooltipConfig {

    private static UItemPlugin plugin;
    private static File tooltipFile;

    private static boolean attributesEnabled = true;
    private static String attributesHeader = "";
    private static String attributesLine = "<gray>%name%: <white>+%amount% <gray>(%slot%)";
    private static String attributesFooter = "";

    private static boolean statsEnabled = true;
    private static String statsHeader = "";
    private static String statsLine = "<gray>%name%: <white>+%amount%%suffix% <gray>(%slot%)";
    private static String statsFooter = "";

    private static final Map<String, String> attributeNames = new LinkedHashMap<>();
    private static final Map<String, String> statNames = new LinkedHashMap<>();

    private TooltipConfig() {
    }

    public static void load(UItemPlugin pluginInstance) {
        plugin = pluginInstance;

        tooltipFile = new File(plugin.getDataFolder(), "tooltip.yml");
        if (!tooltipFile.exists()) {
            plugin.saveResource("tooltip.yml", false);
            plugin.getLogger().info("[UItem] Created default tooltip.yml");
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(tooltipFile);

        attributesEnabled = cfg.getBoolean("attributes.enabled", true);
        attributesHeader = cfg.getString("attributes.header", "");
        attributesLine = cfg.getString("attributes.line", attributesLine);
        attributesFooter = cfg.getString("attributes.footer", "");

        statsEnabled = cfg.getBoolean("stats.enabled", true);
        statsHeader = cfg.getString("stats.header", "");
        statsLine = cfg.getString("stats.line", statsLine);
        statsFooter = cfg.getString("stats.footer", "");

        attributeNames.clear();
        var attrSection = cfg.getConfigurationSection("attribute-names");
        if (attrSection != null) {
            for (String key : attrSection.getKeys(false)) {
                attributeNames.put(key.toLowerCase(Locale.ROOT), attrSection.getString(key, key));
            }
        }

        statNames.clear();
        var statSection = cfg.getConfigurationSection("stat-names");
        if (statSection != null) {
            for (String key : statSection.getKeys(false)) {
                statNames.put(key.toUpperCase(Locale.ROOT), statSection.getString(key, key));
            }
        }
    }

    public static void reload() {
        if (plugin != null) load(plugin);
    }

    public static boolean isAttributesEnabled() {
        return attributesEnabled;
    }

    public static String getAttributesHeader() {
        return attributesHeader;
    }

    public static String getAttributesLine() {
        return attributesLine;
    }

    public static String getAttributesFooter() {
        return attributesFooter;
    }

    public static boolean isStatsEnabled() {
        return statsEnabled;
    }

    public static String getStatsHeader() {
        return statsHeader;
    }

    public static String getStatsLine() {
        return statsLine;
    }

    public static String getStatsFooter() {
        return statsFooter;
    }

    /** Friendly display name for an attribute key (e.g. "generic_attack_damage" -> "Attack Damage"). */
    public static String getAttributeDisplayName(String key) {
        if (key == null) return "";
        String friendly = attributeNames.get(key.toLowerCase(Locale.ROOT));
        return friendly != null ? friendly
                : key.replace("generic_", "").replace("_", " ");
    }

    /** Friendly display name for a Stat enum key (e.g. "CRIT_CHANCE" -> "Crit Chance"). */
    public static String getStatDisplayName(String key) {
        if (key == null) return "";
        String friendly = statNames.get(key.toUpperCase(Locale.ROOT));
        return friendly != null ? friendly : key.replace("_", " ");
    }

    public static File getTooltipFile() {
        return tooltipFile;
    }
}
