package com.example.uitem.listener;

import com.example.uitem.UitemPlugin;
import com.example.uitem.gui.AddItemGUI;
import com.example.uitem.gui.AdminGUI;
import com.example.uitem.gui.CategoryGUI;
import com.example.uitem.gui.EditItemGUI;
import com.example.uitem.gui.RecipeEditorGUI;
import com.example.uitem.gui.RemoveItemGUI;
import com.example.uitem.model.Category;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Listens for inventory clicks and handles GUI interactions.
 */
public class GUIListener implements Listener {

    private final UitemPlugin plugin;

    public GUIListener(UitemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Check if the clicked inventory is one of our GUIs
        var title = event.getView().title();
        if (isMainMenu(title)) {
            handleMainMenuClick(event, player);
        } else if (isCategoryGUI(title)) {
            handleCategoryGUIClick(event, player);
        } else if (isCraftingGUI(title)) {
            handleCraftingGUIClick(event, player);
        } else if (isAdminGUI(title)) {
            handleAdminGUIClick(event, player);
        }
        // Note: We don't cancel the event by default to allow for other interactions (like taking items in crafting)
        // But we should cancel if we don't want the item to be taken (e.g., when clicking on a button)
        // We'll handle cancellation in the specific methods.
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Optional: save any temporary state or clean up
    }

    private boolean isMainMenu(Component title) {
        return Component.text("UITEM Main Menu").equals(title);
    }

    private boolean isCategoryGUI(Component title) {
        if (title == null) return false;
        return title.toString().contains("Category: ");
    }

    private boolean isCraftingGUI(Component title) {
        return Component.text("Crafting GUI").equals(title);
    }

    private boolean isAdminGUI(Component title) {
        return Component.text("Admin Menu").equals(title);
    }

    private void handleMainMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true); // Prevent taking items from the menu
        int slot = event.getSlot();
        // In the main menu, each slot represents a category (we have 9 slots per row, but we only filled the first few)
        // We'll get the category from the slot index.
        // Note: This is simplified. In a real plugin, we would map slots to categories.
        // For now, we assume the categories are in the first few slots (0,1,2,...) and we have a list of categories.
        var categories = plugin.getConfigManager().getAllCategories();
        if (slot < categories.size()) {
            Category category = categories.get(slot);
            new CategoryGUI(plugin, category).open(player);
        }
        // If clicked on a glass pane or empty slot, do nothing.
    }

    private void handleCategoryGUIClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true); // Prevent taking items from the category GUI
        int slot = event.getSlot();
        // We don't have a direct mapping from slot to item in the category GUI because we stored items in the inventory.
        // We'll get the item from the clicked slot.
        var itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType().isAir()) return;

        // In a real plugin, we would map the item stack back to a custom item.
        // For simplicity, we'll just open the item's details or do nothing.
        // We can open an item editor or just notify the player.
        player.sendMessage("You clicked on an item in the category.");
        // TODO: Implement item details view or editing.
    }

    private void handleCraftingGUIClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
    }

    private void handleAdminGUIClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true); // Prevent taking items from the admin GUI
        int slot = event.getSlot();
        if (slot == 10) {
            // Add Item button
            new AddItemGUI(plugin, player).open(player);
        } else if (slot == 11) {
            new EditItemGUI(plugin, player).open(player);
        } else if (slot == 12) {
            new RemoveItemGUI(plugin, player).open(player);
        } else if (slot == 13) {
            new RecipeEditorGUI(plugin, player).open(player);
        }
        // Ignore other slots (glass panes)
    }
}