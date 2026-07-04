package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a custom item is deleted from UItem.
 *
 * <p>Cancel this event to prevent the item from being deleted.
 *
 * <pre>{@code
 * @EventHandler
 * public void onDelete(UItemDeleteEvent event) {
 *     if (event.getItemData().getId().equals("legendary_sword")) {
 *         event.setCancelled(true); // protect the item from deletion
 *         if (event.getDeleter() != null) {
 *             event.getDeleter().sendMessage("This item is protected!");
 *         }
 *     }
 * }
 * }</pre>
 */
public class UItemDeleteEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @Nullable
    private final CommandSender deleter;

    /**
     * @param itemData Data of the item about to be deleted
     * @param deleter  Who is deleting the item (null if deleted via internal API)
     */
    public UItemDeleteEvent(@NotNull UItemData itemData, @Nullable CommandSender deleter) {
        super(itemData);
        this.deleter = deleter;
    }

    /**
     * Who triggered the deletion of this item.
     * null if deleted by the plugin or via {@code UItemAPI}.
     */
    @Nullable
    public CommandSender getDeleter() {
        return deleter;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
