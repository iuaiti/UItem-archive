package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wizard step 4: final setup for the UItem (source already obtained at step 3).
 *
 * <p>The draft UItemData is an instance field, and chat-input callbacks call
 * {@link #reopen()} — not {@code refresh()} — so the inventory is actually shown
 * again. {@code refresh()} only repaints the (currently closed) inventory, which
 * left the player in chat after every edit.
 */

public class SetupItemStepGui extends AbstractGui {

    private final CreateWizardGui.WizardState state;
    private final String defaultItemId;
    private final Consumer<UItemData> onComplete;
    private final UItemData draft;

    public SetupItemStepGui(UItemPlugin plugin, Player player,
                            CreateWizardGui.WizardState state, String defaultItemId,
                            Consumer<UItemData> onComplete) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Setup UItem</bold></gradient>");
        this.state = state;
        this.defaultItemId = defaultItemId;
        this.onComplete = onComplete;
        String draftId = defaultItemId != null ? defaultItemId : "pending_id";
        this.draft = new UItemData(draftId);
        draft.setSource(state.source != null ? state.source : "vanilla:stone");
        draft.setCategory(state.category);
        draft.setDisplayName("<white>" + ("pending_id".equals(draftId) ? "NewItem" : prettify(draftId)));
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Preview (slot 4)
        setItem(4, plugin.getItemManager().buildItemStack(draft));

        // Title info (slot 13 row 2 — slot 22)
        setItem(13, createItem(Material.PAPER,
                "<gray>Source: <white>" + draft.getSource(),
                List.of("<gray>Category: <white>" + (draft.getCategory() != null ? draft.getCategory() : "—"))),
                null);

        // ID (slot 10)
        setItem(10, createItem(Material.NAME_TAG,
                "<gold><bold>Internal ID</bold>",
                List.of(
                        "<gray>Current: <white>" + draft.getId(),
                        "",
                        "<yellow>Click to change ID",
                        "<gray>(letters, digits, underscores only)"
                )), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter ID for the UItem <gray>(letters, digits, _ only):",
                    input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (id.isEmpty()) {
                            player.sendMessage(TextUtil.error("Invalid ID!"));
                            reopen(); return;
                        }
                        if (plugin.getItemManager().hasItem(id)) {
                            player.sendMessage(TextUtil.error("Item '" + id + "' already exists!"));
                            reopen(); return;
                        }
                        applyId(id);
                        reopen();
                    });
        });

        // Display Name (slot 11)
        setItem(11, createItem(Material.OAK_SIGN,
                "<aqua><bold>Display Name</bold>",
                List.of(
                        "<gray>Current: " + draft.getDisplayName(),
                        "",
                        "<yellow>Click to change (supports MiniMessage)"
                )), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter display name (MiniMessage):",
                    input -> { draft.setDisplayName(input); reopen(); });
        });

        // Rarity (slot 12)
        setItem(12, createItem(Material.NETHER_STAR,
                "<gold><bold>Rarity</bold>",
                List.of(
                        "<gray>Current: " + (draft.getRarity() != null
                                ? RarityConfig.getRarityColor(draft.getRarity()) + draft.getRarity() : "<gray>None"),
                        "",
                        "<yellow>Click to choose",
                        "<gray>Known: " + String.join(", ", RarityConfig.getKeys())
                )), ev -> {
            StringBuilder msg = new StringBuilder("<gold> rarities:");
            for (String r : RarityConfig.getKeys()) {
                msg.append("\n").append(RarityConfig.getRarityColor(r)).append("▸ ").append(r);
            }
            msg.append("\n\n<yellow>Enter rarity (or 'none'):");
            plugin.getChatInputManager().requestInput(player, msg.toString(), input -> {
                if (input.equalsIgnoreCase("none")) {
                    draft.setRarity(null);
                } else {
                    String up = input.toUpperCase().trim();
                    if (RarityConfig.getKeys().contains(up)) draft.setRarity(up);
                    else player.sendMessage(TextUtil.error("Rarity '" + up + "' not defined"));
                }
                reopen();
            });
        });

        // Sub-category (slot 14)
        setItem(14, createItem(Material.CHEST_MINECART,
                "<aqua><bold>Sub-Category</bold>",
                List.of(
                        "<gray>Current: <white>" + (draft.getSubCategory() != null ? draft.getSubCategory() : "—"),
                        "",
                        "<yellow>Click to choose"
                )), ev -> new SubCategoryPickerGui(plugin, player, draft.getCategory(), draft.getSubCategory(), picked -> {
            draft.setSubCategory(picked);
            reopen();
        }).openAsChildOf(this));

        // Cooldown (slot 15)
        setItem(15, createItem(Material.CLOCK,
                "<gold><bold>Cooldown</bold>",
                List.of(
                        "<gray>Current: <white>" + (draft.getCooldown() >= 0 ? draft.getCooldown() + "s" : "None"),
                        "",
                        "<yellow>Click to set (seconds, -1 = off)"
                )), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter cooldown (seconds, -1 = off):",
                    input -> {
                        try { draft.setCooldown(Double.parseDouble(input.trim())); }
                        catch (NumberFormatException e) { player.sendMessage(TextUtil.error("Invalid number!")); }
                        reopen();
                    });
        });

        // Permission (slot 16)
        setItem(16, createItem(Material.BARRIER,
                "<red><bold>Permission</bold>",
                List.of(
                        "<gray>Current: <white>" + (draft.getPermission() != null ? draft.getPermission() : "None"),
                        "",
                        "<yellow>Click to set (none = remove)"
                )), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter permission (none = remove):",
                    input -> {
                        if (input.equalsIgnoreCase("none")) draft.setPermission(null);
                        else draft.setPermission(input.trim());
                        reopen();
                    });
        });

        // Stack size (slot 17)
        setItem(17, createItem(Material.CHEST,
                "<gold><bold>Stack Size</bold>",
                List.of(
                        "<gray>Current: <white>" + draft.getStackSize(),
                        "",
                        "<yellow>Click to set (1-64)"
                )), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter stack size (1-64):",
                    input -> {
                        try { draft.setStackSize(Integer.parseInt(input.trim())); }
                        catch (NumberFormatException e) { player.sendMessage(TextUtil.error("Invalid number!")); }
                        reopen();
                    });
        });

        // Required level (slot 18)
        setItem(18, createItem(Material.EXPERIENCE_BOTTLE,
                "<aqua><bold>Required Level</bold>",
                List.of(
                        "<gray>Current: <white>" + draft.getRequiredLevel(),
                        "",
                        "<yellow>Click to set (0 = off)"
                )), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter minimum level (0 = off):",
                    input -> {
                        try { draft.setRequiredLevel(Integer.parseInt(input.trim())); }
                        catch (NumberFormatException e) { player.sendMessage(TextUtil.error("Invalid number!")); }
                        reopen();
                    });
        });

        setItem(45, createBackButton(), ev -> goBack());

        setItem(49, createItem(Material.EMERALD,
                "<green><bold>✔ FINISH</bold>",
                List.of(
                        "<gray>ID: <white>" + draft.getId(),
                        "<gray>Source: <white>" + draft.getSource(),
                        "<gray>Rarity: <white>" + (draft.getRarity() != null ? draft.getRarity() : "—"),
                        "<gray>Category: <white>" + draft.getCategory()
                                + (draft.getSubCategory() != null ? " / " + draft.getSubCategory() : ""),
                        "",
                        "<yellow>Click to create and open ItemEditor"
                )), ev -> {
            if (draft.getId() == null || draft.getId().isBlank() || "pending_id".equals(draft.getId())) {
                player.sendMessage(TextUtil.error("Internal ID not entered!"));
                return;
            }
            if (plugin.getItemManager().hasItem(draft.getId())) {
                player.sendMessage(TextUtil.error("Item '" + draft.getId() + "' already exists!"));
                return;
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            if (onComplete != null) onComplete.accept(draft);
        });
    }

    private void applyId(String newId) {
        // Because id is final in UItemData → create a new item with the same fields but a different id.
        UItemData fresh = new UItemData(newId);
        fresh.setSource(draft.getSource());
        fresh.setCategory(draft.getCategory());
        fresh.setSubCategory(draft.getSubCategory());
        fresh.setDisplayName(draft.getDisplayName());
        fresh.setLore(new ArrayList<>(draft.getLore()));
        fresh.setRarity(draft.getRarity());
        fresh.setEnchantments(new java.util.LinkedHashMap<>(draft.getEnchantments()));
        fresh.setFlags(new java.util.LinkedHashSet<>(draft.getFlags()));
        fresh.setUnbreakable(draft.isUnbreakable());
        fresh.setGlowing(draft.isGlowing());
        fresh.setMaxDurability(draft.getMaxDurability());
        fresh.setStackSize(draft.getStackSize());
        fresh.setCooldown(draft.getCooldown());
        fresh.setPermission(draft.getPermission());
        fresh.setRequiredLevel(draft.getRequiredLevel());
        for (UItemData.AttributeData a : draft.getAttributes()) fresh.getAttributes().add(a.copy());
        for (UItemData.ItemEffect e : draft.getEffects()) fresh.getEffects().add(e.copy());
        for (com.uitem.skill.Skill s : draft.getSkills()) fresh.getSkills().add(s.copy());
        copyInto(fresh);
    }

    private void copyInto(UItemData src) {
        // Safer than creating a new instance: keep the same reference for listener callbacks.
        // There are risks; here we simply replace the final id field via reflection.
        try {
            java.lang.reflect.Field f = UItemData.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(draft, src.getId());
            draft.setSource(src.getSource());
            draft.setDisplayName(src.getDisplayName());
        } catch (Exception ignored) { }
    }

    private String prettify(String id) {
        String[] parts = id.replace("-", "_").split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
