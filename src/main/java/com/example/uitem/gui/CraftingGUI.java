package com.example.uitem.gui;

import com.example.uitem.UitemPlugin;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.manager.RecipeManager;
import com.example.uitem.model.CustomItem;
import com.example.uitem.model.Recipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Crafting GUI for creating custom items.
 */
public class CraftingGUI {

    private final UitemPlugin plugin;
    private final ConfigManager configManager;
    private final RecipeManager recipeManager;
    private Inventory inventory;
    private final Map<Integer, ItemStack> craftingMatrix = new HashMap<>(); // slot -> ItemStack
    private ItemStack resultItem;

    // Crafting grid slots in a 3x3 grid (indices in the inventory)
    private static final int[] CRAFTING_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 22;

    public CraftingGUI(UitemPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.recipeManager = plugin.getRecipeManager();
        // 5x9 inventory = 45 slots (enough for 3x3 grid + result + some padding)
        this.inventory = Bukkit.createInventory(null, 5 * 9, Component.text("Crafting GUI"));
        initialize();
    }

    private void initialize() {
        // Clear inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Clear crafting matrix and result
        craftingMatrix.clear();
        resultItem = null;

        // Update the display
        updateDisplay();
    }

    private void updateDisplay() {
        // Set items in crafting matrix
        for (Map.Entry<Integer, ItemStack> entry : craftingMatrix.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }

        // Set result item
        if (resultItem != null) {
            inventory.setItem(RESULT_SLOT, resultItem);
        } else {
            inventory.setItem(RESULT_SLOT, null);
        }

        // Fill remaining slots with glass panes or other decorative items
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

    /**
     * Sets an item in the crafting matrix at the specified position (0-8).
     * @param position 0-8 representing the 3x3 grid position
     * @param item The item to place
     */
    public void setCraftingItem(int position, ItemStack item) {
        if (position < 0 || position >= CRAFTING_SLOTS.length) return;
        int slot = CRAFTING_SLOTS[position];
        craftingMatrix.put(slot, item == null ? null : item.clone());
        updateResult();
        updateDisplay();
    }

    /**
     * Clears the crafting matrix at the specified position.
     * @param position 0-8 representing the 3x3 grid position
     */
    public void clearCraftingItem(int position) {
        if (position < 0 || position >= CRAFTING_SLOTS.length) return;
        int slot = CRAFTING_SLOTS[position];
        craftingMatrix.remove(slot);
        updateResult();
        updateDisplay();
    }

    /**
     * Gets the item in the crafting matrix at the specified position.
     * @param position 0-8 representing the 3x3 grid position
     * @return The item or null if empty
     */
    public ItemStack getCraftingItem(int position) {
        if (position < 0 || position >= CRAFTING_SLOTS.length) return null;
        int slot = CRAFTING_SLOTS[position];
        return craftingMatrix.getOrDefault(slot, null);
    }

    /**
     * Updates the result item based on the current crafting matrix.
     */
    private void updateResult() {
        // Build the current crafting grid
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = getCraftingItem(i);
        }

        // Check for matching recipes
        resultItem = recipeManager.findMatchingRecipe(matrix);
        updateDisplay();
    }

    /**
     * Clears the entire crafting matrix.
     */
    public void clearAll() {
        craftingMatrix.clear();
        resultItem = null;
        updateDisplay();
    }

    /**
     * Takes the result item (if any) and gives it to the player.
     * @param player The player to give the result to
     * @return true if an item was taken, false otherwise
     */
    public boolean takeResult(Player player) {
        if (resultItem == null) return false;

        // Give the result item to the player
        if (player.getInventory().firstEmpty() == -1) {
            // Inventory full, drop item
            player.getWorld().dropItem(player.getLocation(), resultItem.clone());
        } else {
            player.getInventory().addItem(resultItem.clone());
        }

        // Consume the ingredients
        for (int i = 0; i < 9; i++) {
            ItemStack item = getCraftingItem(i);
            if (item != null && item.getAmount() > 0) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    clearCraftingItem(i);
                }
            }
        }

        return true;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }
}