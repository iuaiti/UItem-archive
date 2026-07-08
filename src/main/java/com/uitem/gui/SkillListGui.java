package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.SkillTrigger;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * List of an item's skills/passives.
 * Allows adding, deleting, and editing skills.
 */
public class SkillListGui extends AbstractGui {

    private final UItemData data;
    private int page;
    private static final int[] SKILL_SLOTS = {10,11,12,13,14,15,16,17, 19,20,21,22,23,24,25,26};
    private static final int SKILLS_PER_PAGE = SKILL_SLOTS.length;

    public SkillListGui(UItemPlugin plugin, Player player, UItemData data) {
        this(plugin, player, data, 0);
    }

    public SkillListGui(UItemPlugin plugin, Player player, UItemData data, int page) {
        super(plugin, player, 54, Messages.skillListTitle(data.getId()));
        this.data = data;
        this.page = page;
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

        List<Skill> current = data.getSkills() != null ? new ArrayList<>(data.getSkills()) : new ArrayList<>();
        int totalPages = Math.max(1, (int) Math.ceil((double) current.size() / SKILLS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, createItem(Material.ENCHANTED_BOOK,
                Messages.skillListHeader(data.getId()),
                List.of(
                        Messages.skillListDesc(),
                        Messages.skillListFolia(),
                        Messages.skillListClickEdit() + " | " + Messages.skillListClickDelete(),
                        "<gray>Page: <white>" + (page + 1) + "/" + totalPages
                )));

        // Current skills
        int startIndex = page * SKILLS_PER_PAGE;
        for (int i = 0; i < SKILL_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex >= current.size()) break;
            final int idx = dataIndex;
            Skill sk = current.get(dataIndex);
            ItemStack icon = buildSkillIcon(sk);
            setItem(SKILL_SLOTS[i], icon, ev -> {
                if (ev.isShiftClick() && ev.isRightClick()) {
                    // Shift + Right Click = Delete
                    if (idx < data.getSkills().size()) {
                        data.getSkills().remove(idx);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.0f);
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), data);
                        refresh();
                    }
                } else {
                    // Left Click = Edit
                    if (idx < data.getSkills().size()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                        new SkillEditorGui(plugin, player, data, data.getSkills().get(idx)).openAsChildOf(this);
                    }
                }
            });
        }

        if (current.isEmpty()) {
            setItem(10, createItem(Material.BARRIER, Messages.skillListEmpty(),
                    List.of(Messages.skillListEmptyHint())));
        }

        // Add new skill
        setItem(40, createItem(Material.EMERALD_BLOCK, Messages.skillListAdd(),
                List.of(
                        Messages.skillListAddDesc(),
                        Messages.skillListAddClick()
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            // Create an empty skill with a random ID
            String newId = "skill_" + (data.getSkills().size() + 1);
            Skill newSkill = new Skill(newId);
            newSkill.setTrigger(SkillTrigger.RIGHT_CLICK);
            newSkill.setCooldown(3.0);
            newSkill.setCancelEvent(true);
            data.getSkills().add(newSkill);

            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), data);

            new SkillEditorGui(plugin, player, data, newSkill).openAsChildOf(this);
        });

        if (page > 0) {
            setItem(46, createPreviousButton(), ev -> {
                page--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                refresh();
            });
        }
        if (page < totalPages - 1) {
            setItem(52, createNextButton(), ev -> {
                page++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                refresh();
            });
        }

        // Back
        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack buildSkillIcon(Skill sk) {
        Material mat = switch (sk.getTrigger()) {
            case RIGHT_CLICK, SHIFT_RIGHT_CLICK -> Material.DIAMOND_SWORD;
            case LEFT_CLICK, SHIFT_LEFT_CLICK -> Material.IRON_SWORD;
            case HIT -> Material.IRON_AXE;
            case TAKE_DAMAGE -> Material.SHIELD;
            case EQUIP, WHILE_WEARING -> Material.DIAMOND_CHESTPLATE;
            case PERIODIC, WHILE_HOLDING -> Material.CLOCK;
            case DROP -> Material.DROPPER;
            default -> Material.ENCHANTED_BOOK;
        };

        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + Messages.skillLabel("trigger", "Trigger") + ": <white>" + sk.getTrigger().getDisplayName());
        lore.add("<gray>" + Messages.skillLabel("cooldown", "Cooldown") + ": <white>" + sk.getCooldown() + "s");
        if (sk.getChance() < 100) lore.add("<gray>" + Messages.skillLabel("chance", "Chance") + ": <white>" + (int)sk.getChance() + "%");
        lore.add("<gray>" + Messages.skillLabel("actions", "Actions") + ": <white>" + sk.getActions().size());
        lore.add("");
        lore.add(Messages.skillListClickEdit());
        lore.add(Messages.skillListShiftDelete());
        return createItem(mat, "<light_purple>" + sk.getId() + " <dark_gray>→ <white>" + sk.getTrigger().name(), lore);
    }


}
