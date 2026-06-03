package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.ability.AbilityHolder;
import com.uitem.ability.ItemAbilityAction;
import com.uitem.ability.ItemAbilityRule;
import com.uitem.ability.ItemActionType;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbilityEditorGui extends AbstractGui {

    private final AbilityHolder holder;
    private final String kindLabel;
    private final int ruleIndex;
    private final Runnable onBack;
    private final Runnable persistFull;
    private final Runnable persistMemory;

    private AbilityEditorGui(UItemPlugin plugin, Player player, AbilityHolder holder, String kindLabel,
                           int ruleIndex, Runnable onBack, Runnable persistFull, Runnable persistMemory) {
        super(plugin, player, 54, GuiTheme.title("Sua rule — " + holder.getAbilityId()));
        this.holder = holder;
        this.kindLabel = kindLabel;
        this.ruleIndex = ruleIndex;
        this.onBack = onBack;
        this.persistFull = persistFull;
        this.persistMemory = persistMemory;
    }

    public static void open(UItemPlugin plugin, Player player, AbilityHolder holder, String kindLabel,
                            int ruleIndex, Runnable onBack, Runnable persistFull, Runnable persistMemory) {
        new AbilityEditorGui(plugin, player, holder, kindLabel, ruleIndex, onBack, persistFull, persistMemory).open();
    }

    private ItemAbilityRule rule() {
        return holder.getAbilities().get(ruleIndex);
    }

    @Override
    protected void setup() {
        if (ruleIndex < 0 || ruleIndex >= holder.getAbilities().size()) {
            if ("potion".equals(kindLabel)) {
                AbilityListGui.forPotion(plugin, player, (com.uitem.potion.UPotionData) holder).open();
            } else {
                AbilityListGui.forItem(plugin, player, (com.uitem.UItemData) holder).open();
            }
            return;
        }
        fillBorder(6);
        ItemAbilityRule rule = rule();

        setItem(10, createItem(Material.COMPASS,
                "<gold>Kich hoat",
                List.of(
                        "<gray>Hien tai: <white>" + rule.getTrigger().label(),
                        "<yellow>Click <gray>doi"
                )), ev -> {
            rule.setTrigger(rule.getTrigger().next());
            refresh();
        });

        setItem(11, createItem(Material.ARMOR_STAND,
                "<gold>Vi tri",
                List.of(
                        "<gray>Hien tai: <white>" + rule.getSlot().label(),
                        "<yellow>Click <gray>doi"
                )), ev -> {
            rule.setSlot(rule.getSlot().next());
            refresh();
        });

        setItem(12, createItem(Material.CLOCK,
                "<gold>Cooldown (giay)",
                List.of("<gray>Hien tai: <white>" + rule.getCooldownSeconds())),
                ev -> plugin.getChatInputManager().requestInput(player,
                "<yellow>Nhap cooldown (giay):", input -> {
                    try {
                        rule.setCooldownSeconds(Integer.parseInt(input.trim()));
                        saveAndReopen();
                    } catch (NumberFormatException ex) {
                        player.sendMessage(TextUtil.error("So khong hop le!"));
                        reopen();
                    }
                }));

        setItem(13, createItem(Material.EXPERIENCE_BOTTLE,
                "<gold>Ti le %",
                List.of("<gray>Hien tai: <white>" + rule.getChancePercent() + "%")),
                ev -> plugin.getChatInputManager().requestInput(player,
                "<yellow>Nhap ti le 0-100:", input -> {
                    try {
                        rule.setChancePercent(Integer.parseInt(input.trim()));
                        saveAndReopen();
                    } catch (NumberFormatException ex) {
                        player.sendMessage(TextUtil.error("So khong hop le!"));
                        reopen();
                    }
                }));

        List<ItemAbilityAction> actions = rule.getActions();
        int[] actionSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < actionSlots.length && i < actions.size(); i++) {
            ItemAbilityAction action = actions.get(i);
            int actionIndex = i;
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Loai: <white>" + action.getType().label());
            for (Map.Entry<String, String> p : action.getParams().entrySet()) {
                lore.add("<dark_gray>" + p.getKey() + ": <white>" + p.getValue());
            }
            lore.add("");
            lore.add("<yellow>Click <gray>doi loai");
            lore.add("<yellow>Shift+Click <gray>tham so key=value");
            lore.add("<red>Right-Click <gray>xoa");
            setItem(actionSlots[i], createItem(Material.FIRE_CHARGE,
                    "<red>Hanh dong #" + (i + 1), lore), ev -> {
                if (ev.isShiftClick()) {
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Nhap tham so (vd: amount=10):", input -> {
                                if (input.contains("=")) {
                                    String[] kv = input.split("=", 2);
                                    action.getParams().put(kv[0].trim(), kv[1].trim());
                                }
                                saveAndReopen();
                            });
                } else if (ev.isRightClick()) {
                    actions.remove(actionIndex);
                    saveAndReopen();
                } else {
                    ItemActionType[] types = ItemActionType.values();
                    int next = (action.getType().ordinal() + 1) % types.length;
                    action.setType(types[next]);
                    action.setParams(types[next].defaultParamMap());
                    refresh();
                }
            });
        }

        setItem(48, createItem(Material.LIME_DYE,
                "<green>+ Them hanh dong", List.of("<yellow>Click")), ev -> {
            rule.getActions().add(new ItemAbilityAction(ItemActionType.DAMAGE));
            refresh();
        });

        setItem(50, GuiTheme.actionDanger(Material.BARRIER, "XOA RULE", "<red>Xoa rule"), ev -> {
            holder.getAbilities().remove(ruleIndex);
            persistMemory.run();
            openList();
        });

        setItem(46, createBackButton(), ev -> {
            GuiFeedback.click(player);
            openList();
        });

        setItem(49, GuiTheme.actionPrimary(Material.LIME_CONCRETE, "SAVE", "<gray>Luu rule"), ev -> {
            persistFull.run();
            player.sendMessage(TextUtil.success("Da luu rule!"));
            GuiFeedback.success(player);
            openList();
        });
    }

    private void openList() {
        if ("potion".equals(kindLabel)) {
            AbilityListGui.forPotion(plugin, player, (com.uitem.potion.UPotionData) holder).open();
        } else {
            AbilityListGui.forItem(plugin, player, (com.uitem.UItemData) holder).open();
        }
    }

    private void saveAndReopen() {
        persistMemory.run();
        reopen();
    }

    private void reopen() {
        open(plugin, player, holder, kindLabel, ruleIndex, onBack, persistFull, persistMemory);
    }
}