package com.example.uitem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Represents a custom item in the plugin.
 */
public class CustomItem {

    private UUID id;
    private Material material;
    private Component name;
    private List<Component> lore;
    private int customModelData;
    private Rarity rarity;
    private boolean glow;
    private Recipe recipe; // Optional recipe for crafting

    public CustomItem(UUID id, Material material, Component name, List<Component> lore,
                      int customModelData, Rarity rarity, boolean glow, Recipe recipe) {
        this.id = id;
        this.material = material;
        this.name = name;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.customModelData = customModelData;
        this.rarity = rarity;
        this.glow = glow;
        this.recipe = recipe;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public Material getMaterial() { return material; }
    public Component getName() { return name; }
    public List<Component> getLore() { return lore; }
    public int getCustomModelData() { return customModelData; }
    public Rarity getRarity() { return rarity; }
    public boolean isGlow() { return glow; }
    public Recipe getRecipe() { return recipe; }

    public void setMaterial(Material material) { this.material = material; }
    public void setName(Component name) { this.name = name; }
    public void setLore(List<Component> lore) { this.lore = lore; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public void setGlow(boolean glow) { this.glow = glow; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }

    /**
     * Creates an ItemStack representation of this custom item.
     * @return ItemStack with the item's properties applied.
     */
    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(name);
        meta.lore(lore);

        // Note: Custom model data is stored in the persistent data container.
        // In a real plugin, you would use a NamespacedKey to store this.
        // For simplicity, we'll skip the persistence here and rely on ConfigManager to apply it.
        // However, we can still set it if we have a way to persist it (like in ConfigManager).

        // Glow effect
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
            meta.addItemFlags(org.bukkit.ItemFlag.HIDE_ENCHANTS);
        }

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Saves the item to a ConfigurationSection.
     * @param section The section to populate
     */
    public void save(ConfigurationSection section) {
        section.set("id", id.toString());
        section.set("material", material.name());
        section.set("name", LegacyComponentSerializer.legacySection().serialize(name));
        List<String> loreStrings = new ArrayList<>();
        for (Component c : lore) {
            loreStrings.add(LegacyComponentSerializer.legacySection().serialize(c));
        }
        section.set("lore", loreStrings);
        section.set("custom_model_data", customModelData);
        section.set("rarity", rarity.name());
        section.set("glow", glow);
        if (recipe != null) {
            ConfigurationSection recipeSection = section.createSection("recipe");
            recipe.save(recipeSection);
        }
    }

    /**
     * Loads a CustomItem from a ConfigurationSection.
     * @param section The section to load from
     * @return The loaded CustomItem, or null if invalid
     */
    public static CustomItem load(ConfigurationSection section) {
        if (section == null) return null;

        try {
            UUID id = UUID.fromString(section.getString("id"));
            Material material = Material.valueOf(section.getString("material"));
            Component name = LegacyComponentSerializer.legacySection().deserialize(section.getString("name"));
            List<String> loreStrings = section.getStringList("lore");
            List<Component> lore = new ArrayList<>();
            for (String s : loreStrings) {
                lore.add(LegacyComponentSerializer.legacySection().deserialize(s));
            }
            int customModelData = section.getInt("custom_model_data", 0);
            Rarity rarity = Rarity.valueOf(section.getString("rarity", "COMMON"));
            boolean glow = section.getBoolean("glow", false);
            Recipe recipe = null;
            if (section.isConfigurationSection("recipe")) {
                recipe = Recipe.load(section.getConfigurationSection("recipe"));
            }

            return new CustomItem(id, material, name, lore, customModelData, rarity, glow, recipe);
        } catch (Exception e) {
            // Log the error in a real plugin
            return null;
        }
    }

    /**
     * Enum for item rarity.
     */
    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
    }

    /**
     * Simple recipe representation.
     * In a full plugin, you would have a more complex recipe structure.
     */
    public static class Recipe {

        private boolean shaped;
        private List<Character> shape; // For shaped: 3x3 grid of characters (or null for shapeless)
        private java.util.Map<Character, ItemStack> ingredientMap; // For shaped: char to ItemStack
        private List<ItemStack> ingredients; // For shapeless: list of ItemStack
        private ItemStack result;

        public Recipe(boolean shaped, List<Character> shape, java.util.Map<Character, ItemStack> ingredientMap,
                      List<ItemStack> ingredients, ItemStack result) {
            this.shaped = shaped;
            this.shape = shape;
            this.ingredientMap = ingredientMap;
            this.ingredients = ingredients;
            this.result = result;
        }

        // Getters
        public boolean isShaped() { return shaped; }
        public List<Character> getShape() { return shape; }
        public java.util.Map<Character, ItemStack> getIngredientMap() { return ingredientMap; }
        public List<ItemStack> getIngredients() { return ingredients; }
        public ItemStack getResult() { return result; }

        /**
         * Saves the recipe to a ConfigurationSection.
         * @param section The section to populate
         */
        public void save(ConfigurationSection section) {
            section.set("shaped", shaped);
            if (shaped) {
                List<String> shapeStrings = new ArrayList<>();
                for (Character c : shape) {
                    shapeStrings.add(c.toString());
                }
                section.set("shape", shapeStrings);
                // Serialize ingredient map
                ConfigurationSection ingSection = section.createSection("ingredients");
                for (java.util.Map.Entry<Character, ItemStack> entry : ingredientMap.entrySet()) {
                    ConfigurationSection is = ingSection.createSection(entry.getKey().toString());
                    // Serialize ItemStack (simplified: we assume we have a way to serialize ItemStack)
                    // In a real plugin, you would use a proper ItemStack serialization.
                    // For brevity, we'll skip and assume we have a method.
                    // This is a placeholder.
                }
            } else {
                // Shapeless: serialize list of ItemStacks
                ConfigurationSection ingSection = section.createSection("ingredients");
                for (int i = 0; i < ingredients.size(); i++) {
                    // Again, placeholder for ItemStack serialization
                }
            }
            // Serialize result ItemStack (placeholder)
        }

        /**
         * Loads a Recipe from a ConfigurationSection.
         * @param section The section to load from
         * @return The loaded Recipe, or null if invalid
         */
        public static Recipe load(ConfigurationSection section) {
            // Placeholder implementation
            return new Recipe(false, null, new java.util.HashMap<>(), new java.util.ArrayList<>(), new ItemStack(Material.AIR));
        }
    }
}