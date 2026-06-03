package com.uitem.listener;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.api.events.UItemEquipEvent;
import com.uitem.api.events.UItemInteractEvent;
import com.uitem.api.events.UItemPickupEvent;
import com.uitem.gui.AbstractGui;
import com.uitem.gui.CraftingGui;
import com.uitem.gui.RecipeEditorGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fire API events cho plugin khác (không chạy kỹ năng item — xem {@link com.uitem.ability.ItemAbilityListener}).
 *
 * <ul>
 *   <li>{@link UItemInteractEvent} — click voi UItem tren tay</li>
 *   <li>{@link UItemPickupEvent} — nhac UItem</li>
 *   <li>{@link UItemEquipEvent} — mac/thao UItem (giap + tay phu)</li>
 * </ul>
 */
public class UItemActionListener implements Listener {

    public UItemActionListener(@NotNull UItemPlugin plugin) {
        // plugin giu de mo rong (config toggle API, v.v.)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            return;
        }

        EquipmentSlot usedHand = event.getHand();
        if (usedHand == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack usedItem = usedHand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        UItemData data = getUItemData(usedItem);
        if (data == null) {
            return;
        }

        UItemInteractEvent uiEvent = new UItemInteractEvent(
                data,
                player,
                event.getAction(),
                usedItem.clone(),
                usedHand,
                event.getClickedBlock(),
                event.getBlockFace()
        );
        Bukkit.getPluginManager().callEvent(uiEvent);

        if (uiEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item itemEntity = event.getItem();
        UItemData data = getUItemData(itemEntity.getItemStack());
        if (data == null) {
            return;
        }

        UItemPickupEvent uiEvent = new UItemPickupEvent(data, player, itemEntity);
        Bukkit.getPluginManager().callEvent(uiEvent);

        if (uiEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(@NotNull PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Sau khi swap: main = getMainHandItem(), off = getOffHandItem()
        ItemStack afterMain = event.getMainHandItem();
        ItemStack afterOff = event.getOffHandItem();
        ItemStack beforeMain = afterOff;
        ItemStack beforeOff = afterMain;

        if (handleSlotChange(event, player, EquipmentSlot.HAND, beforeMain, afterMain)) {
            event.setCancelled(true);
        } else if (handleSlotChange(event, player, EquipmentSlot.OFF_HAND, beforeOff, afterOff)) {
            event.setCancelled(true);
        }
    }

    /**
     * @return true neu can cancel hanh dong (swap tay)
     */
    private boolean handleSlotChange(
            @NotNull Cancellable cancellable,
            @NotNull Player player,
            @NotNull EquipmentSlot slot,
            @Nullable ItemStack before,
            @Nullable ItemStack after) {

        UItemData beforeData = getUItemData(before);
        UItemData afterData = getUItemData(after);
        boolean cancel = false;

        if (beforeData != null && afterData == null) {
            cancel |= fireEquipCancellable(cancellable, player, beforeData, slot, before, air(), false);
        }
        if (afterData != null) {
            cancel |= fireEquipCancellable(cancellable, player, afterData, slot, before, after, true);
        }
        return cancel;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isUItemGuiOpen(event)) {
            return;
        }

        switch (event.getAction()) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> handlePlaceOnArmor(event, player);
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> handlePickupFromArmor(event, player);
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> handleHotbarSwapOnArmor(event, player);
            case MOVE_TO_OTHER_INVENTORY -> handleShiftClick(event, player);
            default -> { }
        }
    }

    private void handlePlaceOnArmor(@NotNull InventoryClickEvent event, @NotNull Player player) {
        if (event.getSlotType() != InventoryType.SlotType.ARMOR) {
            return;
        }
        EquipmentSlot slot = rawSlotToEquipmentSlot(event.getRawSlot());
        if (slot == null) {
            return;
        }

        ItemStack wearing = event.getCurrentItem();
        ItemStack equipping = event.getCursor();

        if (equipping != null && !equipping.getType().isAir()) {
            UItemData data = getUItemData(equipping);
            if (data != null) {
                dispatchEquip(event, player, data, slot, wearing, equipping, true);
            }
        }
        if (wearing != null && !wearing.getType().isAir()) {
            UItemData data = getUItemData(wearing);
            if (data != null) {
                dispatchEquip(event, player, data, slot, wearing, equipping, false);
            }
        }
    }

    private void handlePickupFromArmor(@NotNull InventoryClickEvent event, @NotNull Player player) {
        if (event.getSlotType() != InventoryType.SlotType.ARMOR) {
            return;
        }
        ItemStack wearing = event.getCurrentItem();
        if (wearing == null || wearing.getType().isAir()) {
            return;
        }

        UItemData data = getUItemData(wearing);
        if (data == null) {
            return;
        }

        EquipmentSlot slot = rawSlotToEquipmentSlot(event.getRawSlot());
        if (slot == null) {
            return;
        }

        dispatchEquip(event, player, data, slot, wearing, null, false);
    }

    private void handleHotbarSwapOnArmor(@NotNull InventoryClickEvent event, @NotNull Player player) {
        if (event.getSlotType() != InventoryType.SlotType.ARMOR) {
            return;
        }
        EquipmentSlot slot = rawSlotToEquipmentSlot(event.getRawSlot());
        if (slot == null) {
            return;
        }

        ItemStack wearing = event.getCurrentItem();
        int hotbar = event.getHotbarButton();
        if (hotbar < 0 || hotbar > 8) {
            return;
        }
        ItemStack hotbarItem = player.getInventory().getItem(hotbar);

        if (wearing != null && !wearing.getType().isAir()) {
            UItemData oldData = getUItemData(wearing);
            if (oldData != null) {
                dispatchEquip(event, player, oldData, slot, wearing, hotbarItem, false);
            }
        }
        if (hotbarItem != null && !hotbarItem.getType().isAir()) {
            UItemData newData = getUItemData(hotbarItem);
            if (newData != null) {
                dispatchEquip(event, player, newData, slot, wearing, hotbarItem, true);
            }
        }
    }

    private void handleShiftClick(@NotNull InventoryClickEvent event, @NotNull Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        UItemData data = getUItemData(clicked);
        if (data == null) {
            return;
        }

        EquipmentSlot slot = materialToArmorSlot(clicked);
        if (slot == null) {
            return;
        }

        boolean equipping = event.getSlotType() != InventoryType.SlotType.ARMOR;
        ItemStack current = player.getInventory().getItem(slot);

        dispatchEquip(event, player, data, slot,
                equipping ? current : clicked,
                equipping ? clicked : null,
                equipping);
    }

    private void dispatchEquip(
            @NotNull InventoryClickEvent clickEvent,
            @NotNull Player player,
            @NotNull UItemData data,
            @NotNull EquipmentSlot slot,
            @Nullable ItemStack oldItem,
            @Nullable ItemStack newItem,
            boolean equipping) {

        UItemEquipEvent uiEvent = new UItemEquipEvent(
                data,
                player,
                slot,
                cloneOrNull(oldItem),
                equipping ? cloneOrAir(newItem) : air()
        );
        Bukkit.getPluginManager().callEvent(uiEvent);
        if (uiEvent.isCancelled()) {
            clickEvent.setCancelled(true);
        }
    }

    private boolean fireEquipCancellable(
            @NotNull Cancellable source,
            @NotNull Player player,
            @NotNull UItemData data,
            @NotNull EquipmentSlot slot,
            @Nullable ItemStack oldItem,
            @Nullable ItemStack newItem,
            boolean equipping) {

        UItemEquipEvent uiEvent = new UItemEquipEvent(
                data,
                player,
                slot,
                cloneOrNull(oldItem),
                equipping ? cloneOrAir(newItem) : air()
        );
        Bukkit.getPluginManager().callEvent(uiEvent);
        if (uiEvent.isCancelled()) {
            source.setCancelled(true);
            return true;
        }
        return false;
    }

    private static boolean isUItemGuiOpen(@NotNull InventoryClickEvent event) {
        InventoryHolder top = event.getView().getTopInventory().getHolder();
        return top instanceof AbstractGui
                || top instanceof CraftingGui
                || top instanceof RecipeEditorGui;
    }

    @Nullable
    private static EquipmentSlot rawSlotToEquipmentSlot(int rawSlot) {
        return switch (rawSlot) {
            case 5 -> EquipmentSlot.HEAD;
            case 6 -> EquipmentSlot.CHEST;
            case 7 -> EquipmentSlot.LEGS;
            case 8 -> EquipmentSlot.FEET;
            case 40 -> EquipmentSlot.OFF_HAND;
            default -> null;
        };
    }

    @Nullable
    private static EquipmentSlot materialToArmorSlot(@NotNull ItemStack item) {
        String name = item.getType().name();
        if (name.endsWith("_HELMET") || name.endsWith("_SKULL")
                || name.equals("CARVED_PUMPKIN") || name.equals("PLAYER_HEAD")) {
            return EquipmentSlot.HEAD;
        }
        if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) {
            return EquipmentSlot.CHEST;
        }
        if (name.endsWith("_LEGGINGS")) {
            return EquipmentSlot.LEGS;
        }
        if (name.endsWith("_BOOTS")) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    @Nullable
    private static UItemData getUItemData(@Nullable ItemStack item) {
        if (!UItemAPI.isAvailable()) {
            return null;
        }
        return UItemAPI.get().getUItemData(item).orElse(null);
    }

    @Nullable
    private static ItemStack cloneOrNull(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        return stack.clone();
    }

    @NotNull
    private static ItemStack cloneOrAir(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return air();
        }
        return stack.clone();
    }

    @NotNull
    private static ItemStack air() {
        return new ItemStack(Material.AIR);
    }
}