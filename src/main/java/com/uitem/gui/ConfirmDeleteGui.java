package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * GUI xác nhận xóa item.
 */
public class ConfirmDeleteGui extends AbstractGui {

    private final UItemData data;
    private final int returnPage;

    public ConfirmDeleteGui(UItemPlugin plugin, Player player, UItemData data, int returnPage) {
        super(plugin, player, 27, "<red><bold>Confirm Delete Item");
        this.data = data;
        this.returnPage = returnPage;
    }

    @Override
    protected void setup() {
        // Fill tất cả với glass pane đen
        for (int i = 0; i < 27; i++) {
            setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        // Hiển thị item cần xóa ở giữa
        setItem(13, plugin.getItemManager().buildItemStack(data));

        // Confirm button (green)
        setItem(11, createItem(Material.LIME_WOOL,
                "<green><bold>CONFIRM DELETE",
                List.of(
                        "<gray>Delete item: <white>" + data.getId(),
                        "",
                        "<red>⚠ This action cannot be undone!"
                )), event -> {
            // Dùng API để fire UItemDeleteEvent — plugin khác có thể cancel
            boolean deleted = com.uitem.api.UItemAPI.get()
                    .removeItem(data.getId(), player);
            if (!deleted) {
                player.sendMessage(com.uitem.util.TextUtil.error(
                        "Cannot delete item '" + data.getId() + "' (blocked by another plugin)."));
                new ItemListGui(plugin, player, returnPage).open();
                return;
            }
            player.sendMessage(com.uitem.util.TextUtil.success(
                    "Deleted item '" + data.getId() + "'!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
            new ItemListGui(plugin, player, returnPage).open();
        });

        // Cancel button (red)
        setItem(15, createItem(Material.RED_WOOL,
                "<red><bold>CANCEL",
                List.of(
                        "<gray>Back to item list"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemListGui(plugin, player, returnPage).open();
        });
    }
}
