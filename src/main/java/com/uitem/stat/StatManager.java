package com.uitem.stat;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Computes the total value of a virtual {@link Stat} for a player by scanning
 * their equipped UItems (main hand, off hand, armor). There is no vanilla
 * Attribute backing these stats, so totals must be recomputed on demand
 * rather than cached on the ItemStack.
 */
public class StatManager {

    private final UItemPlugin plugin;

    public StatManager(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    public double getStat(Player player, Stat stat) {
        return getTotals(player).getOrDefault(stat, 0.0);
    }

    public Map<Stat, Double> getTotals(Player player) {
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
