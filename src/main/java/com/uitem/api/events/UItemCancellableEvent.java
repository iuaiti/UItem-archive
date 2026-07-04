package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for cancellable UItem events.
 * Cancelling the event prevents the corresponding action from happening.
 */
public abstract class UItemCancellableEvent extends UItemEvent implements Cancellable {

    private boolean cancelled;

    protected UItemCancellableEvent(@NotNull UItemData itemData) {
        super(itemData);
        this.cancelled = false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
