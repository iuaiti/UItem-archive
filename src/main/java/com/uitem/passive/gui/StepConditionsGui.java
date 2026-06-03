package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.ConditionEntry;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class StepConditionsGui extends AbstractGui {

    private static final int[] ENTRY_SLOTS = {20, 21, 22, 23, 24};
    private static final int SLOT_ADD = 31;
    private static final int SLOT_ADVANCED = 8;

    public StepConditionsGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("Buoc 2 — Dieu kien"));
    }

    @Override
    protected void setup() {
        fillBorder(6);
        PassiveWizardLayout.renderSteps(this, PassiveEditorSession.WizardStep.CONDITIONS);
        var session = plugin.getPassiveBootstrap().manager().session(player);
        var entries = session.conditions().entries();

        setItem(4, GuiTheme.header(Material.OAK_SIGN,
                "Dieu kien",
                List.of(
                        "<gray>Click <white>+ <gray>de them",
                        "<gray>Click entry de sua, Shift+Xoa",
                        "<yellow>" + entries.size() + " dieu kien"
                )));

        for (int i = 0; i < ENTRY_SLOTS.length; i++) {
            if (i < entries.size()) {
                ConditionEntry e = entries.get(i);
                int index = i;
                setItem(ENTRY_SLOTS[i], GuiTheme.button(e.type().icon(),
                        "<white>" + e.type().displayName(),
                        List.of("<gray>" + e.summary(), "", "<yellow>Sua | Shift: Xoa")), ev -> {
                    if (ev.getClick() == ClickType.SHIFT_LEFT) {
                        session.conditions().remove(index);
                        refresh();
                        return;
                    }
                    new PassiveValueEditorGui(plugin, player, true, index).open();
                });
            }
        }

        setItem(SLOT_ADD, GuiTheme.glowing(Material.LIME_DYE, "+ Them dieu kien",
                List.of("<gray>Chon loai dieu kien")), ev -> {
            GuiFeedback.click(player);
            new PassiveTypePickerGui(plugin, player, true).open();
        });

        setItem(SLOT_ADVANCED, GuiTheme.button(Material.NETHER_STAR,
                "Nang cao",
                List.of("<gray>ALL / ANY / NOT", "<gray>Cooldown / Chance / Delay")), ev -> {
            GuiFeedback.click(player);
            new PassiveAdvancedGui(plugin, player).open();
        });

        setItem(45, createBackButton(), ev -> PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.TRIGGER));
        setItem(49, GuiTheme.actionPrimary(Material.ARROW, "Tiep tuc", "<gray>Hanh dong"), ev -> {
            GuiFeedback.click(player);
            PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.ACTIONS);
        });
    }
}