package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class ConfirmDeleteGui extends AbstractGui {

    private final UItemData data;
    private final int returnPage;

    public ConfirmDeleteGui(UItemPlugin plugin, Player player, UItemData data, int returnPage) {
        super(plugin, player, 27, GuiTheme.title("Confirm Delete"));
        this.data = data;
        this.returnPage = returnPage;
    }

    @Override
    protected void setup() {
        for (int i = 0; i < 27; i++) {
            setItem(i, GuiTheme.border());
        }
        setItem(0, GuiTheme.accent());
        setItem(8, GuiTheme.accent());
        setItem(18, GuiTheme.accent());
        setItem(26, GuiTheme.accent());

        setItem(4, GuiTheme.header(Material.BARRIER, "Delete?",
                List.of("<red>Ban sap xoa item nay",
                        "<white>" + data.getId(),
                        "<gray>Hanh dong khong the hoan tac")));

        setItem(13, plugin.getItemManager().buildItemStack(data));

        setItem(11, GuiTheme.actionDanger(Material.RED_CONCRETE,
                "CONFIRM",
                "<gray>Xoa vinh vien item nay"), event -> {
            boolean deleted = com.uitem.api.UItemAPI.get().removeItem(data.getId(), player);
            if (!deleted) {
                player.sendMessage(com.uitem.util.TextUtil.error(
                        "Cannot delete item '" + data.getId() + "' (blocked by another plugin)."));
                new ItemListGui(plugin, player, returnPage).open();
                return;
            }
            player.sendMessage(com.uitem.util.TextUtil.success("Deleted item '" + data.getId() + "'!"));
            GuiFeedback.deny(player);
            new ItemListGui(plugin, player, returnPage).open();
        });

        setItem(15, GuiTheme.actionSecondary(Material.LIME_CONCRETE,
                "CANCEL",
                "<gray>Quay lai danh sach"), event -> {
            GuiFeedback.click(player);
            new ItemListGui(plugin, player, returnPage).open();
        });
    }
}