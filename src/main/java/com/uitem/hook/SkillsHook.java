package com.uitem.hook;

import org.bukkit.entity.Player;

/**
 * Bridge to an external skill/leveling plugin (AuraSkills, EcoSkills, ...).
 * <p>
 * Exactly ONE hook is active at a time — see {@link SkillsHookManager}.
 * Implementations must never throw out of these methods; on any internal
 * failure they return a safe fallback ({@code -1} for levels).
 */
public interface SkillsHook {

    /** Display name of the hooked plugin (e.g. "AuraSkills"). */
    String getName();

    /**
     * Level of the player in the given skill.
     *
     * @param skillId lowercase skill id in the hooked plugin (e.g. "mining", "fighting")
     * @return the level, or {@code -1} if the skill is unknown or the lookup failed
     */
    int getSkillLevel(Player player, String skillId);

    /** True if the skill id exists in the hooked plugin. */
    boolean hasSkill(String skillId);

    /**
     * Give skill XP to the player. Optional operation — implementations that
     * cannot support it simply do nothing and return false.
     */
    boolean giveSkillXp(Player player, String skillId, double amount);

    // ==================== MANA (optional capability) ====================

    /** True if the hooked plugin exposes a mana pool we can read and consume. */
    boolean hasManaSupport();

    /** Current mana of the player, or {@code -1} if unsupported/lookup failed. */
    double getMana(Player player);

    /** Max mana of the player, or {@code -1} if unsupported/lookup failed. */
    double getMaxMana(Player player);

    /**
     * Take mana from the player (clamped at 0).
     * @return false if unsupported or the operation failed
     */
    boolean consumeMana(Player player, double amount);
}
