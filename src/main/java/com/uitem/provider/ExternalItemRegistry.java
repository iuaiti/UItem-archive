package com.uitem.provider;

import java.util.List;

/**
 * Cached view over an external item plugin's registry.
 * <p>
 * Implementations own the whole cache lifecycle (listening to the source
 * plugin's load events, rebuilding, atomic swap). Providers and GUIs only
 * read {@link #entries()} — they never scan disk or query the source plugin
 * to enumerate items.
 */
public interface ExternalItemRegistry {

    /** Immutable snapshot of all known entries, sorted by category/order. Empty until {@link #isReady()}. */
    List<ItemEntry> entries();

    /** False until the source plugin finished loading its items at least once. */
    boolean isReady();

    /** Force a cache rebuild (also triggered internally by the source plugin's reload events). */
    void rebuild();

    /** Detach event listeners. Call on plugin disable or before re-detecting providers. */
    void unregister();
}
