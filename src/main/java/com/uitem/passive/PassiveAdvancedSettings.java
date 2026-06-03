package com.uitem.passive;

/**
 * Optional tuning — edited via [Nang cao] GUI.
 */
public final class PassiveAdvancedSettings {

    private int cooldownSeconds;
    private int delayTicks;
    private double globalChancePercent;

    public PassiveAdvancedSettings() {
        this(0, 0, 100.0);
    }

    public PassiveAdvancedSettings(int cooldownSeconds, int delayTicks, double globalChancePercent) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.delayTicks = Math.max(0, delayTicks);
        this.globalChancePercent = clampPercent(globalChancePercent);
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    public int delayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = Math.max(0, delayTicks);
    }

    public double globalChancePercent() {
        return globalChancePercent;
    }

    public void setGlobalChancePercent(double globalChancePercent) {
        this.globalChancePercent = clampPercent(globalChancePercent);
    }

    public PassiveAdvancedSettings copy() {
        return new PassiveAdvancedSettings(cooldownSeconds, delayTicks, globalChancePercent);
    }

    private static double clampPercent(double v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }
}