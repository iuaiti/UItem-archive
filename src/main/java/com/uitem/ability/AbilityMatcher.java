package com.uitem.ability;

import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.potion.UPotionData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AbilityMatcher {

    private AbilityMatcher() {
    }

    public static Optional<HeldSource> findHeld(UItemPlugin plugin, Player player, ItemSlotRequirement requirement) {
        if (player == null || requirement == null || plugin == null) {
            return Optional.empty();
        }
        PlayerInventory inv = player.getInventory();
        return switch (requirement) {
            case MAIN_HAND -> fromStack(plugin, player, inv.getItemInMainHand(), EquipmentSlot.HAND);
            case OFF_HAND -> fromStack(plugin, player, inv.getItemInOffHand(), EquipmentSlot.OFF_HAND);
            case EITHER_HAND -> {
                Optional<HeldSource> main = fromStack(plugin, player, inv.getItemInMainHand(), EquipmentSlot.HAND);
                if (main.isPresent()) {
                    yield main;
                }
                yield fromStack(plugin, player, inv.getItemInOffHand(), EquipmentSlot.OFF_HAND);
            }
            case ARMOR -> {
                ItemStack[] pieces = inv.getArmorContents();
                EquipmentSlot[] slots = {
                        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
                };
                for (int i = 0; i < pieces.length && i < slots.length; i++) {
                    Optional<HeldSource> found = fromStack(plugin, player, pieces[i], slots[i]);
                    if (found.isPresent()) {
                        yield found;
                    }
                }
                yield Optional.empty();
            }
        };
    }

    public static List<HeldSource> allEquipped(UItemPlugin plugin, Player player) {
        List<HeldSource> out = new ArrayList<>();
        if (player == null || plugin == null) {
            return out;
        }
        PlayerInventory inv = player.getInventory();
        fromStack(plugin, player, inv.getItemInMainHand(), EquipmentSlot.HAND).ifPresent(out::add);
        fromStack(plugin, player, inv.getItemInOffHand(), EquipmentSlot.OFF_HAND).ifPresent(out::add);
        ItemStack[] armor = inv.getArmorContents();
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
        };
        for (int i = 0; i < armor.length && i < armorSlots.length; i++) {
            fromStack(plugin, player, armor[i], armorSlots[i]).ifPresent(out::add);
        }
        return out;
    }

    public static Optional<HeldSource> fromStack(UItemPlugin plugin, Player player, ItemStack stack, EquipmentSlot hand) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        if (UItemAPI.isAvailable()) {
            Optional<AbilityHolder> item = UItemAPI.get().getUItemData(stack).map(d -> (AbilityHolder) d);
            if (item.isPresent()) {
                return Optional.of(new HeldSource(player, item.get(), stack.clone(), hand));
            }
        }
        String potionId = plugin.getPotionManager().getIdFromItem(stack);
        if (potionId != null) {
            UPotionData potion = plugin.getPotionManager().get(potionId);
            if (potion != null) {
                return Optional.of(new HeldSource(player, potion, stack.clone(), hand));
            }
        }
        return Optional.empty();
    }

    public record HeldSource(Player player, AbilityHolder holder, ItemStack stack, EquipmentSlot hand) {
    }
}