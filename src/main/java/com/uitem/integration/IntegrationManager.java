package com.uitem.integration;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Quản lý tích hợp với Oraxen và ItemsAdder.
 * Detect runtime và delegate build item nếu có integration ID.
 */
public class IntegrationManager {

    private final UItemPlugin plugin;
    private OraxenIntegration oraxenIntegration;
    private ItemsAdderIntegration itemsAdderIntegration;
    private boolean hasOraxen;
    private boolean hasItemsAdder;

    public IntegrationManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.hasOraxen = false;
        this.hasItemsAdder = false;
    }

    /**
     * Detect xem Oraxen và ItemsAdder có đang cài không.
     */
    public void detectIntegrations() {
        oraxenIntegration = null;
        itemsAdderIntegration = null;
        hasOraxen = false;
        hasItemsAdder = false;

        // Check Oraxen
        if (Bukkit.getPluginManager().getPlugin("Oraxen") != null
                && Bukkit.getPluginManager().isPluginEnabled("Oraxen")) {
            try {
                oraxenIntegration = new OraxenIntegration();
                hasOraxen = true;
                plugin.getLogger().info("[Integration] Detected Oraxen! Integration enabled.");
            } catch (Exception e) {
                hasOraxen = false;
                plugin.getLogger().warning("[Integration] Detected Oraxen but could not integrate: " + e.getMessage());
            }
        } else {
            hasOraxen = false;
            plugin.getLogger().info("[Integration] Oraxen not found.");
        }

        // Check ItemsAdder
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
                && Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                itemsAdderIntegration = new ItemsAdderIntegration();
                hasItemsAdder = true;
                plugin.getLogger().info("[Integration] Detected ItemsAdder! Integration enabled.");
            } catch (Exception e) {
                hasItemsAdder = false;
                plugin.getLogger().warning("[Integration] Detected ItemsAdder but could not integrate: " + e.getMessage());
            }
        } else {
            hasItemsAdder = false;
            plugin.getLogger().info("[Integration] ItemsAdder not found.");
        }
    }

    /**
     * Thử build ItemStack từ integration (Oraxen/ItemsAdder).
     * Trả về null nếu không có integration hoặc không có ID.
     */
    public ItemStack tryBuildFromIntegration(UItemData data) {
        // Ưu tiên Oraxen
        if (hasOraxen && data.getOraxenId() != null && !data.getOraxenId().isEmpty()) {
            ItemStack item = oraxenIntegration.getItem(data.getOraxenId());
            if (item != null) return item;
        }

        // Rồi ItemsAdder
        if (hasItemsAdder && data.getItemsAdderId() != null && !data.getItemsAdderId().isEmpty()) {
            ItemStack item = itemsAdderIntegration.getItem(data.getItemsAdderId());
            if (item != null) return item;
        }

        return null;
    }

    public boolean hasOraxen() {
        return hasOraxen;
    }

    public boolean hasItemsAdder() {
        return hasItemsAdder;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Oraxen=").append(hasOraxen ? "✔" : "✘");
        sb.append(", ItemsAdder=").append(hasItemsAdder ? "✔" : "✘");
        return sb.toString();
    }
}
