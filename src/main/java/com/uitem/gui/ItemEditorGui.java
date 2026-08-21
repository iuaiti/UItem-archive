package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI for editing a custom item.
 * Only manages LOGIC (rarity, skill, attributes, cooldown, permission, ...).
 * Texture/model is handled by the provider — no "Material" or "Custom Model" button here.
 */
public class ItemEditorGui extends AbstractGui {

    private final UItemData data;

    public ItemEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54,
                "<gradient:#ff6b6b:#ffd93d>Edit: </gradient><white>" + data.getId());
        this.data = data;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    public void handleBottomInventoryClick(InventoryClickEvent event) {
        event.setCancelled(false);
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Preview item (slot 4)
        ItemStack preview = plugin.getItemManager().buildItemStack(data);
        setItem(4, preview);

        // Source (slot 10) - re-open picker to change source
        String source = data.getSource() != null ? data.getSource() : "vanilla:stone";
        setItem(10, createItem(Material.COMPASS,
                "<gold><bold>Item Source</bold>",
                List.of(
                        "<gray>Current: <white>" + source,
                        "",
                        "<yellow>Click to change item source",
                        "<gray>Opens the provider + target picker wizard"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            // Wizard to change source for an existing item: only provider + target steps.
            new CreateWizardGui(plugin, player, data).openAsChildOf(this);
        });

        // Category (slot 11)
        String category = data.getCategory() != null ? data.getCategory() : "-";
        setItem(11, createItem(Material.CHEST,
                "<aqua><bold>Category</bold>",
                List.of(
                        "<gray>Current: <white>" + category,
                        "",
                        "<yellow>Click to choose a category",
                        "<gray>Weapon / Armor / Tool / Ranged / Magic / Consumable / Material / Misc"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CategoryPickerGui(plugin, player, data.getCategory(), picked -> {
                if (picked != null) data.setCategory(picked);
                refresh();
            }).openAsChildOf(this);
        });

        // Sub-category (slot 12)
        String sub = data.getSubCategory() != null ? data.getSubCategory() : "-";
        setItem(12, createItem(Material.CHEST_MINECART,
                "<aqua><bold>Sub-Category</bold>",
                List.of(
                        "<gray>Current: <white>" + sub,
                        "",
                        "<yellow>Click to choose a sub-category",
                        "<gray>Depends on the Category"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new SubCategoryPickerGui(plugin, player, data.getCategory(), data.getSubCategory(), picked -> {
                if (picked != null) data.setSubCategory(picked);
                refresh();
            }).openAsChildOf(this);
        });

        // Display Name (slot 13)
        setItem(13, createItem(Material.NAME_TAG,
                "<gold><bold>Display Name</bold>",
                List.of(
                        "<gray>Current: " + (data.getDisplayName() != null ? data.getDisplayName() : "<gray>Not set"),
                        "",
                        "<yellow>Click to change the name",
                        "<gray>Supports MiniMessage"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter the display name <gray>(supports MiniMessage):",
                    input -> {
                        data.setDisplayName(input);
                        saveAndReopen();
                        player.sendMessage(TextUtil.success(Messages.renamedTo(input)));
                    });
        });

        // Lore (slot 14)
        setItem(14, createItem(Material.WRITABLE_BOOK,
                "<gold><bold>Lore</bold>",
                List.of(
                        "<gray>Lines: <white>" + data.getLore().size(),
                        "",
                        "<yellow>Click to edit lore"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LoreEditorGui(plugin, player, data).openAsChildOf(this);
        });

        // Enchantments (slot 15)
        setItem(15, createItem(Material.ENCHANTING_TABLE,
                "<gold><bold>Enchantments</bold>",
                List.of(
                        "<gray>Enchantments: <white>" + data.getEnchantments().size(),
                        "",
                        enchantmentSummary(),
                        "",
                        "<yellow>Click to edit enchantments"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EnchantmentGui(plugin, player, data).openAsChildOf(this);
        });

        // Flags (slot 16)
        setItem(16, createItem(Material.WHITE_BANNER,
                "<gold><bold>Item Flags</bold>",
                List.of(
                        "<gray>Flags: <white>" + data.getFlags().size(),
                        "",
                        flagsSummary(),
                        "",
                        "<yellow>Click to edit flags"
                )), event -> {
            new FlagGui(plugin, player, data).openAsChildOf(this);
        });

        // Unbreakable (slot 17)
        setItem(17, createItem(
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

        // Durability (slot 18)
        setItem(18, createItem(Material.ANVIL,
                "<gold><bold>Durability</bold>",
                List.of(
                        "<gray>Max durability: <white>" + (data.getMaxDurability() > 0 ? data.getMaxDurability() : "Default"),
                        "",
                        "<yellow>Click to set max durability",
                        "<gray>-1 = default, 0 = no bar, >0 = custom"
                )), event -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter max durability <gray>(-1 = default, 0 = no bar, >0 = custom):",
                    input -> {
                        try {
                            int dur = Integer.parseInt(input.trim());
                            data.setMaxDurability(dur);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success(Messages.durabilitySet(
                                    dur > 0 ? String.valueOf(dur) : (dur == 0 ? "No bar" : "Default"))));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number!"));
                            reopen();
                        }
                    });
        });

        // Glowing (slot 19)
        setItem(19, createItem(
                data.isGlowing() ? Material.GLOWSTONE : Material.STONE,
                "<gold><bold>Glowing</bold>",
                List.of(
                        "<gray>Current: " + (data.isGlowing() ? "<green>ON" : "<red>OFF"),
                        "",
                        "<yellow>Click to toggle"
                )), event -> {
            data.setGlowing(!data.isGlowing());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isGlowing() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // Stats (attributes + virtual stats merged into a single picker) — slot 20
        int statCount = data.getAttributes().size() + data.getStats().size();
        setItem(20, createItem(Material.NETHER_STAR,
                "<gold><bold>Stats</bold>",
                List.of(
                        "<gray>Stats: <white>" + statCount,
                        "",
                        attributeSummary(),
                        statSummary(),
                        "",
                        "<yellow>Click to open the stat picker GUI"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new StatSystemGui(plugin, player, data).openAsChildOf(this);
        });

        // Rarity (slot 21)
        String currentRarity = data.getRarity();
        setItem(21, createItem(Material.NETHER_STAR,
                "<gold><bold>Rarity</bold>",
                List.of(
                        "<gray>Current: " + (currentRarity != null
                                ? RarityConfig.getRarityColor(currentRarity) + currentRarity
                                : "<gray>None"),
                        "",
                        "<yellow>Click to choose a rarity"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            StringBuilder msg = new StringBuilder("<gold>Rarities:<dark_gray> ");
            for (String r : RarityConfig.getKeys()) {
                msg.append("\n").append(RarityConfig.getRarityColor(r)).append("▸ ").append(r);
            }
            msg.append("\n\n<yellow>Enter the rarity name (or 'none' to remove):");
            plugin.getChatInputManager().requestInput(player, msg.toString(), input -> {
                if (input.equalsIgnoreCase("none")) {
                    data.setRarity(null);
                    saveAndReopen();
                    player.sendMessage(TextUtil.success(Messages.guiRarityRemoved()));
                    return;
                }
                String upper = input.toUpperCase().trim();
                if (RarityConfig.getKeys().contains(upper)) {
                    data.setRarity(upper);
                    saveAndReopen();
                    player.sendMessage(TextUtil.success(Messages.guiRaritySet(upper)));
                } else {
                    data.setRarity(upper);
                    plugin.getChatInputManager().requestInput(player,
                        "<yellow>New rarity '" + upper + "'. Enter color (MiniMessage) or press Enter:",
                        colorInput -> {
                            String color = colorInput.trim().isEmpty() ? "<white>" : colorInput.trim();
                            plugin.getChatInputManager().requestInput(player,
                                "<yellow>Prefix or press Enter:",
                                prefixInput -> {
                                    String prefix = prefixInput.trim();
                                    plugin.getChatInputManager().requestInput(player,
                                        "<yellow>Lore lines (separated by | ) or press Enter:",
                                        loreInput -> {
                                            List<String> lore = loreInput.trim().isEmpty()
                                                    ? List.of() : Arrays.asList(loreInput.split("\\|"));
                                            RarityConfig.setRarity(upper, color, prefix, lore);
                                            RarityConfig.saveRarities();
                                            saveAndReopen();
                                            player.sendMessage(TextUtil.success("Created rarity '" + upper + "'"));
                                        });
                                });
                        });
                }
            });
        });

        // Manage Rarities (slot 30)
        setItem(30, createItem(Material.NETHER_STAR,
                "<gold><bold>⚙ Manage Rarities</bold>",
                List.of(
                        "<gray>Open the rarity list to create/edit/delete",
                        "<yellow>Saved to rarity.yml"
                )), event -> new RarityListGui(plugin, player).openAsChildOf(this));

        // Cooldown (slot 22)
        setItem(22, createItem(Material.CLOCK,
                "<gold><bold>Cooldown</bold>",
                List.of(
                        "<gray>Current: <white>" + (data.getCooldown() >= 0 ? data.getCooldown() + "s" : "None"),
                        "",
                        "<yellow>Click to set cooldown (seconds)",
                        "<gray>-1 to disable"
                )), event -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter cooldown (seconds), -1 to disable:",
                    input -> {
                        try {
                            data.setCooldown(Double.parseDouble(input.trim()));
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Cooldown = " + data.getCooldown() + "s"));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number!"));
                            reopen();
                        }
                    });
        });

        // Permission (slot 23)
        setItem(23, createItem(Material.BARRIER,
                "<red><bold>Permission</bold>",
                List.of(
                        "<gray>Current: <white>" + (data.getPermission() != null ? data.getPermission() : "None"),
                        "",
                        "<yellow>Click to set a permission to use/hold",
                        "<gray>'none' to remove"
                )), event -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter permission (or 'none' to remove):",
                    input -> {
                        if (input.equalsIgnoreCase("none")) data.setPermission(null);
                        else data.setPermission(input.trim());
                        saveAndReopen();
                        player.sendMessage(TextUtil.success("Permission = " + data.getPermission()));
                    });
        });

        // Stack size (slot 24)
        setItem(24, createItem(Material.CHEST,
                "<gold><bold>Stack Size</bold>",
                List.of(
                        "<gray>Current: <white>" + data.getStackSize(),
                        "",
                        "<yellow>Click to set (1-64)"
                )), event -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter stack size (1-64):",
                    input -> {
                        try {
                            int n = Integer.parseInt(input.trim());
                            data.setStackSize(n);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Stack size = " + data.getStackSize()));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number!"));
                            reopen();
                        }
                    });
        });

        // Required level (slot 25)
        setItem(25, createItem(Material.EXPERIENCE_BOTTLE,
                "<aqua><bold>Required Level</bold>",
                List.of(
                        "<gray>Current: <white>" + data.getRequiredLevel(),
                        "",
                        "<yellow>Click to set the minimum level to hold/use",
                        "<gray>0 = no requirement"
                )), event -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter minimum level (0 = off):",
                    input -> {
                        try {
                            data.setRequiredLevel(Integer.parseInt(input.trim()));
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Required level = " + data.getRequiredLevel()));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number!"));
                            reopen();
                        }
                    });
        });

        // Skill Requirements (slot 29) — only shown when an AuraSkills/EcoSkills hook is active
        boolean hookActive = plugin.getSkillsHookManager() != null && plugin.getSkillsHookManager().isActive();
        if (hookActive) {
            String hookName = plugin.getSkillsHookManager().getActive().getName();
            List<String> reqLore = new ArrayList<>();
            if (data.getSkillRequirements().isEmpty()) {
                reqLore.add("<gray>None");
            } else {
                for (java.util.Map.Entry<String, Integer> req : data.getSkillRequirements().entrySet()) {
                    reqLore.add("<gray>• <white>" + req.getKey() + " <gray>≥ <yellow>" + req.getValue());
                }
            }
            reqLore.add("");
            reqLore.add("<gray>Hook: <green>" + hookName);
            reqLore.add("<yellow>Left click: <white>Add requirement");
            reqLore.add("<red>Right click: <white>Clear all");
            setItem(29, createItem(Material.KNOWLEDGE_BOOK,
                    "<dark_aqua><bold>Skill Requirements</bold>", reqLore), event -> {
                if (event.isRightClick()) {
                    if (!data.getSkillRequirements().isEmpty()) {
                        data.getSkillRequirements().clear();
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                        saveAndReopen();
                        player.sendMessage(TextUtil.success("Skill requirements cleared"));
                    }
                    return;
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter requirement as <white>skill_id:level</white> (example: mining:30), 'none' to cancel:",
                        input -> {
                            String in = input.trim();
                            if (in.equalsIgnoreCase("none")) {
                                reopen();
                                return;
                            }
                            String[] parts = in.split(":");
                            int level = -1;
                            if (parts.length == 2) {
                                try {
                                    level = Integer.parseInt(parts[1].trim());
                                } catch (NumberFormatException ignored) {}
                            }
                            if (parts.length != 2 || level <= 0) {
                                player.sendMessage(TextUtil.error("Invalid format! Use skill_id:level (example: mining:30)"));
                                reopen();
                                return;
                            }
                            String skillId = parts[0].trim().toLowerCase(java.util.Locale.ROOT);
                            // Warn (but still allow) when the active hook doesn't know this skill id
                            if (!plugin.getSkillsHookManager().getActive().hasSkill(skillId)) {
                                player.sendMessage(TextUtil.info("Warning: skill '" + skillId + "' not found in "
                                        + plugin.getSkillsHookManager().getActive().getName() + " — saved anyway"));
                            }
                            data.getSkillRequirements().put(skillId, level);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Requirement added: " + skillId + " ≥ " + level));
                        });
            });
        }

        // Skills (slot 26)
        int skillCount = data.getSkills() != null ? data.getSkills().size() : 0;
        setItem(26, createItem(Material.ENCHANTED_BOOK,
                "<light_purple><bold>⚡ Skills & Passives</bold>",
                List.of(
                        "<gray>Skills: <white>" + skillCount,
                        "",
                        "<yellow>Click to open the Skill Editor"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new SkillListGui(plugin, player, data).openAsChildOf(this);
        });

        // Slot 27 is free — the built-in recipe editor was removed; crafting will be
        // provided by the external UCraft plugin via a hook.

        // Effects (slot 28)
        int effCount = data.getEffects() != null ? data.getEffects().size() : 0;
        setItem(28, createItem(
                effCount > 0 ? Material.BOOK : Material.PAPER,
                "<light_purple><bold>Attached Effects</bold>",
                List.of(
                        "<gray>Effects: <white>" + effCount,
                        "",
                        "<yellow>Click to open the Effect Editor GUI"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EffectEditorGui(plugin, player, data).openAsChildOf(this);
        });

        // Duplicate (slot 47)
        setItem(47, createItem(Material.PAPER,
                "<aqua><bold>📋 Duplicate Item</bold>",
                List.of(
                        "<gray>Creates a copy with a new ID",
                        "",
                        "<yellow>Click to duplicate"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter an ID for the copy <gray>(letters, digits, underscores only):",
                    input -> {
                        String newId = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (newId.isEmpty()) {
                            player.sendMessage(TextUtil.error("Invalid ID!"));
                            reopen();
                            return;
                        }
                        if (plugin.getItemManager().hasItem(newId)) {
                            player.sendMessage(TextUtil.error("Item '" + newId + "' already exists!"));
                            reopen();
                            return;
                        }
                        UItemData clone = new UItemData(newId) {{
                            setSource(data.getSource());
                            setCategory(data.getCategory());
                            setSubCategory(data.getSubCategory());
                            setDisplayName(data.getDisplayName());
                            setLore(new ArrayList<>(data.getLore()));
                            setRarity(data.getRarity());
                            setEnchantments(new java.util.LinkedHashMap<>(data.getEnchantments()));
                            setFlags(new java.util.LinkedHashSet<>(data.getFlags()));
                            setUnbreakable(data.isUnbreakable());
                            setGlowing(data.isGlowing());
                            setMaxDurability(data.getMaxDurability());
                            setStackSize(data.getStackSize());
                            setCooldown(data.getCooldown());
                            setPermission(data.getPermission());
                            setRequiredLevel(data.getRequiredLevel());
                            for (UItemData.AttributeData a : data.getAttributes()) getAttributes().add(a.copy());
                            for (UItemData.ItemEffect e : data.getEffects()) getEffects().add(e.copy());
                            for (com.uitem.skill.Skill s : data.getSkills()) getSkills().add(s.copy());
                        }};
                        plugin.getItemManager().registerItem(clone);
                        plugin.getStorageManager().saveItem(plugin.getItemManager(), clone);
                        player.sendMessage(TextUtil.success("Duplicated '" + data.getId() + "' → '" + newId + "'!"));
                        Runnable back = getBackAction();
                        ItemEditorGui newGui = new ItemEditorGui(plugin, player, clone);
                        if (back != null) newGui.setBackAction(back);
                        newGui.open();
                    });
        });

        // Back (slot 46)
        setItem(46, createBackButton(), event -> goBack());

        // Save (slot 49)
        setItem(49, createItem(Material.EMERALD,
                "<green><bold>💾 SAVE</bold>",
                List.of(
                        "<gray>Save the item to items/<id>.yml",
                        "",
                        "<yellow>Click to save"
                )), event -> {
            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
            player.sendMessage(TextUtil.success(Messages.savedItem(data.getId())));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            refresh();
        });

        // Delete (slot 52)
        setItem(52, createItem(Material.BARRIER,
                "<red><bold>🗑 DELETE</bold>",
                List.of(
                        "<gray>Delete this item",
                        "",
                        "<red>⚠ Click to delete"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.5f);
            // afterDelete: jump past this editor to the item list. Returning here
            // would leave an editor open for a deleted item, and its Save button
            // would recreate it.
            new ConfirmDeleteGui(plugin, player, data, 0,
                    () -> {
                        Runnable up = getBackAction();
                        if (up != null) up.run();
                        else new ItemListGui(plugin, player).openAsRoot();
                    }).openAsChildOf(this);
        });
    }

    private void saveAndReopen() {
        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
        reopen();
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
        refresh();
    }

    private String attributeSummary() {
        if (data.getAttributes().isEmpty()) return "<gray>No attributes";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (UItemData.AttributeData attr : data.getAttributes()) {
            if (count >= 2) {
                sb.append("<gray> (+").append(data.getAttributes().size() - 2).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(attr.getAttribute().replace("generic_", "")).append(" +").append(attr.getAmount());
            count++;
        }
        return sb.toString();
    }

    private String statSummary() {
        if (data.getStats().isEmpty()) return "<gray>No stats";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (UItemData.StatData stat : data.getStats()) {
            if (count >= 2) {
                sb.append("<gray> (+").append(data.getStats().size() - 2).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(stat.getStat()).append(" +").append(stat.getAmount()).append("%");
            count++;
        }
        return sb.toString();
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
            if (count >= 3) { sb.append("<gray> (+").append(data.getFlags().size() - 3).append(" more)"); break; }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(flag);
            count++;
        }
        return sb.toString();
    }
}
