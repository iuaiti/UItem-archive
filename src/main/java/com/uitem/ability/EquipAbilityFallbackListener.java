package com.uitem.ability;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.gui.CraftingGui;
import com.uitem.gui.RecipeEditorGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
/**
 * Fallback ON_EQUIP detection when Paper's {@code EntityEquipmentChangedEvent} is unavailable.
 */
public final class EquipAbilityFallbackListener implements Listener {

    private final UItemPlugin plugin;

    public EquipAbilityFallbackListener(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();
        if (main != null && !main.getType().isAir()) {
            AbilityEquipTriggers.fire(plugin, player, main, EquipmentSlot.HAND);
        }
        if (off != null && !off.getType().isAir()) {
            AbilityEquipTriggers.fire(plugin, player, off, EquipmentSlot.OFF_HAND);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isUItemGuiOpen(event)) {
            return;
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack main = player.getInventory().getItemInMainHand();
            ItemStack off = player.getInventory().getItemInOffHand();
            if (main != null && !main.getType().isAir()) {
                AbilityEquipTriggers.fire(plugin, player, main, EquipmentSlot.HAND);
            }
            if (off != null && !off.getType().isAir()) {
                AbilityEquipTriggers.fire(plugin, player, off, EquipmentSlot.OFF_HAND);
            }
            return;
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            EquipmentSlot slot = rawSlotToEquipmentSlot(event.getRawSlot());
            if (slot == null) {
                return;
            }
            int hotbar = event.getHotbarButton();
            if (hotbar >= 0 && hotbar <= 8) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbar);
                if (hotbarItem != null && !hotbarItem.getType().isAir()) {
                    AbilityEquipTriggers.fire(plugin, player, hotbarItem, slot);
                }
            }
            return;
        }

        if (event.isShiftClick()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
                return;
            }
            EquipmentSlot slot = materialToArmorSlot(clicked);
            if (slot == null) {
                return;
            }
            if (event.getSlotType() != InventoryType.SlotType.ARMOR) {
                AbilityEquipTriggers.fire(plugin, player, clicked, slot);
            }
            return;
        }

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            EquipmentSlot slot = rawSlotToEquipmentSlot(event.getRawSlot());
            ItemStack cursor = event.getCursor();
            if (slot != null && cursor != null && !cursor.getType().isAir()) {
                AbilityEquipTriggers.fire(plugin, player, cursor, slot);
            }
        }
    }

    private static boolean isUItemGuiOpen(InventoryClickEvent event) {
        var top = event.getView().getTopInventory().getHolder();
        return top instanceof AbstractGui
                || top instanceof CraftingGui
                || top instanceof RecipeEditorGui;
    }

    private static EquipmentSlot rawSlotToEquipmentSlot(int rawSlot) {
        return switch (rawSlot) {
            case 5 -> EquipmentSlot.HEAD;
            case 6 -> EquipmentSlot.CHEST;
            case 7 -> EquipmentSlot.LEGS;
            case 8 -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static EquipmentSlot materialToArmorSlot(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        String name = stack.getType().name();
        if (name.endsWith("_HELMET") || name.equals("TURTLE_HELMET") || name.equals("CARVED_PUMPKIN")
                || name.equals("PLAYER_HEAD") || name.equals("DRAGON_HEAD") || name.endsWith("_SKULL")) {
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
}