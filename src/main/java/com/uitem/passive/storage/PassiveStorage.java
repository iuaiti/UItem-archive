package com.uitem.passive.storage;

import com.uitem.UItemPlugin;
import com.uitem.passive.ActionEntry;
import com.uitem.passive.ConditionEntry;
import com.uitem.passive.ConditionGroup;
import com.uitem.passive.LogicMode;
import com.uitem.passive.PassiveAdvancedSettings;
import com.uitem.passive.PassiveDefinition;
import com.uitem.passive.condition.ConditionType;
import com.uitem.passive.trigger.TriggerType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class PassiveStorage {

    private final UItemPlugin plugin;
    private final File folder;

    public PassiveStorage(UItemPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "passives");
    }

    public File folder() {
        return folder;
    }

    public void ensureFolder() {
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create passives folder: " + folder.getAbsolutePath());
        }
    }

    public List<PassiveDefinition> loadAll() {
        ensureFolder();
        List<PassiveDefinition> result = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return result;
        }
        for (File file : files) {
            try {
                PassiveDefinition def = loadFile(file);
                if (def != null) {
                    result.add(def);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load passive " + file.getName(), e);
            }
        }
        return result;
    }

    public PassiveDefinition loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("id", file.getName().replace(".yml", ""));
        PassiveDefinition def = new PassiveDefinition(id);
        def.setDisplayName(yaml.getString("display-name", id));
        def.setEnabled(yaml.getBoolean("enabled", true));

        ConfigurationSection when = yaml.getConfigurationSection("when");
        if (when != null) {
            def.setTrigger(TriggerType.fromKey(when.getString("trigger")));
        }

        ConfigurationSection iff = yaml.getConfigurationSection("if");
        if (iff != null) {
            LogicMode mode = LogicMode.fromString(iff.getString("mode"));
            List<ConditionEntry> entries = new ArrayList<>();
            List<Map<?, ?>> list = iff.getMapList("list");
            for (Map<?, ?> raw : list) {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<?, ?> e : raw.entrySet()) {
                    map.put(String.valueOf(e.getKey()), e.getValue());
                }
                String type = String.valueOf(map.getOrDefault("type", "chance"));
                map.remove("type");
                entries.add(ConditionEntry.deserialize(type, map));
            }
            def.setConditions(new ConditionGroup(mode, entries));
        }

        List<Map<?, ?>> then = yaml.getMapList("then");
        List<ActionEntry> actions = new ArrayList<>();
        for (Map<?, ?> raw : then) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                map.put(String.valueOf(e.getKey()), e.getValue());
            }
            actions.add(ActionEntry.deserialize(map));
        }
        def.setActions(actions);

        ConfigurationSection adv = yaml.getConfigurationSection("advanced");
        if (adv != null) {
            PassiveAdvancedSettings settings = new PassiveAdvancedSettings(
                    adv.getInt("cooldown-seconds", 0),
                    adv.getInt("delay-ticks", 0),
                    adv.getDouble("chance-percent", 100.0));
            def.setAdvanced(settings);
        }

        return def;
    }

    public void save(PassiveDefinition def) {
        ensureFolder();
        File file = new File(folder, def.id() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", 1);
        yaml.set("id", def.id());
        yaml.set("display-name", def.displayName());
        yaml.set("enabled", def.enabled());

        yaml.set("when.trigger", def.trigger().key());

        yaml.set("if.mode", def.conditions().mode().name());
        List<Map<String, Object>> condList = new ArrayList<>();
        for (ConditionEntry e : def.conditions().entries()) {
            condList.add(e.serializeMap());
        }
        yaml.set("if.list", condList);

        List<Map<String, Object>> actList = new ArrayList<>();
        for (ActionEntry a : def.actions()) {
            actList.add(a.serializeMap());
        }
        yaml.set("then", actList);

        PassiveAdvancedSettings adv = def.advanced();
        yaml.set("advanced.cooldown-seconds", adv.cooldownSeconds());
        yaml.set("advanced.delay-ticks", adv.delayTicks());
        yaml.set("advanced.chance-percent", adv.globalChancePercent());

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save passive " + def.id(), e);
        }
    }

    public void delete(String id) {
        File file = new File(folder, id + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete passive file: " + file.getAbsolutePath());
        }
    }

    public boolean exists(String id) {
        return new File(folder, id + ".yml").exists();
    }
}