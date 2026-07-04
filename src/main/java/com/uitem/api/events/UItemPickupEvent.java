package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player picks up a UItem from the ground.
 *
 * <p>Cancel to prevent the player from picking up the item (the item stays on the ground).
 *
 * <pre>{@code
 * @EventHandler
 * public void onPickup(UItemPickupEvent event) {
 *     // Only the owner can pick up their dropped item
 *     if (!canPickup(event.getPlayer(), event.getItemData())) {
 *         event.setCancelled(true);
 *     }
 * }
 * }</pre>
 */
public class UItemPickupEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player player;

    @NotNull
    private final Item itemEntity;

    public UItemPickupEvent(@NotNull UItemData itemData, @NotNull Player player, @NotNull Item itemEntity) {
        super(itemData);
        this.player = player;
        this.itemEntity = itemEntity;
    }

    /** Player picking up the item. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** Item entity on the ground. */
    @NotNull
    public Item getItemEntity() {
        return itemEntity;
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
