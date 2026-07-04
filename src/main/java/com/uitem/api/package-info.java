/**
 * UItem Public API — for developers wanting to integrate with UItem.
 *
 * <h2>Quick Start</h2>
 *
 * <p>Add UItem to {@code depend} or {@code softdepend} in your {@code plugin.yml}:
 * <pre>
 * softdepend:
 *   - UItem
 * </pre>
 *
 * <p>In {@code onEnable()}:
 * <pre>{@code
 * if (!UItemAPI.isAvailable()) {
 *     getLogger().warning("UItem not found, disabling related feature.");
 *     return;
 * }
 * UItemAPI api = UItemAPI.get();
 * }</pre>
 *
 * <h2>Main classes</h2>
 * <ul>
 *   <li>{@link com.uitem.api.UItemAPI}           — the single entry point</li>
 *   <li>{@link com.uitem.UItemData}               — data of a custom item</li>
 * </ul>
 *
 * <h2>Events (in package {@code com.uitem.api.events})</h2>
 * <ul>
 *   <li>{@link com.uitem.api.events.UItemCreateEvent}   — new item created (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemDeleteEvent}   — item deleted (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemGiveEvent}     — item given to a player (cancellable, modifiable)</li>
 *   <li>{@link com.uitem.api.events.UItemInteractEvent} — player clicks with a UItem (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemPickupEvent}   — player picks up a UItem (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemEquipEvent}    — player equips/unequips a UItem (cancellable)</li>
 * </ul>
 */
package com.uitem.api;
