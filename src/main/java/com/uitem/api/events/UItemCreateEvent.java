package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a new custom item is created in UItem.
 *
 * <p>Cancel this event to prevent the item from being registered into the system.
 *
 * <pre>{@code
 * @EventHandler
 * public void onCreate(UItemCreateEvent event) {
 *     if (event.getItemData().getId().startsWith("test_")) {
 *         event.setCancelled(true); // block items starting with "test_"
 *     }
 * }
 * }</pre>
 */
public class UItemCreateEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @Nullable
    private final CommandSender creator;

    /**
     * @param itemData Data of the new item about to be created
     * @param creator  Who is creating the item (null if created via internal API)
     */
    public UItemCreateEvent(@NotNull UItemData itemData, @Nullable CommandSender creator) {
        super(itemData);
        this.creator = creator;
    }

    /**
     * Who triggered the creation of this item.
     * null if created by the plugin or via {@code UItemAPI} without a sender.
     */
    @Nullable
    public CommandSender getCreator() {
        return creator;
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
