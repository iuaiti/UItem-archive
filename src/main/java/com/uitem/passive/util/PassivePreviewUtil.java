package com.uitem.passive.util;

import com.uitem.passive.ActionEntry;
import com.uitem.passive.ConditionEntry;
import com.uitem.passive.ConditionGroup;
import com.uitem.passive.PassiveAdvancedSettings;
import com.uitem.passive.PassiveDefinition;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.passive.LogicMode;
import com.uitem.passive.trigger.TriggerType;

import java.util.ArrayList;
import java.util.List;

public final class PassivePreviewUtil {

    private PassivePreviewUtil() {
    }

    public static List<String> loreFromSession(PassiveEditorSession session) {
        List<String> lines = new ArrayList<>();
        lines.add("<yellow>Khi: <white>" + session.trigger().displayName());
        ConditionGroup group = session.conditions().build();
        lines.add("<yellow>Neu (<white>" + modeLabel(group.mode()) + "<yellow>):");
        if (group.entries().isEmpty()) {
            lines.add("<gray>  (khong co dieu kien)");
        } else {
            int i = 1;
            for (ConditionEntry e : group.entries()) {
                lines.add("<gray>  " + i + ". <white>" + e.summary());
                i++;
            }
        }
        lines.add("<yellow>Thi:");
        if (session.actions().isEmpty()) {
            lines.add("<gray>  (khong co hanh dong)");
        } else {
            int i = 1;
            for (ActionEntry a : session.actions()) {
                lines.add("<gray>  " + i + ". <white>" + a.summary());
                i++;
            }
        }
        PassiveAdvancedSettings adv = session.advanced();
        if (adv.cooldownSeconds() > 0 || adv.delayTicks() > 0 || adv.globalChancePercent() < 100) {
            lines.add("");
            lines.add("<dark_gray>[Nang cao]");
            if (adv.globalChancePercent() < 100) {
                lines.add("<gray>  Ti le: <white>" + adv.globalChancePercent() + "%");
            }
            if (adv.cooldownSeconds() > 0) {
                lines.add("<gray>  Cho: <white>" + adv.cooldownSeconds() + "s");
            }
            if (adv.delayTicks() > 0) {
                lines.add("<gray>  Tre: <white>" + adv.delayTicks() + " tick");
            }
        }
        return lines;
    }

    public static List<String> loreFromDefinition(PassiveDefinition def) {
        List<String> lines = new ArrayList<>();
        lines.add("<yellow>Khi: <white>" + def.trigger().displayName());
        ConditionGroup group = def.conditions();
        lines.add("<yellow>Neu (<white>" + modeLabel(group.mode()) + "<yellow>):");
        if (group.entries().isEmpty()) {
            lines.add("<gray>  (khong co dieu kien)");
        } else {
            int i = 1;
            for (ConditionEntry e : group.entries()) {
                lines.add("<gray>  " + i + ". <white>" + e.summary());
                i++;
            }
        }
        lines.add("<yellow>Thi:");
        if (def.actions().isEmpty()) {
            lines.add("<gray>  (khong co hanh dong)");
        } else {
            int i = 1;
            for (ActionEntry a : def.actions()) {
                lines.add("<gray>  " + i + ". <white>" + a.summary());
                i++;
            }
        }
        return lines;
    }

    private static String modeLabel(LogicMode mode) {
        return switch (mode) {
            case ALL -> "tat ca";
            case ANY -> "mot trong";
            case NOT -> "khong";
        };
    }
}