package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.passive.PassiveEditorSession;
import org.bukkit.entity.Player;

/**
 * Opens the correct wizard step screen.
 */
public final class PassiveWizardGui {

    private PassiveWizardGui() {
    }

    public static void open(UItemPlugin plugin, Player player, PassiveEditorSession.WizardStep step) {
        plugin.getPassiveBootstrap().manager().session(player).setStep(step);
        switch (step) {
            case TRIGGER -> new StepTriggerGui(plugin, player).open();
            case CONDITIONS -> new StepConditionsGui(plugin, player).open();
            case ACTIONS -> new StepActionsGui(plugin, player).open();
            case PREVIEW -> new StepPreviewGui(plugin, player).open();
            case SAVE -> new StepSaveGui(plugin, player).open();
        }
    }
}