package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.model.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GUI for confirming deletion of an item.
 */
public class ConfirmDeleteGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private Inventory inventory;
    private final Player player;
    private final UUID itemId;

    public ConfirmDeleteGUI(UitemPlugin plugin, Player player, UUID itemId) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.player = player;
        this.itemId = itemId;
        this.inventory = Bukkit.createInventory(null, 3 * 9, Component.text("Confirm Deletion"));
        initialize();
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Get the item to delete
        CustomItem item = configManager.getItem(itemId);
        if (item != null) {
            // Show the item in the middle
            inventory.setItem(13, item.toItemStack());
            // Set a name and lore to indicate it's the item to delete
            ItemStack stack = item.toItemStack();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Delete this item?", NamedTextColor.RED));
                // We could add lore, but for simplicity we just set the display name.
                stack.setItemMeta(meta);
                inventory.setItem(13, stack);
            }
        }

        // Confirm and Cancel buttons
        inventory.setItem(11, createButtonItem(org.bukkit.Material.LIME_STAINED_GLASS_PANE,
                Component.text("Confirm"), NamedTextColor.GREEN));
        inventory.setItem(15, createButtonItem(org.bukkit.Material.RED_STAINED_GLASS_PANE,
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
    public void handleConfirmClick(Player player) {
        // Remove the item
        configManager.removeItem(itemId);
        // Also remove from categories
        for (Category category : configManager.getAllCategories()) {
            category.removeItem(itemId);
        }
        configManager.saveCategories();
        configManager.saveItems();

        player.sendMessage("Item deleted successfully!");
        player.closeInventory();
    }

    public void handleCancelClick(Player player) {
        player.sendMessage("Deletion cancelled.");
        player.closeInventory();
    }
}