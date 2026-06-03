package com.uitem.ability;

import com.uitem.UItemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Fires ON_EQUIP ability rules when armor/hand items change.
 */
public final class AbilityEquipTriggers {

    private AbilityEquipTriggers() {
    }

    public static void fire(UItemPlugin plugin, Player player, ItemStack stack, EquipmentSlot slot) {
        if (plugin == null || player == null || stack == null || stack.getType().isAir() || slot == null) {
            return;
        }
        AbilityMatcher.fromStack(plugin, player, stack, slot).ifPresent(held -> {
            AbilityHolder data = held.holder();
            if (!data.isAbilitiesEnabled()) {
                return;
            }
            ItemAbilityExecutor executor = new ItemAbilityExecutor(plugin);
            for (ItemAbilityRule rule : data.getAbilities()) {
                if (rule.getTrigger() != ItemTrigger.ON_EQUIP || rule.getActions().isEmpty()) {
                    continue;
                }
                if (!matchesSlot(held, rule.getSlot())) {
                    continue;
                }
                AbilityContext ctx = new AbilityContext(held.player(), data, ItemTrigger.ON_EQUIP, null, 0);
                executor.tryTrigger(ctx, rule);
            }
        });
    }

    private static boolean matchesSlot(AbilityMatcher.HeldSource held, ItemSlotRequirement required) {
        EquipmentSlot hand = held.hand();
        return switch (required) {
            case MAIN_HAND -> hand == EquipmentSlot.HAND;
            case OFF_HAND -> hand == EquipmentSlot.OFF_HAND;
            case EITHER_HAND -> hand == EquipmentSlot.HAND || hand == EquipmentSlot.OFF_HAND;
            case ARMOR -> hand == EquipmentSlot.HEAD || hand == EquipmentSlot.CHEST
                    || hand == EquipmentSlot.LEGS || hand == EquipmentSlot.FEET;
        };
    }
}