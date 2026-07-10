package com.uitem.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Scheduler wrapper that supports Paper + Folia via modern scheduler APIs.
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

    public static void runSync(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            runGlobal(plugin, runnable);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public static void runDelayedAsync(Plugin plugin, Runnable runnable, long delay, TimeUnit unit) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delay, unit);
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
        plugin.getServer().getRegionScheduler().execute(plugin, location, runnable);
    }

    /**
     * Repeating task bound to a location's region (Folia) or the main thread (Paper/Spigot).
     * The task returns false to cancel itself.
     */
    public static void runTimerAtLocation(Plugin plugin, Location location, long delayTicks, long periodTicks,
                                          java.util.function.BooleanSupplier task) {
        long delay = Math.max(1, delayTicks);
        long period = Math.max(1, periodTicks);
        if (isFolia()) {
            plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, t -> {
                if (!task.getAsBoolean()) t.cancel();
            }, delay, period);
        } else {
            plugin.getServer().getScheduler().runTaskTimer(plugin, t -> {
                if (!task.getAsBoolean()) t.cancel();
            }, delay, period);
        }
    }

    /**
     * Repeating task on the global region (Folia) or the main thread (Paper/Spigot).
     * Use for work that doesn't belong to one location, e.g. iterating all online
     * players and re-dispatching each to their own entity scheduler.
     */
    public static void runGlobalTimer(Plugin plugin, long delayTicks, long periodTicks, Runnable runnable) {
        long delay = Math.max(1, delayTicks);
        long period = Math.max(1, periodTicks);
        if (isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), delay, period);
        } else {
            plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable runnable) {
        entity.getScheduler().run(plugin, task -> runnable.run(), null);
    }
}
