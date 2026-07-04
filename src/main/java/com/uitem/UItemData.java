package com.uitem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Data class representing a custom item in UItem.
 * <p>
 * Texture/model is NO LONGER managed here — {@link com.uitem.provider.ItemProvider}
 * returns an ItemStack that already has its texture/model. UItemData only holds the
 * LOGIC of the item: skills, rarity, attributes, cooldown, requirements, category, etc.
 */
public class UItemData {

    private final String id;

    /** Source string in "namespace:id" form, e.g., "vanilla:diamond_sword", "ia:ruby_sword". */
    private String source;

    /** Top-level category: Weapon / Armor / Tool / Ranged / Magic / Consumable / Material / Misc. */
    private String category;

    /** Sub-category: Sword / Axe / Helmet / Pickaxe / Bow / ... (optional, may be null). */
    private String subCategory;

    private String displayName; // MiniMessage
    private List<String> lore; // Each line is a MiniMessage string
    private String rarity;

    private Map<String, Integer> enchantments; // key = enchantment key (e.g., "sharpness"), value = level
    private Set<String> flags; // ItemFlag names

    private boolean unbreakable;
    private boolean glowing;
    private int maxDurability; // -1 = use material default
    private int stackSize = 1; // 1-64

    private List<AttributeData> attributes;

    // Permission required to use/hold the item (server-admin may set null = unlimited)
    private String permission;
    // Item-wide cooldown (seconds) — used for skill/use actions. -1 = none.
    private double cooldown = -1;
    // Minimum player level required to hold/use. 0 = no requirement.
    private int requiredLevel = 0;

    // General attachable effects (reflect, extra damage on hit/damage/equip/fullset/die)
    private List<ItemEffect> effects = new ArrayList<>();

    // Native UItem Skills & Passives
    private List<com.uitem.skill.Skill> skills = new ArrayList<>();

    public UItemData(String id) {
        this.id = id;
        this.source = "vanilla:stone";
        this.displayName = "<white>" + id;
        this.lore = new ArrayList<>();
        this.enchantments = new LinkedHashMap<>();
        this.flags = new LinkedHashSet<>();
        this.unbreakable = false;
        this.glowing = false;
        this.maxDurability = -1;
        this.stackSize = 1;
        this.attributes = new ArrayList<>();
        this.effects = new ArrayList<>();
        this.skills = new ArrayList<>();
    }

    // === Getters & Setters ===

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source != null && !source.isBlank() ? source.toLowerCase(Locale.ROOT) : "vanilla:stone";
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null && !category.isBlank() ? category.toUpperCase(Locale.ROOT) : null;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory != null && !subCategory.isBlank()
                ? subCategory.toUpperCase(Locale.ROOT) : null;
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
        this.lore = lore != null ? lore : new ArrayList<>();
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments != null ? enchantments : new LinkedHashMap<>();
    }

    public Set<String> getFlags() {
        return flags;
    }

    public void setFlags(Set<String> flags) {
        this.flags = flags != null ? flags : new LinkedHashSet<>();
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

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = Math.max(1, Math.min(64, stackSize));
    }

    public List<AttributeData> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeData> attributes) {
        this.attributes = attributes != null ? attributes : new ArrayList<>();
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = (permission == null || permission.isBlank()) ? null : permission;
    }

    public double getCooldown() {
        return cooldown;
    }

    public void setCooldown(double cooldown) {
        this.cooldown = cooldown;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = Math.max(0, requiredLevel);
    }

    public List<ItemEffect> getEffects() {
        return effects;
    }

    public void setEffects(List<ItemEffect> effects) {
        this.effects = effects != null ? effects : new ArrayList<>();
    }

    public List<com.uitem.skill.Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<com.uitem.skill.Skill> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    /** Deep clone UItemData. */
    public UItemData clone() {
        UItemData clone = new UItemData(this.id);
        clone.source = this.source;
        clone.category = this.category;
        clone.subCategory = this.subCategory;
        clone.displayName = this.displayName;
        clone.lore = new ArrayList<>(this.lore);
        clone.rarity = this.rarity;
        clone.enchantments = new LinkedHashMap<>(this.enchantments);
        clone.flags = new LinkedHashSet<>(this.flags);
        clone.unbreakable = this.unbreakable;
        clone.glowing = this.glowing;
        clone.maxDurability = this.maxDurability;
        clone.stackSize = this.stackSize;
        clone.attributes = new ArrayList<>();
        for (AttributeData a : this.attributes) clone.attributes.add(a.copy());
        clone.permission = this.permission;
        clone.cooldown = this.cooldown;
        clone.requiredLevel = this.requiredLevel;
        clone.effects = new ArrayList<>();
        for (ItemEffect e : this.effects) clone.effects.add(e.copy());
        clone.skills = new ArrayList<>();
        for (com.uitem.skill.Skill s : this.skills) clone.skills.add(s.copy());
        return clone;
    }

    /** General effect attachable to a custom item. */
    public static class ItemEffect {
        private String trigger;     // HIT, DAMAGE, DIE, EQUIP, FULL_SET
        private String type;        // REFLECT, EXTRA_DAMAGE
        private double chance = 100.0;
        private double value = 0.0;
        private boolean applyToTarget = false;

        public ItemEffect() {}

        public String getTrigger() { return trigger; }
        public void setTrigger(String trigger) {
            this.trigger = trigger != null ? trigger.toUpperCase(Locale.ROOT) : null;
        }

        public String getType() { return type; }
        public void setType(String type) {
            this.type = type != null ? type.toUpperCase(Locale.ROOT) : null;
        }

        public double getChance() { return chance; }
        public void setChance(double chance) { this.chance = Math.max(0, Math.min(100, chance)); }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public boolean isApplyToTarget() { return applyToTarget; }
        public void setApplyToTarget(boolean applyToTarget) { this.applyToTarget = applyToTarget; }

        public ItemEffect copy() {
            ItemEffect c = new ItemEffect();
            c.trigger = this.trigger;
            c.type = this.type;
            c.chance = this.chance;
            c.value = this.value;
            c.applyToTarget = this.applyToTarget;
            return c;
        }
    }

    /** Attribute modifier data. */
    public static class AttributeData {
        private String attribute;
        private double amount;
        private String operation; // ADD_NUMBER | ADD_SCALAR | MULTIPLY_SCALAR_1
        private String slot;      // HAND | OFF_HAND | HEAD | CHEST | LEGS | FEET | ANY

        public AttributeData(String attribute, double amount, String operation, String slot) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
            this.slot = slot;
        }

        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public String getSlot() { return slot; }
        public void setSlot(String slot) { this.slot = slot; }

        public AttributeData copy() { return new AttributeData(attribute, amount, operation, slot); }
    }
}
