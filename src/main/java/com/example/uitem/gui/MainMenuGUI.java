package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.model.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Main menu GUI for the UItem plugin.
 */
public class MainMenuGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private Inventory inventory;

    public MainMenuGUI(UitemPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.inventory = Bukkit.createInventory(null, 3 * 9, Component.text("UITEM Main Menu"));
        initialize();
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Fill with categories (up to 27 slots for 3 rows)
        List<Category> categories = configManager.getAllCategories();
        int slot = 0;
        for (Category category : categories) {
            if (slot >= 27) break; // Only use first 3 rows for categories
            ItemStack icon = createCategoryIcon(category);
            inventory.setItem(slot, icon);
            slot++;
        }
        // Fill remaining slots in the first 3 rows with empty glass pane
        for (int i = slot; i < 27; i++) {
            inventory.setItem(i, createGlassPane());
        }

        // Add a crafting button at the bottom
        inventory.setItem(29, createButtonItem(org.bukkit.Material.CRAFTING_TABLE,
                Component.text("Crafting"), NamedTextColor.BLUE));

        // Add an admin button at the bottom (only visible to admins in listener)
        inventory.setItem(33, createButtonItem(org.bukkit.Material.NETHER_STAR,
                Component.text("Admin"), NamedTextColor.GOLD));

        // Fill the rest of the bottom row with glass panes
        for (int i = 28; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null && i != 29 && i != 33) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private ItemStack createCategoryIcon(Category category) {
        // Create an icon representing the category
        // For simplicity, we use the category's icon item
        ItemStack icon = new ItemStack(category.getIconItem());
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(category.getDisplayName());
            // Add lore indicating how many items are in this category
            // In a real implementation, we would count the items in the category
            meta.lore(List.of(
                    Component.text("Click to browse items", NamedTextColor.GRAY)
            ));
            icon.setItemMeta(meta);
        }
        return icon;
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
}