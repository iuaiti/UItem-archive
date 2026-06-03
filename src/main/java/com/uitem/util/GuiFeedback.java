package com.uitem.util;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Sound and particle feedback for GUI interactions.
 */
public final class GuiFeedback {

    private GuiFeedback() {
    }

    public static void click(Player player) {
        click(player, true);
    }

    public static void click(Player player, boolean actionable) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.45f, actionable ? 1.15f : 0.9f);
        var loc = player.getLocation().add(0, 1.6, 0);
        player.getWorld().spawnParticle(
                Particle.END_ROD,
                loc.getX(), loc.getY(), loc.getZ(),
                4, 0.12, 0.08, 0.12, 0.01);
    }

    public static void success(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.35f);
        var loc = player.getLocation().add(0, 1.6, 0);
        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                loc.getX(), loc.getY(), loc.getZ(),
                3, 0.2, 0.15, 0.2, 0);
    }

    public static void deny(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.45f, 0.85f);
    }
}