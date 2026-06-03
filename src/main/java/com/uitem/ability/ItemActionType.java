package com.uitem.ability;

public enum ItemActionType {
    DAMAGE("Sat thuong muc tieu", "amount", "5"),
    HEAL("Hoi mau ban than", "amount", "4"),
    POTION("Hieu ung potion", "effect", "regeneration", "duration", "5", "amplifier", "0", "target", "SELF"),
    LIGHTNING("Set set muc tieu"),
    THORNS("Phan sat thuong %", "percent", "25"),
    KNOCKBACK("Dua lui muc tieu", "strength", "1.2"),
    MESSAGE("Tin nhan", "text", "<green>Kich hoat!"),
    COMMAND("Lenh console", "command", "say {player} activated {item}");

    private final String label;
    private final String[] defaultParams;

    ItemActionType(String label, String... defaultParams) {
        this.label = label;
        this.defaultParams = defaultParams;
    }

    public String label() {
        return label;
    }

    public java.util.Map<String, String> defaultParamMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < defaultParams.length; i += 2) {
            map.put(defaultParams[i], defaultParams[i + 1]);
        }
        return map;
    }

    public static ItemActionType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return MESSAGE;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MESSAGE;
        }
    }
}