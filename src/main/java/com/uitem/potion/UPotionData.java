package com.uitem.potion;

import com.uitem.ability.AbilityHolder;
import com.uitem.ability.ItemAbilityRule;

import java.util.ArrayList;
import java.util.List;

public final class UPotionData implements AbilityHolder {

    private final String id;
    private String displayName;
    private List<String> lore;
    private PotionKind kind;
    private int colorRgb;
    private List<UPotionEffectEntry> effects;
    private boolean glowing;
    private boolean abilitiesEnabled = true;
    private List<ItemAbilityRule> abilities = new ArrayList<>();

    public UPotionData(String id) {
        this.id = id;
        this.displayName = "<light_purple>" + id;
        this.lore = new ArrayList<>();
        this.kind = PotionKind.DRINK;
        this.colorRgb = 0xFF55FF;
        this.effects = new ArrayList<>();
        this.glowing = false;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getAbilityId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
    }

    public PotionKind getKind() {
        return kind;
    }

    public void setKind(PotionKind kind) {
        this.kind = kind != null ? kind : PotionKind.DRINK;
    }

    public int getColorRgb() {
        return colorRgb;
    }

    public void setColorRgb(int colorRgb) {
        this.colorRgb = colorRgb & 0xFFFFFF;
    }

    public List<UPotionEffectEntry> getEffects() {
        return effects;
    }

    public void setEffects(List<UPotionEffectEntry> effects) {
        this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    @Override
    public boolean isAbilitiesEnabled() {
        return abilitiesEnabled;
    }

    @Override
    public void setAbilitiesEnabled(boolean abilitiesEnabled) {
        this.abilitiesEnabled = abilitiesEnabled;
    }

    @Override
    public List<ItemAbilityRule> getAbilities() {
        return abilities;
    }

    @Override
    public void setAbilities(List<ItemAbilityRule> abilities) {
        this.abilities = abilities != null ? new ArrayList<>(abilities) : new ArrayList<>();
    }

    public UPotionData copy() {
        UPotionData c = new UPotionData(id);
        c.displayName = displayName;
        c.lore = new ArrayList<>(lore);
        c.kind = kind;
        c.colorRgb = colorRgb;
        c.glowing = glowing;
        for (UPotionEffectEntry e : effects) {
            c.effects.add(e.copy());
        }
        c.abilitiesEnabled = abilitiesEnabled;
        for (ItemAbilityRule rule : abilities) {
            c.abilities.add(rule.copy());
        }
        return c;
    }
}