package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.PassiveDefinition;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class StepSaveGui extends AbstractGui {

    public StepSaveGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("Buoc 5 — Luu"));
    }

    @Override
    protected void setup() {
        fillBorder(6);
        PassiveWizardLayout.renderSteps(this, PassiveEditorSession.WizardStep.SAVE);
        var session = plugin.getPassiveBootstrap().manager().session(player);
        var mgr = plugin.getPassiveBootstrap().manager();

        setItem(13, GuiTheme.header(Material.ANVIL,
                "Luu Passive",
                List.of(
                        session.isEditMode()
                                ? "<gray>Dang sua: <white>" + session.editingId()
                                : "<gray>Tao moi — chon ID ben duoi",
                        "<yellow>Trigger: <white>" + session.trigger().displayName()
                )));

        setItem(29, GuiTheme.actionPrimary(Material.NAME_TAG,
                "Dat ten hien thi",
                "<gray>MiniMessage duoc ho tro"), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Ten hien thi (display-name):",
                    input -> {
                        session.setDraftDisplayName(input);
                        player.sendMessage(TextUtil.success("Da dat ten."));
                        open();
                    });
        });

        setItem(31, GuiTheme.glowing(Material.EMERALD,
                session.isEditMode() ? "Cap nhat" : "Nhap ID & Luu",
                List.of("<gray>ID: a-z, 0-9, _", "<yellow>Click de nhap ID")), ev -> {
            if (session.isEditMode()) {
                save(session.editingId(), session);
                return;
            }
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhap ID passive:",
                    input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (id.isEmpty()) {
                            player.sendMessage(TextUtil.error("ID khong hop le!"));
                            open();
                            return;
                        }
                        if (mgr.has(id)) {
                            player.sendMessage(TextUtil.error("ID da ton tai!"));
                            open();
                            return;
                        }
                        save(id, session);
                    });
        });

        setItem(45, createBackButton(), ev -> PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.PREVIEW));
        setItem(40, createHomeButton(), ev -> new PassiveHubGui(plugin, player).open());
    }

    private void save(String id, PassiveEditorSession session) {
        PassiveDefinition def = session.toDefinition(id);
        plugin.getPassiveBootstrap().manager().register(def, true);
        plugin.getPassiveBootstrap().manager().clearSession(player);
        player.sendMessage(TextUtil.success("Da luu passive '" + id + "'!"));
        GuiFeedback.success(player);
        new PassiveListGui(plugin, player).open();
    }
}