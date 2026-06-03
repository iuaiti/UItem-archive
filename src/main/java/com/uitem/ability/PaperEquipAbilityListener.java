package com.uitem.ability;

import com.uitem.UItemPlugin;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Paper {@link EntityEquipmentChangedEvent} path for ON_EQUIP abilities (1.21.4+ Paper).
 */
public final class PaperEquipAbilityListener implements Listener {

    private final UItemPlugin plugin;

    public PaperEquipAbilityListener(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquip(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        for (Map.Entry<EquipmentSlot, EntityEquipmentChangedEvent.EquipmentChange> entry
                : event.getEquipmentChanges().entrySet()) {
            ItemStack stack = entry.getValue().newItem();
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            AbilityEquipTriggers.fire(plugin, player, stack, entry.getKey());
        }
    }
}