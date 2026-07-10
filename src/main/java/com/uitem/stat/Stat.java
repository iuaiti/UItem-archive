package com.uitem.stat;

/**
 * Virtual item stats without a vanilla Attribute equivalent.
 * Values are always additive across equipped items; percentages are stored as
 * raw numbers (e.g. 20 = 20%). Combat-facing attack stats (Attack Damage,
 * Attack Speed, Armor, Health, Movement Speed) already map to vanilla
 * Attributes via {@link com.uitem.gui.StatSystemGui} — they are not
 * duplicated here.
 */
public enum Stat {
    CRIT_CHANCE("Crit Chance", "%"),
    CRIT_DAMAGE("Crit Damage", "%"),
    DEFENSE("Defense", "%"),
    ARMOR_PENETRATION("Armor Penetration", "%"),
    TRUE_DAMAGE("True Damage", "%"),
    LIFESTEAL("Lifesteal", "%"),
    DODGE_CHANCE("Dodge Chance", "%"),
    HEALTH_REGEN("Health Regen", "/s"),
    COOLDOWN_REDUCTION("Cooldown Reduction", "%"),
    MANA_REGEN("Mana Regen", "/s"),
    FORTUNE_BONUS("Fortune Bonus", "%"),
    DOUBLE_DROP_CHANCE("Double Drop Chance", "%"),
    PROJECTILE_DAMAGE("Projectile Damage", "%"),
    PROJECTILE_SPEED("Projectile Speed", "%"),
    XP_BONUS("XP Bonus", "%");

    private final String display;
    private final String suffix;

    Stat(String display, String suffix) {
        this.display = display;
        this.suffix = suffix;
    }

    public String getDisplay() {
        return display;
    }

    public String getSuffix() {
        return suffix;
    }
}
