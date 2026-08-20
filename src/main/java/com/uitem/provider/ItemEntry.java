package com.uitem.provider;

/**
 * A browseable entry from an external item plugin (ItemsAdder, Oraxen, ...).
 * <p>
 * Existence is decided by the plugin's registry API; this record only carries
 * the extra GUI metadata read from config files. Broken/missing metadata must
 * never remove an entry — it just falls back to defaults.
 *
 * @param namespacedId full id as the source plugin knows it (e.g. {@code myitems:ruby_sword})
 * @param category     GUI category name, or {@code "Uncategorized"} when unknown
 * @param hidden       true for menu-icon/internal items that should not show in browse GUIs
 * @param order        display order inside the category (lower = first)
 * @param permission   permission required to see/use the item, or null
 */
public record ItemEntry(String namespacedId, String category, boolean hidden, int order, String permission) {

    public static final String DEFAULT_CATEGORY = "Uncategorized";

    public static ItemEntry bare(String namespacedId) {
        return new ItemEntry(namespacedId, DEFAULT_CATEGORY, false, Integer.MAX_VALUE, null);
    }
}
