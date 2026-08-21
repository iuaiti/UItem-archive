package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.stat.Stat;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified stat/attribute picker — merges what used to be AttributePresetGui
 * (vanilla Attribute-backed stats like Attack Damage, Armor, Health) and the
 * virtual-stat-only presets (Crit, Defense, Lifesteal, ...) into one GUI, since
 * from the item builder's perspective they're both just "stats on the item".
 * Vanilla-backed entries go to {@link UItemData#getAttributes()}, virtual ones
 * to {@link UItemData#getStats()} — the tooltip line for both is rendered the
 * same way by {@link com.uitem.tooltip.TooltipConfig}.
 */
public class StatSystemGui extends AbstractGui {

    private final UItemData data;
    private int page = 0;

    // A unified preset entry. attributeKey != null -> vanilla Attribute-backed (ADD_NUMBER).
    // stat != null -> virtual Stat.
    private record Preset(String name, String attributeKey, Stat stat, double defaultValue,
                           String slot, String desc, Material icon) {
        boolean isAttribute() { return attributeKey != null; }
    }

    private static final List<Preset> PRESETS = List.of(
            // Combat (vanilla attribute)
            new Preset("⚔ Attack Damage", "generic_attack_damage", null, 5.0, "HAND",
                    "Increases damage while held in hand", Material.DIAMOND_SWORD),
            new Preset("⚔ Attack Speed", "generic_attack_speed", null, 1.0, "HAND",
                    "Increases attack speed (default 4.0)", Material.GOLDEN_SWORD),
            new Preset("🏹 Off-Hand Damage", "generic_attack_damage", null, 3.0, "OFF_HAND",
                    "Damage while held in off hand", Material.IRON_SWORD),
            new Preset("💥 Attack Knockback", "generic_attack_knockback", null, 1.0, "HAND",
                    "Increases knockback force when attacking", Material.STICK),

            // Combat (virtual stat)
            new Preset("💢 Crit Chance", null, Stat.CRIT_CHANCE, 10.0, "HAND",
                    "Chance to land a critical hit", Material.IRON_SWORD),
            new Preset("🔥 Crit Damage", null, Stat.CRIT_DAMAGE, 50.0, "HAND",
                    "Bonus damage on a critical hit", Material.BLAZE_POWDER),
            new Preset("🗡 Armor Penetration", null, Stat.ARMOR_PENETRATION, 15.0, "HAND",
                    "Ignores a % of the target's Defense stat", Material.NETHERITE_SWORD),
            new Preset("☠ True Damage", null, Stat.TRUE_DAMAGE, 10.0, "HAND",
                    "% of damage that fully bypasses Defense", Material.WITHER_SKELETON_SKULL),
            new Preset("🩸 Lifesteal", null, Stat.LIFESTEAL, 10.0, "HAND",
                    "Heals for a % of damage dealt", Material.REDSTONE),

            // Defense (vanilla attribute)
            new Preset("🛡 Armor", "generic_armor", null, 4.0, "CHEST",
                    "Increases armor points (default 0)", Material.DIAMOND_CHESTPLATE),
            new Preset("💎 Armor Toughness", "generic_armor_toughness", null, 2.0, "CHEST",
                    "Reduces damage from strong hits", Material.NETHERITE_CHESTPLATE),
            new Preset("🔰 Knockback Resistance", "generic_knockback_resistance", null, 0.5, "CHEST",
                    "Knockback resistance (0-1, 1=immune)", Material.SHIELD),

            // Defense (virtual stat)
            new Preset("🛡 Defense", null, Stat.DEFENSE, 10.0, "CHEST",
                    "Reduces incoming damage", Material.SHIELD),
            new Preset("💨 Dodge Chance", null, Stat.DODGE_CHANCE, 10.0, "CHEST",
                    "Chance to fully dodge an attack", Material.FEATHER),
            new Preset("❤ Health Regen", null, Stat.HEALTH_REGEN, 0.5, "ANY",
                    "Extra HP regenerated per second", Material.GOLDEN_APPLE),

            // Health / Movement (vanilla attribute)
            new Preset("❤ Max Health", "generic_max_health", null, 4.0, "ANY",
                    "Increases max health (+4 = +2 hearts)", Material.GOLDEN_APPLE),
            new Preset("🔄 Absorption", "generic_max_absorption", null, 4.0, "ANY",
                    "Increases max absorption health", Material.ENCHANTED_GOLDEN_APPLE),
            new Preset("🏃 Movement Speed", "generic_movement_speed", null, 0.02, "FEET",
                    "Increases movement speed (default 0.1)", Material.DIAMOND_BOOTS),
            new Preset("🌊 Swimming Speed", "generic_water_movement_efficiency", null, 0.1, "FEET",
                    "Increases swimming speed", Material.TURTLE_HELMET),

            // Skills / Mana (virtual stat)
            new Preset("⏱ Cooldown Reduction", null, Stat.COOLDOWN_REDUCTION, 10.0, "ANY",
                    "Reduces skill cooldowns", Material.CLOCK),
            new Preset("🔵 Mana Regen", null, Stat.MANA_REGEN, 1.0, "ANY",
                    "Extra mana regenerated per second (needs skills hook)", Material.LAPIS_LAZULI),

            // Tool (vanilla attribute)
            new Preset("⛏ Mining Efficiency", "generic_mining_efficiency", null, 2.0, "HAND",
                    "Increases mining speed while held in hand", Material.DIAMOND_PICKAXE),
            new Preset("🎯 Block Interaction Range", "generic_block_interaction_range", null, 1.0, "HAND",
                    "Increases block reach distance", Material.STICK),

            // Tool (virtual stat)
            new Preset("💰 Fortune Bonus", null, Stat.FORTUNE_BONUS, 25.0, "HAND",
                    "Chance to duplicate block drops when mining", Material.DIAMOND_PICKAXE),
            new Preset("📦 Double Drop Chance", null, Stat.DOUBLE_DROP_CHANCE, 20.0, "HAND",
                    "Chance to duplicate mob loot drops", Material.BUNDLE),
            new Preset("⭐ XP Bonus", null, Stat.XP_BONUS, 15.0, "HAND",
                    "Bonus % experience from mob kills", Material.EXPERIENCE_BOTTLE),

            // Ranged (virtual stat)
            new Preset("🏹 Projectile Damage", null, Stat.PROJECTILE_DAMAGE, 20.0, "HAND",
                    "Bonus % damage on shot arrows/tridents", Material.BOW),
            new Preset("💨 Projectile Speed", null, Stat.PROJECTILE_SPEED, 15.0, "HAND",
                    "Bonus % velocity on shot arrows/tridents", Material.SPECTRAL_ARROW),

            // Misc (vanilla attribute)
            new Preset("🍀 Luck", "generic_luck", null, 5.0, "ANY",
                    "Increases luck (affects loot tables)", Material.RABBIT_FOOT),
            new Preset("🪂 Gravity", "generic_gravity", null, -0.04, "ANY",
                    "Changes gravity (negative = floats up)", Material.FEATHER),
            new Preset("📏 Scale", "generic_scale", null, 0.5, "ANY",
                    "Changes character size", Material.SLIME_BALL)
    );

    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public StatSystemGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, "<gold><bold>Stats</bold>");
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

        int totalCount = data.getAttributes().size() + data.getStats().size();
        setItem(4, createItem(Material.NETHER_STAR,
                "<gold><bold>Stat Presets</bold>",
                List.of(
                        "<gray>Choose a stat to add to the item",
                        "<gray>Stats stack from every equipped UItem",
                        "",
                        "<yellow>Current stats: <white>" + totalCount
                )));

        if (totalCount > 0) {
            List<String> lore = new ArrayList<>();
            for (UItemData.AttributeData a : data.getAttributes()) {
                lore.add("<gray>• <white>" + a.getAttribute().replace("generic_", "")
                        + " <yellow>+" + a.getAmount() + " <gray>(" + a.getSlot() + ")");
            }
            for (UItemData.StatData s : data.getStats()) {
                lore.add("<gray>• <white>" + s.getStat() + " <yellow>+" + s.getAmount() + "% <gray>(" + s.getSlot() + ")");
            }
            setItem(8, createItem(Material.BOOK, "<aqua><bold>Current Stats</bold>", lore));
        }

        setItem(0, createItem(Material.TNT,
                "<red><bold>Clear All</bold>",
                List.of("<gray>Removes all attributes and stats", "<red>Cannot be undone!")),
                ev -> {
                    data.getAttributes().clear();
                    data.getStats().clear();
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    player.sendMessage(TextUtil.success("All stats removed!"));
                    refresh();
                });

        int perPage = GRID_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil(PRESETS.size() / (double) perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * perPage;
        int end = Math.min(PRESETS.size(), start + perPage);
        for (int i = start; i < end; i++) {
            Preset preset = PRESETS.get(i);
            int slot = GRID_SLOTS[i - start];
            setItem(slot, createItem(preset.icon(),
                    "<gold>" + preset.name(),
                    List.of(
                            "<gray>" + preset.desc(),
                            "",
                            "<gray>Default: <white>+" + preset.defaultValue() + (preset.isAttribute() ? "" : "%"),
                            "<gray>Slot: <white>" + preset.slot(),
                            "",
                            "<yellow>Click to add"
                    )), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                promptForValue(preset);
            });
        }

        if (totalPages > 1) {
            if (page > 0) {
                setItem(45, createPreviousButton(), ev -> { page--; refresh(); });
            }
            if (page < totalPages - 1) {
                setItem(53, createNextButton(), ev -> { page++; refresh(); });
            }
        }

        // Custom attribute (vanilla Attribute) button — for keys not covered by presets
        setItem(48, createItem(Material.COMMAND_BLOCK,
                "<light_purple><bold>⚙ Custom Attribute</bold>",
                List.of(
                        "<gray>Enter a vanilla attribute manually",
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
                        // No 'cancel' check needed: ChatInputManager intercepts the
                        // cancel words and reopens this GUI before the callback runs.
                        String[] parts = input.trim().split("\\s+");
                        if (parts.length < 4) {
                            player.sendMessage(TextUtil.error("Format: attribute amount operation slot"));
                            reopen();
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
                        reopen();
                    });
        });

        // Back
        setItem(49, createBackButton(), ev -> goBack());
    }

    private void promptForValue(Preset preset) {
        if (preset.isAttribute() && "ANY".equals(preset.slot())) {
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

    private void promptForAmount(Preset preset, String slot) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter value for <gold>" + preset.name() + "\n" +
                "<gray>Default: <white>" + preset.defaultValue() + (preset.isAttribute() ? "" : "%") + "\n" +
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
                    if (preset.isAttribute()) {
                        data.getAttributes().add(new UItemData.AttributeData(preset.attributeKey(), amount, "ADD_NUMBER", slot));
                    } else {
                        data.getStats().add(new UItemData.StatData(preset.stat().name(), amount, slot));
                    }
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                    player.sendMessage(TextUtil.success("Added: " + preset.name() + " +" + amount
                            + (preset.isAttribute() ? "" : "%") + " (" + slot + ")"));
                    reopen();
                });
    }
}
