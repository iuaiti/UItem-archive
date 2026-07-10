package com.uitem;

import com.uitem.provider.ItemProvider;
import com.uitem.provider.ProviderManager;
import com.uitem.rarity.RarityConfig;
import com.uitem.tooltip.TooltipConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all custom items.
 * Provides CRUD operations and builds ItemStacks from UItemData.
 */
public class UItemManager {

    private final UItemPlugin plugin;
    private final Map<String, UItemData> items;
    private final NamespacedKey uitemKey;
    private final MiniMessage miniMessage;

    public UItemManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.items = new ConcurrentHashMap<>();
        this.uitemKey = new NamespacedKey(plugin, "uitem_id");
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Get the NamespacedKey used to mark items.
     */
    public NamespacedKey getUItemKey() {
        return uitemKey;
    }

    /**
     * Add or update a custom item.
     */
    public void registerItem(UItemData data) {
        if (data == null || !UItemData.isValidId(data.getId())) {
            return;
        }
        items.put(data.getId().toLowerCase(Locale.ROOT), data);
    }

    /**
     * Remove a custom item.
     */
    public void removeItem(String id) {
        if (id == null) {
            return;
        }
        items.remove(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Get a UItemData by ID.
     */
    public UItemData getItem(String id) {
        if (id == null) {
            return null;
        }
        return items.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Get all items.
     */
    public Map<String, UItemData> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Clear all items (used on reload).
     */
    public void clearAll() {
        items.clear();
    }


    /**
     * Create a deep-copy snapshot for safe async saving.
     */
    public Map<String, UItemData> snapshotAll() {
        Map<String, UItemData> snapshot = new LinkedHashMap<>();
        List<String> sortedKeys = new ArrayList<>(items.keySet());
        sortedKeys.sort(String::compareToIgnoreCase);
        for (String key : sortedKeys) {
            UItemData data = items.get(key);
            if (data != null) {
                snapshot.put(key, data.clone());
            }
        }
        return snapshot;
    }

    /**
     * Check whether an item exists.
     */
    public boolean hasItem(String id) {
        return id != null && items.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Build an ItemStack from a UItemData.
     * This is the most important method — it turns data into an actual item.
     * The source ItemStack is now decided by the {@link ProviderManager} (vanilla / ia / oraxen / ...).
     */
    public ItemStack buildItemStack(UItemData data) {
        if (data == null) return new ItemStack(Material.STONE);

        // 1) Resolve source via ProviderManager — texture/model is already present in the base ItemStack.
        ItemStack base = plugin.getProviderManager().build(data.getSource()).orElse(null);
        ItemStack itemStack;
        if (base != null) {
            itemStack = base.clone();
        } else {
            // Fallback: parse vanilla material from the source
            String[] parts = ProviderManager.split(data.getSource());
            String fallback = "stone";
            if (parts != null) fallback = parts[1];
            Material material = parseVanillaMaterial(fallback);
            itemStack = new ItemStack(material);
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        // Always apply/override display name + lore from UItemData (for both vanilla and integration base)
        String displayName = data.getDisplayName();
        String rarity = data.getRarity();
        if (displayName != null && !displayName.isEmpty()) {
            if (rarity != null) {
                String prefix = RarityConfig.getRarityPrefix(rarity);
                if (prefix != null && !prefix.isEmpty()) {
                    displayName = prefix + " " + displayName;
                }
            }
            meta.displayName(safeDeserialize(displayName, data.getId()));
        }

        List<Component> loreComponents = new ArrayList<>();
        if (data.getLore() != null) {
            for (String line : data.getLore()) {
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }
        if (rarity != null) {
            List<String> rarityLore = RarityConfig.getRarityLore(rarity);
            for (String line : rarityLore) {
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }

        // Attributes & Stats have no vanilla tooltip once lore is overridden above —
        // render them as lore lines per tooltip.yml so they're actually visible.
        appendAttributeLore(loreComponents, data);
        appendStatLore(loreComponents, data);

        if (!loreComponents.isEmpty()) {
            meta.lore(loreComponents);
        }

        // Enchantments
        if (data.getEnchantments() != null) {
            for (Map.Entry<String, Integer> entry : data.getEnchantments().entrySet()) {
                Enchantment ench = getEnchantmentByKey(entry.getKey());
                if (ench != null) {
                    meta.addEnchant(ench, entry.getValue(), true);
                }
            }
        }

        // Flags
        if (data.getFlags() != null) {
            for (String flagName : data.getFlags()) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Custom Model Data — removed. Texture/model is managed by third-party plugins.
        // The provider returns an ItemStack with the correct CustomModelData already, do not override.

        // Unbreakable
        meta.setUnbreakable(data.isUnbreakable());

        // Glowing (enchant glow effect mà không hiện enchant text)
        if (data.isGlowing() && (data.getEnchantments() == null || data.getEnchantments().isEmpty())) {
            meta.setEnchantmentGlintOverride(true);
        }

        // Durability
        if (meta instanceof Damageable damageable) {
            int maxDur = data.getMaxDurability();
            if (maxDur > 0) {
                damageable.setMaxDamage(maxDur);
                damageable.setDamage(0);
            } else if (maxDur == 0) {
                damageable.setMaxDamage(0);
                damageable.setDamage(0);
            }
        }

        // Attributes
        if (data.getAttributes() != null) {
            for (UItemData.AttributeData attrData : data.getAttributes()) {
                try {
                    Attribute attribute = getAttributeByKey(attrData.getAttribute());
                    if (attribute == null) continue;
                    AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(attrData.getOperation());
                    EquipmentSlotGroup slotGroup = parseEquipmentSlotGroup(attrData.getSlot());

                    NamespacedKey attrKey = new NamespacedKey(plugin, data.getId() + "_" + attrData.getAttribute().toLowerCase() + "_" + attrData.getOperation() + "_" + attrData.getSlot());
                    AttributeModifier modifier = new AttributeModifier(
                            attrKey,
                            attrData.getAmount(),
                            operation,
                            slotGroup
                    );
                    meta.addAttributeModifier(attribute, modifier);
                } catch (Exception e) {
                    plugin.getLogger().warning("Cannot add attribute " + attrData.getAttribute()
                            + " to item " + data.getId() + ": " + e.getMessage());
                }
            }
        }

        // Stack size = MAX stack size of the item (1 = never stacks).
        // Must be set on the meta component — setAmount() only changes the count,
        // it does not stop identical items from merging.
        int stackSize = Math.max(1, Math.min(64, data.getStackSize()));
        meta.setMaxStackSize(stackSize);

        // Tag the item with the PersistentDataContainer
        meta.getPersistentDataContainer().set(uitemKey, PersistentDataType.STRING, data.getId());

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /** Renders data.getAttributes() as lore lines per tooltip.yml's "attributes" section. */
    private void appendAttributeLore(List<Component> loreComponents, UItemData data) {
        if (!TooltipConfig.isAttributesEnabled()) return;
        if (data.getAttributes() == null || data.getAttributes().isEmpty()) return;

        if (!TooltipConfig.getAttributesHeader().isEmpty()) {
            loreComponents.add(safeDeserialize(TooltipConfig.getAttributesHeader(), data.getId()));
        }
        for (UItemData.AttributeData attr : data.getAttributes()) {
            String line = TooltipConfig.getAttributesLine()
                    .replace("%name%", TooltipConfig.getAttributeDisplayName(attr.getAttribute()))
                    .replace("%amount%", formatAmount(attr.getAmount()))
                    .replace("%slot%", attr.getSlot() == null ? "ANY" : attr.getSlot());
            loreComponents.add(safeDeserialize(line, data.getId()));
        }
        if (!TooltipConfig.getAttributesFooter().isEmpty()) {
            loreComponents.add(safeDeserialize(TooltipConfig.getAttributesFooter(), data.getId()));
        }
    }

    /** Renders data.getStats() as lore lines per tooltip.yml's "stats" section. */
    private void appendStatLore(List<Component> loreComponents, UItemData data) {
        if (!TooltipConfig.isStatsEnabled()) return;
        if (data.getStats() == null || data.getStats().isEmpty()) return;

        if (!TooltipConfig.getStatsHeader().isEmpty()) {
            loreComponents.add(safeDeserialize(TooltipConfig.getStatsHeader(), data.getId()));
        }
        for (UItemData.StatData stat : data.getStats()) {
            String suffix = "%";
            try {
                suffix = com.uitem.stat.Stat.valueOf(stat.getStat().toUpperCase(Locale.ROOT)).getSuffix();
            } catch (IllegalArgumentException ignored) {
            }
            String line = TooltipConfig.getStatsLine()
                    .replace("%name%", TooltipConfig.getStatDisplayName(stat.getStat()))
                    .replace("%amount%", formatAmount(stat.getAmount()))
                    .replace("%suffix%", suffix)
                    .replace("%slot%", stat.getSlot() == null ? "ANY" : stat.getSlot());
            loreComponents.add(safeDeserialize(line, data.getId()));
        }
        if (!TooltipConfig.getStatsFooter().isEmpty()) {
            loreComponents.add(safeDeserialize(TooltipConfig.getStatsFooter(), data.getId()));
        }
    }

    /** Formats a double without a trailing ".0" for whole numbers. */
    private String formatAmount(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }

    /**
     * Build an ItemStack for preview in the GUI (with extra info).
     * By default, shows the ID and internal info (for admins/editors).
     */
    public ItemStack buildPreviewItemStack(UItemData data) {
        return buildPreviewItemStack(data, true);
    }

    /**
     * Build an ItemStack for preview in the GUI.
     *
     * @param data    the item data
     * @param showId  if true, appends ID, Material, CMD and click instructions (for admins).
     *                If false, only keeps the original item + player lore (hides the ID for normal players).
     */
    public ItemStack buildPreviewItemStack(UItemData data, boolean showId) {
        ItemStack item = buildItemStack(data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();

            if (showId) {
                lore.add(Component.empty());
                lore.add(miniMessage.deserialize("<dark_gray>──────────────"));
                lore.add(miniMessage.deserialize("<gray>ID: <white>" + data.getId()));
                lore.add(miniMessage.deserialize("<gray>Source: <white>" + data.getSource()));
                if (data.getCategory() != null) {
                    lore.add(miniMessage.deserialize("<gray>Category: <white>" + data.getCategory()
                            + (data.getSubCategory() != null ? " / " + data.getSubCategory() : "")));
                }
                lore.add(miniMessage.deserialize("<dark_gray>──────────────"));
                lore.add(miniMessage.deserialize("<yellow>Left click: <white>Edit"));
                lore.add(miniMessage.deserialize("<red>Right click: <white>Delete"));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // === Helper methods ===

    /**
     * Safe MiniMessage deserialize — fallback to plain text on parse error.
     * Prevents crashes from user-input MiniMessage syntax errors.
     */
    private Component safeDeserialize(String miniMessageStr, String itemId) {
        try {
            return miniMessage.deserialize(miniMessageStr);
        } catch (Exception e) {
            plugin.getLogger().warning(
                "[UItem] Invalid MiniMessage in item '" + itemId + "': " + miniMessageStr
                + " — falling back to plain text. Error: " + e.getMessage());
            return Component.text(miniMessageStr);
        }
    }

    // (old comment removed: logic only applies via the provider base item now)

    /**
     * Parse a vanilla material name (case-insensitive). Returns STONE if invalid.
     */
    public Material parseVanillaMaterial(String name) {
        if (name == null || name.isBlank()) return Material.STONE;
        try {
            Material m = Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
            return m.isItem() ? m : Material.STONE;
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    /**
     * Lấy enchantment từ key string (e.g., "sharpness", "minecraft:sharpness").
     */
    public Enchantment getEnchantmentByKey(String key) {
        if (key == null) return null;
        String cleanKey = key.toLowerCase(Locale.ROOT).replace(" ", "_");
        if (!cleanKey.contains(":")) {
            cleanKey = "minecraft:" + cleanKey;
        }
        try {
            NamespacedKey namespacedKey = NamespacedKey.fromString(cleanKey);
            if (namespacedKey == null) return null;
            @SuppressWarnings("deprecation")
            Registry<Enchantment> registry = Registry.ENCHANTMENT;
            return registry.get(namespacedKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lấy attribute từ key string.
     */
    private Attribute getAttributeByKey(String key) {
        if (key == null) return null;
        String cleanKey = key.toLowerCase(Locale.ROOT).replace(" ", "_");
        if (!cleanKey.contains(":")) {
            cleanKey = "minecraft:" + cleanKey;
        }
        try {
            NamespacedKey namespacedKey = NamespacedKey.fromString(cleanKey);
            if (namespacedKey == null) return null;
            Registry<Attribute> registry = Registry.ATTRIBUTE;
            return registry.get(namespacedKey);
        } catch (Exception e) {
            return null;
        }
    }

    private EquipmentSlotGroup parseEquipmentSlotGroup(String slot) {
        if (slot == null) return EquipmentSlotGroup.ANY;
        return switch (slot.toUpperCase(Locale.ROOT)) {
            case "HAND", "MAINHAND", "MAIN_HAND" -> EquipmentSlotGroup.MAINHAND;
            case "OFF_HAND", "OFFHAND" -> EquipmentSlotGroup.OFFHAND;
            case "HEAD", "HELMET" -> EquipmentSlotGroup.HEAD;
            case "CHEST", "CHESTPLATE" -> EquipmentSlotGroup.CHEST;
            case "LEGS", "LEGGINGS" -> EquipmentSlotGroup.LEGS;
            case "FEET", "BOOTS" -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ANY;
        };
    }

    /**
     * Lấy danh sách tất cả enchantment phổ biến cho GUI.
     */
    public List<Enchantment> getCommonEnchantments() {
        List<Enchantment> list = new ArrayList<>();
        String[] keys = {
                "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect",
                "looting", "sweeping_edge", "efficiency", "silk_touch", "fortune",
                "unbreaking", "mending", "power", "punch", "flame", "infinity",
                "luck_of_the_sea", "lure", "protection", "fire_protection",
                "blast_protection", "projectile_protection", "feather_falling",
                "respiration", "aqua_affinity", "thorns", "depth_strider",
                "frost_walker", "soul_speed", "swift_sneak",
                "channeling", "loyalty", "riptide", "impaling",
                "multishot", "quick_charge", "piercing",
                "wind_burst", "breach", "density"
        };
        for (String key : keys) {
            Enchantment ench = getEnchantmentByKey(key);
            if (ench != null) {
                list.add(ench);
            }
        }
        return list;
    }

    /**
     * Lấy tên hiển thị dễ đọc cho enchantment.
     */
    public String getEnchantmentDisplayName(Enchantment ench) {
        String key = ench.getKey().getKey();
        return switch (key) {
            case "sharpness" -> "Sharpness";
            case "smite" -> "Smite";
            case "bane_of_arthropods" -> "Bane of Arthropods";
            case "knockback" -> "Knockback";
            case "fire_aspect" -> "Fire Aspect";
            case "looting" -> "Looting";
            case "sweeping_edge" -> "Sweeping Edge";
            case "efficiency" -> "Efficiency";
            case "silk_touch" -> "Silk Touch";
            case "fortune" -> "Fortune";
            case "unbreaking" -> "Unbreaking";
            case "mending" -> "Mending";
            case "power" -> "Power";
            case "punch" -> "Punch";
            case "flame" -> "Flame";
            case "infinity" -> "Infinity";
            case "luck_of_the_sea" -> "Luck of the Sea";
            case "lure" -> "Lure";
            case "protection" -> "Protection";
            case "fire_protection" -> "Fire Protection";
            case "blast_protection" -> "Blast Protection";
            case "projectile_protection" -> "Projectile Protection";
            case "feather_falling" -> "Feather Falling";
            case "respiration" -> "Respiration";
            case "aqua_affinity" -> "Aqua Affinity";
            case "thorns" -> "Thorns";
            case "depth_strider" -> "Depth Strider";
            case "frost_walker" -> "Frost Walker";
            case "soul_speed" -> "Soul Speed";
            case "swift_sneak" -> "Swift Sneak";
            case "channeling" -> "Channeling";
            case "loyalty" -> "Loyalty";
            case "riptide" -> "Riptide";
            case "impaling" -> "Impaling";
            case "multishot" -> "Multishot";
            case "quick_charge" -> "Quick Charge";
            case "piercing" -> "Piercing";
            case "wind_burst" -> "Wind Burst";
            case "breach" -> "Breach";
            case "density" -> "Density";
            default -> key.replace("_", " ");
        };
    }
}
