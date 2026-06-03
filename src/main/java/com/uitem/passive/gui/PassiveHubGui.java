package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.gui.MainHubGui;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PassiveHubGui extends AbstractGui {

    public PassiveHubGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("Passive Studio"));
    }

    @Override
    protected void setup() {
        GuiTheme.fillBorder(this, 6);
        int count = plugin.getPassiveBootstrap().manager().all().size();

        setItem(4, GuiTheme.header(Material.BEACON, "Passive",
                List.of(
                        "<gray>Tao hanh dong tu dong bang GUI",
                        "<yellow>" + count + " <gray>passive dang co",
                        "",
                        "<gray>Khi nao? → Dieu kien? → Lam gi?"
                )));

        setItem(20, GuiTheme.actionPrimary(Material.LIME_DYE,
                "+ Tao Passive moi",
                "<gray>Wizard 5 buoc",
                "<yellow>Khong can sua config"), ev -> {
            GuiFeedback.success(player);
            var session = plugin.getPassiveBootstrap().manager().session(player);
            session.resetForCreate();
            PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.TRIGGER);
        });

        setItem(22, GuiTheme.actionSecondary(Material.BOOK,
                "Danh sach Passive",
                "<gray>Sua / xoa / bat tat"), ev -> {
            GuiFeedback.click(player);
            new PassiveListGui(plugin, player).open();
        });

        setItem(24, GuiTheme.button(Material.WRITABLE_BOOK,
                "Huong dan nhanh",
                List.of(
                        "<gray>1. Chon <white>khi nao <gray>kich hoat",
                        "<gray>2. Them <white>dieu kien <gray>(neu can)",
                        "<gray>3. Them <white>hanh dong",
                        "<gray>4. Xem truoc & luu"
                )));

        setItem(40, createHomeButton(), ev -> {
            GuiFeedback.click(player);
            new MainHubGui(plugin, player).open();
        });
    }
}