package com.uitem.util;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Scheduler wrapper for Paper, Spigot, and Folia (region / entity / global / async).
 */
public final class SchedulerUtil {

    private SchedulerUtil() {
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run());
    }

    public static void runDelayedAsync(Plugin plugin, Runnable runnable, long delay, TimeUnit unit) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delay, unit);
    }

    /**
     * Global main-thread work (console messages, server-wide coordination). On Folia uses the global region.
     */
    public static void runSync(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            runGlobal(plugin, runnable);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    /**
     * Sync work tied to a command sender; uses the player's entity scheduler when applicable.
     */
    public static void runSync(Plugin plugin, CommandSender sender, Runnable runnable) {
        if (sender instanceof Player player && player.isOnline()) {
            runForEntity(plugin, player, runnable);
            return;
        }
        runSync(plugin, runnable);
    }

    /**
     * Sync work tied to a player (inventory, GUI, resource pack, etc.).
     */
    public static void runSync(Plugin plugin, Player player, Runnable runnable) {
        if (player != null && player.isOnline()) {
            runForEntity(plugin, player, runnable);
            return;
        }
        runSync(plugin, runnable);
    }

    public static void runGlobal(Plugin plugin, Runnable runnable) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
    }

    public static void runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        if (location == null || location.getWorld() == null) {
            runSync(plugin, runnable);
            return;
        }
        plugin.getServer().getRegionScheduler().execute(plugin, location, runnable);
    }

    /**
     * Runs on the owning region thread of the entity (required on Folia for player/world mutations).
     */
    public static void runForEntity(Plugin plugin, Entity entity, Runnable runnable) {
        if (entity == null || !entity.isValid()) {
            runSync(plugin, runnable);
            return;
        }
        entity.getScheduler().run(plugin, task -> runnable.run(), null);
    }
}