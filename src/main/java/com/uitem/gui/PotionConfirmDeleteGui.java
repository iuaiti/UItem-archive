package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.potion.UPotionData;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PotionConfirmDeleteGui extends AbstractGui {

    private final UPotionData data;
    private final int returnPage;

    public PotionConfirmDeleteGui(UItemPlugin plugin, Player player, UPotionData data, int returnPage) {
        super(plugin, player, 27, GuiTheme.title(Messages.potionDeleteTitle()));
        this.data = data;
        this.returnPage = returnPage;
    }

    @Override
    protected void setup() {
        fillBorder(3);
        setItem(4, GuiTheme.header(Material.BARRIER, Messages.potionDeleteTitle(),
                List.of("<gray>ID: <white>" + data.getId())));

        setItem(11, GuiTheme.actionDanger(Material.RED_CONCRETE, Messages.potionDeleteConfirm(),
                "<red>Hanh dong khong the hoan tac"), ev -> {
            GuiFeedback.click(player);
            plugin.getPotionManager().remove(data.getId());
            player.sendMessage(TextUtil.success(Messages.potionDeleted(data.getId())));
            new PotionListGui(plugin, player, returnPage).open();
        });

        setItem(15, GuiTheme.actionSecondary(Material.LIME_CONCRETE, Messages.potionDeleteCancel(),
                "<gray>Quay lai danh sach"), ev -> {
            GuiFeedback.click(player);
            new PotionListGui(plugin, player, returnPage).open();
        });
    }
}