package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI chỉnh sửa một custom item.
 * Hiển thị tất cả thuộc tính có thể chỉnh sửa.
 */
public class ItemEditorGui extends AbstractGui {

    private final UItemData data;

    public ItemEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, GuiTheme.titleEdit(data.getId()));
        this.data = data;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // === Preview item (slot 4) ===
        ItemStack preview = plugin.getItemManager().buildItemStack(data);
        setItem(4, preview);

        // === Material (slot 10) - Open Material Picker ===
        setItem(10, createItem(Material.CRAFTING_TABLE,
                "<gold><bold>Material</bold>",
                List.of(
                        "<gray>Current: <white>" + (data.getMaterial() != null ? data.getMaterial().name() : "STONE"),
                        "",
                        "<yellow>Click to open material list",
                        "<gray>View all blocks, items, tools, armor..."
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new MaterialPickerGui(plugin, player, data, null).open();
        });

        // === Display Name (slot 11) ===
        setItem(11, createItem(Material.NAME_TAG,
                "<gold><bold>Display Name</bold>",
                List.of(
                        "<gray>Current: " + (data.getDisplayName() != null ? data.getDisplayName() : "<gray>Not set"),
                        "",
                        "<yellow>Click to change name",
                        "<gray>Supports MiniMessage format",
                        "<gray>Example: <red>Ruby <gold>Sword"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter display name <gray>(supports MiniMessage):",
                    input -> {
                        data.setDisplayName(input);
                        saveAndReopen();
                        player.sendMessage(TextUtil.success("Renamed to: " + input));
                    });
        });

        // === Lore (slot 12) ===
        setItem(12, createItem(Material.WRITABLE_BOOK,
                "<gold><bold>Lore</bold>",
                List.of(
                        "<gray>Lines: <white>" + data.getLore().size(),
                        "",
                        "<yellow>Click to edit lore",
                        "<gray>Open Lore Editor GUI"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LoreEditorGui(plugin, player, data).open();
        });

        // === Enchantments (slot 13) ===
        setItem(13, createItem(Material.ENCHANTING_TABLE,
                "<gold><bold>Enchantments</bold>",
                List.of(
                        "<gray>Enchantments: <white>" + data.getEnchantments().size(),
                        "",
                        enchantmentSummary(),
                        "",
                        "<yellow>Click to edit enchantments"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EnchantmentGui(plugin, player, data).open();
        });

        // === Flags (slot 14) ===
        setItem(14, createItem(Material.WHITE_BANNER,
                "<gold><bold>Item Flags</bold>",
                List.of(
                        "<gray>Flags: <white>" + data.getFlags().size(),
                        "",
                        flagsSummary(),
                        "",
                        "<yellow>Click to edit flags"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new FlagGui(plugin, player, data).open();
        });

        // === Custom Model Data (slot 15) ===
        setItem(15, createItem(Material.PAINTING,
                "<gold><bold>Custom Model Data</bold>",
                List.of(
                        "<gray>Current: <white>" + (data.getCustomModelData() > 0 ? data.getCustomModelData() : "None"),
                        "",
                        "<yellow>Click to change",
                        "<gray>Enter number (-1 to disable)"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter Custom Model Data number <gray>(-1 to disable):",
                    input -> {
                        try {
                            int cmd = Integer.parseInt(input.trim());
                            data.setCustomModelData(cmd);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Custom Model Data = " + cmd));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Unbreakable (slot 16) ===
        setItem(16, createItem(
                data.isUnbreakable() ? Material.BEDROCK : Material.COBBLESTONE,
                "<gold><bold>Unbreakable</bold>",
                List.of(
                        "<gray>Current: " + (data.isUnbreakable() ? "<green>ON" : "<red>OFF"),
                        "",
                        "<yellow>Click to toggle"
                )), event -> {
            data.setUnbreakable(!data.isUnbreakable());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isUnbreakable() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // === Durability (slot 17) ===
        setItem(17, createItem(Material.ANVIL,
                "<gold><bold>Durability</bold>",
                List.of(
                        "<gray>Max Durability: <white>" + (data.getMaxDurability() > 0 ? data.getMaxDurability() : "Default"),
                        "",
                        "<yellow>Click to set max durability",
                        "<gray>Enter -1 for default material durability",
                        "<gray>Enter 0 for unbreakable-like (no damage bar)",
                        "<gray>Or enter any number > 0"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter max durability <gray>(-1 = default, 0 = no damage bar, >0 = custom):",
                    input -> {
                        try {
                            int dur = Integer.parseInt(input.trim());
                            data.setMaxDurability(dur);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Max durability = " + (dur > 0 ? dur : (dur == 0 ? "No bar" : "Default"))));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Glowing (slot 19) ===
        setItem(19, createItem(
                data.isGlowing() ? Material.GLOWSTONE : Material.STONE,
                "<gold><bold>Glowing</bold>",
                List.of(
                        "<gray>Current: " + (data.isGlowing() ? "<green>ON" : "<red>OFF"),
                        "",
                        "<yellow>Click to toggle",
                        "<gray>Add glowing effect to item"
                )), event -> {
            data.setGlowing(!data.isGlowing());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isGlowing() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // === Attributes (slot 20) ===
        setItem(20, createItem(Material.IRON_SWORD,
                "<gold><bold>Attributes</bold>",
                List.of(
                        "<gray>Attributes: <white>" + data.getAttributes().size(),
                        "",
                        "<yellow>Click to add attribute",
                        "<gray>Format: attribute amount operation slot",
                        "<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter attribute <gray>(format: attribute amount operation slot)\n" +
                            "<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND\n" +
                            "<gray>Type 'clear' to remove all attributes:",
                    input -> {
                        if (input.equalsIgnoreCase("clear")) {
                            data.getAttributes().clear();
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Removed all attributes!"));
                            return;
                        }
                        String[] parts = input.trim().split("\\s+");
                        if (parts.length < 4) {
                            player.sendMessage(TextUtil.error("Format: attribute amount operation slot"));
                            new ItemEditorGui(plugin, player, data).open();
                            return;
                        }
                        try {
                            String attr = parts[0];
                            double amount = Double.parseDouble(parts[1]);
                            String operation = parts[2].toUpperCase();
                            String slot = parts[3].toUpperCase();
                            data.getAttributes().add(new UItemData.AttributeData(attr, amount, operation, slot));
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Added attribute " + attr + "!"));
                        } catch (Exception e) {
                            player.sendMessage(TextUtil.error("Error: " + e.getMessage()));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Rarity (slot 21) ===
        String currentRarity = data.getRarity();
        String rarityDisplay = currentRarity != null ? currentRarity : "None";
        setItem(21, createItem(Material.NETHER_STAR,
                "<gold><bold>Rarity</bold>",
                List.of(
                        "<gray>Current: " + (currentRarity != null ? RarityConfig.getRarityColor(currentRarity) + currentRarity : "<gray>None"),
                        "",
                        "<yellow>Click to choose rarity",
                        "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            StringBuilder msg = new StringBuilder("<gold>Available rarities:<dark_gray> ");
            for (String r : RarityConfig.getKeys()) {
                msg.append("\n").append(RarityConfig.getRarityColor(r)).append("▸ ").append(r);
            }
            msg.append("\n\n<yellow>Enter rarity name (or 'none' to remove):");
            plugin.getChatInputManager().requestInput(player,
                    msg.toString(),
                    input -> {
                        if (input.equalsIgnoreCase("none")) {
                            data.setRarity(null);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Removed rarity!"));
                            return;
                        }
                        String upper = input.toUpperCase().trim();
                        if (RarityConfig.getKeys().contains(upper)) {
                            data.setRarity(upper);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Set rarity = " + upper));
                        } else {
                            player.sendMessage(TextUtil.error("Rarity '" + input + "' does not exist!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === ExecutableItems ID (slot 22) - only show if ExecutableItems is present ===
        if (plugin.getIntegrationManager().hasExecutableItems()) {
            setItem(22, createItem(Material.BLAZE_POWDER,
                    "<gold><bold>⚡ ExecutableItems</bold>",
                    List.of(
                            "<gray>Current: <white>" + (data.getExecutableItemsId() != null ? data.getExecutableItemsId() : "None"),
                            "",
                            "<yellow>Click to set ExecutableItems ID",
                            "<gray>Skills/Passives will be handled by EI"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter ExecutableItems item ID <gray>(or 'none' to remove):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setExecutableItemsId(null);
                            } else {
                                data.setExecutableItemsId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === Oraxen ID (slot 23) - only show if Oraxen is present ===
        if (plugin.getIntegrationManager().hasOraxen()) {
            setItem(23, createItem(Material.DIAMOND,
                    "<aqua><bold>Oraxen ID</bold>",
                    List.of(
                            "<gray>Current: <white>" + (data.getOraxenId() != null ? data.getOraxenId() : "None"),
                            "",
                            "<yellow>Click to set Oraxen item ID",
                            "<gray>Use item from Oraxen as base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter Oraxen item ID <gray>(or 'none' to remove):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setOraxenId(null);
                            } else {
                                data.setOraxenId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === ItemsAdder ID (slot 24) - only show if ItemsAdder is present ===
        if (plugin.getIntegrationManager().hasItemsAdder()) {
            setItem(24, createItem(Material.EMERALD,
                    "<green><bold>ItemsAdder ID</bold>",
                    List.of(
                            "<gray>Current: <white>" + (data.getItemsAdderId() != null ? data.getItemsAdderId() : "None"),
                            "",
                            "<yellow>Click to set ItemsAdder item ID",
                            "<gray>Use item from ItemsAdder as base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter ItemsAdder namespaced ID <gray>(or 'none' to remove):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setItemsAdderId(null);
                            } else {
                                data.setItemsAdderId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === Recipe (slot 25) - Open Recipe Editor ===
        boolean hasRecipe = plugin.getRecipeManager().hasRecipe(data.getId());
        setItem(25, createItem(
                hasRecipe ? Material.CRAFTING_TABLE : Material.FURNACE,
                "<gold><bold>Crafting Recipe</bold>",
                List.of(
                        hasRecipe ? "<green>Has recipe" : "<red>No recipe",
                        "",
                        "<yellow>Click to edit recipe",
                        "<gray>Set shaped/shapeless crafting recipe"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RecipeEditorGui(plugin, player, data).open();
        });

        setItem(45, createHomeButton(), event -> {
            GuiFeedback.click(player);
            new MainHubGui(plugin, player).open();
        });

        setItem(46, createBackButton(), event -> {
            GuiFeedback.click(player);
            new ItemListGui(plugin, player).open();
        });

        setItem(49, GuiTheme.actionPrimary(Material.LIME_CONCRETE,
                "SAVE",
                "<gray>Luu thay doi vao file",
                "<yellow>Click de luu"), event -> {
            plugin.getItemManager().registerItem(data);
            plugin.getResourcePackManager().syncCmdsToLiveItems();
            plugin.getStorageManager().saveAllNow(plugin.getItemManager());
            player.sendMessage(TextUtil.success("Saved item '" + data.getId() + "'!"));
            GuiFeedback.success(player);
            refresh();
        });

        setItem(52, GuiTheme.actionDanger(Material.RED_CONCRETE,
                "DELETE",
                "<gray>Xoa item nay",
                "<red>Khong the hoan tac"), event -> {
            GuiFeedback.click(player, false);
            new ConfirmDeleteGui(plugin, player, data, 0).open();
        });
    }

    private void saveAndReopen() {
        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveAllNow(plugin.getItemManager());
        new ItemEditorGui(plugin, player, data).open();
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveAllNow(plugin.getItemManager());
        refresh();
    }

    private String enchantmentSummary() {
        if (data.getEnchantments().isEmpty()) return "<gray>No enchantments";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (var entry : data.getEnchantments().entrySet()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getEnchantments().size() - 3).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(entry.getKey()).append(" ").append(entry.getValue());
            count++;
        }
        return sb.toString();
    }

    private String flagsSummary() {
        if (data.getFlags().isEmpty()) return "<gray>No flags";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String flag : data.getFlags()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getFlags().size() - 3).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(flag);
            count++;
        }
        return sb.toString();
    }
}
