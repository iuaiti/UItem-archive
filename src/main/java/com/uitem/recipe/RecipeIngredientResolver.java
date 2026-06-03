package com.uitem.recipe;

import com.uitem.UItemPlugin;
import org.bukkit.inventory.ItemStack;

/**
 * Nhan dien nguyen lieu recipe tu ItemStack (uitem / material). Potion UItem khong dung lam NL recipe.
 */
public final class RecipeIngredientResolver {

    private RecipeIngredientResolver() {
    }

    public static UItemRecipe.Ingredient fromStack(UItemPlugin plugin, ItemStack item) {
        if (plugin == null || item == null || item.getType().isAir()) {
            return null;
        }
        if (plugin.getPotionManager().isUPotion(item)) {
            return null;
        }
        var itemManager = plugin.getItemManager();
        if (item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(itemManager.getUItemKey(), org.bukkit.persistence.PersistentDataType.STRING)) {
            String uitemId = item.getItemMeta().getPersistentDataContainer()
                    .get(itemManager.getUItemKey(), org.bukkit.persistence.PersistentDataType.STRING);
            if (uitemId != null && !uitemId.isBlank()) {
                return new UItemRecipe.Ingredient(uitemId, item.getAmount());
            }
        }
        return new UItemRecipe.Ingredient(item.getType(), item.getAmount());
    }
}