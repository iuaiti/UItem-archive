package com.uitem.listener;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.potion.UPotionData;
import com.uitem.util.Messages;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Khi item UItem co {@code linked-potion}, click phai ap hieu ung potion do.
 */
public final class ItemLinkedPotionListener implements Listener {

    private static final long COOLDOWN_MS = 500L;

    private final UItemPlugin plugin;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public ItemLinkedPotionListener(@NotNull UItemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUseLinkedPotion(@NotNull PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack stack = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        UItemData data = UItemAPI.get().getUItemData(stack).orElse(null);
        if (data == null) {
            return;
        }
        String potionId = data.getLinkedPotionId();
        if (potionId == null || potionId.isBlank()) {
            return;
        }

        UPotionData potion = plugin.getPotionManager().get(potionId);
        if (potion == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long prev = lastUse.get(player.getUniqueId());
        if (prev != null && now - prev < COOLDOWN_MS) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        SchedulerUtil.runForEntity(plugin, player, () -> {
            plugin.getPotionManager().applyEffectsToPlayer(player, potion);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.6f, 1.1f);
            if (plugin.getConfig().getBoolean("display.linked-potion-use-message", false)) {
                player.sendMessage(TextUtil.success(Messages.itemLinkedPotionUsed(potionId)));
            }
        });

        // Prevent vanilla interaction (eating, placing, etc.) when using linked potion
        event.setCancelled(true);
    }
}