package com.uitem.listener;

import com.uitem.UItemPlugin;
import com.uitem.passive.PassiveManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class PassiveListener implements Listener {

    private final UItemPlugin plugin;
    private final PassiveManager passiveManager;
    private final Set<UUID> equippedPlayers = new HashSet<>();

    public PassiveListener(UItemPlugin plugin) {
        this.plugin = plugin;
        this.passiveManager = new PassiveManager(plugin, plugin.getItemManager());
    }

    public PassiveManager getPassiveManager() {
        return passiveManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        context.put("target", target);
        context.put("damage", event.getDamage());

        passiveManager.executePassives(player, item, "HIT", context);

        if (context.containsKey("damage_multiplier")) {
            double mult = ((Number) context.get("damage_multiplier")).doubleValue();
            event.setDamage(event.getDamage() * mult);
        }
        if (context.containsKey("force_crit")) {
            event.setDamage(event.getDamage() * 1.5);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        context.put("damage", event.getDamage());
        context.put("cause", event.getCause().name());

        passiveManager.executePassives(player, item, "DAMAGED", context);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        context.put("target", event.getEntity());

        passiveManager.executePassives(killer, item, "KILL", context);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        context.put("block", event.getBlock());

        passiveManager.executePassives(player, item, "BREAK_BLOCK", context);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        passiveManager.executePassives(player, item, "HOLD", context);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (equippedPlayers.contains(uuid)) return;
        equippedPlayers.add(uuid);

        checkAndTriggerEquipPassives(player, EquipmentSlot.HEAD);
        checkAndTriggerEquipPassives(player, EquipmentSlot.CHEST);
        checkAndTriggerEquipPassives(player, EquipmentSlot.LEGS);
        checkAndTriggerEquipPassives(player, EquipmentSlot.FEET);
        checkAndTriggerEquipPassives(player, EquipmentSlot.HAND);
        checkAndTriggerEquipPassives(player, EquipmentSlot.OFF_HAND);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        equippedPlayers.remove(event.getPlayer().getUniqueId());
    }

    public void checkAndTriggerEquipPassives(Player player, EquipmentSlot slot) {
        if (player == null) return;
        ItemStack item = getItemForSlot(player, slot);
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        context.put("slot", slot.name());

        passiveManager.executePassives(player, item, "EQUIP", context);
    }

    private ItemStack getItemForSlot(Player player, EquipmentSlot slot) {
        PlayerInventory inv = player.getInventory();
        return switch (slot) {
            case HEAD -> inv.getHelmet();
            case CHEST -> inv.getChestplate();
            case LEGS -> inv.getLeggings();
            case FEET -> inv.getBoots();
            case HAND -> inv.getItemInMainHand();
            case OFF_HAND -> inv.getItemInOffHand();
            default -> null;
        };
    }
}
