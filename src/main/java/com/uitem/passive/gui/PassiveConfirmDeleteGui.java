package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.PassiveDefinition;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PassiveConfirmDeleteGui extends AbstractGui {

    private final PassiveDefinition def;
    private final int returnPage;

    public PassiveConfirmDeleteGui(UItemPlugin plugin, Player player, PassiveDefinition def, int returnPage) {
        super(plugin, player, 27, GuiTheme.title("Xoa Passive?"));
        this.def = def;
        this.returnPage = returnPage;
    }

    @Override
    protected void setup() {
        for (int i = 0; i < 27; i++) {
            setItem(i, GuiTheme.border());
        }
        setItem(4, GuiTheme.header(Material.BARRIER, "Xoa passive?",
                List.of("<red>" + def.id(), "<gray>Khong the hoan tac")));
        setItem(11, GuiTheme.actionDanger(Material.RED_CONCRETE, "XAC NHAN", "<gray>Xoa vinh vien"), ev -> {
            plugin.getPassiveBootstrap().manager().unregister(def.id(), true);
            player.sendMessage(TextUtil.success("Da xoa passive " + def.id()));
            GuiFeedback.deny(player);
            new PassiveListGui(plugin, player, returnPage).open();
        });
        setItem(15, GuiTheme.actionSecondary(Material.LIME_CONCRETE, "HUY", "<gray>Quay lai"), ev -> {
            GuiFeedback.click(player);
            new PassiveListGui(plugin, player, returnPage).open();
        });
    }
}