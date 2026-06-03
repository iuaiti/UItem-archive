package com.uitem.passive;

import com.uitem.passive.condition.ConditionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ConditionEntry {

    private final ConditionType type;
    private final Map<String, Object> params;

    public ConditionEntry(ConditionType type, Map<String, Object> params) {
        this.type = type;
        this.params = params == null ? Map.of() : Map.copyOf(params);
    }

    public ConditionType type() {
        return type;
    }

    public Map<String, Object> params() {
        return params;
    }

    public String summary() {
        return type.formatSummary(params);
    }

    public ConditionEntry copy() {
        return new ConditionEntry(type, new HashMap<>(params));
    }

    public static ConditionEntry of(ConditionType type) {
        return new ConditionEntry(type, type.defaultParams());
    }

    public static ConditionEntry deserialize(String typeKey, Map<String, Object> raw) {
        ConditionType type = ConditionType.fromKey(typeKey);
        Map<String, Object> merged = new HashMap<>(type.defaultParams());
        if (raw != null) {
            merged.putAll(raw);
        }
        return new ConditionEntry(type, merged);
    }

    public Map<String, Object> serializeMap() {
        Map<String, Object> out = new HashMap<>();
        out.put("type", type.key());
        out.putAll(params);
        return Collections.unmodifiableMap(out);
    }
}