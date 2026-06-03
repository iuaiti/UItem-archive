package com.uitem.passive.trigger;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public final class TriggerContextMapper {

    private TriggerContextMapper() {
    }

    public static Optional<TriggerContext> map(TriggerType type, Event event) {
        return switch (type) {
            case BLOCK_BREAK -> fromBlockBreak((BlockBreakEvent) event);
            case BLOCK_PLACE -> fromBlockPlace((BlockPlaceEvent) event);
            case ENTITY_DAMAGE -> fromEntityDamage((EntityDamageEvent) event);
            case ENTITY_KILL -> fromEntityKill((EntityDeathEvent) event);
            case PLAYER_INTERACT -> fromInteract((PlayerInteractEvent) event);
            case PLAYER_JOIN -> fromJoin((PlayerJoinEvent) event);
            case PLAYER_QUIT -> fromQuit((PlayerQuitEvent) event);
            case PLAYER_COMMAND -> fromCommand((PlayerCommandPreprocessEvent) event);
            case PLAYER_CHAT -> fromChat((AsyncChatEvent) event);
            case CONSUME_ITEM -> fromConsume((PlayerItemConsumeEvent) event);
            case FISH -> fromFish((PlayerFishEvent) event);
            case CRAFT -> fromCraft((CraftItemEvent) event);
            case ENCHANT -> fromEnchant((EnchantItemEvent) event);
        };
    }

    private static Optional<TriggerContext> fromBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.BLOCK_BREAK, e)
                .player(p)
                .world(p.getWorld())
                .block(e.getBlock())
                .itemInHand(p.getInventory().getItemInMainHand())
                .build());
    }

    private static Optional<TriggerContext> fromBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.BLOCK_PLACE, e)
                .player(p)
                .world(p.getWorld())
                .block(e.getBlockPlaced())
                .itemInHand(p.getInventory().getItemInMainHand())
                .build());
    }

    private static Optional<TriggerContext> fromEntityDamage(EntityDamageEvent e) {
        Entity victim = e.getEntity();
        Player player = null;
        Entity damager = null;
        if (e instanceof EntityDamageByEntityEvent by) {
            damager = by.getDamager();
            if (damager instanceof Player p) {
                player = p;
            } else if (damager != null && damager instanceof org.bukkit.entity.Projectile proj
                    && proj.getShooter() instanceof Player p) {
                player = p;
            }
        }
        if (player == null && victim instanceof Player p) {
            player = p;
        }
        return Optional.of(TriggerContext.builder(TriggerType.ENTITY_DAMAGE, e)
                .player(player)
                .world(victim.getWorld())
                .entity(victim)
                .extra("damager", damager)
                .build());
    }

    private static Optional<TriggerContext> fromEntityKill(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return Optional.empty();
        }
        return Optional.of(TriggerContext.builder(TriggerType.ENTITY_KILL, e)
                .player(killer)
                .world(victim.getWorld())
                .entity(victim)
                .build());
    }

    private static Optional<TriggerContext> fromInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        return Optional.of(TriggerContext.builder(TriggerType.PLAYER_INTERACT, e)
                .player(p)
                .world(p.getWorld())
                .block(b)
                .itemInHand(e.getItem())
                .extra("action", e.getAction().name())
                .build());
    }

    private static Optional<TriggerContext> fromJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.PLAYER_JOIN, e)
                .player(p)
                .world(p.getWorld())
                .build());
    }

    private static Optional<TriggerContext> fromQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.PLAYER_QUIT, e)
                .player(p)
                .world(p.getWorld())
                .build());
    }

    private static Optional<TriggerContext> fromCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.PLAYER_COMMAND, e)
                .player(p)
                .world(p.getWorld())
                .extra("command", e.getMessage())
                .build());
    }

    private static Optional<TriggerContext> fromChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.PLAYER_CHAT, e)
                .player(p)
                .world(p.getWorld())
                .extra("chat_message", e.message())
                .build());
    }

    private static Optional<TriggerContext> fromConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.CONSUME_ITEM, e)
                .player(p)
                .world(p.getWorld())
                .itemInHand(e.getItem())
                .build());
    }

    private static Optional<TriggerContext> fromFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        return Optional.of(TriggerContext.builder(TriggerType.FISH, e)
                .player(p)
                .world(p.getWorld())
                .extra("state", e.getState().name())
                .build());
    }

    private static Optional<TriggerContext> fromCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return Optional.empty();
        }
        return Optional.of(TriggerContext.builder(TriggerType.CRAFT, e)
                .player(p)
                .world(p.getWorld())
                .itemInHand(e.getRecipe() != null && e.getRecipe().getResult() != null
                        ? e.getRecipe().getResult() : null)
                .build());
    }

    private static Optional<TriggerContext> fromEnchant(EnchantItemEvent e) {
        Player p = e.getEnchanter();
        return Optional.of(TriggerContext.builder(TriggerType.ENCHANT, e)
                .player(p)
                .world(p.getWorld())
                .itemInHand(e.getItem())
                .build());
    }
}