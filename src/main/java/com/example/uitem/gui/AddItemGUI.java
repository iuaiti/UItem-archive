package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.model.Category;
import com.example.uitem.model.CustomItem;
import com.example.uitem.model.CustomItem.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for adding a new custom item.
 */
public class AddItemGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private Inventory inventory;
    private final Player player;

    // Fields for the item being created
    private UUID id;
    private Material material;
    private Component name;
    private List<Component> lore;
    private int customModelData;
    private Rarity rarity;
    private boolean glow;

    public AddItemGUI(UitemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.player = player;
        this.id = UUID.randomUUID();
        // Default values
        this.material = Material.STONE;
        this.name = Component.text("New Item");
        this.lore = new ArrayList<>();
        this.customModelData = 0;
        this.rarity = Rarity.COMMON;
        this.glow = false;
        this.inventory = Bukkit.createInventory(null, 6 * 9, Component.text("Add New Item"));
        initialize();
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Set up the form fields
        // We'll use a simple approach: each field is represented by an item
        // Row 0: Material selector
        inventory.setItem(10, createFieldItem(org.bukkit.Material.GRASS_BLOCK, Component.text("Material: " + material.name()), NamedTextColor.YELLOW));
        // Row 1: Name field
        inventory.setItem(19, createFieldItem(org.bukkit.Material.NAME_TAG, Component.text("Name: " + name), NamedTextColor.YELLOW));
        // Row 2: Lore field
        inventory.setItem(28, createFieldItem(org.bukkit.Material.BOOK, Component.text("Lore: " + lore.size() + " lines"), NamedTextColor.YELLOW));
        // Row 3: Custom Model Data
        inventory.setItem(37, createFieldItem(org.bukkit.Material.CLOCK, Component.text("Custom Model Data: " + customModelData), NamedTextColor.YELLOW));
        // Row 4: Rarity
        inventory.setItem(46, createFieldItem(org.bukkit.Material.EMERALD, Component.text("Rarity: " + rarity), NamedTextColor.YELLOW));
        // Row 5: Glow
        inventory.setItem(48, createFieldItem(glow ? org.bukkit.Material.GLOW_INK_SAC : org.bukkit.Material.INK_SAC,
                Component.text("Glow: " + (glow ? "Yes" : "No")),
                glow ? NamedTextColor.GREEN : NamedTextColor.RED));

        // Action buttons
        inventory.setItem(49, createButtonItem(org.bukkit.Material.LIME_STAINED_GLASS_PANE, Component.text("Save"), NamedTextColor.GREEN));
        inventory.setItem(50, createButtonItem(org.bukkit.Material.RED_STAINED_GLASS_PANE, Component.text("Cancel"), NamedTextColor.RED));

        // Fill remaining slots with glass panes
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private ItemStack createFieldItem(Material material, Component name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            // In a real plugin, we would add lore to explain how to edit
            item.setItemMeta(meta);
        }
        return item;
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

    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        return glass;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    // Methods to handle clicks would be implemented in the listener
    // For brevity, we'll just note that they exist
    public void handleMaterialClick(Player player) {
        // Cycle through materials or open a material picker
        player.sendMessage("Material picker not implemented yet.");
    }

    public void handleNameClick(Player player) {
        // Open a text input for the name (would require chat input or external GUI)
        player.sendMessage("Name editor not implemented yet.");
    }

    // Similar methods for other fields...

    public void handleSaveClick(Player player) {
        // Create the item and save it
        CustomItem item = new CustomItem(id, material, name, lore, customModelData, rarity, glow, null);
        configManager.addItem(item);
        configManager.saveItems();

        // Add to default category (or first category)
        List<Category> categories = configManager.getAllCategories();
        if (!categories.isEmpty()) {
            Category firstCategory = categories.get(0);
            firstCategory.addItem(id);
            configManager.updateCategory(firstCategory);
            configManager.saveCategories();
        }

        player.sendMessage("Item saved successfully!");
        player.closeInventory();
    }

    public void handleCancelClick(Player player) {
        player.sendMessage("Item creation cancelled.");
        player.closeInventory();
    }
}