package com.uitem.passive.runtime;

import com.uitem.UItemPlugin;
import com.uitem.passive.PassiveDefinition;
import com.uitem.passive.PassiveManager;
import com.uitem.passive.trigger.TriggerContextMapper;
import com.uitem.passive.trigger.TriggerType;
import com.uitem.util.SchedulerUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.List;

public final class PassiveDispatcher implements Listener {

    private final UItemPlugin plugin;
    private final PassiveManager manager;
    private final PassiveRuntime runtime;

    public PassiveDispatcher(UItemPlugin plugin, PassiveManager manager, PassiveRuntime runtime) {
        this.plugin = plugin;
        this.manager = manager;
        this.runtime = runtime;
    }

    private void dispatch(TriggerType type, Event event) {
        TriggerContextMapper.map(type, event).ifPresent(ctx -> {
            List<PassiveDefinition> list = manager.byTrigger(type);
            for (PassiveDefinition passive : list) {
                runtime.execute(passive, ctx);
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        dispatch(TriggerType.BLOCK_BREAK, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        dispatch(TriggerType.BLOCK_PLACE, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        dispatch(TriggerType.ENTITY_DAMAGE, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        dispatch(TriggerType.ENTITY_KILL, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        dispatch(TriggerType.PLAYER_INTERACT, e);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        dispatch(TriggerType.PLAYER_JOIN, e);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.clearSession(e.getPlayer());
        manager.cooldownTracker().clearPlayer(e.getPlayer().getUniqueId());
        dispatch(TriggerType.PLAYER_QUIT, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        dispatch(TriggerType.PLAYER_COMMAND, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        SchedulerUtil.runForEntity(plugin, e.getPlayer(), () -> dispatch(TriggerType.PLAYER_CHAT, e));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        dispatch(TriggerType.CONSUME_ITEM, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        dispatch(TriggerType.FISH, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        dispatch(TriggerType.CRAFT, e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        dispatch(TriggerType.ENCHANT, e);
    }
}