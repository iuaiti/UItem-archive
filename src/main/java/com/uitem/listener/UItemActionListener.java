package com.uitem.listener;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.api.events.UItemEquipEvent;
import com.uitem.api.events.UItemInteractEvent;
import com.uitem.api.events.UItemPickupEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listener bắt các action liên quan đến UItem và fire custom events.
 *
 * <p>Các event được fire:
 * <ul>
 *   <li>{@link UItemInteractEvent} — khi player click với UItem trên tay</li>
 *   <li>{@link UItemPickupEvent}   — khi player nhặt UItem</li>
 *   <li>{@link UItemEquipEvent}    — khi player mặc/tháo UItem (thay đổi slot armor/offhand)</li>
 * </ul>
 */
public class UItemActionListener implements Listener {

    private final UItemPlugin plugin;

    public UItemActionListener(@NotNull UItemPlugin plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    // Interact: RIGHT/LEFT click với UItem
    // ================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        // Bỏ qua off-hand để không fire event 2 lần (Bukkit fires cho cả 2 tay)
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();

        // Kiểm tra tay chính trước, sau đó off-hand
        ItemStack mainItem = player.getInventory().getItemInMainHand();
        ItemStack offItem  = player.getInventory().getItemInOffHand();

        // Thử main hand
        UItemData data = getUItemData(mainItem);
        EquipmentSlot usedHand = EquipmentSlot.HAND;

        // Nếu main hand không phải UItem, thử off hand
        if (data == null && event.getAction() != Action.PHYSICAL) {
            data = getUItemData(offItem);
            if (data != null) {
                usedHand = EquipmentSlot.OFF_HAND;
            }
        }

        if (data == null) return;

        Action action = event.getAction();
        if (action == Action.PHYSICAL) return; // pressure plate / trampling — bỏ qua

        ItemStack usedItem = usedHand == EquipmentSlot.HAND ? mainItem : offItem;

        UItemInteractEvent uiEvent = new UItemInteractEvent(
                data,
                player,
                action,
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

    // ================================================================
    // Pickup: nhặt UItem từ dưới đất
    // ================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item itemEntity = event.getItem();
        UItemData data = getUItemData(itemEntity.getItemStack());
        if (data == null) return;

        UItemPickupEvent uiEvent = new UItemPickupEvent(data, player, itemEntity);
        Bukkit.getPluginManager().callEvent(uiEvent);

        if (uiEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }

    // ================================================================
    // Equip: mặc/tháo UItem qua InventoryClickEvent (tương thích mọi Paper/Spigot)
    // Bắt: click trực tiếp vào armor slot, shift-click auto-equip,
    //       và numberkey swap vào armor slot.
    // ================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        switch (event.getAction()) {
            // Click trực tiếp / swap vào slot armor
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> {
                if (event.getSlotType() != InventoryType.SlotType.ARMOR) return;
                ItemStack wearing = event.getCurrentItem();  // đang mặc (có thể null)
                ItemStack equipping = event.getCursor();     // sắp mặc vào

                EquipmentSlot slot = rawSlotToEquipmentSlot(event.getRawSlot());
                if (slot == null) return;

                // Item vừa mặc vào
                if (equipping != null && !equipping.getType().isAir()) {
                    UItemData data = getUItemData(equipping);
                    if (data != null) fireEquipEvent(event, player, data, slot, wearing, equipping, true);
                }
                // Item vừa tháo ra
                if (wearing != null && !wearing.getType().isAir()) {
                    UItemData data = getUItemData(wearing);
                    if (data != null) fireEquipEvent(event, player, data, slot, wearing, equipping, false);
                }
            }

            // Lấy item ra khỏi slot armor
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> {
                if (event.getSlotType() != InventoryType.SlotType.ARMOR) return;
                ItemStack wearing = event.getCurrentItem();
                if (wearing == null || wearing.getType().isAir()) return;

                UItemData data = getUItemData(wearing);
                if (data == null) return;

                EquipmentSlot slot = rawSlotToEquipmentSlot(event.getRawSlot());
                if (slot == null) return;

                fireEquipEvent(event, player, data, slot, wearing, null, false);
            }

            // Shift-click tự động equip/unequip armor
            case MOVE_TO_OTHER_INVENTORY -> {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) return;

                UItemData data = getUItemData(clicked);
                if (data == null) return;

                // Xác định slot từ material type (shift-click không cho biết slot đích trực tiếp)
                EquipmentSlot slot = materialToArmorSlot(clicked);
                if (slot == null) return;

                // Kiểm tra đang equip (từ inventory lên) hay unequip (từ armor slot xuống)
                boolean equipping = event.getSlotType() != InventoryType.SlotType.ARMOR;
                ItemStack current = player.getInventory().getItem(slot);

                fireEquipEvent(event, player, data, slot, equipping ? current : clicked,
                        equipping ? clicked : null, equipping);
            }

            default -> { /* bỏ qua các action khác */ }
        }
    }

    private void fireEquipEvent(
            @NotNull InventoryClickEvent clickEvent,
            @NotNull Player player,
            @NotNull UItemData data,
            @NotNull EquipmentSlot slot,
            @Nullable ItemStack oldItem,
            @Nullable ItemStack newItem,
            boolean equipping) {

        // isEquipping() trong UItemEquipEvent được suy ra từ newItem có phải AIR không,
        // nên truyền AIR khi tháo ra để event tự detect đúng.
        ItemStack safeNew = (newItem == null || newItem.getType().isAir())
                ? new ItemStack(org.bukkit.Material.AIR)
                : newItem.clone();

        UItemEquipEvent uiEvent = new UItemEquipEvent(
                data, player, slot,
                (oldItem == null || oldItem.getType().isAir()) ? null : oldItem.clone(),
                safeNew
        );
        Bukkit.getPluginManager().callEvent(uiEvent);
        if (uiEvent.isCancelled()) {
            clickEvent.setCancelled(true);
        }
    }

    /**
     * Map raw slot index trong PlayerInventory sang EquipmentSlot armor.
     * Slot 5=helmet, 6=chestplate, 7=leggings, 8=boots trong inventory view.
     */
    @Nullable
    private EquipmentSlot rawSlotToEquipmentSlot(int rawSlot) {
        return switch (rawSlot) {
            case 5 -> EquipmentSlot.HEAD;
            case 6 -> EquipmentSlot.CHEST;
            case 7 -> EquipmentSlot.LEGS;
            case 8 -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    /**
     * Đoán armor slot từ material khi shift-click (không có thông tin slot đích).
     */
    @Nullable
    private EquipmentSlot materialToArmorSlot(@NotNull ItemStack item) {
        String name = item.getType().name();
        if (name.endsWith("_HELMET") || name.endsWith("_SKULL")
                || name.equals("CARVED_PUMPKIN") || name.equals("PLAYER_HEAD")) {
            return EquipmentSlot.HEAD;
        }
        if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS"))    return EquipmentSlot.FEET;
        return null;
    }

    /**
     * Lấy UItemData từ ItemStack, trả về null nếu không phải UItem.
     */
    @Nullable
    private UItemData getUItemData(@Nullable ItemStack item) {
        if (!UItemAPI.isAvailable()) return null;
        return UItemAPI.get().getUItemData(item).orElse(null);
    }
}
