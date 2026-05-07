package com.example.uitem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Represents a category for grouping custom items.
 */
public class Category implements ConfigurationSerializable {

    private String name;
    private Component displayName;
    private Material iconItem;
    private List<UUID> items; // List of item IDs in this category

    public Category(String name, Component displayName, Material iconItem, List<UUID> items) {
        this.name = name;
        this.displayName = displayName;
        this.iconItem = iconItem;
        this.items = items != null ? items : new ArrayList<>();
    }

    // Getters and Setters
    public String getName() { return name; }
    public Component getDisplayName() { return displayName; }
    public Material getIconItem() { return iconItem; }
    public List<UUID> getItems() { return items; }

    public void setName(String name) { this.name = name; }
    public void setDisplayName(Component displayName) { this.displayName = displayName; }
    public void setIconItem(Material iconItem) { this.iconItem = iconItem; }
    public void setItems(List<UUID> items) { this.items = items; }

    /**
     * Adds an item ID to this category.
     * @param itemId The UUID of the item to add
     */
    public void addItem(UUID itemId) {
        if (!items.contains(itemId)) {
            items.add(itemId);
        }
    }

    /**
     * Removes an item ID from this category.
     * @param itemId The UUID of the item to remove
     */
    public void removeItem(UUID itemId) {
        items.remove(itemId);
    }

    // ConfigurationSerializable implementation
    @Override
    public void serialize(ConfigurationSection section) {
        section.set("name", name);
        section.set("display_name", LegacyComponentSerializer.legacySection().serialize(displayName));
        section.set("icon_item", iconItem.name());
        List<String> itemStrings = new ArrayList<>();
        for (UUID uuid : items) {
            itemStrings.add(uuid.toString());
        }
        section.set("items", itemStrings);
    }

    public static Category deserialize(ConfigurationSection section) {
        if (section == null) return null;

        try {
            String name = section.getString("name");
            Component displayName = LegacyComponentSerializer.legacySection().deserialize(section.getString("display_name"));
            Material iconItem = Material.valueOf(section.getString("icon_item"));
            List<String> itemStrings = section.getStringList("items");
            List<UUID> items = new ArrayList<>();
            for (String s : itemStrings) {
                items.add(UUID.fromString(s));
            }

            return new Category(name, displayName, iconItem, items);
        } catch (Exception e) {
            // Log the error in a real plugin
            return null;
        }
    }
}