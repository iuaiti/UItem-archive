package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.passive.trigger.TriggerType;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class StepTriggerGui extends AbstractGui {

    private static final int[] SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33
    };

    public StepTriggerGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("Buoc 1 — Khi nao?"));
    }

    @Override
    protected void setup() {
        fillBorder(6);
        PassiveWizardLayout.renderSteps(this, PassiveEditorSession.WizardStep.TRIGGER);
        var session = plugin.getPassiveBootstrap().manager().session(player);

        setItem(4, GuiTheme.header(Material.COMPASS,
                "Khi nao kich hoat?",
                List.of(
                        "<gray>Chon mot su kien",
                        "<yellow>Dang chon: <white>" + session.trigger().displayName()
                )));

        List<TriggerType> types = TriggerType.all();
        for (int i = 0; i < SLOTS.length && i < types.size(); i++) {
            TriggerType t = types.get(i);
            setItem(SLOTS[i], GuiTheme.button(t.icon(),
                    "<white>" + t.displayName(),
                    List.of("<gray>" + t.description(), "", "<yellow>Click de chon")), ev -> {
                GuiFeedback.success(player);
                session.setTrigger(t);
                PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.CONDITIONS);
            });
        }

        setItem(45, createBackButton(), ev -> new PassiveHubGui(plugin, player).open());
        setItem(49, GuiTheme.actionPrimary(Material.ARROW,
                "Tiep tuc",
                "<gray>Bo qua neu da chon"), ev -> {
            GuiFeedback.click(player);
            PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.CONDITIONS);
        });
    }
}