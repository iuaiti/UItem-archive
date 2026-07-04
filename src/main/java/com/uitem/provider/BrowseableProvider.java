package com.uitem.provider;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Restricted view of a provider used by the GUI to list browseable items.
 * Implementations MUST be safe to call on the main thread (i.e. list retrieval
 * must not block). Providers are encouraged to cache their id list.
 */
public interface BrowseableProvider extends ItemProvider {

    /**
     * @return all item ids this provider can produce, without the namespace
     *     prefix. Empty for providers that cannot enumerate (e.g. vanilla).
     */
    List<String> listIds();

    /**
     * Build a lightweight preview ItemStack for use in pickers. The returned
     * stack should reflect what the player will see (texture/model included).
     * It MUST NOT be tagged as a UItem.
     */
    ItemStack buildPreview(String id);
}
