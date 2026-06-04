package com.uitem.poison;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý toàn bộ hiệu ứng độc (Poison DoT).
 *
 * <h3>Kiến trúc</h3>
 * <ul>
 *   <li>State runtime được lưu trong {@link ConcurrentHashMap} keyed bởi UUID.</li>
 *   <li>Tick loop chạy async qua {@link SchedulerUtil#runAsync} lặp lại
 *       mỗi {@code GLOBAL_TICK_PERIOD_MS} ms — tương đương 1 server tick (50 ms).
 *       Damage thực sự được apply về main thread để tránh async CME với NMS.</li>
 *   <li>Khi player chết hoặc quit, state tự động bị xóa.</li>
 *   <li>Không có BukkitRunnable / deprecated scheduler — dùng hoàn toàn Paper API.</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * ConcurrentHashMap + {@code compute()} đảm bảo không race condition khi nhiều
 * thread cùng gọi {@code applyPoison()} (ví dụ từ multi-region trên Folia).
 * Damage cuối cùng luôn được dispatch về main/region thread.
 */
public final class PoisonManager implements Listener {

    // Tick loop interval: 50 ms = 1 server tick
    private static final long GLOBAL_TICK_PERIOD_MS = 50L;

    private final UItemPlugin plugin;

    /** UUID → trạng thái độc. ConcurrentHashMap để thread-safe với Folia. */
    private final ConcurrentHashMap<UUID, PoisonState> poisonMap = new ConcurrentHashMap<>();

    /** True khi tick loop đang chạy, tránh khởi động nhiều lần. */
    private volatile boolean tickRunning = false;

    public PoisonManager(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Khởi động PoisonManager: đăng ký listener và bắt đầu tick loop.
     * Phải gọi từ onEnable() sau khi plugin đã init.
     */
    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startTickLoop();
        plugin.getLogger().info("[UItem-Poison] PoisonManager started.");
    }

    /**
     * Dừng PoisonManager và dọn toàn bộ state.
     * Gọi từ onDisable().
     */
    public void shutdown() {
        tickRunning = false;
        poisonMap.clear();
        plugin.getLogger().info("[UItem-Poison] PoisonManager stopped.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Áp hiệu ứng độc lên một entity.
     *
     * <p>Nếu entity đã bị độc:
     * <ul>
     *   <li>Stack tăng thêm 1 (tối đa {@link PoisonEffect#getMaxStacks()}).</li>
     *   <li>Duration được refresh về giá trị mới nhất.</li>
     *   <li>Effect config được cập nhật theo lần áp mới nhất.</li>
     * </ul>
     *
     * @param entity entity bị áp độc (LivingEntity hoặc Player)
     * @param effect cấu hình của lần áp này
     */
    public void applyPoison(LivingEntity entity, PoisonEffect effect) {
        if (entity == null || entity.isDead() || effect == null) return;

        UUID uuid = entity.getUniqueId();
        poisonMap.compute(uuid, (id, existing) -> {
            if (existing == null) {
                // Lần đầu áp độc
                return new PoisonState(effect);
            }
            // Cộng dồn stack, refresh duration & config
            existing.currentStacks  = Math.min(existing.currentStacks + 1, effect.getMaxStacks());
            existing.remainingTicks = effect.getDurationTicks();
            existing.lastEffect     = effect;
            return existing;
        });
    }

    /**
     * Xóa toàn bộ stack độc của entity.
     *
     * @param entity entity cần clean
     */
    public void removePoison(LivingEntity entity) {
        if (entity == null) return;
        poisonMap.remove(entity.getUniqueId());
    }

    /**
     * Lấy số stack độc hiện tại của entity.
     *
     * @return số stack (0 nếu không bị độc)
     */
    public int getPoisonStacks(LivingEntity entity) {
        if (entity == null) return 0;
        PoisonState state = poisonMap.get(entity.getUniqueId());
        return (state != null && state.isActive()) ? state.currentStacks : 0;
    }

    /**
     * Kiểm tra entity có đang bị độc không.
     */
    public boolean isPoisoned(LivingEntity entity) {
        return getPoisonStacks(entity) > 0;
    }

    /**
     * Lấy số tick còn lại của độc (0 nếu không bị độc).
     */
    public int getRemainingTicks(LivingEntity entity) {
        if (entity == null) return 0;
        PoisonState state = poisonMap.get(entity.getUniqueId());
        return (state != null && state.isActive()) ? state.remainingTicks : 0;
    }

    /**
     * Buộc tick toàn bộ poison effects ngay lập tức (dùng cho testing / API ngoài).
     * Thường không cần gọi thủ công — tick loop tự xử lý.
     */
    public void tickPoisonEffects() {
        doTick();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Tick loop
    // ─────────────────────────────────────────────────────────────────────────

    private void startTickLoop() {
        if (tickRunning) return;
        tickRunning = true;
        scheduleNextTick();
    }

    /**
     * Recursive async scheduling — tự gọi lại sau mỗi GLOBAL_TICK_PERIOD_MS.
     * Khi tickRunning = false (shutdown), chuỗi tự dừng.
     */
    private void scheduleNextTick() {
        SchedulerUtil.runDelayedAsync(plugin, () -> {
            if (!tickRunning) return;
            try {
                doTick();
            } finally {
                if (tickRunning) scheduleNextTick();
            }
        }, GLOBAL_TICK_PERIOD_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Một tick xử lý: giảm remainingTicks, tính damage, dispatch về main thread.
     *
     * <p>Chạy trên async thread — KHÔNG được gọi Bukkit API trực tiếp ở đây,
     * chỉ đọc/ghi ConcurrentHashMap và dispatch runnable về sync.
     */
    private void doTick() {
        if (poisonMap.isEmpty()) return;

        poisonMap.forEach((uuid, state) -> {
            synchronized (state) {
                if (!state.isActive()) {
                    poisonMap.remove(uuid);
                    return;
                }

                // Giảm thời gian còn lại
                state.remainingTicks--;

                // Tăng accumulator
                state.tickAccumulator++;

                // Đến lúc damage chưa?
                if (state.tickAccumulator < state.lastEffect.getTickInterval()) return;
                state.tickAccumulator = 0;

                // Snapshot data để dùng trong lambda (tránh capture state trực tiếp)
                double damage       = state.computeDamage();
                int    stacks       = state.currentStacks;

                // Dispatch damage về main/region thread
                dispatchDamage(uuid, damage, stacks);

                // Hết duration → xóa state
                if (state.remainingTicks <= 0) {
                    poisonMap.remove(uuid);
                }
            }
        });
    }

    /**
     * Dispatch sát thương về đúng thread theo môi trường (Folia / Paper / Spigot).
     *
     * <p>Tìm entity bằng UUID — nếu đã offline/dead thì tự động drop,
     * không gây lỗi hay memory leak.
     */
    private void dispatchDamage(UUID uuid, double damage, int stacks) {
        SchedulerUtil.runSync(plugin, () -> {
            Entity raw = plugin.getServer().getEntity(uuid);
            if (!(raw instanceof LivingEntity entity)) {
                poisonMap.remove(uuid);
                return;
            }
            if (entity.isDead() || entity.getHealth() <= 0) {
                poisonMap.remove(uuid);
                return;
            }

            // Apply damage
            entity.damage(damage);

            // Visual feedback (chỉ cho Player)
            if (entity instanceof Player player) {
                sendPoisonFeedback(player, stacks);
            }
        });
    }

    /**
     * Hiệu ứng visual/feedback cho player bị độc.
     * Có thể mở rộng: particle, title, sound…
     */
    private void sendPoisonFeedback(Player player, int stacks) {
        // Action bar hiển thị stack count — nhẹ, không spam chat
        if (stacks > 1) {
            player.sendActionBar(
                net.kyori.adventure.text.Component.text(
                    "☠ Poisoned ×" + stacks,
                    net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN
                )
            );
        } else {
            player.sendActionBar(
                net.kyori.adventure.text.Component.text(
                    "☠ Poisoned",
                    net.kyori.adventure.text.format.NamedTextColor.GREEN
                )
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cleanup listeners — tự động dọn khi player chết hoặc thoát
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        poisonMap.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        poisonMap.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        poisonMap.remove(event.getPlayer().getUniqueId());
    }
}
