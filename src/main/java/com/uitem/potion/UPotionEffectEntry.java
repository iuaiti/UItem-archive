package com.uitem.potion;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class UPotionEffectEntry {

    private String typeKey;
    private int durationTicks;
    private int amplifier;
    private boolean ambient;
    private boolean particles;
    private boolean icon;

    public UPotionEffectEntry(String typeKey, int durationTicks, int amplifier) {
        this.typeKey = typeKey;
        this.durationTicks = Math.max(1, durationTicks);
        this.amplifier = Math.max(0, amplifier);
        this.ambient = false;
        this.particles = true;
        this.icon = true;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }

    public int getDurationSeconds() {
        return Math.max(1, durationTicks / 20);
    }

    public void setDurationSeconds(int seconds) {
        this.durationTicks = Math.max(1, seconds) * 20;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = Math.max(0, amplifier);
    }

    public int getLevelDisplay() {
        return amplifier + 1;
    }

    public boolean isAmbient() {
        return ambient;
    }

    public void setAmbient(boolean ambient) {
        this.ambient = ambient;
    }

    public boolean hasParticles() {
        return particles;
    }

    public void setParticles(boolean particles) {
        this.particles = particles;
    }

    public boolean hasIcon() {
        return icon;
    }

    public void setIcon(boolean icon) {
        this.icon = icon;
    }

    public PotionEffect toBukkit(PotionEffectType type) {
        if (type == null) {
            return null;
        }
        return new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon);
    }

    public void save(ConfigurationSection section) {
        section.set("type", typeKey);
        section.set("duration-ticks", durationTicks);
        section.set("amplifier", amplifier);
        section.set("ambient", ambient);
        section.set("particles", particles);
        section.set("icon", icon);
    }

    public static UPotionEffectEntry load(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String type = section.getString("type");
        if (type == null || type.isBlank()) {
            return null;
        }
        int ticks = section.getInt("duration-ticks", 200);
        if (section.contains("duration-seconds")) {
            ticks = section.getInt("duration-seconds", 10) * 20;
        }
        int amp = section.getInt("amplifier", 0);
        UPotionEffectEntry entry = new UPotionEffectEntry(type, ticks, amp);
        entry.setAmbient(section.getBoolean("ambient", false));
        entry.setParticles(section.getBoolean("particles", true));
        entry.setIcon(section.getBoolean("icon", true));
        return entry;
    }

    public UPotionEffectEntry copy() {
        UPotionEffectEntry c = new UPotionEffectEntry(typeKey, durationTicks, amplifier);
        c.ambient = ambient;
        c.particles = particles;
        c.icon = icon;
        return c;
    }
}