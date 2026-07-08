package com.uitem.skill;

import java.util.*;

/**
 * Represents a Skill or Passive attached to a UItem.
 *
 * Example YAML structure:
 * skills:
 *   fireball:
 *     trigger: RIGHT_CLICK
 *     cooldown: 6.5
 *     cancel-event: true
 *     display-name: "<red>Fireball"
 *     chance: 100
 *     conditions:
 *       - type: CHANCE
 *         value: 100
 *     actions:
 *       - type: SHOOT_PROJECTILE
 *         projectile: FIREBALL
 *         speed: 1.8
 *         damage: 8.0
 *       - type: PLAY_SOUND
 *         sound: ENTITY_BLAZE_SHOOT
 *         volume: 1.0
 *         pitch: 0.9
 */
public class Skill {

    private String id;                    // unique within item (e.g., "fireball", "lifesteal_passive")
    private String displayName;           // used for cooldown display / lore (MiniMessage)
    private SkillTrigger trigger = SkillTrigger.RIGHT_CLICK;
    private double cooldown = 0.0;        // seconds
    private boolean cancelEvent = true;   // whether to cancel the interact event (only applies to active)
    private double chance = 100.0;        // 0-100, % activation chance (handy for passive combat)
    private double manaCost = 0.0;        // mana taken per cast via the hooked skills plugin; 0 = free

    private List<SkillCondition> conditions = new ArrayList<>();
    private List<SkillAction> actions = new ArrayList<>();

    // For PERIODIC / WHILE_*
    private int periodTicks = 20;         // run once every N ticks (default 1s)

    public Skill() {}

    public Skill(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName != null ? displayName : id; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public SkillTrigger getTrigger() { return trigger; }
    public void setTrigger(SkillTrigger trigger) { this.trigger = trigger != null ? trigger : SkillTrigger.RIGHT_CLICK; }

    public double getCooldown() { return cooldown; }
    public void setCooldown(double cooldown) { this.cooldown = Math.max(0, cooldown); }

    public boolean isCancelEvent() { return cancelEvent; }
    public void setCancelEvent(boolean cancelEvent) { this.cancelEvent = cancelEvent; }

    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = Math.max(0, Math.min(100, chance)); }

    public double getManaCost() { return manaCost; }
    public void setManaCost(double manaCost) { this.manaCost = Math.max(0, manaCost); }

    public List<SkillCondition> getConditions() { return conditions; }
    public void setConditions(List<SkillCondition> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public List<SkillAction> getActions() { return actions; }
    public void setActions(List<SkillAction> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public int getPeriodTicks() { return Math.max(1, periodTicks); }
    public void setPeriodTicks(int periodTicks) { this.periodTicks = Math.max(1, periodTicks); }

    public void addAction(SkillAction action) {
        if (action != null) actions.add(action);
    }

    public void addCondition(SkillCondition cond) {
        if (cond != null) conditions.add(cond);
    }

    /**
     * Deep clone.
     */
    public Skill copy() {
        Skill s = new Skill(this.id);
        s.displayName = this.displayName;
        s.trigger = this.trigger;
        s.cooldown = this.cooldown;
        s.cancelEvent = this.cancelEvent;
        s.chance = this.chance;
        s.manaCost = this.manaCost;
        s.periodTicks = this.periodTicks;

        s.conditions = new ArrayList<>();
        for (SkillCondition c : this.conditions) s.conditions.add(c.copy());

        s.actions = new ArrayList<>();
        for (SkillAction a : this.actions) s.actions.add(a.copy());

        return s;
    }

    @Override
    public String toString() {
        return "Skill{" + id + ", trigger=" + trigger + ", cd=" + cooldown + "s, actions=" + actions.size() + "}";
    }
}
