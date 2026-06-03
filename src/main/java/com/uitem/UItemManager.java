package com.uitem;

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

import com.uitem.rarity.RarityConfig;

/**
 * Quản lý toàn bộ custom items.
 * Cung cấp CRUD operations và build ItemStack từ UItemData.
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
     * Lấy NamespacedKey dùng để đánh dấu item.
     */
    public NamespacedKey getUItemKey() {
        return uitemKey;
    }

    /**
     * Thêm hoặc cập nhật một custom item.
     */
    public void registerItem(UItemData data) {
        if (data == null || data.getId() == null || data.getId().isBlank()) {
            return;
        }
        items.put(data.getId().toLowerCase(Locale.ROOT), data);
    }

    /**
     * Xóa một custom item.
     */
    public void removeItem(String id) {
        if (id == null) {
            return;
        }
        items.remove(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Lấy UItemData theo ID.
     */
    public UItemData getItem(String id) {
        if (id == null) {
            return null;
        }
        return items.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Lấy tất cả items.
     */
    public Map<String, UItemData> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Xóa tất cả items (dùng khi reload).
     */
    public void clearAll() {
        items.clear();
    }


    /**
     * Tạo snapshot deep-copy để lưu async an toàn.
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
     * Kiểm tra item tồn tại.
     */
    public boolean hasItem(String id) {
        return id != null && items.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Build ItemStack từ UItemData.
     * Đây là method quan trọng nhất — chuyển đổi data thành item thực tế.
     */
    public ItemStack buildItemStack(UItemData data) {
        if (data == null) return new ItemStack(Material.STONE);

        // Kiểm tra integration trước
        ItemStack integrationItem = plugin.getIntegrationManager().tryBuildFromIntegration(data);
        if (integrationItem != null) {
            // Nếu integration trả về item, chỉ áp dụng thêm display name và lore
            applyDisplayProperties(integrationItem, data);
            tagItem(integrationItem, data.getId());
            return integrationItem;
        }

        // Build từ vanilla
        Material material = data.getMaterial() != null && data.getMaterial().isItem() ? data.getMaterial() : Material.STONE;
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        // Display Name with rarity prefix
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

        // Lore with rarity lore (bo dong ky thuat neu cau hinh an ID)
        boolean hideMeta = plugin.getConfig().getBoolean("display.hide-item-id-in-game", true);
        List<Component> loreComponents = new ArrayList<>();
        if (data.getLore() != null) {
            for (String line : com.uitem.util.TextUtil.expandLoreLines(data.getLore())) {
                if (hideMeta && com.uitem.util.ItemDisplayUtil.isTechnicalLoreLine(line)) {
                    continue;
                }
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }
        if (rarity != null) {
            List<String> rarityLore = RarityConfig.getRarityLore(rarity);
            for (String line : rarityLore) {
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }
        if (!loreComponents.isEmpty()) {
            meta.lore(com.uitem.util.TextUtil.withLeadingSpacer(loreComponents));
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

        // Custom Model Data
        if (data.getCustomModelData() > 0) {
            meta.setCustomModelData(data.getCustomModelData());
        }

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

                    NamespacedKey attrKey = new NamespacedKey(plugin, data.getId() + "_" + attrData.getAttribute().toLowerCase());
                    AttributeModifier modifier = new AttributeModifier(
                            attrKey,
                            attrData.getAmount(),
                            operation,
                            slotGroup
                    );
                    meta.addAttributeModifier(attribute, modifier);
                } catch (Exception e) {
                    plugin.getLogger().warning("Không thể thêm attribute " + attrData.getAttribute()
                            + " cho item " + data.getId() + ": " + e.getMessage());
                }
            }
        }

        if (plugin.getConfig().getBoolean("display.hide-item-id-in-game", true)) {
            com.uitem.util.ServerVersionUtil.applyHideAdditionalTooltip(meta);
        }

        // Tag item với PersistentDataContainer
        meta.getPersistentDataContainer().set(uitemKey, PersistentDataType.STRING, data.getId());

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Build ItemStack cho preview trong GUI (có thêm thông tin phụ).
     */
    public ItemStack buildPreviewItemStack(UItemData data) {
        return buildPreviewItemStack(data, plugin.getConfig().getBoolean("display.show-id-in-editor-preview", true));
    }

    /**
     * Preview trong GUI admin — có thể hiện ID / material / CMD.
     */
    public ItemStack buildPreviewItemStack(UItemData data, boolean showAdminMeta) {
        ItemStack item = buildItemStack(data);
        if (!showAdminMeta) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            else lore = new ArrayList<>(com.uitem.util.TextUtil.withLeadingSpacer(lore));
            lore.add(Component.empty());
            lore.add(miniMessage.deserialize("<dark_gray>──────────────"));
            lore.add(miniMessage.deserialize("<gray>ID: <white>" + data.getId()));
            Material previewMaterial = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
            lore.add(miniMessage.deserialize("<gray>Material: <white>" + previewMaterial.name()));
            lore.add(miniMessage.deserialize("<gray>CMD: <white>" + (data.getCustomModelData() > 0 ? data.getCustomModelData() : "Không")));
            lore.add(miniMessage.deserialize("<dark_gray>──────────────"));
            lore.add(miniMessage.deserialize("<yellow>Click trái: <white>Chỉnh sửa"));
            lore.add(miniMessage.deserialize("<red>Click phải: <white>Xóa"));
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

    private void applyDisplayProperties(ItemStack item, UItemData data) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
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
        boolean hideMeta = plugin.getConfig().getBoolean("display.hide-item-id-in-game", true);
        if (data.getLore() != null) {
            for (String line : com.uitem.util.TextUtil.expandLoreLines(data.getLore())) {
                if (hideMeta && com.uitem.util.ItemDisplayUtil.isTechnicalLoreLine(line)) {
                    continue;
                }
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }
        if (rarity != null) {
            for (String line : RarityConfig.getRarityLore(rarity)) {
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }
        if (!loreComponents.isEmpty()) {
            meta.lore(com.uitem.util.TextUtil.withLeadingSpacer(loreComponents));
        }
        item.setItemMeta(meta);
    }

    private void tagItem(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(uitemKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
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
