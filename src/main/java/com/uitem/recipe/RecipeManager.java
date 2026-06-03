package com.uitem.recipe;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.storage.ExternalStorageSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class RecipeManager {

    private static final List<String> DEFAULT_RECIPE_RESOURCES = List.of(
            "recipes/ruby_sword.yml",
            "recipes/vial_blade.yml"
    );

    private final UItemPlugin plugin;
    private final UItemManager itemManager;
    private final Map<String, UItemRecipe> recipes;
    private ExternalStorageSettings settings;

    public RecipeManager(UItemPlugin plugin, UItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.recipes = new LinkedHashMap<>();
        this.settings = new ExternalStorageSettings(plugin);
        ensureDefaultExternalFiles();
        loadRecipes();
    }

    public void reloadSettings() {
        this.settings = new ExternalStorageSettings(plugin);
    }

    public void ensureDefaultExternalFiles() {
        if (!settings.useRecipeFolder()) {
            return;
        }
        if (!settings.recipesDirectory().exists()) {
            settings.recipesDirectory().mkdirs();
        }
        for (String resourcePath : DEFAULT_RECIPE_RESOURCES) {
            File target = new File(plugin.getDataFolder(), resourcePath);
            if (!target.exists()) {
                plugin.saveResource(resourcePath, false);
            }
        }
        if (settings.importLegacyOnLoad() && settings.recipesLegacyFile().exists()
                && isDirectoryEmpty(settings.recipesDirectory())) {
            plugin.getLogger().info("[UItem] Importing recipes from legacy recipes.yml into recipes/ folder...");
            importLegacyRecipesFile();
        }
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
        String key = itemId.toLowerCase(Locale.ROOT);
        recipes.remove(key);
        if (settings.useRecipeFolder()) {
            File file = recipeFileForId(key);
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("Could not delete recipe file: " + file.getName());
            }
        }
    }

    public boolean hasRecipe(String itemId) {
        return recipes.containsKey(itemId.toLowerCase(Locale.ROOT));
    }

    /**
     * Các recipe có output khác dùng {@code uitemId} làm nguyên liệu (craft chain).
     */
    public List<String> findRecipesUsingIngredient(String uitemId) {
        return findRecipesUsingUItemIngredient(uitemId);
    }

    public List<String> findRecipesUsingUItemIngredient(String uitemId) {
        if (uitemId == null || uitemId.isBlank()) {
            return List.of();
        }
        String id = uitemId.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, UItemRecipe> entry : recipes.entrySet()) {
            if (recipeUsesUItem(entry.getValue(), id)) {
                out.add(entry.getKey());
            }
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static boolean recipeUsesUItem(UItemRecipe recipe, String uitemId) {
        if (recipe == null) {
            return false;
        }
        if (recipe.isShaped()) {
            for (UItemRecipe.Ingredient ing : recipe.getIngredientMap().values()) {
                if (ing != null && uitemId.equalsIgnoreCase(ing.getUitemId())) {
                    return true;
                }
            }
            return false;
        }
        for (UItemRecipe.Ingredient ing : recipe.getIngredients()) {
            if (ing != null && uitemId.equalsIgnoreCase(ing.getUitemId())) {
                return true;
            }
        }
        return false;
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

    @Deprecated
    public ItemStack matchRecipeAndConsume(ItemStack[] matrix) {
        return matchRecipe(matrix);
    }

    public void loadRecipes() {
        reloadSettings();
        recipes.clear();

        if (settings.useRecipeFolder()) {
            if (!settings.recipesDirectory().exists()) {
                settings.recipesDirectory().mkdirs();
            }
            loadFromFolder();
            if (settings.importLegacyOnLoad()) {
                loadLegacyMerge();
            }
        } else {
            loadFromLegacyFile();
        }
        plugin.getLogger().info("[UItem] Loaded " + recipes.size() + " recipe(s).");
    }

    private void loadFromFolder() {
        File[] files = settings.recipesDirectory().listFiles((dir, name) ->
                name != null && name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            String key = recipeKeyFromFile(file);
            if (key == null) continue;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = yaml.getConfigurationSection("recipe");
            if (section == null) {
                section = yaml;
            }
            UItemRecipe recipe = UItemRecipe.load(section);
            if (recipe != null) {
                recipes.put(key, recipe);
                plugin.getLogger().info("  Loaded recipe: " + key + " (" + file.getName() + ")");
            }
        }
    }

    private void loadFromLegacyFile() {
        File legacy = settings.recipesLegacyFile();
        if (!legacy.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(key);
            UItemRecipe recipe = UItemRecipe.load(recipeSection);
            if (recipe != null) {
                recipes.put(key.toLowerCase(Locale.ROOT), recipe);
            }
        }
    }

    private void loadLegacyMerge() {
        File legacy = settings.recipesLegacyFile();
        if (!legacy.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String id = key.toLowerCase(Locale.ROOT);
            if (recipes.containsKey(id)) continue;
            ConfigurationSection recipeSection = section.getConfigurationSection(key);
            UItemRecipe recipe = UItemRecipe.load(recipeSection);
            if (recipe != null) {
                recipes.put(id, recipe);
                plugin.getLogger().info("  Loaded recipe (legacy): " + id);
            }
        }
    }

    private void importLegacyRecipesFile() {
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(settings.recipesLegacyFile());
        ConfigurationSection section = legacy.getConfigurationSection("recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String id = key.toLowerCase(Locale.ROOT);
            File target = recipeFileForId(id);
            if (target.exists()) continue;
            ConfigurationSection recipeSection = section.getConfigurationSection(key);
            if (recipeSection == null) continue;
            YamlConfiguration out = new YamlConfiguration();
            UItemRecipe recipe = UItemRecipe.load(recipeSection);
            if (recipe == null) continue;
            recipe.save(out.createSection("recipe"));
            try {
                out.save(target);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not import recipe '" + id + "'", e);
            }
        }
    }

    public void saveRecipes() {
        doSave();
    }

    public void saveRecipesAsync() {
        Map<String, UItemRecipe> snapshot = new LinkedHashMap<>(recipes);
        boolean useFolder = settings.useRecipeFolder();
        com.uitem.util.SchedulerUtil.runAsync(plugin, () -> writeSnapshot(snapshot, useFolder));
    }

    private void doSave() {
        writeSnapshot(new LinkedHashMap<>(recipes), settings.useRecipeFolder());
    }

    private void writeSnapshot(Map<String, UItemRecipe> snapshot, boolean useFolder) {
        if (useFolder) {
            if (!settings.recipesDirectory().exists()) {
                settings.recipesDirectory().mkdirs();
            }
            for (Map.Entry<String, UItemRecipe> entry : snapshot.entrySet()) {
                String id = entry.getKey().toLowerCase(Locale.ROOT);
                YamlConfiguration yaml = new YamlConfiguration();
                entry.getValue().save(yaml.createSection("recipe"));
                try {
                    yaml.save(recipeFileForId(id));
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save recipe: " + id + ".yml", e);
                }
            }
            File[] files = settings.recipesDirectory().listFiles((dir, name) ->
                    name != null && name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String id = recipeKeyFromFile(file);
                    if (id != null && !snapshot.containsKey(id)) {
                        if (!file.delete()) {
                            plugin.getLogger().warning("Could not delete recipe file: " + file.getName());
                        }
                    }
                }
            }
        } else {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<String, UItemRecipe> entry : snapshot.entrySet()) {
                ConfigurationSection section = config.createSection("recipes." + entry.getKey());
                entry.getValue().save(section);
            }
            try {
                config.save(settings.recipesLegacyFile());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save recipes.yml", e);
            }
        }
    }

    private File recipeFileForId(String id) {
        return new File(settings.recipesDirectory(), id.toLowerCase(Locale.ROOT) + ".yml");
    }

    private static String recipeKeyFromFile(File file) {
        String name = file.getName();
        if (!name.endsWith(".yml")) return null;
        return name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
    }

    private static boolean isDirectoryEmpty(File directory) {
        if (!directory.isDirectory()) return true;
        File[] files = directory.listFiles((dir, name) -> name != null && name.endsWith(".yml"));
        return files == null || files.length == 0;
    }
}