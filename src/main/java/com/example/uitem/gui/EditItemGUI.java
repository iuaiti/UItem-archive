package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.model.CustomItem;
import com.example.uitem.model.CustomItem.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for editing an existing custom item.
 */
public class EditItemGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private Inventory inventory;
    private final Player player;
    private CustomItem editingItem; // The item being edited

    // Fields for the item being edited (copy of the item's properties)
    private UUID id;
    private Material material;
    private Component name;
    private List<Component> lore;
    private int customModelData;
    private Rarity rarity;
    private boolean glow;

    public EditItemGUI(UitemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.player = player;
        // In a real implementation, we would select an item to edit (e.g., from a list)
        // For simplicity, we'll edit the first item in the config, or create a dummy if none.
        List<CustomItem> items = configManager.getAllItems();
        if (!items.isEmpty()) {
            this.editingItem = items.get(0);
        } else {
            // Create a dummy item to edit
            this.editingItem = new CustomItem(UUID.randomUUID(), Material.STONE,
                    Component.text("Dummy Item"), new ArrayList<>(), 0, Rarity.COMMON, false, null);
        }
        // Copy the properties from the editingItem to our fields
        this.id = editingItem.getId();
        this.material = editingItem.getMaterial();
        this.name = editingItem.getName();
        this.lore = new ArrayList<>(editingItem.getLore());
        this.customModelData = editingItem.getCustomModelData();
        this.rarity = editingItem.getRarity();
        this.glow = editingItem.isGlow();
        this.inventory = Bukkit.createInventory(null, 6 * 9, Component.text("Edit Item: " + name));
        initialize();
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Set up the form fields (similar to AddItemGUI)
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

    // Methods to handle clicks (similar to AddItemGUI)
    public void handleSaveClick(Player player) {
        // Update the editing item with the new values
        editingItem.setMaterial(material);
        editingItem.setName(name);
        editingItem.setLore(lore);
        editingItem.setCustomModelData(customModelData);
        editingItem.setRarity(rarity);
        editingItem.setGlow(glow);

        // Save the changes
        configManager.updateItem(editingItem);
        configManager.saveItems();

        player.sendMessage("Item updated successfully!");
        player.closeInventory();
    }

    public void handleCancelClick(Player player) {
        player.sendMessage("Item editing cancelled.");
        player.closeInventory();
    }
}