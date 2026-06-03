package com.uitem.passive;

import com.uitem.passive.action.ActionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ActionEntry {

    private final ActionType type;
    private final Map<String, Object> params;
    private final int delayTicks;

    public ActionEntry(ActionType type, Map<String, Object> params, int delayTicks) {
        this.type = type;
        this.params = params == null ? Map.of() : Map.copyOf(params);
        this.delayTicks = Math.max(0, delayTicks);
    }

    public ActionEntry(ActionType type, Map<String, Object> params) {
        this(type, params, 0);
    }

    public ActionType type() {
        return type;
    }

    public Map<String, Object> params() {
        return params;
    }

    public int delayTicks() {
        return delayTicks;
    }

    public String summary() {
        return type.formatSummary(params);
    }

    public ActionEntry copy() {
        return new ActionEntry(type, new HashMap<>(params), delayTicks);
    }

    public static ActionEntry of(ActionType type) {
        return new ActionEntry(type, type.defaultParams());
    }

    public static ActionEntry deserialize(Map<String, Object> section) {
        if (section == null || section.isEmpty()) {
            return ActionEntry.of(ActionType.SEND_MESSAGE);
        }
        String typeKey = stringVal(section.get("type"));
        ActionType type = ActionType.fromKey(typeKey);
        Map<String, Object> merged = new HashMap<>(type.defaultParams());
        for (Map.Entry<String, Object> e : section.entrySet()) {
            if (!"type".equals(e.getKey()) && !"delay-ticks".equals(e.getKey())) {
                merged.put(e.getKey(), e.getValue());
            }
        }
        int delay = intVal(section.get("delay-ticks"), 0);
        return new ActionEntry(type, merged, delay);
    }

    public Map<String, Object> serializeMap() {
        Map<String, Object> out = new HashMap<>();
        out.put("type", type.key());
        out.putAll(params);
        if (delayTicks > 0) {
            out.put("delay-ticks", delayTicks);
        }
        return Collections.unmodifiableMap(out);
    }

    private static String stringVal(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static int intVal(Object o, int def) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}