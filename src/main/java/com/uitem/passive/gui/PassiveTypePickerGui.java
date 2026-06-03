package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.ActionEntry;
import com.uitem.passive.ConditionEntry;
import com.uitem.passive.condition.ConditionType;
import com.uitem.passive.action.ActionType;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.entity.Player;

import java.util.List;

public class PassiveTypePickerGui extends AbstractGui {

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final boolean conditionMode;

    public PassiveTypePickerGui(UItemPlugin plugin, Player player, boolean conditionMode) {
        super(plugin, player, 54, GuiTheme.title(conditionMode ? "Chon dieu kien" : "Chon hanh dong"));
        this.conditionMode = conditionMode;
    }

    @Override
    protected void setup() {
        fillBorder(6);
        var session = plugin.getPassiveBootstrap().manager().session(player);

        if (conditionMode) {
            List<ConditionType> types = ConditionType.all();
            for (int i = 0; i < SLOTS.length && i < types.size(); i++) {
                ConditionType t = types.get(i);
                setItem(SLOTS[i], GuiTheme.button(t.icon(), "<white>" + t.displayName(),
                        List.of("<gray>Click de cau hinh")), ev -> {
                    GuiFeedback.click(player);
                    session.conditions().add(ConditionEntry.of(t));
                    int idx = session.conditions().entries().size() - 1;
                    new PassiveValueEditorGui(plugin, player, true, idx).open();
                });
            }
        } else {
            List<ActionType> types = ActionType.all();
            for (int i = 0; i < SLOTS.length && i < types.size(); i++) {
                ActionType t = types.get(i);
                setItem(SLOTS[i], GuiTheme.button(t.icon(), "<white>" + t.displayName(),
                        List.of("<gray>Click de cau hinh")), ev -> {
                    GuiFeedback.click(player);
                    session.actions().add(ActionEntry.of(t));
                    int idx = session.actions().size() - 1;
                    new PassiveValueEditorGui(plugin, player, false, idx).open();
                });
            }
        }

        setItem(49, createBackButton(), ev -> {
            if (conditionMode) {
                new StepConditionsGui(plugin, player).open();
            } else {
                new StepActionsGui(plugin, player).open();
            }
        });
    }
}