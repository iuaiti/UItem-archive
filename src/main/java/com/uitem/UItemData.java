package com.uitem;

import com.uitem.passive.PassiveData;
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
    private List<AttributeData> attributes;

    // Oraxen/ItemsAdder integration
    private String rarity;
    private List<PassiveData> passives;

    private String oraxenId;
    private String itemsAdderId;

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
        this.attributes = new ArrayList<>();
        this.rarity = null;
        this.passives = new ArrayList<>();
        this.oraxenId = null;
        this.itemsAdderId = null;
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

    public List<PassiveData> getPassives() {
        return passives;
    }

    public void setPassives(List<PassiveData> passives) {
        this.passives = passives;
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

    /**
     * Deep clone UItemData.
     */
    public UItemData clone() {
        UItemData clone = new UItemData(this.id);
        clone.material = this.material;
        clone.displayName = this.displayName;
        clone.lore = new ArrayList<>(this.lore);
        clone.enchantments = new LinkedHashMap<>(this.enchantments);
        clone.flags = new LinkedHashSet<>(this.flags);
        clone.customModelData = this.customModelData;
        clone.unbreakable = this.unbreakable;
        clone.glowing = this.glowing;
        clone.attributes = new ArrayList<>();
        for (AttributeData attribute : this.attributes) {
            clone.attributes.add(attribute.copy());
        }
        clone.rarity = this.rarity;
        clone.passives = new ArrayList<>();
        if (this.passives != null) {
            for (PassiveData p : this.passives) {
                clone.passives.add(p.copy());
            }
        }
        clone.oraxenId = this.oraxenId;
        clone.itemsAdderId = this.itemsAdderId;
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
