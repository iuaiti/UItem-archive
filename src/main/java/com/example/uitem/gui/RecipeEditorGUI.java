package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.model.CustomItem;
import com.example.uitem.model.CustomItem.Rarity;
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
import java.util.UUID;

/**
 * GUI for editing a recipe for a custom item.
 * For simplicity, this just opens the crafting GUI and allows the player to set up a recipe.
 * In a real implementation, you would save the recipe to the item when the player confirms.
 */
public class RecipeEditorGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private Inventory inventory;
    private final Player player;
    private CustomItem editingItem; // The item whose recipe is being edited

    public RecipeEditorGUI(UitemPlugin plugin, Player player) {
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
        this.inventory = Bukkit.createInventory(null, 5 * 9, Component.text("Edit Recipe for " + editingItem.getName()));
        initialize();
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // We'll use a crafting GUI inside this GUI? Or we can just open the crafting GUI and let the player set the recipe.
        // For simplicity, we'll just show a message and a button to open the crafting GUI.
        inventory.setItem(10, createButtonItem(org.bukkit.Material.CRAFTING_TABLE,
                Component.text("Open Crafting to Set Recipe"), NamedTextColor.BLUE));
        inventory.setItem(12, createButtonItem(org.bukkit.Material.LIME_STAINED_GLASS_PANE,
                Component.text("Save"), NamedTextColor.GREEN));
        inventory.setItem(14, createButtonItem(org.bukkit.Material.RED_STAINED_GLASS_PANE,
                Component.text("Cancel"), NamedTextColor.RED));

        // Fill remaining slots with glass panes
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
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

    // Methods to handle clicks
    public void handleOpenCraftingClick(Player player) {
        // Open the crafting GUI for this player
        new CraftingGUI(plugin).open(player);
        // We would need to somehow link the crafting GUI to this recipe editor to save the recipe when done.
        // For simplicity, we just open it and note that the player can set up a recipe and then save it via the save button.
        // In a real implementation, we would pass a callback or store the state.
    }

    public void handleSaveClick(Player player) {
        // Save the current crafting matrix as the recipe for the editingItem
        // We would need to get the crafting GUI's matrix for this player. Since we don't have a reference, we skip.
        player.sendMessage("Recipe saving not fully implemented.");
        player.closeInventory();
    }

    public void handleCancelClick(Player player) {
        player.sendMessage("Recipe editing cancelled.");
        player.closeInventory();
    }
}