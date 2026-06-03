package com.uitem;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

/**
 * Data class đại diện cho một custom item trong UItem.
 * Chứa toàn bộ thông tin cần thiết để build một ItemStack.
 */
public class UItemData {

    private final String id;
    private Material material;
    private String displayName; // MiniMessage format
    private List<String> lore; // Mỗi dòng là MiniMessage string
    private Map<String, Integer> enchantments; // key = enchantment key (e.g., "sharpness"), value = level
    private Set<String> flags; // ItemFlag names
    private int customModelData; // -1 = không set
    private boolean unbreakable;
    private boolean glowing;
    private int maxDurability; // -1 = use default material durability
    private List<AttributeData> attributes;

    // Oraxen/ItemsAdder/ExecutableItems integration
    private String rarity;

    private String oraxenId;
    private String itemsAdderId;
    private String executableItemsId;

    /** Resource pack overrides (Oraxen / ItemsAdder style per-item config). */
    private String texturePath;
    private String modelParent;
    private List<String> textureLayers;
    private boolean excludeFromPack;

    public UItemData(String id) {
        this.id = id;
        this.material = Material.STONE;
        this.displayName = "<white>" + id;
        this.lore = new ArrayList<>();
        this.enchantments = new LinkedHashMap<>();
        this.flags = new LinkedHashSet<>();
        this.customModelData = -1;
        this.unbreakable = false;
        this.glowing = false;
        this.maxDurability = -1;
        this.attributes = new ArrayList<>();
        this.rarity = null;
        this.oraxenId = null;
        this.itemsAdderId = null;
        this.executableItemsId = null;
        this.texturePath = null;
        this.modelParent = null;
        this.textureLayers = new ArrayList<>();
        this.excludeFromPack = false;
    }

    // === Getters & Setters ===

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public void setFlags(Set<String> flags) {
        this.flags = flags;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public void setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
    }

    public List<AttributeData> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeData> attributes) {
        this.attributes = attributes;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getOraxenId() {
        return oraxenId;
    }

    public void setOraxenId(String oraxenId) {
        this.oraxenId = oraxenId;
    }

    public String getItemsAdderId() {
        return itemsAdderId;
    }

    public void setItemsAdderId(String itemsAdderId) {
        this.itemsAdderId = itemsAdderId;
    }

    public String getExecutableItemsId() {
        return executableItemsId;
    }

    public void setExecutableItemsId(String executableItemsId) {
        this.executableItemsId = executableItemsId;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public String getModelParent() {
        return modelParent;
    }

    public void setModelParent(String modelParent) {
        this.modelParent = modelParent;
    }

    public List<String> getTextureLayers() {
        return textureLayers;
    }

    public void setTextureLayers(List<String> textureLayers) {
        this.textureLayers = textureLayers != null ? new ArrayList<>(textureLayers) : new ArrayList<>();
    }

    public boolean isExcludeFromPack() {
        return excludeFromPack;
    }

    public void setExcludeFromPack(boolean excludeFromPack) {
        this.excludeFromPack = excludeFromPack;
    }

    /**
     * Deep clone UItemData.
     */
    public UItemData clone() {
        UItemData clone = new UItemData(this.id);
        clone.material = this.material;
        clone.displayName = this.displayName;
        clone.lore = this.lore != null ? new ArrayList<>(this.lore) : new ArrayList<>();
        clone.enchantments = this.enchantments != null ? new LinkedHashMap<>(this.enchantments) : new LinkedHashMap<>();
        clone.flags = this.flags != null ? new LinkedHashSet<>(this.flags) : new LinkedHashSet<>();
        clone.customModelData = this.customModelData;
        clone.unbreakable = this.unbreakable;
        clone.glowing = this.glowing;
        clone.maxDurability = this.maxDurability;
        clone.attributes = new ArrayList<>();
        for (AttributeData attribute : this.attributes) {
            clone.attributes.add(attribute.copy());
        }
        clone.rarity = this.rarity;
        clone.oraxenId = this.oraxenId;
        clone.itemsAdderId = this.itemsAdderId;
        clone.executableItemsId = this.executableItemsId;
        clone.texturePath = this.texturePath;
        clone.modelParent = this.modelParent;
        clone.textureLayers = this.textureLayers != null ? new ArrayList<>(this.textureLayers) : new ArrayList<>();
        clone.excludeFromPack = this.excludeFromPack;
        return clone;
    }

    /**
     * Data class cho attribute modifier.
     */
    public static class AttributeData {
        private String attribute; // e.g., "GENERIC_ATTACK_DAMAGE"
        private double amount;
        private String operation; // "ADD_NUMBER", "ADD_SCALAR", "MULTIPLY_SCALAR_1"
        private String slot; // "HAND", "OFF_HAND", "HEAD", "CHEST", "LEGS", "FEET", "ANY"

        public AttributeData(String attribute, double amount, String operation, String slot) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
            this.slot = slot;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot) {
            this.slot = slot;
        }

        public AttributeData copy() {
            return new AttributeData(attribute, amount, operation, slot);
        }
    }
}
