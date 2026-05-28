package com.uitem.passive;

import java.util.Map;

public class PassiveData {

    private String type;
    private String trigger;
    private Map<String, Object> conditions;
    private int cooldown;

    public PassiveData() {
    }

    public PassiveData(String type, String trigger, Map<String, Object> conditions, int cooldown) {
        this.type = type;
        this.trigger = trigger;
        this.conditions = conditions;
        this.cooldown = cooldown;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public PassiveData copy() {
        return new PassiveData(type, trigger, conditions != null ? Map.copyOf(conditions) : null, cooldown);
    }
}
