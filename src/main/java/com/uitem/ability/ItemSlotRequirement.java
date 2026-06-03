package com.uitem.ability;

public enum ItemSlotRequirement {
    MAIN_HAND("Tay chinh"),
    OFF_HAND("Tay phu"),
    EITHER_HAND("Hai tay"),
    ARMOR("Giap");

    private final String label;

    ItemSlotRequirement(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ItemSlotRequirement next() {
        ItemSlotRequirement[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static ItemSlotRequirement fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return EITHER_HAND;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EITHER_HAND;
        }
    }
}