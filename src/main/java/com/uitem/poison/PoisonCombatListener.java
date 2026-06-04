package com.uitem.poison;

import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.UItemData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Kết nối hệ thống Poison vào combat event hiện tại.
 *
 * <p>Khi một player tấn công bằng item có {@code poison-on-hit: true} được cấu hình
 * trong UItemData, listener này tự động gọi {@link PoisonManager#applyPoison(LivingEntity, PoisonEffect)}.
 *
 * <h3>Vị trí trong pipeline</h3>
 * <pre>
 * EntityDamageByEntityEvent (HIGH)
 *   └─ PoisonCombatListener.onHit()
 *       └─ đọc UItemData từ weapon
 *       └─ nếu có poison config → applyPoison()
 * </pre>
 *
 * <h3>Config trong items.yml (ví dụ)</h3>
 * <pre>
 * items:
 *   venom_dagger:
 *     material: IRON_SWORD
 *     display-name: "<dark_green>Venom Dagger"
 *     poison-on-hit:
 *       duration-ticks: 100
 *       amplifier: 1
 *       max-stacks: 5
 *       tick-interval: 20
 *       damage-per-tick: 0.5
 * </pre>
 */
public final class PoisonCombatListener implements Listener {

    private final UItemPlugin  plugin;
    private final PoisonManager poisonManager;

    public PoisonCombatListener(UItemPlugin plugin, PoisonManager poisonManager) {
        this.plugin        = plugin;
        this.poisonManager = poisonManager;
    }

    /**
     * Bắt event đánh trúng — kiểm tra weapon có poison config không.
     *
     * <p>Ưu tiên HIGH để chạy sau các protection plugin nhưng trước HIGHEST
     * (damage reduction). Chỉ xử lý nếu event không bị cancel trước đó.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityHit(EntityDamageByEntityEvent event) {
        // Chỉ xử lý khi attacker là Player
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Target phải là LivingEntity (mob, player khác, …)
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Lấy item trên tay
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) return;

        // Kiểm tra có phải UItem không
        if (!UItemAPI.isAvailable()) return;
        UItemData data = UItemAPI.get().getUItemData(weapon).orElse(null);
        if (data == null) return;

        // Lấy poison config từ UItemData
        UItemData.PoisonConfig poisonCfg = data.getPoisonOnHit();
        if (poisonCfg == null) return;

        // Build PoisonEffect từ config
        PoisonEffect effect = PoisonEffect.builder()
                .durationTicks(poisonCfg.getDurationTicks())
                .amplifier(poisonCfg.getAmplifier())
                .maxStacks(poisonCfg.getMaxStacks())
                .tickInterval(poisonCfg.getTickInterval())
                .damagePerTick(poisonCfg.getDamagePerTick())
                .build();

        poisonManager.applyPoison(target, effect);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format(
                "[UItem-Poison] Applied poison to %s (stacks=%d, dur=%dt)",
                target.getName(), poisonManager.getPoisonStacks(target), effect.getDurationTicks()
            ));
        }
    }
}
