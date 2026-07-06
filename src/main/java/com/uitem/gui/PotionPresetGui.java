package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.SkillAction;
import com.uitem.util.PotionEffectRegistry;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * GUI to quickly pick a potion effect and configure duration/amplifier.
 */
public class PotionPresetGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 16; // 4x4 grid (slot 10-26, skip borders)

    public PotionPresetGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, "<light_purple><bold>Choose Potion Effect</bold>");
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Clear bottom row for navigation (overwrite fillBorder's bottom border)
        ItemStack navBg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i <= 53; i++) {
            inventory.setItem(i, navBg);
        }

        // Header
        int totalEffects = PotionEffectRegistry.getTotalEffects();
        int totalPages = (int) Math.ceil((double) totalEffects / ITEMS_PER_PAGE);
        
        setItem(4, createItem(Material.POTION,
                "<light_purple><bold>Potion Effects</bold>",
                List.of(
                        "<gray>Page: <white>" + (currentPage + 1) + "/" + totalPages,
                        "<gray>Total: <white>" + totalEffects + " effects",
                        "",
                        "<yellow>Click to add a potion"
                )));

        // Display potion effects in grid
        List<PotionEffectRegistry.PotionInfo> allEffects = new ArrayList<>(PotionEffectRegistry.getAllEffects().values());
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, allEffects.size());

        int slot = 10;
        int slotsInRow = 0;
        for (int i = startIdx; i < endIdx; i++) {
            if (slotsInRow == 7) {
                slotsInRow = 0;
                slot += 2;
            }

            final PotionEffectRegistry.PotionInfo info = allEffects.get(i);
            ItemStack icon = createPotionIcon(info);
            
            setItem(slot, icon, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                showPotionDurationPrompt(info);
            });

            slot++;
            slotsInRow++;
        }

        // Navigation row (always show prev/next when applicable, back always visible)
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
        setItem(47, endIdx < allEffects.size()
                ? createItem(Material.ARROW, "<gray>Next Page >")
                : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                endIdx < allEffects.size() ? ev -> {
                    currentPage++;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                    refresh();
                } : null);
        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack createPotionIcon(PotionEffectRegistry.PotionInfo info) {
        return createItem(Material.POTION,
                "<aqua>" + info.displayName,
                List.of(
                        "<gray>Effect: <white>" + info.key,
                        "<gray>Desc: <white>" + info.description,
                        "",
                        "<yellow>Click to choose"
                ));
    }

    private void showPotionDurationPrompt(PotionEffectRegistry.PotionInfo info) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter duration (seconds, example: 10):",
                input -> {
                    try {
                        int duration = Integer.parseInt(input.trim());
                        showPotionAmplifierPrompt(info, duration);
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.error("Invalid number. Using default 10 seconds."));
                        showPotionAmplifierPrompt(info, 10);
                    }
                });
    }

    private void showPotionAmplifierPrompt(PotionEffectRegistry.PotionInfo info, int duration) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter amplifier (0-5, example: 0 for level 1):",
                input -> {
                    try {
                        int amplifier = Math.max(0, Math.min(5, Integer.parseInt(input.trim())));
                        addPotionAction(info, duration, amplifier);
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.error("Invalid number. Using default 0."));
                        addPotionAction(info, duration, 0);
                    }
                });
    }

    private void addPotionAction(PotionEffectRegistry.PotionInfo info, int duration, int amplifier) {
        SkillAction action = SkillAction.givePotion(info.key, duration, amplifier);
        skill.addAction(action);

        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), itemData);

        String msg = String.format("<green>✓ Added potion: %s (%ds, Amplifier %d)", 
            info.displayName, duration, amplifier + 1);
        player.sendMessage(TextUtil.success(msg));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        // Back to SkillEditorGui (use goBack to keep the parent chain)
        goBack();
    }
}
