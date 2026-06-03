package com.uitem.passive;

import com.uitem.passive.trigger.TriggerType;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * In-memory draft while creating/editing a passive in GUI.
 */
public final class PassiveEditorSession {

    public enum WizardStep {
        TRIGGER(1),
        CONDITIONS(2),
        ACTIONS(3),
        PREVIEW(4),
        SAVE(5);

        private final int number;

        WizardStep(int number) {
            this.number = number;
        }

        public int number() {
            return number;
        }
    }

    private final UUID playerId;
    private String editingId;
    private String draftDisplayName;
    private TriggerType trigger = TriggerType.BLOCK_BREAK;
    private final ConditionGroup.Builder conditions = new ConditionGroup.Builder();
    private final java.util.List<ActionEntry> actions = new java.util.ArrayList<>();
    private PassiveAdvancedSettings advanced = new PassiveAdvancedSettings();
    private boolean enabled = true;
    private WizardStep step = WizardStep.TRIGGER;

    public PassiveEditorSession(Player player) {
        this.playerId = player.getUniqueId();
    }

    public UUID playerId() {
        return playerId;
    }

    public String editingId() {
        return editingId;
    }

    public void setEditingId(String editingId) {
        this.editingId = editingId;
    }

    public boolean isEditMode() {
        return editingId != null && !editingId.isBlank();
    }

    public String draftDisplayName() {
        return draftDisplayName;
    }

    public void setDraftDisplayName(String draftDisplayName) {
        this.draftDisplayName = draftDisplayName;
    }

    public TriggerType trigger() {
        return trigger;
    }

    public void setTrigger(TriggerType trigger) {
        this.trigger = trigger;
    }

    public ConditionGroup.Builder conditions() {
        return conditions;
    }

    public java.util.List<ActionEntry> actions() {
        return actions;
    }

    public PassiveAdvancedSettings advanced() {
        return advanced;
    }

    public void setAdvanced(PassiveAdvancedSettings advanced) {
        this.advanced = advanced;
    }

    public WizardStep step() {
        return step;
    }

    public void setStep(WizardStep step) {
        this.step = step;
    }

    public void loadFrom(PassiveDefinition def) {
        editingId = def.id();
        draftDisplayName = def.displayName();
        trigger = def.trigger();
        conditions.clear().mode(def.conditions().mode());
        for (ConditionEntry e : def.conditions().entries()) {
            conditions.add(e.copy());
        }
        actions.clear();
        actions.addAll(def.actions().stream().map(ActionEntry::copy).toList());
        advanced = def.advanced().copy();
        enabled = def.enabled();
        step = WizardStep.TRIGGER;
    }

    public PassiveDefinition toDefinition(String id) {
        PassiveDefinition def = new PassiveDefinition(id);
        def.setDisplayName(draftDisplayName != null && !draftDisplayName.isBlank() ? draftDisplayName : id);
        def.setTrigger(trigger);
        def.setConditions(conditions.build());
        def.setActions(new java.util.ArrayList<>(actions));
        def.setAdvanced(advanced);
        def.setEnabled(enabled);
        return def;
    }

    public void resetForCreate() {
        editingId = null;
        draftDisplayName = null;
        trigger = TriggerType.BLOCK_BREAK;
        conditions.clear().mode(LogicMode.ALL);
        actions.clear();
        advanced = new PassiveAdvancedSettings();
        step = WizardStep.TRIGGER;
    }
}