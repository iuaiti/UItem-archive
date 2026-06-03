package com.uitem.storage;

import com.uitem.UItemData;
import com.uitem.ability.ItemAbilityYaml;
import com.uitem.UItemPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Load/save one UItem from a YAML section (root of items/&lt;id&gt;.yml or items.&lt;id&gt; in items.yml).
 */
public final class ItemYamlSerializer {

    private ItemYamlSerializer() {
    }

    public static UItemData load(UItemPlugin plugin, String id, ConfigurationSection section) {
        if (section == null) return null;
        try {
            UItemData data = new UItemData(id);

            String materialStr = section.getString("material", "STONE");
            try {
                data.setMaterial(Material.valueOf(materialStr.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material '" + materialStr + "' for item '" + id + "'. Using STONE.");
                data.setMaterial(Material.STONE);
            }

            data.setDisplayName(section.getString("display-name", "<white>" + id));
            data.setLore(com.uitem.util.TextUtil.expandLoreLines(section.getStringList("lore")));

            ConfigurationSection enchSection = section.getConfigurationSection("enchantments");
            if (enchSection != null) {
                Map<String, Integer> enchants = new LinkedHashMap<>();
                for (String enchKey : enchSection.getKeys(false)) {
                    enchants.put(enchKey, enchSection.getInt(enchKey));
                }
                data.setEnchantments(enchants);
            }

            List<String> flagList = section.getStringList("flags");
            data.setFlags(new LinkedHashSet<>(flagList));
            data.setCustomModelData(section.getInt("custom-model-data", -1));
            data.setUnbreakable(section.getBoolean("unbreakable", false));
            data.setGlowing(section.getBoolean("glowing", false));
            data.setMaxDurability(section.getInt("max-durability", -1));

            ConfigurationSection attrSection = section.getConfigurationSection("attributes");
            if (attrSection != null) {
                List<UItemData.AttributeData> attributes = new ArrayList<>();
                for (String attrKey : attrSection.getKeys(false)) {
                    ConfigurationSection attr = attrSection.getConfigurationSection(attrKey);
                    if (attr == null) continue;
                    attributes.add(new UItemData.AttributeData(
                            attr.getString("attribute", ""),
                            attr.getDouble("amount", 0),
                            attr.getString("operation", "ADD_NUMBER"),
                            attr.getString("slot", "ANY")
                    ));
                }
                data.setAttributes(attributes);
            }

            data.setOraxenId(section.getString("oraxen-id", null));
            data.setNexoId(section.getString("nexo-id", null));
            data.setItemsAdderId(section.getString("itemsadder-id", null));
            data.setExecutableItemsId(section.getString("executableitems-id", null));
            data.setRarity(section.getString("rarity", null));

            ConfigurationSection rp = section.getConfigurationSection("resourcepack");
            if (rp != null) {
                data.setTexturePath(rp.getString("texture"));
                data.setModelParent(rp.getString("parent"));
                data.setTextureLayers(rp.getStringList("layers"));
                data.setExcludeFromPack(rp.getBoolean("exclude", false));
            }

            ConfigurationSection abilitiesSec = section.getConfigurationSection("abilities");
            if (abilitiesSec != null) {
                ItemAbilityYaml.loadInto(data, abilitiesSec);
            }

            return data;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading item '" + id + "'", e);
            return null;
        }
    }

    public static void save(UItemData data, YamlConfiguration config) {
        Material material = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
        config.set("material", material.name());
        config.set("display-name", data.getDisplayName());
        config.set("lore", data.getLore());

        if (data.getEnchantments() != null && !data.getEnchantments().isEmpty()) {
            for (Map.Entry<String, Integer> ench : data.getEnchantments().entrySet()) {
                config.set("enchantments." + ench.getKey(), ench.getValue());
            }
        } else {
            config.set("enchantments", null);
        }

        if (data.getFlags() != null && !data.getFlags().isEmpty()) {
            config.set("flags", new ArrayList<>(data.getFlags()));
        } else {
            config.set("flags", new ArrayList<>());
        }

        config.set("custom-model-data", data.getCustomModelData());
        config.set("unbreakable", data.isUnbreakable());
        config.set("glowing", data.isGlowing());
        config.set("max-durability", data.getMaxDurability());

        if (data.getAttributes() != null && !data.getAttributes().isEmpty()) {
            for (int i = 0; i < data.getAttributes().size(); i++) {
                UItemData.AttributeData attr = data.getAttributes().get(i);
                String attrPath = "attributes.attr_" + i;
                config.set(attrPath + ".attribute", attr.getAttribute());
                config.set(attrPath + ".amount", attr.getAmount());
                config.set(attrPath + ".operation", attr.getOperation());
                config.set(attrPath + ".slot", attr.getSlot());
            }
        } else {
            config.set("attributes", null);
        }

        if (data.getOraxenId() != null) {
            config.set("oraxen-id", data.getOraxenId());
        } else {
            config.set("oraxen-id", null);
        }
        if (data.getNexoId() != null) {
            config.set("nexo-id", data.getNexoId());
        } else {
            config.set("nexo-id", null);
        }
        if (data.getItemsAdderId() != null) {
            config.set("itemsadder-id", data.getItemsAdderId());
        } else {
            config.set("itemsadder-id", null);
        }
        if (data.getRarity() != null) {
            config.set("rarity", data.getRarity());
        } else {
            config.set("rarity", null);
        }
        if (data.getExecutableItemsId() != null) {
            config.set("executableitems-id", data.getExecutableItemsId());
        } else {
            config.set("executableitems-id", null);
        }

        if (data.getTexturePath() != null || data.getModelParent() != null
                || (data.getTextureLayers() != null && !data.getTextureLayers().isEmpty())
                || data.isExcludeFromPack()) {
            if (data.getTexturePath() != null) {
                config.set("resourcepack.texture", data.getTexturePath());
            }
            if (data.getModelParent() != null) {
                config.set("resourcepack.parent", data.getModelParent());
            }
            if (data.getTextureLayers() != null && !data.getTextureLayers().isEmpty()) {
                config.set("resourcepack.layers", data.getTextureLayers());
            }
            if (data.isExcludeFromPack()) {
                config.set("resourcepack.exclude", true);
            }
        } else {
            config.set("resourcepack", null);
        }

        if (!data.getAbilities().isEmpty() || !data.isAbilitiesEnabled()) {
            ItemAbilityYaml.saveFrom(data, config.createSection("abilities"));
        } else {
            config.set("abilities", null);
        }
    }

    public static String idFromFileName(String fileName) {
        if (fileName == null || !fileName.endsWith(".yml")) return null;
        return fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
    }

    public static File fileForId(File directory, String id) {
        return new File(directory, id.toLowerCase(Locale.ROOT) + ".yml");
    }
}