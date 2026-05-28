package com.example.uitem.manager;

import com.example.uitem.UitemPlugin;
import com.example.uitem.model.CustomItem;
import com.example.uitem.model.Category;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigManager {

    private final UitemPlugin plugin;
    private File itemsFile;
    private FileConfiguration itemsConfig;
    private File categoriesFile;
    private FileConfiguration categoriesConfig;

    private final Map<UUID, CustomItem> items = new HashMap<>();
    private final Map<String, Category> categories = new HashMap<>();

    public ConfigManager(UitemPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadItems() {
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        items.clear();

        if (itemsConfig.contains("items")) {
            ConfigurationSection section = itemsConfig.getConfigurationSection("items");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ConfigurationSection itemSection = section.getConfigurationSection(key);
                    if (itemSection != null) {
                        CustomItem item = CustomItem.load(itemSection);
                        if (item != null) {
                            items.put(item.getId(), item);
                        }
                    }
                }
            }
        }
    }

    public void saveItems() {
        if (itemsConfig == null) return;
        itemsConfig.set("items", null);
        ConfigurationSection section = itemsConfig.createSection("items");
        for (Map.Entry<UUID, CustomItem> entry : items.entrySet()) {
            CustomItem item = entry.getValue();
            ConfigurationSection itemSection = section.createSection(entry.getKey().toString());
            item.save(itemSection);
        }
        try {
            itemsConfig.save(itemsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save items.yml: " + e.getMessage());
        }
    }

    public void loadCategories() {
        categoriesFile = new File(plugin.getDataFolder(), "categories.yml");
        if (!categoriesFile.exists()) {
            plugin.saveResource("categories.yml", false);
        }
        categoriesConfig = YamlConfiguration.loadConfiguration(categoriesFile);
        categories.clear();

        if (categoriesConfig.contains("categories")) {
            ConfigurationSection section = categoriesConfig.getConfigurationSection("categories");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ConfigurationSection catSection = section.getConfigurationSection(key);
                    if (catSection != null) {
                        Category category = Category.load(catSection);
                        if (category != null) {
                            categories.put(category.getName(), category);
                        }
                    }
                }
            }
        }
    }

    public void saveCategories() {
        if (categoriesConfig == null) return;
        categoriesConfig.set("categories", null);
        ConfigurationSection section = categoriesConfig.createSection("categories");
        for (Map.Entry<String, Category> entry : categories.entrySet()) {
            Category category = entry.getValue();
            ConfigurationSection catSection = section.createSection(entry.getKey());
            category.save(catSection);
        }
        try {
            categoriesConfig.save(categoriesFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save categories.yml: " + e.getMessage());
        }
    }

    public CustomItem getItem(UUID id) {
        return items.get(id);
    }

    public List<CustomItem> getAllItems() {
        return new ArrayList<>(items.values());
    }

    public void addItem(CustomItem item) {
        items.put(item.getId(), item);
    }

    public void removeItem(UUID id) {
        items.remove(id);
    }

    public void updateItem(CustomItem item) {
        items.put(item.getId(), item);
    }

    public Category getCategory(String name) {
        return categories.get(name);
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categories.values());
    }

    public void addCategory(Category category) {
        categories.put(category.getName(), category);
    }

    public void removeCategory(String name) {
        categories.remove(name);
    }

    public void updateCategory(Category category) {
        categories.put(category.getName(), category);
    }

    public ItemStack applyItemMeta(ItemStack stack, CustomItem item) {
        if (stack == null || item == null) return stack;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Set display name
        meta.displayName(item.getName());

        // Set lore
        meta.lore(item.getLore());

        // Set custom model data
        if (item.getCustomModelData() > 0) {
            NamespacedKey key = new NamespacedKey(plugin, "custom_model_data");
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(key, PersistentDataType.INTEGER, item.getCustomModelData());
        }

        // Set rarity (using enchantment glow as example)
        if (item.isGlow()) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        stack.setItemMeta(meta);
        return stack;
    }
}