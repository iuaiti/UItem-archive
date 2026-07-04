package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all UItem events.
 *
 * <p>External developers use it like this:
 * <pre>{@code
 * @EventHandler
 * public void onUItemGive(UItemGiveEvent event) {
 *     UItemData data = event.getItemData();
 *     // handle...
 * }
 * }</pre>
 */
public abstract class UItemEvent extends Event {

    private final UItemData itemData;

    protected UItemEvent(@NotNull UItemData itemData) {
        this.itemData = itemData;
    }

    protected UItemEvent(@NotNull UItemData itemData, boolean async) {
        super(async);
        this.itemData = itemData;
    }

    /**
     * Get the data of the custom item related to this event.
     */
    @NotNull
    public UItemData getItemData() {
        return itemData;
    }
}
