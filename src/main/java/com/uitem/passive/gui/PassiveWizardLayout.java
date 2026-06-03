package com.uitem.passive.gui;

import com.uitem.gui.AbstractGui;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;

import java.util.List;

final class PassiveWizardLayout {

    private PassiveWizardLayout() {
    }

    static void renderSteps(AbstractGui gui, PassiveEditorSession.WizardStep current) {
        PassiveEditorSession.WizardStep[] steps = PassiveEditorSession.WizardStep.values();
        int[] slots = {2, 3, 4, 5, 6};
        for (int i = 0; i < steps.length && i < slots.length; i++) {
            PassiveEditorSession.WizardStep s = steps[i];
            Material mat = s.ordinal() <= current.ordinal() ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            String color = s == current ? "<yellow>" : "<gray>";
            gui.setItemPublic(slots[i], GuiTheme.pane(mat, color + s.number() + ". " + stepName(s)));
        }
    }

    private static String stepName(PassiveEditorSession.WizardStep s) {
        return switch (s) {
            case TRIGGER -> "Khi nao";
            case CONDITIONS -> "Dieu kien";
            case ACTIONS -> "Hanh dong";
            case PREVIEW -> "Xem truoc";
            case SAVE -> "Luu";
        };
    }
}