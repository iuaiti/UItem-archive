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
import java.util.Locale;

/**
 * GUI editor for attaching effects (potion on triggers, reflect, extra damage).
 * Supports selecting trigger (incl. DIE), type, potion, and especially amplifier (level).
 */
public class EffectEditorGui extends AbstractGui {

    private static final List<String> TRIGGERS = List.of("HIT", "DAMAGE", "DIE", "EQUIP", "FULL_SET");
    private static final List<String> TYPES = List.of("POTION", "REFLECT", "EXTRA_DAMAGE");

    // Popular potion keys (these .toLowerCase() resolve via Registry in listener)
    private static final List<String> COMMON_POTIONS = List.of(
            "SPEED", "STRENGTH", "REGENERATION", "DAMAGE_RESISTANCE", "FIRE_RESISTANCE",
            "NIGHT_VISION", "INVISIBILITY", "JUMP_BOOST", "FAST_DIGGING", "HEALTH_BOOST",
            "ABSORPTION", "SATURATION", "POISON", "WEAKNESS", "SLOW", "DARKNESS"
    );

    private final UItemData data;
    private UItemData.ItemEffect pending;

    public EffectEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, "<light_purple><bold>Effect Editor</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
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

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.BREWING_STAND,
                "<light_purple><bold>Effects for " + data.getId() + "</bold>",
                List.of(
                        "<gray>Trigger on action → apply potion or combat effect",
                        "<yellow>Choose trigger + type + potion (if any) + level"
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
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
                        refresh();
                    });
        }

        // Type buttons (safe inner cols)
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
                    List.of("<gray>REFLECT % or EXTRA flat dmg")));
            setItem(52, createItem(Material.RED_DYE, "<red>−", List.of()), ev -> {
                pending.setValue(Math.max(0, pending.getValue() - 1));
                refresh();
            });
            setItem(53, createItem(Material.LIME_DYE, "<green>+", List.of()), ev -> {
                pending.setValue(pending.getValue() + 1);
                refresh();
            });
        }

        // Potion picker grid (clicking sets type + potion). Slots 19-25 (title + 6 choices to avoid overlap)
        setItem(19, createItem(Material.POTION, "<aqua><bold>Pick Potion</bold>", List.of("<gray>Click → forces POTION type")));
        for (int pi = 0; pi < Math.min(COMMON_POTIONS.size(), 6); pi++) {
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

        // ADD button (slot 36 near controls)
        setItem(36, createItem(Material.EMERALD_BLOCK, "<green><bold>✔ ADD THIS EFFECT</bold>",
                List.of(
                        "<gray>Trigger: <white>" + safe(pending.getTrigger()),
                        "<gray>Type: <white>" + safe(pending.getType()),
                        "<gray>Potion: <white>" + safe(pending.getPotion()),
                        "<gray>Lvl <white>" + pending.getAmplifier() + "  " + (pending.getDurationTicks() / 20) + "s  " + (int) pending.getChance() + "%",
                        "<yellow>Click to attach"
                )), ev -> {
            if (pending.getTrigger() == null || pending.getType() == null) {
                player.sendMessage(TextUtil.error("Need Trigger + Type at minimum"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            data.getEffects().add(pending.copy());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.4f);
            player.sendMessage(TextUtil.success("Effect added: " + pending.getTrigger() + " / " + pending.getType()));

            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());

            // fresh pending
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

        // Back (use top area slot to avoid conflict with value +/- )
        setItem(8, createBackButton(), ev -> {
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

        String name = "<light_purple>" + trig + " <gray>→ <aqua>" + (e.getType() != null ? e.getType() : "");
        List<String> lore = new ArrayList<>();
        if (e.getPotion() != null) lore.add("<gray>Potion: <white>" + e.getPotion() + " <gray>lvl " + e.getAmplifier());
        lore.add("<gray>Duration: <white>" + (e.getDurationTicks() / 20) + "s");
        lore.add("<gray>Chance: <white>" + (int) e.getChance() + "%");
        if (e.getValue() != 0) lore.add("<gray>Value: <white>" + e.getValue());
        lore.add("");
        lore.add("<red>Click to remove");
        return createItem(mat, name, lore);
    }

}