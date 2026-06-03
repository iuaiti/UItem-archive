package com.uitem.ability;

public enum ItemTrigger {
    ON_HIT("Khi danh trung"),
    ON_KILL("Khi giet quai"),
    ON_DAMAGE_TAKEN("Khi bi danh"),
    ON_RIGHT_CLICK("Click phai"),
    ON_LEFT_CLICK("Click trai"),
    ON_EQUIP("Khi mac do"),
    ON_DRINK("Khi uong potion");

    private final String label;

    ItemTrigger(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ItemTrigger next() {
        ItemTrigger[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static ItemTrigger fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return ON_HIT;
        }
        String n = raw.trim().toUpperCase().replace('-', '_');
        if (n.equals("HIT")) {
            return ON_HIT;
        }
        if (n.equals("KILL")) {
            return ON_KILL;
        }
        if (n.equals("DAMAGE") || n.equals("TAKE_DAMAGE") || n.equals("ON_DAMAGE")) {
            return ON_DAMAGE_TAKEN;
        }
        if (n.equals("DRINK") || n.equals("CONSUME")) {
            return ON_DRINK;
        }
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return ON_HIT;
        }
    }
}