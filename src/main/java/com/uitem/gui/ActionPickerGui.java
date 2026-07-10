package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.SkillAction;
import com.uitem.util.Messages;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ActionPickerGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private final Consumer<SkillAction.ActionType> callback;
    private int currentPage = 0;

    private static final List<SkillAction.ActionType> ALL_ACTIONS = List.of(
            SkillAction.ActionType.MESSAGE,
            SkillAction.ActionType.ACTION_BAR,
            SkillAction.ActionType.TITLE,
            SkillAction.ActionType.PLAY_SOUND,
            SkillAction.ActionType.SPAWN_PARTICLE,
            SkillAction.ActionType.DAMAGE,
            SkillAction.ActionType.HEAL,
            SkillAction.ActionType.FEED,
            SkillAction.ActionType.SET_FIRE,
            SkillAction.ActionType.LAUNCH,
            SkillAction.ActionType.TELEPORT,
            SkillAction.ActionType.STRIKE_LIGHTNING,
            SkillAction.ActionType.SHOOT_PROJECTILE,
            SkillAction.ActionType.PLAYER_COMMAND,
            SkillAction.ActionType.CONSOLE_COMMAND,
            SkillAction.ActionType.CONSUME_DURABILITY,
            SkillAction.ActionType.CONSUME_ITEM,
            SkillAction.ActionType.GIVE_ITEM,
            SkillAction.ActionType.GIVE_POTION,
            SkillAction.ActionType.REMOVE_POTION,
            SkillAction.ActionType.NOTHING,
            SkillAction.ActionType.BLOCK_BREAK,
            SkillAction.ActionType.EXPLOSION,
            SkillAction.ActionType.AREA_DAMAGE,
            SkillAction.ActionType.POTION_AREA,
            SkillAction.ActionType.BLIND,
            SkillAction.ActionType.GLOW,
            SkillAction.ActionType.CLEAR_EFFECTS,
            SkillAction.ActionType.LEVITATE,
            SkillAction.ActionType.SPAWN_MOB,
            SkillAction.ActionType.FREEZE,
            SkillAction.ActionType.BEAM_DAMAGE
    );

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int ITEMS_PER_PAGE = SLOTS.length;

    public ActionPickerGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill, Consumer<SkillAction.ActionType> callback) {
        super(plugin, player, 54, "<gold><bold>Action Picker</bold>");
        this.itemData = itemData;
        this.skill = skill;
        this.callback = callback;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        int total = ALL_ACTIONS.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        setItem(4, createItem(Material.KNOWLEDGE_BOOK,
                "<gold><bold>Select an Action</bold>",
                List.of(
                        "<gray>Page: <white>" + (currentPage + 1) + "/" + totalPages,
                        "<gray>Total: <white>" + total + " actions",
                        "",
                        "<yellow>Click to add action to skill"
                )));

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, total);

        for (int i = startIdx; i < endIdx; i++) {
            SkillAction.ActionType type = ALL_ACTIONS.get(i);
            int slot = SLOTS[i - startIdx];
            setItem(slot, createActionIcon(type), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                // Do NOT goBack() here: prompt-based actions close this GUI and wait
                // for chat input — reopening a GUI on top would block the chat prompt.
                // The callback flow (SkillEditorGui) reopens the editor itself when done.
                callback.accept(type);
            });
        }

        if (currentPage > 0) {
            setItem(48, createItem(Material.ARROW, "<gray>< Previous Page"), ev -> {
                currentPage--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        if (endIdx < total) {
            setItem(50, createItem(Material.ARROW, "<gray>Next Page >"), ev -> {
                currentPage++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack createActionIcon(SkillAction.ActionType type) {
        Material mat = iconFor(type);
        String display = Messages.skillActionDisplay(type);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + type.getDisplay());
        lore.add("");
        lore.add("<yellow>" + Messages.skillText("click-to-add", "Click to add this action"));
        return createItem(mat, "<green>+ " + display, lore);
    }

    private static Material iconFor(SkillAction.ActionType type) {
        return switch (type) {
            case MESSAGE -> Material.PAPER;
            case ACTION_BAR -> Material.NAME_TAG;
            case TITLE -> Material.FILLED_MAP;
            case PLAY_SOUND -> Material.NOTE_BLOCK;
            case SPAWN_PARTICLE -> Material.FIREWORK_STAR;
            case GIVE_POTION -> Material.POTION;
            case REMOVE_POTION -> Material.GLASS_BOTTLE;
            case HEAL -> Material.GOLDEN_APPLE;
            case DAMAGE -> Material.IRON_SWORD;
            case AREA_DAMAGE -> Material.DRAGON_BREATH;
            case POTION_AREA -> Material.LINGERING_POTION;
            case SHOOT_PROJECTILE -> Material.BOW;
            case CONSOLE_COMMAND -> Material.COMMAND_BLOCK;
            case PLAYER_COMMAND -> Material.REPEATER;
            case LAUNCH -> Material.FIREWORK_ROCKET;
            case TELEPORT -> Material.ENDER_PEARL;
            case STRIKE_LIGHTNING -> Material.NETHER_STAR;
            case SET_FIRE -> Material.FLINT_AND_STEEL;
            case FEED -> Material.COOKED_BEEF;
            case CONSUME_DURABILITY -> Material.ANVIL;
            case CONSUME_ITEM -> Material.ITEM_FRAME;
            case GIVE_ITEM -> Material.CHEST;
            case BLOCK_BREAK -> Material.DIAMOND_PICKAXE;
            case EXPLOSION -> Material.TNT;
            case BLIND -> Material.INK_SAC;
            case GLOW -> Material.GLOWSTONE_DUST;
            case CLEAR_EFFECTS -> Material.MILK_BUCKET;
            case LEVITATE -> Material.FEATHER;
            case SPAWN_MOB -> Material.EGG;
            case FREEZE -> Material.ICE;
            case BEAM_DAMAGE -> Material.ENDER_EYE;
            case NOTHING -> Material.BARRIER;
        };
    }
}
