package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * GUI to pick an attribute preset instead of typing it manually.
 * Similar to Oraxen/ItemsAdder but entirely through a GUI.
 *
 * The player picks an attribute type, then a slot, then enters a value.
 */
public class AttributePresetGui extends AbstractGui {

    private final UItemData data;

    // Preset: name, attribute key, default value, operation, description
    private record AttrPreset(String name, String key, double defaultValue, String operation, String slot, String desc, Material icon) {}

    private static final List<AttrPreset> PRESETS = List.of(
            // Combat
            new AttrPreset("⚔ Main Hand Damage", "generic_attack_damage", 5.0, "ADD_NUMBER", "HAND",
                    "Increases damage while held in hand", Material.DIAMOND_SWORD),
            new AttrPreset("⚔ Attack Speed", "generic_attack_speed", 1.0, "ADD_NUMBER", "HAND",
                    "Increases attack speed (default 4.0)", Material.GOLDEN_SWORD),
            new AttrPreset("🏹 Off-Hand Damage", "generic_attack_damage", 3.0, "ADD_NUMBER", "OFF_HAND",
                    "Damage while held in off hand", Material.IRON_SWORD),
            new AttrPreset("💥 Attack Knockback", "generic_attack_knockback", 1.0, "ADD_NUMBER", "HAND",
                    "Increases knockback force when attacking", Material.STICK),

            // Defense
            new AttrPreset("🛡 Armor", "generic_armor", 4.0, "ADD_NUMBER", "CHEST",
                    "Increases armor points (default 0)", Material.DIAMOND_CHESTPLATE),
            new AttrPreset("💎 Armor Toughness", "generic_armor_toughness", 2.0, "ADD_NUMBER", "CHEST",
                    "Reduces damage from strong hits", Material.NETHERITE_CHESTPLATE),
            new AttrPreset("🔰 Knockback Resistance", "generic_knockback_resistance", 0.5, "ADD_NUMBER", "CHEST",
                    "Knockback resistance (0-1, 1=immune)", Material.SHIELD),

            // Movement
            new AttrPreset("🏃 Movement Speed", "generic_movement_speed", 0.02, "ADD_NUMBER", "FEET",
                    "Increases movement speed (default 0.1)", Material.DIAMOND_BOOTS),
            new AttrPreset("🌊 Swimming Speed", "generic_water_movement_efficiency", 0.1, "ADD_NUMBER", "FEET",
                    "Increases swimming speed", Material.TURTLE_HELMET),

            // Health
            new AttrPreset("❤ Max Health", "generic_max_health", 4.0, "ADD_NUMBER", "ANY",
                    "Increases max health (+4 = +2 hearts)", Material.GOLDEN_APPLE),
            new AttrPreset("🔄 Absorption", "generic_max_absorption", 4.0, "ADD_NUMBER", "ANY",
                    "Increases max absorption health", Material.ENCHANTED_GOLDEN_APPLE),

            // Luck
            new AttrPreset("🍀 Luck", "generic_luck", 5.0, "ADD_NUMBER", "ANY",
                    "Increases luck (affects loot tables)", Material.RABBIT_FOOT),

            // Gravity
            new AttrPreset("🪂 Gravity", "generic_gravity", -0.04, "ADD_NUMBER", "ANY",
                    "Changes gravity (negative = floats up)", Material.FEATHER),

            // Scale
            new AttrPreset("📏 Scale", "generic_scale", 0.5, "ADD_NUMBER", "ANY",
                    "Changes character size", Material.SLIME_BALL)
    );

    public AttributePresetGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, "<gold><bold>Choose Attribute</bold>");
        this.data = data;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.IRON_SWORD,
                "<gold><bold>Attribute Presets</bold>",
                List.of(
                        "<gray>Choose an attribute to add to the item",
                        "<gray>You will enter the value afterwards",
                        "",
                        "<yellow>Current attributes: <white>" + data.getAttributes().size()
                )));

        // Display current attributes
        if (!data.getAttributes().isEmpty()) {
            setItem(8, createItem(Material.BOOK,
                    "<aqua><bold>Current Attributes</bold>",
                    data.getAttributes().stream()
                            .map(a -> "<gray>• <white>" + a.getAttribute() + " <yellow>+" + a.getAmount() + " <gray>(" + a.getSlot() + ")")
                            .toList()));
        }

        // Clear all button
        setItem(0, createItem(Material.TNT,
                "<red><bold>Clear All</bold>",
                List.of("<gray>Removes all attached attributes", "<red>Cannot be undone!")),
                ev -> {
                    data.getAttributes().clear();
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    player.sendMessage(TextUtil.success("All attributes removed!"));
                    refresh();
                });

        // Preset buttons
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(PRESETS.size(), slots.length); i++) {
            AttrPreset preset = PRESETS.get(i);
            setItem(slots[i], createItem(preset.icon(),
                    "<gold>" + preset.name(),
                    List.of(
                            "<gray>" + preset.desc(),
                            "",
                            "<gray>Key: <white>" + preset.key(),
                            "<gray>Default: <white>+" + preset.defaultValue(),
                            "<gray>Slot: <white>" + preset.slot(),
                            "",
                            "<yellow>Click to add"
                    )), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                promptForValue(preset);
            });
        }

        // Custom attribute button
        setItem(31, createItem(Material.COMMAND_BLOCK,
                "<light_purple><bold>⚙ Custom Attribute</bold>",
                List.of(
                        "<gray>Enter an attribute manually",
                        "<gray>Format: attribute amount operation slot",
                        "<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND",
                        "",
                        "<yellow>Click to enter"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter attribute <gray>(attribute amount operation slot)\n" +
                    "<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND\n" +
                    "<gray>Type 'cancel' to abort:",
                    input -> {
                        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("huy")) {
                            new AttributePresetGui(plugin, player, data).openAsChildOf(null);
                            return;
                        }
                        String[] parts = input.trim().split("\\s+");
                        if (parts.length < 4) {
                            player.sendMessage(TextUtil.error("Format: attribute amount operation slot"));
                            new AttributePresetGui(plugin, player, data).open();
                            return;
                        }
                        try {
                            String attr = parts[0];
                            double amount = Double.parseDouble(parts[1]);
                            String operation = parts[2].toUpperCase();
                            String slot = parts[3].toUpperCase();
                            data.getAttributes().add(new UItemData.AttributeData(attr, amount, operation, slot));
                            plugin.getItemManager().registerItem(data);
                            plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                            player.sendMessage(TextUtil.success("Added attribute: " + attr + " +" + amount));
                        } catch (Exception e) {
                            player.sendMessage(TextUtil.error("Error: " + e.getMessage()));
                        }
                        new AttributePresetGui(plugin, player, data).open();
                    });
        });

        // Back
        setItem(49, createBackButton(), ev -> goBack());
    }

    private void promptForValue(AttrPreset preset) {
        // Ask for slot first if it's ANY
        if ("ANY".equals(preset.slot())) {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Choose slot for <gold>" + preset.name() + "\n" +
                    "<gray>Enter: HAND / OFF_HAND / HEAD / CHEST / LEGS / FEET / ANY\n" +
                    "<gray>Or press Enter to use ANY:",
                    slotInput -> {
                        String slot = slotInput.trim().isEmpty() ? "ANY" : slotInput.trim().toUpperCase();
                        promptForAmount(preset, slot);
                    });
        } else {
            promptForAmount(preset, preset.slot());
        }
    }

    private void promptForAmount(AttrPreset preset, String slot) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter value for <gold>" + preset.name() + "\n" +
                "<gray>Default: <white>" + preset.defaultValue() + "\n" +
                "<gray>Enter a number or press Enter to use the default:",
                input -> {
                    double amount = preset.defaultValue();
                    if (!input.trim().isEmpty()) {
                        try {
                            amount = Double.parseDouble(input.trim());
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number, using default value: " + preset.defaultValue()));
                        }
                    }
                    data.getAttributes().add(new UItemData.AttributeData(preset.key(), amount, preset.operation(), slot));
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                    player.sendMessage(TextUtil.success("Added: " + preset.name() + " +" + amount + " (" + slot + ")"));
                    new AttributePresetGui(plugin, player, data).open();
                });
    }
}
