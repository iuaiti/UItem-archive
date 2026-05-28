package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.model.Category;
import com.example.uitem.model.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for browsing items in a category with pagination.
 */
public class CategoryGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private Inventory inventory;
    private final Category category;

    // Pagination
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 54; // 6 rows * 9 columns
    private int totalPages;

    public CategoryGUI(UitemPlugin plugin, Category category) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.category = category;
        updateTotalPages();
        // 54 slots = 6 rows
        this.inventory = Bukkit.createInventory(null, 6 * 9, Component.text("Category: " + category.getDisplayName() + " (Page " + (currentPage + 1) + "/" + totalPages + ")"));
        initialize();
    }

    private void updateTotalPages() {
        List<CustomItem> items = getItemsInCategory();
        totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) {
            totalPages = 1;
        }
    }

    private List<CustomItem> getItemsInCategory() {
        List<CustomItem> result = new ArrayList<>();
        for (CustomItem item : configManager.getAllItems()) {
            // In a real implementation, we would check if the item is in this category.
            // For now, we show all items (simplified).
            result.add(item);
        }
        return result;
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Get items for the current page
        List<CustomItem> items = getItemsInCategory();
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());

        // Fill inventory with items for this page
        int slot = 0;
        for (int i = start; i < end; i++) {
            if (slot >= ITEMS_PER_PAGE) break; // Safety
            inventory.setItem(slot, items.get(i).toItemStack());
            slot++;
        }

        // Fill remaining item slots with glass pane
        for (int i = slot; i < ITEMS_PER_PAGE; i++) {
            inventory.setItem(i, createGlassPane());
        }

        // Add pagination buttons (if needed)
        if (totalPages > 1) {
            // Previous button (left)
            if (currentPage > 0) {
                inventory.setItem(ITEMS_PER_PAGE + 3, createButtonItem(org.bukkit.Material.ARROW,
                        Component.text("Previous Page"), NamedTextColor.YELLOW));
            } else {
                inventory.setItem(ITEMS_PER_PAGE + 3, createGlassPane());
            }
            // Next button (right)
            if (currentPage < totalPages - 1) {
                inventory.setItem(ITEMS_PER_PAGE + 5, createButtonItem(org.bukkit.Material.ARROW,
                        Component.text("Next Page"), NamedTextColor.YELLOW));
            } else {
                inventory.setItem(ITEMS_PER_PAGE + 5, createGlassPane());
            }
        }

        // Fill the rest of the inventory (below the buttons) with glass panes
        for (int i = ITEMS_PER_PAGE + 6; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ")); // Empty name to make it less intrusive
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private ItemStack createButtonItem(Material material, Component name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    // Methods to handle pagination clicks
    public void handlePreviousClick(Player player) {
        if (currentPage > 0) {
            currentPage--;
            updateTotalPages();
            updateInventoryTitle();
            initialize();
            player.openInventory(inventory); // Re-open to update
        }
    }

    public void handleNextClick(Player player) {
        if (currentPage < totalPages - 1) {
            currentPage++;
            updateTotalPages();
            updateInventoryTitle();
            initialize();
            player.openInventory(inventory); // Re-open to update
        }
    }

    private void updateInventoryTitle() {
        this.inventory = Bukkit.createInventory(null, 6 * 9, Component.text("Category: " + category.getDisplayName() + " (Page " + (currentPage + 1) + "/" + totalPages + ")"));
    }
}