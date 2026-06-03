package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.passive.util.PassivePreviewUtil;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StepPreviewGui extends AbstractGui {

    public StepPreviewGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("Buoc 4 — Xem truoc"));
    }

    @Override
    protected void setup() {
        fillBorder(6);
        PassiveWizardLayout.renderSteps(this, PassiveEditorSession.WizardStep.PREVIEW);
        var session = plugin.getPassiveBootstrap().manager().session(player);
        List<String> lore = new ArrayList<>(PassivePreviewUtil.loreFromSession(session));

        setItem(13, GuiTheme.glowing(Material.WRITABLE_BOOK,
                "Tom tat Passive",
                lore));

        setItem(45, createBackButton(), ev -> PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.ACTIONS));
        setItem(49, GuiTheme.actionPrimary(Material.EMERALD, "Luu Passive", "<gray>Buoc cuoi"), ev -> {
            GuiFeedback.success(player);
            PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.SAVE);
        });
    }
}