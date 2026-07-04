package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player equips or unequips a UItem from an equipment slot (armor + offhand).
 *
 * <p>Cancel to prevent the equipment change.
 *
 * <pre>{@code
 * @EventHandler
 * public void onEquip(UItemEquipEvent event) {
 *     if (!event.getItemData().getId().equals("cursed_armor")) return;
 *     if (!event.isEquipping()) return; // only when equipping
 *
 *     event.setCancelled(true);
 *     event.getPlayer().sendMessage("\u26a0 This armor is cursed and cannot be equipped!");
 * }
 * }</pre>
 */
public class UItemEquipEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player player;

    @NotNull
    private final EquipmentSlot slot;

    @Nullable
    private final ItemStack oldItem;

    @NotNull
    private final ItemStack newItem;

    /**
     * @param itemData  UItemData of the related item (being equipped or unequipped)
     * @param player    Player
     * @param slot      Equipment slot being changed
     * @param oldItem   Previous item in that slot (null or AIR if empty)
     * @param newItem   New item that will go into that slot (AIR if unequipping)
     */
    public UItemEquipEvent(
            @NotNull UItemData itemData,
            @NotNull Player player,
            @NotNull EquipmentSlot slot,
            @Nullable ItemStack oldItem,
            @NotNull ItemStack newItem) {
        super(itemData);
        this.player = player;
        this.slot = slot;
        this.oldItem = oldItem;
        this.newItem = newItem;
    }

    /** Player changing equipment. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** Slot being changed (HEAD, CHEST, LEGS, FEET, OFF_HAND). */
    @NotNull
    public EquipmentSlot getSlot() {
        return slot;
    }

    /** Previous item in the slot. Null or AIR if empty. */
    @Nullable
    public ItemStack getOldItem() {
        return oldItem;
    }

    /** New item that will go into the slot. AIR if unequipping. */
    @NotNull
    public ItemStack getNewItem() {
        return newItem;
    }

    /**
     * true if the player is EQUIPPING the item (newItem is a UItem),
     * false if the player is UNEQUIPPING the item (oldItem is a UItem).
     */
    public boolean isEquipping() {
        return newItem.getType() != org.bukkit.Material.AIR;
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
