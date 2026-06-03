package com.uitem.potion;

import com.uitem.UItemPlugin;
import com.uitem.ability.ItemAbilityYaml;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class PotionStorage {

    private static final List<String> DEFAULT_RESOURCES = List.of("potions/heal_burst.yml");

    private final UItemPlugin plugin;
    private final File directory;

    public PotionStorage(UItemPlugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "potions");
    }

    public File directory() {
        return directory;
    }

    public void ensureDefaults() {
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("[UItem] Could not create potions folder.");
        }
        for (String path : DEFAULT_RESOURCES) {
            File target = new File(plugin.getDataFolder(), path);
            if (!target.exists()) {
                plugin.saveResource(path, false);
            }
        }
    }

    public void loadAll(PotionManager manager) {
        manager.clearAll();
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles((dir, name) ->
                name != null && name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            String id = idFromFile(file);
            if (id == null) continue;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = yaml.getConfigurationSection("potion");
            if (root == null) {
                root = yaml;
            }
            UPotionData data = loadData(id, root);
            if (data != null) {
                manager.register(data);
            }
        }
    }

    public void saveAll(PotionManager manager) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        for (UPotionData data : manager.getAll().values()) {
            saveOne(data);
        }
        File[] files = directory.listFiles((dir, name) ->
                name != null && name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = idFromFile(file);
                if (id != null && !manager.has(id)) {
                    if (!file.delete()) {
                        plugin.getLogger().warning("Could not delete potion file: " + file.getName());
                    }
                }
            }
        }
    }

    public void saveOne(UPotionData data) {
        if (data == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        saveData(data, yaml.createSection("potion"));
        try {
            yaml.save(new File(directory, data.getId().toLowerCase(Locale.ROOT) + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save potion " + data.getId(), e);
        }
    }

    public void deleteFile(String id) {
        File file = new File(directory, id.toLowerCase(Locale.ROOT) + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete potion file: " + file.getName());
        }
    }

    private static UPotionData loadData(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        UPotionData data = new UPotionData(id);
        data.setDisplayName(section.getString("display-name", data.getDisplayName()));
        data.setLore(section.getStringList("lore"));
        data.setKind(PotionKind.fromString(section.getString("kind", "DRINK")));
        if (section.contains("color-rgb")) {
            data.setColorRgb(section.getInt("color-rgb"));
        } else if (section.contains("color")) {
            data.setColorRgb(parseColor(section.getString("color"), data.getColorRgb()));
        }
        data.setGlowing(section.getBoolean("glowing", false));

        List<UPotionEffectEntry> effects = new ArrayList<>();
        ConfigurationSection effectsSec = section.getConfigurationSection("effects");
        if (effectsSec != null) {
            for (String key : effectsSec.getKeys(false)) {
                UPotionEffectEntry entry = UPotionEffectEntry.load(effectsSec.getConfigurationSection(key));
                if (entry != null) {
                    effects.add(entry);
                }
            }
        }
        data.setEffects(effects);

        ConfigurationSection abilitiesSec = section.getConfigurationSection("abilities");
        if (abilitiesSec != null) {
            ItemAbilityYaml.loadInto(data, abilitiesSec);
        }
        return data;
    }

    private static void saveData(UPotionData data, ConfigurationSection section) {
        section.set("display-name", data.getDisplayName());
        section.set("lore", data.getLore());
        section.set("kind", data.getKind().name());
        section.set("color-rgb", data.getColorRgb());
        section.set("glowing", data.isGlowing());
        ConfigurationSection effectsSec = section.createSection("effects");
        List<UPotionEffectEntry> effects = data.getEffects();
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).save(effectsSec.createSection("effect_" + i));
        }
        if (!data.getAbilities().isEmpty() || !data.isAbilitiesEnabled()) {
            ItemAbilityYaml.saveFrom(data, section.createSection("abilities"));
        } else {
            section.set("abilities", null);
        }
    }

    private static int parseColor(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            try {
                return Integer.parseInt(s.substring(1), 16) & 0xFFFFFF;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        if (s.contains(",")) {
            String[] parts = s.split(",");
            if (parts.length >= 3) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private static String idFromFile(File file) {
        String name = file.getName();
        if (!name.endsWith(".yml")) {
            return null;
        }
        return name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
    }
}