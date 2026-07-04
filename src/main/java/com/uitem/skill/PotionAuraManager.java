package com.uitem.skill;

import java.util.*;

/**
 * Helper class to manage potion "auras" - potions automatically applied while a player holds/wears an item.
 * Unlike skill actions, these are passive effects.
 */
public class PotionAuraManager {

    /**
     * Create WHILE_HOLDING skill with potion effect
     */
    public static Skill createAuraPotionSkill(String skillId, String displayName, 
                                              String potionEffect, int amplifier, int intervalSeconds) {
        Skill skill = new Skill(skillId);
        skill.setDisplayName(displayName);
        skill.setTrigger(SkillTrigger.WHILE_HOLDING);
        skill.setCooldown(0);
        skill.setPeriodTicks(intervalSeconds * 20); // Convert seconds to ticks
        
        // Add potion action
        SkillAction action = SkillAction.givePotion(potionEffect, intervalSeconds, amplifier);
        skill.addAction(action);
        
        return skill;
    }

    /**
     * Create WHILE_WEARING (armor) skill with potion effect
     */
    public static Skill createWearingPotionSkill(String skillId, String displayName,
                                                 String potionEffect, int amplifier, int intervalSeconds) {
        Skill skill = new Skill(skillId);
        skill.setDisplayName(displayName);
        skill.setTrigger(SkillTrigger.WHILE_WEARING);
        skill.setCooldown(0);
        skill.setPeriodTicks(intervalSeconds * 20);
        
        SkillAction action = SkillAction.givePotion(potionEffect, intervalSeconds, amplifier);
        skill.addAction(action);
        
        return skill;
    }

    /**
     * Create multi-potion aura (multiple effects active at once)
     */
    public static Skill createMultiAuraSkill(String skillId, String displayName,
                                             List<PotionEffectHelper.PotionEffectCombo> effects,
                                             int intervalSeconds) {
        Skill skill = new Skill(skillId);
        skill.setDisplayName(displayName);
        skill.setTrigger(SkillTrigger.WHILE_HOLDING);
        skill.setCooldown(0);
        skill.setPeriodTicks(intervalSeconds * 20);
        
        for (PotionEffectHelper.PotionEffectCombo combo : effects) {
            SkillAction action = SkillAction.givePotion(combo.effectType, intervalSeconds, combo.amplifier);
            skill.addAction(action);
        }
        
        return skill;
    }

    /**
     * Some common preset aura combos
     */
    public static class AuraPresets {
        public static Skill speedAura(String itemId) {
            return createAuraPotionSkill(itemId + "_speed_aura", 
                "<aqua>Speed Aura", "SPEED", 0, 5);
        }

        public static Skill strengthAura(String itemId) {
            return createAuraPotionSkill(itemId + "_strength_aura",
                "<red>Strength Aura", "STRENGTH", 0, 5);
        }

        public static Skill resistanceAura(String itemId) {
            return createAuraPotionSkill(itemId + "_resistance_aura",
                "<green>Resistance Aura", "RESISTANCE", 0, 5);
        }

        public static Skill nightVisionAura(String itemId) {
            return createAuraPotionSkill(itemId + "_night_vision_aura",
                "<dark_purple>Night Vision Aura", "NIGHT_VISION", 0, 10);
        }

        public static Skill regenAura(String itemId) {
            return createAuraPotionSkill(itemId + "_regen_aura",
                "<light_purple>Regeneration Aura", "REGENERATION", 0, 4);
        }

        public static Skill lifeStealerAura(String itemId) {
            // Combination: Strength + Health Boost
            List<PotionEffectHelper.PotionEffectCombo> effects = List.of(
                new PotionEffectHelper.PotionEffectCombo("STRENGTH", 5, 1),
                new PotionEffectHelper.PotionEffectCombo("HEALTH_BOOST", 10, 0)
            );
            return createMultiAuraSkill(itemId + "_lifesteal_aura",
                "<red>Lifesteal Aura", effects, 5);
        }
    }
}
