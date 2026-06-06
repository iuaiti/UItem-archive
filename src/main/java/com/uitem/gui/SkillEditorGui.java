package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.SkillAction;
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
 * Editor cho 1 skill/passive.
 * Cho phép:
 * - Đổi trigger
 * - Đặt cooldown + chance + cancel
 * - Thêm action nhanh (preset buttons)
 * - Nhập action chi tiết qua chat
 */
public class SkillEditorGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;

    private static final List<SkillTrigger> ACTIVE_TRIGGERS = List.of(
            SkillTrigger.RIGHT_CLICK, SkillTrigger.LEFT_CLICK,
            SkillTrigger.SHIFT_RIGHT_CLICK, SkillTrigger.SHIFT_LEFT_CLICK,
            SkillTrigger.DROP, SkillTrigger.SWAP_HAND
    );

    private static final List<SkillTrigger> PASSIVE_TRIGGERS = List.of(
            SkillTrigger.HIT, SkillTrigger.TAKE_DAMAGE, SkillTrigger.KILL,
            SkillTrigger.EQUIP, SkillTrigger.UNEQUIP,
            SkillTrigger.PERIODIC, SkillTrigger.WHILE_HOLDING, SkillTrigger.WHILE_WEARING
    );

    public SkillEditorGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, Messages.skillEditorTitle(skill.getId()));
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.ENCHANTED_BOOK,
                "<light_purple><bold>" + skill.getId() + "</bold> <dark_gray>- " + skill.getTrigger().getDisplayName(),
                List.of(
                        "<gray>Cooldown: <white>" + skill.getCooldown() + "s",
                        "<gray>Chance: <white>" + (int) skill.getChance() + "%",
                        "<gray>Actions: <white>" + skill.getActions().size()
                )));

        // === Trigger selection (active) ===
        setItem(9, createItem(Material.DIAMOND_SWORD, Messages.skillEditorActiveTriggers(), List.of("<gray>Right / Left Click")));

        int slot = 10;
        for (SkillTrigger t : ACTIVE_TRIGGERS) {
            boolean sel = t == skill.getTrigger();
            Material m = sel ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(slot++, createItem(m, (sel ? "<green>✔ " : "<gray>") + t.getDisplayName(),
                    List.of("<gray>" + t.getDescription())), ev -> {
                skill.setTrigger(t);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                saveAndRefresh();
            });
        }

        // === Passive triggers ===
        setItem(18, createItem(Material.CLOCK, Messages.skillEditorPassiveTriggers(), List.of()));

        slot = 19;
        for (SkillTrigger t : PASSIVE_TRIGGERS) {
            boolean sel = t == skill.getTrigger();
            Material m = sel ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(slot++, createItem(m, (sel ? "<green>✔ " : "<gray>") + t.getDisplayName(),
                    List.of("<gray>" + t.getDescription())), ev -> {
                skill.setTrigger(t);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                saveAndRefresh();
            });
        }

        // === Cooldown ===
        setItem(28, createItem(Material.CLOCK, String.format(Messages.skillEditorCooldown(), skill.getCooldown()),
                List.of("<yellow>" + Messages.skillEditorCooldownHint().replace("<yellow>", ""))), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>Nhập cooldown (giây, ví dụ 4.5):", input -> {
                try {
                    double cd = Double.parseDouble(input.trim());
                    skill.setCooldown(cd);
                } catch (Exception ignored) {}
                saveAndRefresh();
            });
        });

        // Quick cooldown presets
        double[] cds = {0, 1, 3, 5, 8, 12, 20};
        int[] cdSlots = {29,30,31,32,33,34,35};
        for (int i = 0; i < cds.length; i++) {
            final double c = cds[i];
            boolean sel = Math.abs(skill.getCooldown() - c) < 0.1;
            setItem(cdSlots[i], createItem(sel ? Material.LIME_DYE : Material.GRAY_DYE,
                    (sel ? "<green>" : "<gray>") + c + "s"), ev -> {
                skill.setCooldown(c);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                saveAndRefresh();
            });
        }

        // Chance
        setItem(37, createItem(Material.GHAST_TEAR, "<gold><bold>Chance: <white>" + (int)skill.getChance() + "%</bold>",
                List.of("<gray>% kích hoạt (hữu ích cho passive)")), ev -> {
            plugin.getChatInputManager().requestInput(player, "<yellow>Nhập chance (0-100):", input -> {
                try { skill.setChance(Double.parseDouble(input.trim())); } catch (Exception ignored) {}
                saveAndRefresh();
            });
        });

        // Cancel event
        boolean cancel = skill.isCancelEvent();
        setItem(38, createItem(cancel ? Material.BARRIER : Material.PAPER,
                "<gold><bold>Cancel Event: <white>" + (cancel ? "BẬT" : "TẮT") + "</bold>",
                List.of("<gray>Chỉ ảnh hưởng active trigger (click/drop)",
                        "<yellow>Click để bật/tắt")), ev -> {
            skill.setCancelEvent(!cancel);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            saveAndRefresh();
        });

        // === Actions section ===
        setItem(39, createItem(Material.WRITABLE_BOOK,
                Messages.skillEditorActionsHeader(skill.getActions().size()),
                List.of(
                        Messages.skillEditorActionsHint(),
                        "<yellow>" + Messages.skillEditorAddAction().replace("<green><bold>+ ", "").replace("</bold>", "")
                )));

        // List current actions (click trái = edit, phải = xóa)
        List<SkillAction> acts = skill.getActions();
        int[] actSlots = {40, 41, 42, 43, 44, 45, 46, 47};
        for (int i = 0; i < Math.min(acts.size(), actSlots.length); i++) {
            final int idx = i;
            SkillAction a = acts.get(i);
            ItemStack icon = buildActionIcon(a);
            setItem(actSlots[i], icon, ev -> {
                if (idx >= skill.getActions().size()) return;

                if (ev.isRightClick()) {
                    // Xóa
                    skill.getActions().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    saveAndRefresh();
                } else {
                    // Edit giá trị chính của action
                    editActionMainValue(a);
                }
            });
        }

        if (acts.isEmpty()) {
            setItem(49, createItem(Material.BARRIER, "<gray><italic>" + Messages.skillEditorNoActions() + "</italic>",
                    List.of("<yellow>Click buttons below to add actions")));
        }

        setItem(26, createItem(Material.EMERALD_BLOCK, Messages.skillEditorChooseAction(),
                List.of("<gray>Native skill system - fully GUI driven")) );

        // Quick condition buttons
        setItem(50, createItem(Material.EMERALD, "<green>Cond: Chance 75%", List.of("<gray>Click to add 75% chance")), ev -> {
            skill.addCondition(com.uitem.skill.SkillCondition.chance(75));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            saveAndRefresh();
        });
        setItem(51, createItem(Material.REDSTONE, "<red>Cond: Chance 50%", List.of("<gray>Click to add 50% chance")), ev -> {
            skill.addCondition(com.uitem.skill.SkillCondition.chance(50));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            saveAndRefresh();
        });
        setItem(52, createItem(Material.BOOK, "<blue>Cond: Permission...", List.of("<gray>Click to set permission")), ev -> {
            plugin.getChatInputManager().requestInput(player, "<yellow>Enter required permission:", input -> {
                skill.addCondition(com.uitem.skill.SkillCondition.permission(input.trim()));
                saveAndRefresh();
            });
        });

        // ==================== GRID THÊM ACTION BẰNG GUI (dùng slot nội dung an toàn) ====================
        // Hàng trên (36-44)
        addActionTypeButton(36, Material.PAPER, "MESSAGE", Messages.skillActionDisplay(SkillAction.ActionType.MESSAGE), () -> addActionWithPrompt(SkillAction.ActionType.MESSAGE));
        addActionTypeButton(37, Material.PAPER, "ACTION_BAR", Messages.skillActionDisplay(SkillAction.ActionType.ACTION_BAR), () -> addActionWithPrompt(SkillAction.ActionType.ACTION_BAR));
        addActionTypeButton(38, Material.PAPER, "TITLE", Messages.skillActionDisplay(SkillAction.ActionType.TITLE), () -> addActionWithPrompt(SkillAction.ActionType.TITLE));

        addActionTypeButton(39, Material.NOTE_BLOCK, "PLAY_SOUND", Messages.skillActionDisplay(SkillAction.ActionType.PLAY_SOUND), () -> addActionWithPrompt(SkillAction.ActionType.PLAY_SOUND));
        addActionTypeButton(40, Material.BLAZE_POWDER, "SPAWN_PARTICLE", Messages.skillActionDisplay(SkillAction.ActionType.SPAWN_PARTICLE), () -> addActionWithPrompt(SkillAction.ActionType.SPAWN_PARTICLE));

        addActionTypeButton(41, Material.POTION, "GIVE_POTION", Messages.skillActionDisplay(SkillAction.ActionType.GIVE_POTION), () -> addActionWithPrompt(SkillAction.ActionType.GIVE_POTION));
        addActionTypeButton(42, Material.MILK_BUCKET, "REMOVE_POTION", Messages.skillActionDisplay(SkillAction.ActionType.REMOVE_POTION), () -> addDirectAction(SkillAction.ActionType.REMOVE_POTION));
        addActionTypeButton(43, Material.GOLDEN_APPLE, "HEAL", Messages.skillActionDisplay(SkillAction.ActionType.HEAL), () -> addNumericAction(SkillAction.ActionType.HEAL, "Enter heal amount (e.g. 3.5):", 3.0));
        addActionTypeButton(44, Material.IRON_SWORD, "DAMAGE", Messages.skillActionDisplay(SkillAction.ActionType.DAMAGE), () -> addNumericAction(SkillAction.ActionType.DAMAGE, "Enter damage amount (e.g. 5):", 4.0));

        // Second row
        addActionTypeButton(27, Material.FIRE_CHARGE, "SET_FIRE", Messages.skillActionDisplay(SkillAction.ActionType.SET_FIRE), () -> addNumericAction(SkillAction.ActionType.SET_FIRE, "Enter seconds on fire:", 4));
        addActionTypeButton(28, Material.ELYTRA, "LAUNCH", Messages.skillActionDisplay(SkillAction.ActionType.LAUNCH), () -> addActionWithPrompt(SkillAction.ActionType.LAUNCH));
        addActionTypeButton(29, Material.ENDER_PEARL, "TELEPORT", Messages.skillActionDisplay(SkillAction.ActionType.TELEPORT), () -> addActionWithPrompt(SkillAction.ActionType.TELEPORT));
        addActionTypeButton(30, Material.FIRE_CHARGE, "STRIKE_LIGHTNING", Messages.skillActionDisplay(SkillAction.ActionType.STRIKE_LIGHTNING), () -> addDirectAction(SkillAction.ActionType.STRIKE_LIGHTNING));
        addActionTypeButton(31, Material.BOW, "SHOOT_PROJECTILE", Messages.skillActionDisplay(SkillAction.ActionType.SHOOT_PROJECTILE), () -> addActionWithPrompt(SkillAction.ActionType.SHOOT_PROJECTILE));

        addActionTypeButton(32, Material.COMMAND_BLOCK, "CONSOLE_COMMAND", Messages.skillActionDisplay(SkillAction.ActionType.CONSOLE_COMMAND), () -> addActionWithPrompt(SkillAction.ActionType.CONSOLE_COMMAND));
        addActionTypeButton(33, Material.COMMAND_BLOCK, "PLAYER_COMMAND", Messages.skillActionDisplay(SkillAction.ActionType.PLAYER_COMMAND), () -> addActionWithPrompt(SkillAction.ActionType.PLAYER_COMMAND));
        addActionTypeButton(34, Material.ANVIL, "CONSUME_DURABILITY", Messages.skillActionDisplay(SkillAction.ActionType.CONSUME_DURABILITY), () -> addNumericAction(SkillAction.ActionType.CONSUME_DURABILITY, "Enter durability to consume:", 1));
        addActionTypeButton(35, Material.COOKED_BEEF, "FEED", Messages.skillActionDisplay(SkillAction.ActionType.FEED), () -> addNumericAction(SkillAction.ActionType.FEED, "Enter hunger amount:", 4));

        // Back
        setItem(53, createBackButton(), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new SkillListGui(plugin, player, itemData).open();
        });
    }

    private ItemStack buildActionIcon(SkillAction a) {
        Material mat = Material.PAPER;
        String name = "<white>" + a.getType().getDisplay();
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Loại: <white>" + a.getType().name());

        // Hiển thị tham số chính cho dễ đọc
        switch (a.getType()) {
            case MESSAGE, ACTION_BAR -> {
                String txt = a.getStringParam("text", "");
                if (!txt.isEmpty()) lore.add("<gray>Nội dung: <white>" + (txt.length() > 25 ? txt.substring(0, 25) + "..." : txt));
            }
            case PLAY_SOUND -> lore.add("<gray>Âm thanh: <white>" + a.getStringParam("sound", "?"));
            case GIVE_POTION -> lore.add("<gray>Effect: <white>" + a.getStringParam("effect", "?") + " " + a.getIntParam("amplifier", 0));
            case HEAL, DAMAGE -> lore.add("<gray>Giá trị: <white>" + a.getDoubleParam("amount", 0));
            case SHOOT_PROJECTILE -> lore.add("<gray>Projectile: <white>" + a.getStringParam("projectile", "FIREBALL"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> {
                String cmd = a.getStringParam("command", "");
                if (!cmd.isEmpty()) lore.add("<gray>Lệnh: <white>" + (cmd.length() > 28 ? cmd.substring(0, 28) + "..." : cmd));
            }
            case LAUNCH -> lore.add("<gray>Velocity: <white>" + a.getDoubleParam("y", 0));
        }

        lore.add("");
        lore.add("<yellow>Left: Edit value");
        lore.add("<red>Right: Delete");
        return createItem(mat, name, lore);
    }

    /** Add quick action type button (bilingual) */
    private void addActionTypeButton(int slot, Material mat, String typeName, String display, Runnable onClick) {
        SkillAction.ActionType type;
        try {
            type = SkillAction.ActionType.valueOf(typeName);
        } catch (Exception e) {
            type = SkillAction.ActionType.MESSAGE;
        }
        setItem(slot, createItem(mat, "<green>+ " + display, List.of("<gray>" + Messages.skillActionDisplay(type))), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            onClick.run();
        });
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
        refresh();
    }

    // ==================== Helpers thêm action hoàn toàn bằng GUI ====================

    /**
     * Thêm action và ngay lập tức hỏi giá trị chính qua chat (dễ dùng nhất cho text/command).
     */
    private void addActionWithPrompt(SkillAction.ActionType type) {
        SkillAction action = new SkillAction(type);

        // Đặt giá trị mặc định hợp lý cho một số loại
        switch (type) {
            case PLAY_SOUND -> {
                action.setParam("sound", "UI_BUTTON_CLICK");
                action.setParam("volume", 1.0);
                action.setParam("pitch", 1.0);
            }
            case GIVE_POTION -> {
                action.setParam("effect", "SPEED");
                action.setParam("duration", 10);
                action.setParam("amplifier", 0);
            }
            case SHOOT_PROJECTILE -> {
                action.setParam("projectile", "SMALL_FIREBALL");
                action.setParam("speed", 1.5);
                action.setParam("damage", 5.0);
            }
            case LAUNCH -> {
                action.setParam("x", 0);
                action.setParam("y", 1.2);
                action.setParam("z", 0);
                action.setParam("relative", true);
            }
        }

        skill.addAction(action);
        saveAndRefresh();

        // Ask for the most important value right after adding (bilingual via Messages fallback)
        switch (type) {
            case MESSAGE -> promptForActionParam(action, "text", "Enter message content (supports %player% %target%):", Messages.skillUpdated("MESSAGE"));
            case ACTION_BAR -> promptForActionParam(action, "text", "Enter action bar text:", Messages.skillUpdated("ACTION_BAR"));
            case TITLE -> promptForActionParam(action, "title", "Enter Title:", Messages.skillUpdated("TITLE"));
            case PLAY_SOUND -> promptForActionParam(action, "sound", "Enter sound name (e.g. ENTITY_BLAZE_SHOOT):", Messages.skillUpdated("SOUND"));
            case GIVE_POTION -> promptForActionParam(action, "effect", "Enter potion effect (e.g. SPEED, STRENGTH):", Messages.skillUpdated("POTION"));
            case SHOOT_PROJECTILE -> promptForActionParam(action, "projectile", "Projectile (SMALL_FIREBALL / ARROW / WITHER_SKULL...):", Messages.skillUpdated("PROJECTILE"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> promptForActionParam(action, "command", "Enter command (supports %player% %target%):", Messages.skillUpdated("COMMAND"));
            case LAUNCH -> promptForActionParam(action, "y", "Enter launch Y power (e.g. 1.5):", Messages.skillUpdated("LAUNCH"));
            case TELEPORT -> promptForActionParam(action, "y", "Enter teleport Y offset:", Messages.skillUpdated("TELEPORT"));
            default -> {
                player.sendMessage(TextUtil.success(Messages.skillAdded(type.name())));
            }
        }
    }

    /**
     * Thêm action có giá trị số, cho phép chọn nhanh bằng button trước khi hỏi custom.
     */
    private void addNumericAction(SkillAction.ActionType type, String prompt, double defaultValue) {
        SkillAction action = new SkillAction(type);

        // Preset nhanh trước
        switch (type) {
            case HEAL -> action.setParam("amount", defaultValue);
            case DAMAGE -> action.setParam("amount", defaultValue);
            case CONSUME_DURABILITY -> action.setParam("amount", (int) defaultValue);
            case FEED -> action.setParam("amount", (int) defaultValue);
            case SET_FIRE -> action.setParam("seconds", (int) defaultValue);
        }

        skill.addAction(action);
        saveAndRefresh();

        // Cho người dùng chỉnh ngay giá trị số
        promptForActionParam(action, (type == SkillAction.ActionType.SET_FIRE ? "seconds" : "amount"), prompt, Messages.skillUpdated(type.name()));
    }

    /** Thêm action không cần input (có giá trị mặc định tốt) */
    private void addDirectAction(SkillAction.ActionType type) {
        SkillAction action = new SkillAction(type);
        skill.addAction(action);
        player.sendMessage(TextUtil.success(Messages.skillAdded(type.getDisplay())));
        saveAndRefresh();
    }

    /** Hỏi giá trị 1 param rồi lưu + refresh */
    private void promptForActionParam(SkillAction action, String paramKey, String promptMessage, String successMsg) {
        plugin.getChatInputManager().requestInput(player, "<yellow>" + promptMessage, input -> {
            try {
                String val = input.trim();
                // Thử parse số nếu param là amount / y / seconds...
                if (paramKey.equals("amount") || paramKey.equals("y") || paramKey.equals("seconds") || paramKey.equals("speed") || paramKey.equals("damage")) {
                    double num = Double.parseDouble(val);
                    action.setParam(paramKey, num);
                } else {
                    action.setParam(paramKey, val);
                }
                player.sendMessage(TextUtil.success(successMsg));
            } catch (Exception e) {
                // Giữ giá trị mặc định nếu parse fail
                player.sendMessage(TextUtil.info("Added with default value (click the action again to edit)."));
            }
            saveAndRefresh();
        });
    }

    /**
     * Khi click vào action đã có trong list → cho sửa giá trị chính.
     */
    private void editActionMainValue(SkillAction action) {
        switch (action.getType()) {
            case MESSAGE, ACTION_BAR -> promptForActionParam(action, "text", "Enter new content:", Messages.skillUpdated("content"));
            case PLAY_SOUND -> promptForActionParam(action, "sound", "Enter new sound:", Messages.skillUpdated("sound"));
            case GIVE_POTION -> promptForActionParam(action, "effect", "Enter new effect (e.g. STRENGTH):", Messages.skillUpdated("potion"));
            case HEAL, DAMAGE -> promptForActionParam(action, "amount", "Enter new value:", Messages.skillUpdated("value"));
            case SHOOT_PROJECTILE -> promptForActionParam(action, "projectile", "Enter new projectile type:", Messages.skillUpdated("projectile"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> promptForActionParam(action, "command", "Enter new command:", Messages.skillUpdated("command"));
            case LAUNCH -> promptForActionParam(action, "y", "Enter new Y power:", Messages.skillUpdated("launch"));
            case CONSUME_DURABILITY -> promptForActionParam(action, "amount", "Enter durability cost:", Messages.skillUpdated("durability"));
            default -> {
                player.sendMessage(TextUtil.info("This action mainly uses default values. You can delete and re-add it."));
            }
        }
    }
}
