package com.uitem.ability;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ItemAbilityRule {

    private String ruleId;
    private ItemTrigger trigger;
    private ItemSlotRequirement slot;
    private int cooldownSeconds;
    private int chancePercent;
    private List<ItemAbilityAction> actions;

    public ItemAbilityRule() {
        this.ruleId = UUID.randomUUID().toString().substring(0, 8);
        this.trigger = ItemTrigger.ON_HIT;
        this.slot = ItemSlotRequirement.EITHER_HAND;
        this.cooldownSeconds = 0;
        this.chancePercent = 100;
        this.actions = new ArrayList<>();
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public ItemTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(ItemTrigger trigger) {
        this.trigger = trigger != null ? trigger : ItemTrigger.ON_HIT;
    }

    public ItemSlotRequirement getSlot() {
        return slot;
    }

    public void setSlot(ItemSlotRequirement slot) {
        this.slot = slot != null ? slot : ItemSlotRequirement.EITHER_HAND;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    public int getChancePercent() {
        return chancePercent;
    }

    public void setChancePercent(int chancePercent) {
        this.chancePercent = Math.max(0, Math.min(100, chancePercent));
    }

    public List<ItemAbilityAction> getActions() {
        return actions;
    }

    public void setActions(List<ItemAbilityAction> actions) {
        this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
    }

    public void save(ConfigurationSection section) {
        section.set("trigger", trigger.name());
        section.set("slot", slot.name());
        section.set("cooldown", cooldownSeconds);
        section.set("chance", chancePercent);
        ConfigurationSection actionsSec = section.createSection("actions");
        for (int i = 0; i < actions.size(); i++) {
            actions.get(i).save(actionsSec.createSection("a" + i));
        }
    }

    public static ItemAbilityRule load(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ItemAbilityRule rule = new ItemAbilityRule();
        rule.trigger = ItemTrigger.fromString(section.getString("trigger"));
        rule.slot = ItemSlotRequirement.fromString(section.getString("slot"));
        rule.cooldownSeconds = section.getInt("cooldown", 0);
        rule.chancePercent = section.getInt("chance", 100);
        ConfigurationSection actionsSec = section.getConfigurationSection("actions");
        if (actionsSec != null) {
            for (String key : actionsSec.getKeys(false)) {
                ItemAbilityAction action = ItemAbilityAction.load(actionsSec.getConfigurationSection(key));
                if (action != null) {
                    rule.actions.add(action);
                }
            }
        }
        return rule;
    }

    public ItemAbilityRule copy() {
        ItemAbilityRule c = new ItemAbilityRule();
        c.ruleId = ruleId;
        c.trigger = trigger;
        c.slot = slot;
        c.cooldownSeconds = cooldownSeconds;
        c.chancePercent = chancePercent;
        for (ItemAbilityAction a : actions) {
            c.actions.add(a.copy());
        }
        return c;
    }
}