package com.example.uitem.manager;

import com.example.uitem.UitemPlugin;
import com.example.uitem.model.CustomItem;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages recipes for custom items.
 */
public class RecipeManager {

    private final UitemPlugin plugin;
    private final Map<String, CustomItem.Recipe> recipes = new HashMap<>(); // recipe key -> Recipe

    public RecipeManager(UitemPlugin plugin) {
        this.plugin = plugin;
        // Load recipes from config or custom items
        loadRecipes();
    }

    private void loadRecipes() {
        // For now, we will get recipes from custom items that have a recipe set.
        // In a more advanced system, recipes could be stored separately.
        for (CustomItem item : plugin.getConfigManager().getAllItems()) {
            if (item.getRecipe() != null) {
                // Generate a key for the recipe (could be based on the item id or something else)
                String key = item.getId().toString();
                recipes.put(key, item.getRecipe());
            }
        }
    }

    /**
     * Finds a recipe that matches the given crafting matrix.
     * @param matrix 3x3 grid of items (null for empty)
     * @return the resulting ItemStack if a recipe matches, null otherwise
     */
    public ItemStack findMatchingRecipe(ItemStack[] matrix) {
        // We'll check both shaped and shapeless recipes from our recipe list.
        for (CustomItem.Recipe recipe : recipes.values()) {
            if (recipe.isShaped()) {
                if (matchesShapedRecipe(matrix, recipe)) {
                    return recipe.getResult().clone();
                }
            } else {
                if (matchesShapelessRecipe(matrix, recipe)) {
                    return recipe.getResult().clone();
                }
            }
        }
        return null;
    }

    private boolean matchesShapedRecipe(ItemStack[] matrix, CustomItem.Recipe recipe) {
        // This is a simplified version. In a real plugin, you would compare the shape and ingredients.
        // For brevity, we assume the recipe is for a specific item and we just check if the matrix matches a known pattern.
        // Since we don't have a complex recipe structure in the CustomItem.Recipe (we used a placeholder),
        // we will return false for now.
        // TODO: Implement proper recipe matching.
        return false;
    }

    private boolean matchesShapelessRecipe(ItemStack[] matrix, CustomItem.Recipe recipe) {
        // Similarly, placeholder.
        return false;
    }

    // Methods to add, remove, update recipes (for admin GUI)
    public void addRecipe(String key, CustomItem.Recipe recipe) {
        recipes.put(key, recipe);
    }

    public void removeRecipe(String key) {
        recipes.remove(key);
    }

    public CustomItem.Recipe getRecipe(String key) {
        return recipes.get(key);
    }

    public List<CustomItem.Recipe> getAllRecipes() {
        return new ArrayList<>(recipes.values());
    }
}