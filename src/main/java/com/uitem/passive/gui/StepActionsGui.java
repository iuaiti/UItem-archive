package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.ActionEntry;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class StepActionsGui extends AbstractGui {

    private static final int[] ENTRY_SLOTS = {20, 21, 22, 23, 24};
    private static final int SLOT_ADD = 31;

    public StepActionsGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("Buoc 3 — Hanh dong"));
    }

    @Override
    protected void setup() {
        fillBorder(6);
        PassiveWizardLayout.renderSteps(this, PassiveEditorSession.WizardStep.ACTIONS);
        var session = plugin.getPassiveBootstrap().manager().session(player);
        var actions = session.actions();

        setItem(4, GuiTheme.header(Material.CHEST,
                "Hanh dong",
                List.of(
                        "<gray>Chay theo thu tu tu tren xuong",
                        "<yellow>" + actions.size() + " hanh dong"
                )));

        for (int i = 0; i < ENTRY_SLOTS.length; i++) {
            if (i < actions.size()) {
                ActionEntry a = actions.get(i);
                int index = i;
                setItem(ENTRY_SLOTS[i], GuiTheme.button(a.type().icon(),
                        "<white>" + a.type().displayName(),
                        List.of("<gray>" + a.summary(), "", "<yellow>Sua | Shift: Xoa")), ev -> {
                    if (ev.getClick() == ClickType.SHIFT_LEFT) {
                        actions.remove(index);
                        refresh();
                        return;
                    }
                    new PassiveValueEditorGui(plugin, player, false, index).open();
                });
            }
        }

        setItem(SLOT_ADD, GuiTheme.glowing(Material.LIME_DYE, "+ Them hanh dong",
                List.of("<gray>Cho item, heal, lenh...")), ev -> {
            GuiFeedback.click(player);
            new PassiveTypePickerGui(plugin, player, false).open();
        });

        setItem(45, createBackButton(), ev -> PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.CONDITIONS));
        setItem(49, GuiTheme.actionPrimary(Material.ARROW, "Tiep tuc", "<gray>Xem truoc"), ev -> {
            GuiFeedback.click(player);
            PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.PREVIEW);
        });
    }
}