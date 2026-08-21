package com.uitem.stat;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes the total value of a virtual {@link Stat} for a player by scanning
 * their equipped UItems (main hand, off hand, armor). There is no vanilla
 * Attribute backing these stats, so totals must be recomputed on demand
 * rather than cached on the ItemStack.
 *
 * <p>Totals are memoised for the duration of a single server tick. A full
 * recompute deserialises the PDC of up to six ItemStacks, and the combat path
 * asks for several different stats per hit ({@code CRIT_CHANCE},
 * {@code DEFENSE}, {@code LIFESTEAL}, ... plus {@code COOLDOWN_REDUCTION} once
 * per skill), which meant a dozen full scans per attack per player. Caching per
 * tick collapses those into one scan while staying correct: equipment cannot
 * change between two points inside the same tick.
 */
public class StatManager {

    /** Memoisation window — one tick. Long enough to collapse a combat burst. */
    private static final long CACHE_WINDOW_NANOS = 50_000_000L; // 50ms

    private final UItemPlugin plugin;

    private final Map<UUID, CachedTotals> cache = new ConcurrentHashMap<>();

    private record CachedTotals(long computedAtNanos, Map<Stat, Double> totals) {
    }

    public StatManager(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    public double getStat(Player player, Stat stat) {
        return getTotals(player).getOrDefault(stat, 0.0);
    }

    public Map<Stat, Double> getTotals(Player player) {
        // Wall-clock rather than Bukkit.getCurrentTick(): on Folia each region
        // ticks independently, so a tick counter is not a comparable value
        // across the threads that call into here.
        long now = System.nanoTime();
        CachedTotals cached = cache.get(player.getUniqueId());
        if (cached != null && now - cached.computedAtNanos() < CACHE_WINDOW_NANOS) {
            return cached.totals();
        }

        Map<Stat, Double> totals = computeTotals(player);
        cache.put(player.getUniqueId(), new CachedTotals(now, totals));
        return totals;
    }

    /**
     * Drop the memoised totals for a player. Call on quit; also safe to call
     * after any programmatic equipment change that must be visible immediately
     * within the same tick.
     */
    public void invalidate(Player player) {
        if (player != null) cache.remove(player.getUniqueId());
    }

    private Map<Stat, Double> computeTotals(Player player) {
        Map<Stat, Double> totals = new EnumMap<>(Stat.class);
        PlayerInventory inv = player.getInventory();

        accumulate(totals, inv.getItemInMainHand(), "HAND");
        accumulate(totals, inv.getItemInOffHand(), "OFF_HAND");
        accumulate(totals, inv.getHelmet(), "HEAD");
        accumulate(totals, inv.getChestplate(), "CHEST");
        accumulate(totals, inv.getLeggings(), "LEGS");
        accumulate(totals, inv.getBoots(), "FEET");

        return totals;
    }

    private void accumulate(Map<Stat, Double> totals, ItemStack item, String equippedSlot) {
        if (item == null || item.getType().isAir()) return;
        if (!UItemAPI.isAvailable()) return;
        UItemData data = UItemAPI.get().getUItemData(item).orElse(null);
        if (data == null || data.getStats() == null) return;

        for (UItemData.StatData sd : data.getStats()) {
            String slot = sd.getSlot() == null ? "ANY" : sd.getSlot().toUpperCase();
            if (!slot.equals("ANY") && !slot.equals(equippedSlot)) continue;

            Stat stat;
            try {
                stat = Stat.valueOf(sd.getStat().toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }
            totals.merge(stat, sd.getAmount(), Double::sum);
        }
    }
}
