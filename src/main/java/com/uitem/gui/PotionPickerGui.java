package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.function.Consumer;

/**
 * Paged GUI to browse and select ANY PotionEffectType available on the server.
 * Used by EffectEditorGui so users can pick every possible potion for effects
 * (including obscure ones like DOLPHINS_GRACE, BAD_OMEN, etc.).
 */
public class PotionPickerGui extends AbstractGui {

    private final Consumer<String> callback;
    private final Runnable onBack;
    private int page;
    private String filter;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private List<String> potions;

    public PotionPickerGui(UItemPlugin plugin, Player player, Consumer<String> callback, Runnable onBack) {
        this(plugin, player, callback, onBack, 0, null);
    }

    public PotionPickerGui(UItemPlugin plugin, Player player,
                           Consumer<String> callback, Runnable onBack,
                           int page, String filter) {
        super(plugin, player, 54, "<aqua><bold>Choose Potion Effect</bold>");
        this.callback = callback;
        this.onBack = onBack;
        this.page = page;
        this.filter = filter;
        this.potions = loadAllPotions();
    }

    private List<String> loadAllPotions() {
        Set<String> set = new LinkedHashSet<>();

        // Primary: use Registry (1.21+ Paper/Spigot with Registry)
        try {
            Registry.EFFECT.forEach(pet -> {
                if (pet != null) {
                    String key = pet.getKey().getKey().toUpperCase(Locale.ROOT);
                    set.add(key);
                }
            });
        } catch (Throwable ignored) {
            // Fallback for older or different impl
        }

        // Fallback / supplement with PotionEffectType.values()
        for (PotionEffectType pet : PotionEffectType.values()) {
            if (pet != null) {
                // getName() or key
                String name = pet.getName() != null ? pet.getName().toUpperCase(Locale.ROOT) : pet.getKey().getKey().toUpperCase(Locale.ROOT);
                set.add(name);
            }
        }

        List<String> list = new ArrayList<>(set);
        // Normalize a few common aliases to the style used in examples (STRENGTH not INCREASE_DAMAGE etc.)
        // We keep the registry keys (strength -> STRENGTH, jump_boost -> JUMP_BOOST, etc.)
        Collections.sort(list);

        if (filter != null && !filter.isBlank()) {
            String lower = filter.toLowerCase(Locale.ROOT);
            list.removeIf(k -> !k.toLowerCase(Locale.ROOT).contains(lower));
        }

        return list;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        int total = potions.size();
        setItem(4, createItem(Material.POTION,
                "<aqua><bold>All Potion Effects</bold>",
                List.of(
                        "<gray>Total available: <white>" + total,
                        filter != null && !filter.isBlank() ? "<gray>Filter: <white>" + filter : "<gray>No filter",
                        "<yellow>Click any to use for POTION effects"
                )));

        // Search / filter
        setItem(53, createItem(Material.OAK_SIGN,
                "<gold><bold>Search / Filter</bold>",
                List.of(
                        filter != null ? "<gray>Current: <white>" + filter : "<gray>Click to filter potion names",
                        "<gray>Empty input = clear filter"
                )),
                ev -> {
                    player.closeInventory();
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Enter potion name filter (e.g. speed, jump, dark):",
                            input -> {
                                String f = input.trim();
                                if (f.isEmpty()) f = null;
                                // Re-open picker with filter (preserves callback + onBack)
                                new PotionPickerGui(plugin, player, callback, onBack, 0, f).open();
                            });
                });

        int totalPages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= total) break;

            final String key = potions.get(idx);

            ItemStack item = new ItemStack(Material.POTION);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse("<aqua><bold>" + key + "</bold>"));
                meta.lore(List.of(
                        TextUtil.parse("<gray>Click to select this potion effect"),
                        TextUtil.parse("<yellow>Will be used as 'potion' in the effect config"),
                        TextUtil.parse("<dark_gray>Key: " + key.toLowerCase(Locale.ROOT))
                ));
                // Add glint for nicer look
                meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }

            setItem(ITEM_SLOTS[i], item, ev -> {
                if (callback != null) {
                    callback.accept(key);
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.3f);
                // The callback is responsible for reopening the EffectEditorGui
            });
        }

        // Pagination
        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { page--; refresh(); } : null);

        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { page++; refresh(); } : null);

        // Back to effect editor
        setItem(45, createBackButton(), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            if (onBack != null) {
                onBack.run();
            } else {
                player.closeInventory();
            }
        });
    }
}