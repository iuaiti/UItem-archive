package com.uitem.passive;

/**
 * How multiple conditions are combined (shown in Advanced GUI only).
 */
public enum LogicMode {
    ALL("Tat ca dieu kien"),
    ANY("Mot trong cac dieu kien"),
    NOT("Khong duoc thoa");

    private final String displayName;

    LogicMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static LogicMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}