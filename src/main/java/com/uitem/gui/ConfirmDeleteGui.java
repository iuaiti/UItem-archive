package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Confirm delete item GUI — fully bilingual.
 */
public class ConfirmDeleteGui extends AbstractGui {

    private final UItemData data;
    private final int returnPage;
    /** Where to go once the item is actually gone. May be null. */
    private final Runnable afterDelete;

    public ConfirmDeleteGui(UItemPlugin plugin, Player player, UItemData data, int returnPage) {
        this(plugin, player, data, returnPage, null);
    }

    /**
     * @param afterDelete where to go after a successful delete. Pass this when the
     *                    parent GUI edits the very item being deleted (the item
     *                    editor): returning to it would show an editor for an item
     *                    that no longer exists and silently re-save it.
     */
    public ConfirmDeleteGui(UItemPlugin plugin, Player player, UItemData data, int returnPage, Runnable afterDelete) {
        super(plugin, player, 27, Messages.confirmDeleteTitle());
        this.data = data;
        this.returnPage = returnPage;
        this.afterDelete = afterDelete;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        // Fill everything with black glass panes
        for (int i = 0; i < 27; i++) {
            setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        // Show the item to delete in the center
        setItem(13, plugin.getItemManager().buildItemStack(data));

        // Confirm button (green)
        setItem(11, createItem(Material.LIME_WOOL,
                Messages.confirmDeleteYes(),
                List.of(
                        Messages.confirmDeleteDesc() + "<white>" + data.getId(),
                        "",
                        Messages.confirmDeleteWarn()
                )), event -> {
            // Use API to fire UItemDeleteEvent - other plugins can cancel it
            boolean deleted = com.uitem.api.UItemAPI.get()
                    .removeItem(data.getId(), player);
            if (!deleted) {
                player.sendMessage(com.uitem.util.TextUtil.error(
                        Messages.cannotDeleteBlocked(data.getId())));
                goBackOrList();
                return;
            }
            player.sendMessage(com.uitem.util.TextUtil.success(
                    Messages.deletedItem(data.getId())));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
            if (afterDelete != null) {
                afterDelete.run();
            } else {
                goBackOrList();
            }
        });

        // Cancel button (red) - returns to the previous GUI
        setItem(15, createItem(Material.RED_WOOL,
                Messages.confirmDeleteNo(),
                List.of(
                        Messages.confirmDeleteBack()
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            goBack();
        });
    }

    private void goBackOrList() {
        if (hasBackAction()) {
            goBack();
        } else {
            new ItemListGui(plugin, player, returnPage).open();
        }
    }
}
