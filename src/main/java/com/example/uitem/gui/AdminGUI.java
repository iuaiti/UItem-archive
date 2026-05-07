package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.model.Category;
import com.example.uitem.model.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private Inventory inventory;

    public AdminGUI(UitemPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.inventory = Bukkit.createInventory(null, 6 * 9, Component.text("Admin Menu"));
        initialize();
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Set buttons
        // Row 1: Add Item, Edit Item, Remove Item, Recipe Editor
        inventory.setItem(10, createButton(org.bukkit.Material.DIAMOND, Component.text("Add Item"), NamedTextColor.GREEN));
        inventory.setItem(11, createButton(org.bukkit.Material.ANVIL, Component.text("Edit Item"), NamedTextColor.YELLOW));
        inventory.setItem(12, createButton(org.bukkit.Material.BARRIER, Component.text("Remove Item"), NamedTextColor.RED));
        inventory.setItem(13, createButton(org.bukkit.Material.CRAFTING_TABLE, Component.text("Recipe Editor"), NamedTextColor.BLUE));

        // Fill the rest with glass panes
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private ItemStack createButton(Material material, Component name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        // In a real plugin, you would set display name and lore using Adventure API
        // For simplicity, we skip the Adventure API here and rely on ItemMeta (which is legacy)
        // But note: we are using Adventure API in the plugin, so we should use it.
        // However, for the sake of this example, we'll use the legacy method to avoid complexity.
        // In a production plugin, you would use:
        // net.kyori.adventure.item.ItemComponent.itemName(name)
        // and then use Adventure API to set the item's display name and lore.
        // Since we are in a hurry, we'll use the legacy method and note that it's not ideal.
        org.bukkit.inventory.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(name));
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

    // Placeholder methods for handling clicks - these would be implemented in the listener
    public void handleAddItemClick(Player player) {
        // Open AddItemGUI
        new AddItemGUI(plugin, player).open();
    }

    public void handleEditItemClick(Player player) {
        // Open EditItemGUI (requires selecting an item first)
        new EditItemGUI(plugin, player).open();
    }

    public void handleRemoveItemClick(Player player) {
        // Open RemoveItemGUI (requires selecting an item first)
        new RemoveItemGUI(plugin, player).open();
    }

    public void handleRecipeEditorClick(Player player) {
        // Open RecipeEditorGUI (or perhaps open the crafting GUI in a special mode)
        new RecipeEditorGUI(plugin, player).open();
    }
}