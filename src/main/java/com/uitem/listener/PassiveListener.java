package com.uitem.listener;

import com.uitem.UItemPlugin;
import com.uitem.passive.PassiveManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
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
import java.util.logging.Level;

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
        // Case 1: Player is the ATTACKER -> HIT trigger
        if (event.getDamager() instanceof Player player) {
            if (event.getEntity() instanceof LivingEntity target) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() != Material.AIR) {
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
            }
        }

        // Case 2: Player is the VICTIM -> DAMAGED trigger
        if (event.getEntity() instanceof Player victim) {
            ItemStack item = victim.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) return;

            Map<String, Object> context = new HashMap<>();
            context.put("damage", event.getDamage());
            context.put("cause", event.getCause().name());

            passiveManager.executePassives(victim, item, "DAMAGED", context);
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
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        Player killer = living.getKiller();
        if (killer == null) return;

        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        context.put("target", living);

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

        if (context.containsKey("auto_smelt")) {
            org.bukkit.block.Block block = event.getBlock();
            event.setDropItems(false);
            List<ItemStack> drops = new ArrayList<>(block.getDrops(item));
            for (ItemStack drop : drops) {
                ItemStack smelted = getSmeltedResult(drop);
                if (smelted != null) {
                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    loc.getWorld().dropItemNaturally(loc, smelted);
                } else {
                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    loc.getWorld().dropItemNaturally(loc, drop);
                }
            }
        }
    }

    private ItemStack getSmeltedResult(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        Material mat = item.getType();
        Material result = switch (mat) {
            case IRON_ORE -> Material.IRON_INGOT;
            case DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case RAW_IRON -> Material.IRON_INGOT;
            case GOLD_ORE -> Material.GOLD_INGOT;
            case DEEPSLATE_GOLD_ORE -> Material.GOLD_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case COPPER_ORE -> Material.COPPER_INGOT;
            case DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            case NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            case NETHER_QUARTZ_ORE -> Material.QUARTZ;
            case LAPIS_ORE -> Material.LAPIS_LAZULI;
            case DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case REDSTONE_ORE -> Material.REDSTONE;
            case DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case DIAMOND_ORE -> Material.DIAMOND;
            case DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE -> Material.EMERALD;
            case DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            case COAL_ORE -> Material.COAL;
            case DEEPSLATE_COAL_ORE -> Material.COAL;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case IRON_INGOT -> Material.IRON_INGOT;
            case GOLD_INGOT -> Material.GOLD_INGOT;
            case SAND -> Material.GLASS;
            case COBBLESTONE -> Material.STONE;
            case STONE -> Material.STONE;
            case COBBLED_DEEPSLATE -> Material.DEEPSLATE;
            case CLAY_BALL -> Material.TERRACOTTA;
            case NETHERRACK -> Material.NETHER_BRICK;
            case WET_SPONGE -> Material.SPONGE;
            case CHORUS_FRUIT -> Material.POPPED_CHORUS_FRUIT;
            default -> null;
        };
        if (result != null) {
            ItemStack smelted = new ItemStack(result);
            smelted.setAmount(item.getAmount());
            if (mat.name().endsWith("_ORE") || mat.name().startsWith("DEEPSLATE_") || mat == Material.NETHER_GOLD_ORE) {
                java.util.Random rand = new java.util.Random();
                int bonus = 0;
                if (item.containsEnchantment(Enchantment.FORTUNE)) {
                    int level = item.getEnchantmentLevel(Enchantment.FORTUNE);
                    bonus = rand.nextInt(level + 1);
                }
                smelted.setAmount(item.getAmount() + bonus);
            }
            return smelted;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Object> context = new HashMap<>();
        passiveManager.executePassives(player, item, "HOLD", context);
    }

    public void tickHoldPassives(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
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

    public void startHoldTickTask() {
        if (com.uitem.util.SchedulerUtil.isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    tickHoldPassives(player);
                }
            }, 20L, 20L);
        } else {
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    tickHoldPassives(player);
                }
            }, 20L, 20L);
        }
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
