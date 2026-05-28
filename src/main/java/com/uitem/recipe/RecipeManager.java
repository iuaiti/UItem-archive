package com.uitem.recipe;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class RecipeManager {

    private final UItemPlugin plugin;
    private final UItemManager itemManager;
    private final Map<String, UItemRecipe> recipes;
    private final File recipesFile;

    public RecipeManager(UItemPlugin plugin, UItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.recipes = new LinkedHashMap<>();
        this.recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        loadRecipes();
    }

    public Map<String, UItemRecipe> getAllRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    public UItemRecipe getRecipe(String itemId) {
        return recipes.get(itemId.toLowerCase(Locale.ROOT));
    }

    public void setRecipe(String itemId, UItemRecipe recipe) {
        if (recipe != null) {
            recipes.put(itemId.toLowerCase(Locale.ROOT), recipe);
        }
    }

    public void removeRecipe(String itemId) {
        recipes.remove(itemId.toLowerCase(Locale.ROOT));
    }

    public boolean hasRecipe(String itemId) {
        return recipes.containsKey(itemId.toLowerCase(Locale.ROOT));
    }

    public ItemStack matchRecipe(ItemStack[] matrix) {
        for (UItemRecipe recipe : recipes.values()) {
            if (recipe.matches(matrix)) {
                UItemData data = itemManager.getItem(recipe.getResultItemId());
                if (data != null) {
                    ItemStack result = itemManager.buildItemStack(data);
                    result.setAmount(recipe.getResultAmount());
                    return result;
                }
            }
        }
        return null;
    }

    public ItemStack matchRecipeAndConsume(ItemStack[] matrix) {
        for (Map.Entry<String, UItemRecipe> entry : recipes.entrySet()) {
            UItemRecipe recipe = entry.getValue();
            if (recipe.matches(matrix)) {
                UItemData data = itemManager.getItem(recipe.getResultItemId());
                if (data != null) {
                    ItemStack result = itemManager.buildItemStack(data);
                    result.setAmount(recipe.getResultAmount());
                    return result;
                }
            }
        }
        return null;
    }

    public void loadRecipes() {
        recipes.clear();
        if (!recipesFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection recipeSection = section.getConfigurationSection(key);
                if (recipeSection == null) continue;
                UItemRecipe recipe = UItemRecipe.load(recipeSection);
                if (recipe != null) {
                    recipes.put(key.toLowerCase(Locale.ROOT), recipe);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not load recipe '" + key + "'", e);
            }
        }
    }

    public void saveRecipes() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, UItemRecipe> entry : recipes.entrySet()) {
            ConfigurationSection section = config.createSection("recipes." + entry.getKey());
            entry.getValue().save(section);
        }
        try {
            config.save(recipesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save recipes.yml", e);
        }
    }
}
