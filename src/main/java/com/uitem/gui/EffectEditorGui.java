package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI editor for attaching effects (potion on triggers, reflect, extra damage).
 * Supports selecting trigger (incl. DIE), type, potion, and especially amplifier (level).
 */
public class EffectEditorGui extends AbstractGui {

    private static final List<String> TRIGGERS = List.of("HIT", "DAMAGE", "DIE", "EQUIP", "FULL_SET");
    private static final List<String> TYPES = List.of("POTION", "REFLECT", "EXTRA_DAMAGE");

    // Popular potion keys using canonical registry names (upper) so lookup always succeeds.
    // These .toLowerCase() work with Registry.EFFECT.get(NamespacedKey.minecraft(...))
    private static final List<String> COMMON_POTIONS = List.of(
            "SPEED", "STRENGTH", "REGENERATION", "RESISTANCE", "FIRE_RESISTANCE",
            "NIGHT_VISION", "INVISIBILITY", "JUMP_BOOST", "HASTE", "HEALTH_BOOST",
            "ABSORPTION", "SATURATION", "POISON", "WEAKNESS", "SLOWNESS", "DARKNESS"
    );

    private final UItemData data;
    private UItemData.ItemEffect pending;

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
            this.pending.setType("POTION");
            this.pending.setPotion("SPEED");
            this.pending.setAmplifier(0);
            this.pending.setDurationTicks(200); // ~10s
            this.pending.setChance(100.0);
            this.pending.setValue(20.0);
        }
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.BREWING_STAND,
                "<light_purple><bold>Effects for " + data.getId() + "</bold>",
                List.of(
                        "<gray>Trigger on action → apply potion or combat effect",
                        "<yellow>Choose trigger + type + potion (if any) + level",
                        "<red>IMPORTANT: After save, use /uitem give <id> or craft to get updated item!",
                        "<gray>HIT target effects only work when holding the item in main hand & attacking"
                )));

        // === Current effects list (click to remove) - first row only to leave room ===
        setItem(9, createItem(Material.WRITABLE_BOOK, "<gold><bold>Current Effects</bold>",
                List.of("<gray>Click an effect to remove it", "<gray>Max recommended: 4-6 per item")));

        List<UItemData.ItemEffect> current = data.getEffects() != null ? new ArrayList<>(data.getEffects()) : new ArrayList<>();
        int[] currentSlots = {10, 11, 12, 13, 14, 15, 16, 17};
        for (int i = 0; i < Math.min(current.size(), currentSlots.length); i++) {
            final int idx = i;
            UItemData.ItemEffect e = current.get(i);
            ItemStack icon = buildEffectIcon(e);
            setItem(currentSlots[i], icon, ev -> {
                if (idx < data.getEffects().size()) {
                    data.getEffects().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                    refresh();
                }
            });
        }
        if (current.isEmpty()) {
            setItem(10, createItem(Material.BARRIER, "<gray><italic>No effects yet</italic>",
                    List.of("<yellow>Use builder below to add one")));
        }

        // Clear all button
        setItem(18, createItem(Material.TNT, "<red><bold>Clear All Effects</bold>",
                List.of("<gray>Remove every attached effect")), ev -> {
            if (!data.getEffects().isEmpty()) {
                data.getEffects().clear();
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.2f);
                plugin.getItemManager().registerItem(data);
                plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                refresh();
            }
        });

        // === BUILDER (uses lower area) ===
        setItem(27, createItem(Material.ANVIL, "<gold><bold>NEW EFFECT BUILDER</bold>",
                List.of("<gray>Pick trigger → type → potion (if any) → level (amplifier) then Add")));

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

        // Affects: Self / Target - always visible in builder (only has effect for HIT trigger)
        boolean toTarget = pending.isApplyToTarget();
        setItem(27, createItem(!toTarget ? Material.EMERALD : Material.GRAY_STAINED_GLASS_PANE,
                (!toTarget ? "<green><bold>SELF (holder)</bold>" : "<gray>SELF (holder)"),
                List.of(
                        "<gray>Who receives the effect?",
                        "<green>Self = the player holding/wearing the item",
                        "<yellow>Click to apply to holder"
                )),
                ev -> {
                    pending.setApplyToTarget(false);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                    refresh();
                });

        // (target button moved to after value block to avoid overwrite)

        // Type buttons (after triggers, before target toggle)
        int typeStart = 33;
        for (int ty = 0; ty < TYPES.size(); ty++) {
            final String typ = TYPES.get(ty);
            boolean sel = typ.equalsIgnoreCase(pending.getType());
            Material mat = sel ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(typeStart + ty, createItem(mat,
                    (sel ? "<aqua>✔ " : "<gray>") + typ,
                    List.of("<gray>Effect type: " + typ, sel ? "<green>Selected" : "<yellow>Click to select")),
                    ev -> {
                        pending.setType(typ);
                        if (!"POTION".equals(typ)) {
                            pending.setPotion(null);
                        } else if (pending.getPotion() == null) {
                            pending.setPotion("SPEED");
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
                        refresh();
                    });
        }

        // Amplifier (level) - the requested feature front and center (row 4)
        setItem(37, createItem(Material.EXPERIENCE_BOTTLE, "<gold><bold>Amplifier (Level)</bold>",
                List.of("<gray>0 = I, 1 = II ...", "<gray>Current: <white>" + pending.getAmplifier())));
        setItem(38, createItem(Material.RED_DYE, "<red>−1", List.of("<yellow>Lower level")), ev -> {
            pending.setAmplifier(Math.max(0, pending.getAmplifier() - 1));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 0.8f);
            refresh();
        });
        setItem(39, createItem(Material.PAPER, "<white>Lvl <gold><bold>" + pending.getAmplifier() + "</bold>",
                List.of("<yellow>Click to type exact level")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>Enter amplifier (0-20):", input -> {
                try { pending.setAmplifier(Math.max(0, Math.min(20, Integer.parseInt(input.trim())))); } catch (Exception ignored) {}
                refresh();
            });
        });
        setItem(40, createItem(Material.LIME_DYE, "<green>+1", List.of("<yellow>Higher level")), ev -> {
            pending.setAmplifier(Math.min(20, pending.getAmplifier() + 1));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.2f);
            refresh();
        });

        // Duration (UX shows seconds)
        int secs = Math.max(1, pending.getDurationTicks() / 20);
        setItem(41, createItem(Material.CLOCK, "<gold><bold>Duration</bold> <white>" + secs + "s",
                List.of("<gray>Current ticks: " + pending.getDurationTicks())));
        setItem(42, createItem(Material.RED_DYE, "<red>−5s", List.of()), ev -> {
            pending.setDurationTicks(Math.max(20, pending.getDurationTicks() - 100));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 0.8f);
            refresh();
        });
        setItem(43, createItem(Material.PAPER, secs + "s", List.of("<yellow>Click for exact seconds")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>Duration seconds:", input -> {
                try { pending.setDurationTicks(Math.max(20, Integer.parseInt(input.trim()) * 20)); } catch (Exception ignored) {}
                refresh();
            });
        });
        setItem(44, createItem(Material.LIME_DYE, "<green>+10s", List.of()), ev -> {
            pending.setDurationTicks(Math.min(12000, pending.getDurationTicks() + 200));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.2f);
            refresh();
        });

        // Chance presets
        double ch = pending.getChance();
        setItem(45, createItem(Material.GHAST_TEAR, "<gold><bold>Chance: <white>" + (int)ch + "%</bold>", List.of()));
        int[] chanceSlots = {46, 47, 48, 49, 50};
        double[] chances = {100, 75, 50, 25, 10};
        for (int ci = 0; ci < chances.length; ci++) {
            final double c = chances[ci];
            boolean sel = Math.abs(ch - c) < 0.1;
            setItem(chanceSlots[ci], createItem(sel ? Material.LIME_DYE : Material.GRAY_DYE,
                    (sel ? "<green>" : "<gray>") + (int) c + "%",
                    List.of("<yellow>Set proc chance to " + (int) c + "%")), ev -> {
                pending.setChance(c);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        // Value controls (non-potion only)
        if (!"POTION".equalsIgnoreCase(pending.getType())) {
            setItem(51, createItem(Material.DIAMOND_SWORD, "<gold>Value <white>" + pending.getValue(),
                    List.of("<gray>REFLECT % or EXTRA flat dmg", "<yellow>Click label for exact value via chat")), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                plugin.getChatInputManager().requestInput(player, "<yellow>Enter value (e.g. 25 for 25% or amount):", input -> {
                    try {
                        double v = Double.parseDouble(input.trim());
                        pending.setValue(Math.max(0, v));
                    } catch (Exception ignored) {}
                    refresh();
                });
            });
            // +/- hidden by target/back, use label click or switch to potion type to see target
        }

        // Target button placed late so it wins the slot over value +/- when non-potion
        boolean toTargetLate = pending.isApplyToTarget();
        setItem(52, createItem(toTargetLate ? Material.REDSTONE : Material.GRAY_STAINED_GLASS_PANE,
                (toTargetLate ? "<red><bold>TARGET (victim)</bold>" : "<gray>TARGET (victim)"),
                List.of(
                        "<gray>Only useful with HIT trigger:",
                        "<red>Target = the mob/player being hit",
                        "<yellow>Click to apply to the one you attack"
                )),
                ev -> {
                    pending.setApplyToTarget(true);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                    refresh();
                });

        // Potion area: quick favorites + full browser for ALL potions
        setItem(19, createItem(Material.POTION, "<aqua><bold>Quick Potions</bold>", List.of("<gray>Common ones for speed")));
        for (int pi = 0; pi < Math.min(COMMON_POTIONS.size(), 5); pi++) {
            final String pot = COMMON_POTIONS.get(pi);
            boolean sel = pot.equalsIgnoreCase(pending.getPotion());
            Material m = sel ? Material.LIME_STAINED_GLASS_PANE : Material.POTION;
            setItem(20 + pi, createItem(m,
                    (sel ? "<green>✔ " : "<white>") + pot,
                    List.of("<gray>Effect: " + pot, sel ? "<green>Selected" : "<yellow>Click to choose")), ev -> {
                pending.setType("POTION");
                pending.setPotion(pot);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.3f);
                refresh();
            });
        }

        // The important one: full access to every potion effect on the server
        setItem(26, createItem(Material.ENCHANTED_BOOK, "<light_purple><bold>Browse ALL Potions...</bold>",
                List.of(
                        "<gray>Access to <white>every</white> PotionEffectType",
                        "<yellow>Including rare ones: DOLPHINS_GRACE, BAD_OMEN,",
                        "<yellow>CONDUIT_POWER, SLOW_FALLING, etc.",
                        "<gray>Click to open full paginated picker with search"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            // Capture current pending state so selection doesn't reset to defaults
            final UItemData.ItemEffect capturedPending = this.pending;
            new PotionPickerGui(plugin, player,
                    selected -> {
                        capturedPending.setType("POTION");
                        capturedPending.setPotion(selected);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.4f);
                        // Re-open this editor passing the (updated) pending so choices stick
                        SchedulerUtil.runSync(plugin, () -> new EffectEditorGui(plugin, player, data, capturedPending).open());
                    },
                    () -> new EffectEditorGui(plugin, player, data, capturedPending).open()
            ).open();
        });

        // ADD button
        String targetInfo = "";
        if ("HIT".equalsIgnoreCase(pending.getTrigger())) {
            targetInfo = pending.isApplyToTarget() ? " <red>(on TARGET)" : " <green>(on SELF)";
        }
        setItem(36, createItem(Material.EMERALD_BLOCK, "<green><bold>✔ ADD THIS EFFECT</bold>",
                List.of(
                        "<gray>Trigger: <white>" + safe(pending.getTrigger()) + targetInfo,
                        "<gray>Type: <white>" + safe(pending.getType()),
                        "<gray>Potion: <white>" + safe(pending.getPotion()),
                        "<gray>Lvl <white>" + pending.getAmplifier() + "  Dur <white>" + (pending.getDurationTicks() / 20) + "s  Chance <white>" + (int) pending.getChance() + "%",
                        "<yellow>Click to attach to the item"
                )), ev -> {
            if (pending.getTrigger() == null || pending.getType() == null) {
                player.sendMessage(TextUtil.error("Need Trigger + Type at minimum"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            data.getEffects().add(pending.copy());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.4f);
            player.sendMessage(TextUtil.success("Effect added: " + pending.getTrigger() + " / " + pending.getType() + (pending.isApplyToTarget() ? " (target)" : "")));

            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());

            // fresh pending for next
            pending = new UItemData.ItemEffect();
            pending.setTrigger("HIT");
            pending.setType("POTION");
            pending.setPotion("SPEED");
            pending.setAmplifier(0);
            pending.setDurationTicks(200);
            pending.setChance(100.0);
            pending.setValue(20.0);

            refresh();
        });

        // Back at bottom to free top slots
        setItem(53, createBackButton(), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemEditorGui(plugin, player, data).open();
        });
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }

    private ItemStack buildEffectIcon(UItemData.ItemEffect e) {
        Material mat = Material.POTION;
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
        if (e.getPotion() != null) lore.add("<gray>Potion: <white>" + e.getPotion() + " <gray>lvl " + e.getAmplifier());
        lore.add("<gray>Duration: <white>" + (e.getDurationTicks() / 20) + "s");
        lore.add("<gray>Chance: <white>" + (int) e.getChance() + "%");
        if (e.getValue() != 0) lore.add("<gray>Value: <white>" + e.getValue());
        if ("HIT".equals(trig)) {
            lore.add("<gray>Affects: <white>" + (e.isApplyToTarget() ? "Target (victim)" : "Self (holder)"));
        }
        lore.add("");
        lore.add("<red>Click to remove");
        return createItem(mat, name, lore);
    }

}