package com.uitem.listener;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.api.events.UItemEquipEvent;
import com.uitem.api.events.UItemInteractEvent;
import com.uitem.skill.SkillExecutor;
import com.uitem.skill.SkillTrigger;
import com.uitem.util.DamageGuard;
import com.uitem.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener chuyên biệt cho hệ thống Skill & Passive native của UItem.
 * 
 * Ưu điểm so với ExecutableItems:
 * - Toàn bộ logic Java rõ ràng, không script phức tạp.
 * - Cooldown lưu PDC + memory → rất ổn định sau restart.
 * - Folia-safe qua SchedulerUtil (global timer fan-out sang entity scheduler).
 * - Dễ mở rộng action/condition.
 */
public class UItemSkillListener implements Listener {

    /** Nhịp của periodic ticker (tick). 5 tick ≈ 0.25s, đủ mượt cho aura/regen. */
    public static final long PERIODIC_TICK_RATE = 5L;

    private final UItemPlugin plugin;
    private final SkillExecutor executor;

    // Track item đang cầm để WHILE_HOLDING.
    // ConcurrentHashMap: trên Folia các region tick song song, mỗi player nằm ở
    // region riêng nên map này bị đọc/ghi từ nhiều thread.
    private final Map<UUID, String> lastHeldUItem = new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastSneakAt = new ConcurrentHashMap<>();
    private static final long DOUBLE_SNEAK_WINDOW_MS = 450L;

    // Cờ tắt ticker khi plugin disable. volatile vì được set từ main thread
    // nhưng đọc từ thread của region.
    private volatile boolean shutdown = false;

    public UItemSkillListener(UItemPlugin plugin, SkillExecutor executor) {
        this.plugin = plugin;
        this.executor = executor;
        startPeriodicTask();
    }

    /**
     * Ticker cho PERIODIC / WHILE_HOLDING / WHILE_WEARING.
     *
     * <p>Folia-safe: global timer chỉ liệt kê player rồi đẩy từng người sang
     * entity scheduler của chính họ, nên mọi truy cập inventory/world đều chạy
     * đúng region thread. Trên Paper/Spigot SchedulerUtil rơi về BukkitScheduler
     * nên hành vi không đổi.
     */
    private void startPeriodicTask() {
        SchedulerUtil.runGlobalTimer(plugin, 20L, PERIODIC_TICK_RATE, () -> {
            if (shutdown) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                SchedulerUtil.runForEntity(plugin, p, () -> {
                    if (shutdown || !p.isOnline()) return;
                    try {
                        tickPlayerPeriodic(p);
                    } catch (Exception e) {
                        // Không để 1 player làm crash toàn bộ ticker — log lỗi để debug
                        plugin.getLogger().warning("[Skill] Periodic tick error for "
                                + p.getName() + ": " + e.getMessage());
                    }
                });
            }
        });
    }

    public void shutdown() {
        this.shutdown = true;
    }

    private void tickPlayerPeriodic(Player player) {
        // WHILE_HOLDING (main + offhand)
        checkAndTickHolding(player, player.getInventory().getItemInMainHand(), SkillTrigger.WHILE_HOLDING);
        checkAndTickHolding(player, player.getInventory().getItemInOffHand(), SkillTrigger.WHILE_HOLDING);

        // PERIODIC trên mọi item đang cầm/mặc (để đơn giản: chỉ cầm tay chính + giáp)
        checkAndTickPeriodic(player, player.getInventory().getItemInMainHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                checkAndTickHolding(player, armor, SkillTrigger.WHILE_WEARING);
                checkAndTickPeriodic(player, armor);
            }
        }
    }

    private void checkAndTickHolding(Player player, ItemStack item, SkillTrigger trigger) {
        UItemData data = getUItemData(item);
        if (data == null || data.getSkills() == null || data.getSkills().isEmpty()) return;

        String itemId = data.getId();
        executor.tickPeriodicSkills(player, itemId, data.getSkills(), trigger);
    }

    private void checkAndTickPeriodic(Player player, ItemStack item) {
        UItemData data = getUItemData(item);
        if (data == null || data.getSkills() == null || data.getSkills().isEmpty()) return;

        String itemId = data.getId();
        executor.tickPeriodicSkills(player, itemId, data.getSkills(), SkillTrigger.PERIODIC);
    }

    // =========================================================
    // Interact (active skills)
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onUItemInteract(UItemInteractEvent event) {
        Player player = event.getPlayer();
        UItemData data = event.getItemData();
        if (data == null || data.getSkills() == null || data.getSkills().isEmpty()) return;

        org.bukkit.event.block.Action bukkitAction = event.getAction();
        boolean sneaking = player.isSneaking();

        SkillTrigger trigger = switch (bukkitAction) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> sneaking ? SkillTrigger.SHIFT_RIGHT_CLICK : SkillTrigger.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> sneaking ? SkillTrigger.SHIFT_LEFT_CLICK : SkillTrigger.LEFT_CLICK;
            default -> null;
        };

        if (trigger == null) return;

        boolean executed = executor.executeSkills(player, data.getId(), data.getSkills(), trigger, null);

        if (executed && shouldCancel(data, trigger)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        UItemData data = getUItemData(item);
        if (data == null || data.getSkills() == null) return;

        // DROP vs THROW is decided by sneak state, not by the dropped item's velocity.
        // Vanilla gives every Q-drop a velocity of roughly 0.3, so the old
        // "lengthSquared() > 0.1" test landed either side of the threshold depending
        // on the player's pitch — the same keypress produced DROP or THROW at random.
        // Sneak is deterministic and mirrors the SNEAK/DOUBLE_SNEAK pair below:
        //   Q          -> DROP
        //   Shift + Q  -> THROW
        SkillTrigger trigger = player.isSneaking() ? SkillTrigger.THROW : SkillTrigger.DROP;

        boolean executed = executor.executeSkills(player, data.getId(), data.getSkills(), trigger, null);

        // Never cancel for THROW: cancelling would prevent the item from actually leaving the
        // player's hand, so the skill would "finish" and consume its cooldown while the throw
        // never happens. DROP may cancel (e.g. a soulbound item that refuses to be dropped).
        if (executed && trigger == SkillTrigger.DROP && shouldCancel(data, trigger)) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    // SHOOT trigger: bow/crossbow/trident (user request: ném đinh ba / bắn cung)
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType().isAir()) return;

        UItemData data = getUItemData(bow);
        if (data == null || data.getSkills() == null || data.getSkills().isEmpty()) return;

        // Never cancel SHOOT: cancelling the shoot event prevents the arrow from ever being
        // created, so the skill "finishes" and consumes its cooldown while nothing actually fires.
        executor.executeSkills(player, data.getId(), data.getSkills(), SkillTrigger.SHOOT, null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTridentOrProjectileThrow(ProjectileLaunchEvent event) {
        // Arrows/spectral arrows fired from a bow or crossbow are already handled by
        // onBowShoot(EntityShootBowEvent) - without this guard SHOOT would fire twice per shot.
        if (event.getEntity() instanceof org.bukkit.entity.AbstractArrow) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        // For trident throws (and potentially other projectile weapons), look at held items.
        // Prefer main hand, then off hand. We trigger SHOOT on the UItem weapon used to launch.
        // (No extra TRIDENT-specific pass: both hands are already resolved here, so a
        // material check afterwards could only re-read an ItemStack that just returned null.)
        UItemData data = getUItemData(player.getInventory().getItemInMainHand());
        if (data == null) {
            data = getUItemData(player.getInventory().getItemInOffHand());
        }

        if (data == null || data.getSkills() == null || data.getSkills().isEmpty()) return;

        // Never cancel SHOOT here either - same reasoning as onBowShoot above.
        executor.executeSkills(player, data.getId(), data.getSkills(), SkillTrigger.SHOOT, null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        UItemData data = getUItemData(main);
        if (data != null && data.getSkills() != null) {
            boolean executed = executor.executeSkills(player, data.getId(), data.getSkills(), SkillTrigger.SWAP_HAND, null);
            if (executed && shouldCancel(data, SkillTrigger.SWAP_HAND)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        UItemData data = getUItemData(item);
        if (data == null || data.getSkills() == null || data.getSkills().isEmpty()) return;

        boolean executedSneak = executor.executeSkills(player, data.getId(), data.getSkills(), SkillTrigger.SNEAK, null);
        boolean executedDoubleSneak = false;

        long now = System.currentTimeMillis();
        Long last = lastSneakAt.put(player.getUniqueId(), now);
        if (last != null && now - last <= DOUBLE_SNEAK_WINDOW_MS) {
            executedDoubleSneak = executor.executeSkills(player, data.getId(), data.getSkills(), SkillTrigger.DOUBLE_SNEAK, null);
        }

        if ((executedSneak && shouldCancel(data, SkillTrigger.SNEAK))
                || (executedDoubleSneak && shouldCancel(data, SkillTrigger.DOUBLE_SNEAK))) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    // Combat
    // =========================================================

    // HIGHEST, not MONITOR: this handler mutates state (skill actions deal damage,
    // teleport, break blocks...). MONITOR is contractually read-only and is where
    // logging plugins observe the final outcome.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Reentrancy guard: DAMAGE action gọi target.damage() sinh ra EntityDamageByEntityEvent mới,
        // nếu không guard sẽ gây đệ quy vô hạn (HIT → DAMAGE → HIT → DAMAGE → ...).
        // Guard theo damager nên hit của player khác trong cùng tick không bị bỏ.
        if (!DamageGuard.enter(event.getDamager())) return;
        try {
            // HIT: damager dùng UItem
            if (event.getDamager() instanceof Player damager) {
                ItemStack weapon = damager.getInventory().getItemInMainHand();
                UItemData data = getUItemData(weapon);
                if (data != null && data.getSkills() != null) {
                    LivingEntity victim = (event.getEntity() instanceof LivingEntity le) ? le : null;
                    executor.executeSkills(damager, data.getId(), data.getSkills(), SkillTrigger.HIT, victim);
                }
            }

            // TAKE_DAMAGE: victim đang cầm hoặc mặc UItem có skill
            if (event.getEntity() instanceof Player victim) {
                // held
                ItemStack held = victim.getInventory().getItemInMainHand();
                UItemData heldData = getUItemData(held);
                if (heldData != null && heldData.getSkills() != null) {
                    executor.executeSkills(victim, heldData.getId(), heldData.getSkills(), SkillTrigger.TAKE_DAMAGE, null);
                }
                // armor
                for (ItemStack armor : victim.getInventory().getArmorContents()) {
                    if (armor == null || armor.getType().isAir()) continue;
                    UItemData ad = getUItemData(armor);
                    if (ad != null && ad.getSkills() != null) {
                        executor.executeSkills(victim, ad.getId(), ad.getSkills(), SkillTrigger.TAKE_DAMAGE, null);
                    }
                }
            }
        } finally {
            DamageGuard.exit(event.getDamager());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        UItemData data = getUItemData(weapon);
        if (data == null || data.getSkills() == null || data.getSkills().isEmpty()) return;

        executor.executeSkills(killer, data.getId(), data.getSkills(), SkillTrigger.KILL, event.getEntity());
    }

    // =========================================================
    // Equip
    // =========================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEquip(UItemEquipEvent event) {
        Player p = event.getPlayer();
        UItemData data = event.getItemData();
        if (data == null || data.getSkills() == null) return;

        SkillTrigger trig = event.isEquipping() ? SkillTrigger.EQUIP : SkillTrigger.UNEQUIP;
        executor.executeSkills(p, data.getId(), data.getSkills(), trig, null);
    }

    // =========================================================
    // Held tracking for WHILE_HOLDING (chuyển item)
    // =========================================================

    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        ItemStack newItem = p.getInventory().getItem(event.getNewSlot());
        String newId = getUItemId(newItem);

        String oldId = lastHeldUItem.get(p.getUniqueId());
        if (!Objects.equals(oldId, newId) && newId != null) {
            // Trigger WHILE_HOLDING ngay khi cầm lên (nếu có skill loại này)
            UItemData data = getUItemData(newItem);
            if (data != null) {
                executor.executeSkills(p, newId, data.getSkills(), SkillTrigger.WHILE_HOLDING, null);
            }
        }
        if (newId != null) lastHeldUItem.put(p.getUniqueId(), newId);
        else lastHeldUItem.remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastHeldUItem.remove(event.getPlayer().getUniqueId());
        lastSneakAt.remove(event.getPlayer().getUniqueId());
        executor.getCooldowns().onPlayerQuit(event.getPlayer());
        if (plugin.getStatManager() != null) {
            plugin.getStatManager().invalidate(event.getPlayer());
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private UItemData getUItemData(ItemStack item) {
        if (!UItemAPI.isAvailable()) return null;
        return UItemAPI.get().getUItemData(item).orElse(null);
    }

    private String getUItemId(ItemStack item) {
        if (!UItemAPI.isAvailable()) return null;
        return UItemAPI.get().getUItemId(item).orElse(null);
    }

    /**
     * True when at least one skill bound to {@code trigger} asked for the
     * originating event to be cancelled.
     */
    private boolean shouldCancel(UItemData data, SkillTrigger trigger) {
        if (data.getSkills() == null) return false;
        return data.getSkills().stream()
                .anyMatch(s -> s.getTrigger() == trigger && s.isCancelEvent());
    }
}
