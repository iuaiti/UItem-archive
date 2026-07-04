package com.uitem.provider;

import org.bukkit.inventory.ItemStack;

/**
 * Abstraction over a single source of {@link ItemStack}s.
 * <p>
 * A provider is identified by its <em>namespace</em> — the short prefix used in
 * config files, e.g. {@code vanilla:diamond_sword}, {@code ia:ruby_sword},
 * {@code oraxen:hammer}. The namespace MUST be stable across plugin restarts.
 * <p>
 * Providers are registered with {@link ProviderManager} at startup. Calling
 * code never depends on a concrete provider — only on this interface — so new
 * providers can be added without touching callers (Open/Closed principle).
 */
public interface ItemProvider {

    /** Short, stable namespace prefix. e.g. {@code "vanilla"}, {@code "ia"}, {@code "oraxen"}. */
    String getNamespace();

    /** Human-friendly display name, e.g. {@code "Vanilla"}, {@code "ItemsAdder"}. */
    String getDisplayName();

    /**
     * @return {@code true} if the backing plugin/runtime is currently available.
     *     Vanilla providers always return {@code true}. Optional hooks return
     *     {@code false} when the 3rd-party plugin is missing or disabled.
     */
    boolean isAvailable();

    /**
     * @param id item id <strong>without</strong> the namespace prefix
     *     (e.g. {@code "diamond_sword"} or {@code "ruby_sword"})
     * @return {@code true} if the provider can build an ItemStack for this id
     */
    boolean exists(String id);

    /**
     * Build the raw ItemStack for this id. The returned stack is a fresh clone
     * — callers may freely mutate it. Returns {@code null} if the id is
     * unknown or the backing plugin failed to produce the stack.
     * <p>
     * Implementations MUST NOT tag the stack with UItem's PDC key — that is the
     * responsibility of {@link com.uitem.UItemManager#buildItemStack}.
     */
    ItemStack getItem(String id);

    /**
     * @return {@code true} if this provider can enumerate items for the
     *     "browse" UI (search + pagination). Vanilla currently returns
     *     {@code false} because there is nothing useful to enumerate beyond
     *     raw {@code Material} names — vanilla picker uses Material list
     *     directly. Override to enable {@link BrowseableProvider}.
     */
    default boolean isBrowseable() {
        return this instanceof BrowseableProvider;
    }
}
