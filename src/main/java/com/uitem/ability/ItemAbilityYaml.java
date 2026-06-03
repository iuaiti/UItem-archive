package com.uitem.ability;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class ItemAbilityYaml {

    private ItemAbilityYaml() {
    }

    public static void loadInto(AbilityHolder data, ConfigurationSection section) {
        if (data == null || section == null) {
            return;
        }
        data.setAbilitiesEnabled(section.getBoolean("enabled", true));
        List<ItemAbilityRule> rules = new ArrayList<>();
        ConfigurationSection rulesSec = section.getConfigurationSection("rules");
        if (rulesSec != null) {
            int i = 0;
            for (String key : rulesSec.getKeys(false)) {
                ItemAbilityRule rule = ItemAbilityRule.load(rulesSec.getConfigurationSection(key));
                if (rule != null) {
                    rule.setRuleId(key);
                    rules.add(rule);
                    i++;
                }
            }
        }
        if (rules.isEmpty()) {
            rules.addAll(migrateLegacyPassives(section.getConfigurationSection("legacy-passives")));
        }
        data.setAbilities(rules);
    }

    public static void saveFrom(AbilityHolder data, ConfigurationSection section) {
        if (data == null || section == null) {
            return;
        }
        section.set("enabled", data.isAbilitiesEnabled());
        if (data.getAbilities().isEmpty()) {
            section.set("rules", null);
            return;
        }
        ConfigurationSection rulesSec = section.createSection("rules");
        int i = 0;
        for (ItemAbilityRule rule : data.getAbilities()) {
            rule.save(rulesSec.createSection("rule_" + i));
            i++;
        }
    }

    /** Chuyen khoi passives cu trong items.yml (neu con). */
    private static List<ItemAbilityRule> migrateLegacyPassives(ConfigurationSection legacy) {
        return List.of();
    }
}