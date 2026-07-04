package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a custom item is given to a player.
 *
 * <p>This event can modify the amount or ItemStack before it is given.
 * Cancel to prevent the item from being given.
 *
 * <pre>{@code
 * @EventHandler
 * public void onGive(UItemGiveEvent event) {
 *     // VIP receives double
 *     if (event.getRecipient().hasPermission("vip.double_items")) {
 *         event.setAmount(event.getAmount() * 2);
 *     }
 *     // Disallow giving in the nether world
 *     if (event.getRecipient().getWorld().getEnvironment() == World.Environment.NETHER) {
 *         event.setCancelled(true);
 *     }
 * }
 * }</pre>
 */
public class UItemGiveEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player recipient;

    @Nullable
    private final CommandSender giver;

    @NotNull
    private ItemStack itemStack;

    private int amount;

    /**
     * @param itemData  Data of the item being given
     * @param recipient Player receiving the item
     * @param giver     Who issued the give command (null if given via API)
     * @param itemStack ItemStack about to be given (clone)
     * @param amount    Amount
     */
    public UItemGiveEvent(
            @NotNull UItemData itemData,
            @NotNull Player recipient,
            @Nullable CommandSender giver,
            @NotNull ItemStack itemStack,
            int amount) {
        super(itemData);
        this.recipient = recipient;
        this.giver = giver;
        this.itemStack = itemStack;
        this.amount = Math.max(1, amount);
    }

    /** Player who will receive the item. */
    @NotNull
    public Player getRecipient() {
        return recipient;
    }

    /**
     * Who issued the give. null if given via API or automatically.
     */
    @Nullable
    public CommandSender getGiver() {
        return giver;
    }

    /** ItemStack about to be given. Can be replaced via {@link #setItemStack}. */
    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Override the ItemStack being given.
     * Useful when you want to add enchantments/lore depending on context.
     */
    public void setItemStack(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    /** Amount of the item that will be given. */
    public int getAmount() {
        return amount;
    }

    /**
     * Change the amount of the item that will be given.
     * The value is clamped to [1, 64].
     */
    public void setAmount(int amount) {
        this.amount = Math.max(1, Math.min(64, amount));
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
