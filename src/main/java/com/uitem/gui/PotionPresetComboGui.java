package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.PotionEffectHelper;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * GUI to select potion preset combos (buff combos, debuff combos, etc.).
 */
public class PotionPresetComboGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private int currentPage = 0;
    private static final int[] PRESET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int ITEMS_PER_PAGE = PRESET_SLOTS.length;

    public PotionPresetComboGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, "<light_purple><bold>Potion Preset Combos</bold>");
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Clear bottom row for navigation
        ItemStack navBg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i <= 53; i++) {
            inventory.setItem(i, navBg);
        }

        // Header
        int totalPresets = PotionEffectHelper.getTotalPresets();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPresets / ITEMS_PER_PAGE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        setItem(4, createItem(Material.BREWING_STAND,
                "<light_purple><bold>Potion Combos</bold>",
                List.of(
                        "<gray>Page: <white>" + (currentPage + 1) + "/" + totalPages,
                        "<gray>Total: <white>" + totalPresets + " presets",
                        "",
                        "<yellow>Click to add a combo"
                )));

        // Display presets in grid
        List<PotionEffectHelper.PotionPreset> allPresets = new ArrayList<>(PotionEffectHelper.getAllPresets().values());
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, allPresets.size());

        for (int i = startIdx; i < endIdx; i++) {
            final PotionEffectHelper.PotionPreset preset = allPresets.get(i);
            ItemStack icon = createPresetIcon(preset);
            int slot = PRESET_SLOTS[i - startIdx];
            
            setItem(slot, icon, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                confirmAddPreset(preset);
            });
        }

        // Navigation row
        setItem(45, currentPage > 0
                ? createItem(Material.ARROW, "<gray>< Previous Page")
                : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                currentPage > 0 ? ev -> {
                    currentPage--;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                    refresh();
                } : null);
        setItem(46, createItem(Material.PAPER,
                "<gray>Page <white>" + (currentPage + 1) + "/" + totalPages));
        setItem(47, endIdx < allPresets.size()
                ? createItem(Material.ARROW, "<gray>Next Page >")
                : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                endIdx < allPresets.size() ? ev -> {
                    currentPage++;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                    refresh();
                } : null);
        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack createPresetIcon(PotionEffectHelper.PotionPreset preset) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Preset Combo:");
        for (PotionEffectHelper.PotionEffectCombo combo : preset.effects) {
            String level = combo.amplifier >= 1 ? " " + toRoman(combo.amplifier + 1) : "";
            lore.add("<aqua>  • " + combo.effectType + level + " (" + combo.durationSeconds + "s)");
        }
        lore.add("");
        lore.add("<yellow>Click to add");

        return createItem(Material.POTION,
                "<light_purple>" + preset.name,
                lore);
    }

    private void confirmAddPreset(PotionEffectHelper.PotionPreset preset) {
        PotionEffectHelper.addPresetToSkill(skill, preset);

        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), itemData);

        String msg = String.format("<green>✓ Added preset: %s (%d effects)",
            preset.name, preset.effects.size());
        player.sendMessage(TextUtil.success(msg));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        // Go back to SkillEditorGui (keep parent chain)
        goBack();
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> String.valueOf(num);
        };
    }
}
