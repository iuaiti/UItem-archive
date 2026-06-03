package com.uitem.passive;

import com.uitem.passive.trigger.TriggerType;

import java.util.ArrayList;
import java.util.List;

public final class PassiveDefinition {

    private final String id;
    private String displayName;
    private boolean enabled;
    private TriggerType trigger;
    private ConditionGroup conditions;
    private List<ActionEntry> actions;
    private PassiveAdvancedSettings advanced;

    public PassiveDefinition(String id) {
        this.id = id;
        this.displayName = id;
        this.enabled = true;
        this.trigger = TriggerType.BLOCK_BREAK;
        this.conditions = ConditionGroup.empty();
        this.actions = new ArrayList<>();
        this.advanced = new PassiveAdvancedSettings();
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TriggerType trigger() {
        return trigger;
    }

    public void setTrigger(TriggerType trigger) {
        this.trigger = trigger == null ? TriggerType.BLOCK_BREAK : trigger;
    }

    public ConditionGroup conditions() {
        return conditions;
    }

    public void setConditions(ConditionGroup conditions) {
        this.conditions = conditions == null ? ConditionGroup.empty() : conditions;
    }

    public List<ActionEntry> actions() {
        return actions;
    }

    public void setActions(List<ActionEntry> actions) {
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }

    public PassiveAdvancedSettings advanced() {
        return advanced;
    }

    public void setAdvanced(PassiveAdvancedSettings advanced) {
        this.advanced = advanced == null ? new PassiveAdvancedSettings() : advanced;
    }

    public PassiveDefinition copy() {
        PassiveDefinition c = new PassiveDefinition(id);
        c.displayName = displayName;
        c.enabled = enabled;
        c.trigger = trigger;
        c.conditions = conditions.copy();
        List<ActionEntry> act = new ArrayList<>();
        for (ActionEntry a : actions) {
            act.add(a.copy());
        }
        c.actions = act;
        c.advanced = advanced.copy();
        return c;
    }
}