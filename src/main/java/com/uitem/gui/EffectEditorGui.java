package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI editor for attaching effects (reflect, extra damage on triggers).
 * Supports selecting trigger (incl. DIE, HIT for target), type (REFLECT/EXTRA_DAMAGE), chance, value.
 */
public class EffectEditorGui extends AbstractGui {

    private static final List<String> TRIGGERS = List.of("HIT", "DAMAGE", "DIE", "EQUIP", "FULL_SET");
    private static final List<String> TYPES = List.of("REFLECT", "EXTRA_DAMAGE");
    private static final int[] CURRENT_EFFECT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17};

    private final UItemData data;
    private UItemData.ItemEffect pending;
    private int effectPage;

    public EffectEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        this(plugin, player, data, null);
    }

    public EffectEditorGui(UItemPlugin plugin, Player player, UItemData data, UItemData.ItemEffect initialPending) {
        super(plugin, player, 54, "<light_purple><bold>Effect Editor</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
        if (initialPending != null) {
            this.pending = initialPending.copy();
        } else {
            this.pending = new UItemData.ItemEffect();
            // sensible defaults
            this.pending.setTrigger("HIT");
            this.pending.setType("REFLECT");
            this.pending.setChance(100.0);
            this.pending.setValue(20.0);
        }
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
        setItem(4, createItem(Material.BOOK,
                "<light_purple><bold>Effects for " + data.getId() + "</bold>",
                List.of(
                        "<gray>Activated on action → applies combat effect (reflect/extra damage)",
                        "<yellow>Choose trigger + type + chance + value",
                        "<red>IMPORTANT: After saving, use /uitem give <id> or craft to get the updated item!",
                        "<gray>HIT target: the effect can be applied to the victim (if type supports it)"
                )));

        // === Current effects list (click to delete) - only first row to save space ===
        setItem(9, createItem(Material.WRITABLE_BOOK, "<gold><bold>Current Effects</bold>",
                List.of("<gray>Click an effect to delete", "<gray>Recommended max: 4-6 per item")));

        List<UItemData.ItemEffect> current = data.getEffects() != null ? new ArrayList<>(data.getEffects()) : new ArrayList<>();
        int totalEffectPages = Math.max(1, (int) Math.ceil((double) current.size() / CURRENT_EFFECT_SLOTS.length));
        if (effectPage >= totalEffectPages) effectPage = totalEffectPages - 1;
        if (effectPage < 0) effectPage = 0;
        int startIndex = effectPage * CURRENT_EFFECT_SLOTS.length;
        for (int i = 0; i < CURRENT_EFFECT_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex >= current.size()) break;
            final int idx = dataIndex;
            UItemData.ItemEffect e = current.get(dataIndex);
            ItemStack icon = buildEffectIcon(e);
            setItem(CURRENT_EFFECT_SLOTS[i], icon, ev -> {
                if (idx < data.getEffects().size()) {
                    data.getEffects().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    refresh();
                }
            });
        }
        if (current.isEmpty()) {
            setItem(10, createItem(Material.BARRIER, "<gray><italic>No effects yet</italic>",
                    List.of("<yellow>Use the builder below to add")));
        }
        if (effectPage > 0) {
            setItem(19, createPreviousButton(), ev -> {
                effectPage--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                refresh();
            });
        }
        if (effectPage < totalEffectPages - 1) {
            setItem(26, createNextButton(), ev -> {
                effectPage++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                refresh();
            });
        }

        // Clear all button
        setItem(18, createItem(Material.TNT, "<red><bold>Clear All Effects</bold>",
                List.of("<gray>Remove all attached effects")), ev -> {
            if (!data.getEffects().isEmpty()) {
                data.getEffects().clear();
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.2f);
                plugin.getItemManager().registerItem(data);
                plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                refresh();
            }
        });

        // === BUILDER (uses lower area) ===
        setItem(25, createItem(Material.ANVIL, "<gold><bold>NEW EFFECT BUILDER</bold>",
                List.of("<gray>Choose trigger → type (REFLECT/EXTRA) → chance + value then Add")));

        // Trigger buttons
        int tStart = 28;
        for (int ti = 0; ti < TRIGGERS.size(); ti++) {
            final String trig = TRIGGERS.get(ti);
            boolean sel = trig.equalsIgnoreCase(pending.getTrigger());
            Material mat = sel ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(tStart + ti, createItem(mat,
                    (sel ? "<green>✔ " : "<gray>") + trig,
                    List.of("<gray>Trigger: " + trig,
                            sel ? "<green>Selected" : "<yellow>Click to select")),
                    ev -> {
                        pending.setTrigger(trig);
                        if (!"HIT".equals(trig)) {
                            pending.setApplyToTarget(false);
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
                        refresh();
                    });
        }

        // Affects: Self / Target - always shown in builder (only effective with HIT trigger)
        boolean toTarget = pending.isApplyToTarget();
        setItem(33, createItem(!toTarget ? Material.EMERALD : Material.GRAY_STAINED_GLASS_PANE,
                (!toTarget ? "<green><bold>SELF (holder/wearer)</bold>" : "<gray>SELF (holder/wearer)"),
                List.of(
                        "<gray>Who receives the effect?",
                        "<green>Self = the player holding or wearing the item",
                        "<yellow>Click to apply to self"
                )),
                ev -> {
                    pending.setApplyToTarget(false);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                    refresh();
                });

        // Target button (slot 34 - set only once here)
        setItem(34, createItem(toTarget ? Material.REDSTONE : Material.GRAY_STAINED_GLASS_PANE,
                (toTarget ? "<red><bold>TARGET (victim)</bold>" : "<gray>TARGET (victim)"),
                List.of(
                        "<gray>Only useful with HIT trigger:",
                        "<red>Target = mob/player being hit",
                        "<yellow>Click to apply to the one being hit"
                )),
                ev -> {
                    pending.setApplyToTarget(true);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                    refresh();
                });

        // Small label for the pair
        setItem(35, createItem(Material.PAPER, "<gray>Apply to", List.of()));

        // Type button (after the affects self/target section at 33/34/35)
        int typeStart = 36;
        for (int ty = 0; ty < TYPES.size(); ty++) {
            final String typ = TYPES.get(ty);
            boolean sel = typ.equalsIgnoreCase(pending.getType());
            Material mat = sel ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(typeStart + ty, createItem(mat,
                    (sel ? "<aqua>✔ " : "<gray>") + typ,
                    List.of("<gray>Effect type: " + typ, sel ? "<green>Selected" : "<yellow>Click to select")),
                    ev -> {
                        pending.setType(typ);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
                        refresh();
                    });
        }

        // Chance presets (after value at 39)
        double ch = pending.getChance();
        setItem(40, createItem(Material.GHAST_TEAR, "<gold><bold>Chance: <white>" + (int)ch + "%</bold>", List.of()));
        int[] chanceSlots = {41, 42, 43, 44};
        double[] chances = {100, 75, 50, 25};
        for (int ci = 0; ci < chances.length; ci++) {
            final double c = chances[ci];
            boolean sel = Math.abs(ch - c) < 0.1;
            setItem(chanceSlots[ci], createItem(sel ? Material.LIME_DYE : Material.GRAY_DYE,
                    (sel ? "<green>" : "<gray>") + (int) c + "%",
                    List.of("<yellow>Set activation chance to " + (int) c + "%")), ev -> {
                pending.setChance(c);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        // Value controls (for REFLECT / EXTRA_DAMAGE) - at 39
        setItem(39, createItem(Material.DIAMOND_SWORD, "<gold>Value <white>" + pending.getValue(),
                List.of("<gray>REFLECT = % of damage reflected", "<gray>EXTRA_DAMAGE = flat extra damage amount", "<yellow>Click the label to enter an exact value via chat")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>Enter value (e.g. 25 for 25% or amount):", input -> {
                try {
                    double v = Double.parseDouble(input.trim());
                    pending.setValue(Math.max(0, v));
                } catch (Exception ignored) {}
                refresh();
            });
        });

        // Add button
        String targetInfo = "";
        if ("HIT".equalsIgnoreCase(pending.getTrigger())) {
            targetInfo = pending.isApplyToTarget() ? " <red>(for TARGET)" : " <green>(for SELF)";
        }
        setItem(45, createItem(Material.EMERALD_BLOCK, "<green><bold>✔ ADD THIS EFFECT</bold>",
                List.of(
                        "<gray>Trigger: <white>" + safe(pending.getTrigger()) + targetInfo,
                        "<gray>Type: <white>" + safe(pending.getType()),
                        "<gray>Chance: <white>" + (int) pending.getChance() + "%  Value: <white>" + pending.getValue(),
                        "<yellow>Click to attach to the item"
                )), ev -> {
            if (pending.getTrigger() == null || pending.getType() == null) {
                player.sendMessage(TextUtil.error("Need at least Trigger + Type"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            data.getEffects().add(pending.copy());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.4f);
            player.sendMessage(TextUtil.success("Added effect: " + pending.getTrigger() + " / " + pending.getType() + (pending.isApplyToTarget() ? " (target)" : "")));

            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveItem(plugin.getItemManager(), data);

            // fresh pending for next
            pending = new UItemData.ItemEffect();
            pending.setTrigger("HIT");
            pending.setType("REFLECT");
            pending.setChance(100.0);
            pending.setValue(20.0);

            refresh();
        });

        // Back button at the bottom
        setItem(53, createBackButton(), ev -> goBack());
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }

    private ItemStack buildEffectIcon(UItemData.ItemEffect e) {
        Material mat = Material.IRON_SWORD;
        String trig = e.getTrigger() != null ? e.getTrigger() : "?";
        if ("HIT".equals(trig) || "DAMAGE".equals(trig)) mat = Material.IRON_SWORD;
        else if ("DIE".equals(trig)) mat = Material.SKELETON_SKULL;
        else if ("EQUIP".equals(trig) || "FULL_SET".equals(trig)) mat = Material.DIAMOND_CHESTPLATE;

        String targetNote = "";
        if ("HIT".equals(trig) && e.isApplyToTarget()) {
            targetNote = " <red>→ TARGET";
        } else if ("HIT".equals(trig)) {
            targetNote = " <green>→ SELF";
        }

        String name = "<light_purple>" + trig + targetNote + " <gray>→ <aqua>" + (e.getType() != null ? e.getType() : "");
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Chance: <white>" + (int) e.getChance() + "%");
        if (e.getValue() != 0) lore.add("<gray>Value: <white>" + e.getValue());
        if ("HIT".equals(trig)) {
            lore.add("<gray>Apply to: <white>" + (e.isApplyToTarget() ? "Target (victim)" : "Self (holder/wearer)"));
        }
        lore.add("");
        lore.add("<red>Click to delete");
        return createItem(mat, name, lore);
    }

}
