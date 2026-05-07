package com.uitem.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener tập trung xử lý tất cả GUI click events.
 * Dispatch event đến đúng AbstractGui instance.
 */
public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof AbstractGui gui) {
            event.setCancelled(true);
            gui.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof AbstractGui) {
            event.setCancelled(true);
        }
    }
}
