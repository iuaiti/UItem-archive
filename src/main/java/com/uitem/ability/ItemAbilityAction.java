package com.uitem.ability;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ItemAbilityAction {

    private ItemActionType type;
    private Map<String, String> params;

    public ItemAbilityAction(ItemActionType type) {
        this.type = type;
        this.params = new LinkedHashMap<>(type.defaultParamMap());
    }

    public ItemAbilityAction(ItemActionType type, Map<String, String> params) {
        this.type = type;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public ItemActionType getType() {
        return type;
    }

    public void setType(ItemActionType type) {
        this.type = type;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

    public double paramDouble(String key, double fallback) {
        try {
            return Double.parseDouble(params.get(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    public int paramInt(String key, int fallback) {
        try {
            return Integer.parseInt(params.get(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    public void save(ConfigurationSection section) {
        section.set("type", type.name());
        for (Map.Entry<String, String> e : params.entrySet()) {
            section.set("params." + e.getKey(), e.getValue());
        }
    }

    public static ItemAbilityAction load(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ItemActionType type = ItemActionType.fromString(section.getString("type"));
        ItemAbilityAction action = new ItemAbilityAction(type);
        ConfigurationSection paramsSec = section.getConfigurationSection("params");
        if (paramsSec != null) {
            for (String key : paramsSec.getKeys(false)) {
                action.params.put(key, paramsSec.getString(key, ""));
            }
        }
        return action;
    }

    public ItemAbilityAction copy() {
        return new ItemAbilityAction(type, params);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemAbilityAction that)) {
            return false;
        }
        return type == that.type && Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, params);
    }
}