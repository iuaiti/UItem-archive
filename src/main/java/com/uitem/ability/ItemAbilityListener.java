package com.uitem.ability;

import com.uitem.UItemPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ItemAbilityListener implements Listener {

    private final UItemPlugin plugin;
    private final ItemAbilityExecutor executor;

    public ItemAbilityListener(UItemPlugin plugin) {
        this.plugin = plugin;
        this.executor = new ItemAbilityExecutor(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        dispatch(player, ItemTrigger.ON_HIT, event.getEntity(), event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        dispatch(killer, ItemTrigger.ON_KILL, event.getEntity(), 0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Entity damager = event instanceof EntityDamageByEntityEvent by ? by.getDamager() : null;
        dispatch(player, ItemTrigger.ON_DAMAGE_TAKEN, damager, event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) {
            return;
        }
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        ItemStack stack = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        ItemTrigger trigger = switch (action) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> ItemTrigger.ON_RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> ItemTrigger.ON_LEFT_CLICK;
            default -> null;
        };
        if (trigger == null) {
            return;
        }
        AbilityMatcher.fromStack(plugin, player, stack, hand).ifPresent(held ->
                fireRules(held, trigger, null, 0));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrink(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItem();
        AbilityMatcher.fromStack(plugin, player, stack, EquipmentSlot.HAND).ifPresent(held ->
                fireRules(held, ItemTrigger.ON_DRINK, null, 0));
    }

    private void dispatch(Player player, ItemTrigger trigger, Entity target, double damage) {
        for (AbilityMatcher.HeldSource held : AbilityMatcher.allEquipped(plugin, player)) {
            fireRules(held, trigger, target, damage);
        }
    }

    private void fireRules(AbilityMatcher.HeldSource held, ItemTrigger trigger, Entity target, double damage) {
        AbilityHolder data = held.holder();
        if (!data.isAbilitiesEnabled()) {
            return;
        }
        for (ItemAbilityRule rule : data.getAbilities()) {
            if (rule.getTrigger() != trigger || rule.getActions().isEmpty()) {
                continue;
            }
            if (!matchesSlot(held, rule.getSlot())) {
                continue;
            }
            AbilityContext ctx = new AbilityContext(held.player(), data, trigger, target, damage);
            executor.tryTrigger(ctx, rule);
        }
    }

    private boolean matchesSlot(AbilityMatcher.HeldSource held, ItemSlotRequirement required) {
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