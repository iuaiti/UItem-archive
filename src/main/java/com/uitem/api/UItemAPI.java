package com.uitem.api;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.events.UItemCreateEvent;
import com.uitem.api.events.UItemDeleteEvent;
import com.uitem.api.events.UItemGiveEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Public API of UItem — the single entry point for external developers.
 *
 * <h2>How to use in your plugin:</h2>
 *
 * <pre>{@code
 * // 1. Check whether UItem is available (in onEnable)
 * if (!UItemAPI.isAvailable()) {
 *     getLogger().warning("UItem is not installed — disabling related feature.");
 *     return;
 * }
 * UItemAPI api = UItemAPI.get();
 *
 * // 2. Get item by ID
 * Optional<ItemStack> item = api.buildItem("ruby_sword");
 * item.ifPresent(i -> player.getInventory().addItem(i));
 *
 * // 3. Check whether an ItemStack is a UItem
 * if (api.isUItem(itemInHand)) {
 *     String id = api.getUItemId(itemInHand).orElse("unknown");
 * }
 *
 * // 4. Register a new item from code
 * UItemData data = new UItemData("my_item");
 * data.setMaterial(Material.DIAMOND);
 * data.setDisplayName("<aqua>My Item");
 * api.registerItem(data, null); // null = no sender
 * }</pre>
 *
 * <h2>Events you can listen to:</h2>
 * <ul>
 *   <li>{@code UItemCreateEvent} — a new item is created</li>
 *   <li>{@code UItemDeleteEvent} — an item is deleted</li>
 *   <li>{@code UItemGiveEvent}   — an item is given to a player</li>
 *   <li>{@code UItemInteractEvent} — a player clicks with a UItem in hand</li>
 *   <li>{@code UItemPickupEvent}  — a player picks up a UItem</li>
 *   <li>{@code UItemEquipEvent}   — a player equips/unequips a UItem</li>
 * </ul>
 */
public final class UItemAPI {

    private static UItemAPI instance;

    private final UItemPlugin plugin;

    private UItemAPI(@NotNull UItemPlugin plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // Internal lifecycle — only called by UItemPlugin
    // =========================================================

    /**
     * Initialize the API instance. Only called from {@code UItemPlugin.onEnable()}.
     */
    public static void init(@NotNull UItemPlugin plugin) {
        instance = new UItemAPI(plugin);
    }

    /**
     * Destroy the API instance. Only called from {@code UItemPlugin.onDisable()}.
     */
    public static void shutdown() {
        instance = null;
    }

    // =========================================================
    // Public access
    // =========================================================

    /**
     * Get the UItemAPI instance.
     *
     * @throws IllegalStateException if UItem is not enabled or has been disabled
     */
    @NotNull
    public static UItemAPI get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "UItem is not loaded. Check with UItemAPI.isAvailable() first.");
        }
        return instance;
    }

    /**
     * Check whether UItem is running and the API is available.
     * Always call this before {@link #get()} in your plugin.
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    // =========================================================
    // Item lookup
    // =========================================================

    /**
     * Get the {@link UItemData} by ID.
     *
     * @param id the item ID (case-insensitive)
     * @return Optional containing the data, empty if not found
     */
    @NotNull
    public Optional<UItemData> getItem(@NotNull String id) {
        return Optional.ofNullable(plugin.getItemManager().getItem(id));
    }

    /**
     * Get all registered custom items.
     *
     * @return Unmodifiable map of id → data
     */
    @NotNull
    public Map<String, UItemData> getAllItems() {
        return plugin.getItemManager().getAllItems();
    }

    /**
     * Check whether an item ID exists.
     */
    public boolean hasItem(@NotNull String id) {
        return plugin.getItemManager().hasItem(id);
    }

    // =========================================================
    // ItemStack building
    // =========================================================

    /**
     * Build an {@link ItemStack} from an ID.
     *
     * @param id the item ID
     * @return Optional containing the ItemStack, empty if the ID does not exist
     */
    @NotNull
    public Optional<ItemStack> buildItem(@NotNull String id) {
        UItemData data = plugin.getItemManager().getItem(id);
        if (data == null) return Optional.empty();
        return Optional.of(plugin.getItemManager().buildItemStack(data));
    }

    /**
     * Build an {@link ItemStack} from a {@link UItemData}.
     */
    @NotNull
    public ItemStack buildItem(@NotNull UItemData data) {
        return plugin.getItemManager().buildItemStack(data);
    }

    // =========================================================
    // ItemStack identification
    // =========================================================

    /**
     * Check whether an ItemStack is a UItem (has a PersistentDataContainer tag).
     *
     * @param item the ItemStack to check (null-safe)
     * @return true if it is a UItem
     */
    public boolean isUItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer()
                .has(plugin.getItemManager().getUItemKey(), PersistentDataType.STRING);
    }

    /**
     * Get the ID of the UItem from an ItemStack.
     *
     * @param item the ItemStack to check
     * @return Optional containing the ID, empty if it is not a UItem
     */
    @NotNull
    public Optional<String> getUItemId(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        var meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer()
                .get(plugin.getItemManager().getUItemKey(), PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    /**
     * Get the {@link UItemData} from an ItemStack.
     * Combines {@link #getUItemId} and {@link #getItem}.
     */
    @NotNull
    public Optional<UItemData> getUItemData(@Nullable ItemStack item) {
        return getUItemId(item).flatMap(this::getItem);
    }

    // =========================================================
    // Management (fires events)
    // =========================================================

    /**
     * Register (create or update) a custom item into UItem.
     *
     * <p>If an item with this ID does not yet exist → fire {@link UItemCreateEvent}.
     * If it already exists → update directly, no event fired (this is an update, not a create).
     *
     * @param data    the item data
     * @param creator the sender performing the action (null if from code)
     * @return true if the item was registered successfully (not cancelled)
     */
    public boolean registerItem(@NotNull UItemData data, @Nullable CommandSender creator) {
        boolean isNew = !plugin.getItemManager().hasItem(data.getId());

        if (isNew) {
            UItemCreateEvent event = new UItemCreateEvent(data, creator);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
        }

        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveAll(plugin.getItemManager());
        return true;
    }

    /**
     * Remove a custom item from UItem.
     *
     * <p>Fires {@link UItemDeleteEvent} — cancel to prevent deletion.
     *
     * @param id      the ID of the item to delete
     * @param deleter the sender performing the action (null if from code)
     * @return true if the item existed and was deleted successfully
     */
    public boolean removeItem(@NotNull String id, @Nullable CommandSender deleter) {
        UItemData data = plugin.getItemManager().getItem(id);
        if (data == null) return false;

        UItemDeleteEvent event = new UItemDeleteEvent(data, deleter);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        plugin.getItemManager().removeItem(id);
        // Also delete the corresponding split file(s) so the item does not reappear after reload
        // when using items/*.yml split config (addresses "edits + delete lost on reload")
        plugin.getStorageManager().deleteItemFile(id);
        plugin.getStorageManager().saveAll(plugin.getItemManager());
        return true;
    }

    // =========================================================
    // Give (fires event)
    // =========================================================

    /**
     * Give an item to a player. Fires {@link UItemGiveEvent} before giving.
     *
     * <p>Leftover items (full inventory) will be dropped at the player's location.
     *
     * @param player the player receiving the item
     * @param id     the item ID
     * @param amount the amount (1-64)
     * @param giver  the sender performing the give (null if from code)
     * @return true if the give succeeded (not cancelled and the item exists)
     */
    public boolean giveItem(
            @NotNull Player player,
            @NotNull String id,
            int amount,
            @Nullable CommandSender giver) {

        UItemData data = plugin.getItemManager().getItem(id);
        if (data == null) return false;

        int clampedAmount = Math.max(1, Math.min(64, amount));
        ItemStack itemStack = plugin.getItemManager().buildItemStack(data);
        itemStack.setAmount(clampedAmount);

        UItemGiveEvent event = new UItemGiveEvent(data, player, giver, itemStack.clone(), clampedAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        // Use the item and amount from the event (may have been modified by a listener)
        ItemStack finalItem = event.getItemStack();
        finalItem.setAmount(event.getAmount());

        var leftovers = player.getInventory().addItem(finalItem);
        leftovers.values().forEach(left ->
                player.getWorld().dropItemNaturally(player.getLocation(), left));

        return true;
    }

    /**
     * Convenience overload that does not require a giver.
     *
     * @see #giveItem(Player, String, int, CommandSender)
     */
    public boolean giveItem(@NotNull Player player, @NotNull String id, int amount) {
        return giveItem(player, id, amount, null);
    }

    // =========================================================
    // Provider introspection
    // =========================================================

    /**
     * Get the "namespace:id" (e.g. {@code "ia:ruby_sword"}) describing the source of
     * an ItemStack that an admin is currently holding. Returns {@code null} if no
     * provider claims it.
     */
    @Nullable
    public String detectSource(@Nullable ItemStack item) {
        return plugin.getProviderManager().detectSource(item);
    }

    /**
     * Get the registered source for a UItem (config string in the form "namespace:id").
     */
    @NotNull
    public Optional<String> getSourceForItem(@NotNull String uitemId) {
        return getItem(uitemId).map(UItemData::getSource);
    }

    // =========================================================
    // Utility
    // =========================================================

    /**
     * Get the NamespacedKey UItem uses to tag items in the PersistentDataContainer.
     * Useful if you want to read/write the PDC yourself.
     */
    @NotNull
    public NamespacedKey getUItemKey() {
        return plugin.getItemManager().getUItemKey();
    }

    /**
     * Get the plugin instance. Use to access internal managers if needed.
     */
    @NotNull
    public UItemPlugin getPlugin() {
        return plugin;
    }
}
