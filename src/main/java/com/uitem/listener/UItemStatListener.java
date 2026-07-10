package com.uitem.listener;

import com.uitem.UItemPlugin;
import com.uitem.hook.SkillsHook;
import com.uitem.stat.Stat;
import com.uitem.stat.StatManager;
import com.uitem.util.SchedulerUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies every virtual combat/utility stat computed by {@link StatManager}
 * to the actual game — crit, defense, penetration, true damage, lifesteal,
 * dodge, regen (health/mana), mining/loot bonuses, projectiles and XP bonus.
 * Combat runs at HIGH priority — before the MONITOR-priority skill/effect
 * listeners (which read final health via setHealth() rather than event
 * damage) — so the damage they react to already reflects stats.
 */
public class UItemStatListener implements Listener {

    private final UItemPlugin plugin;
    private final StatManager statManager;
    private final org.bukkit.NamespacedKey projDamageKey;
    private final org.bukkit.NamespacedKey projSpeedKey;

    public UItemStatListener(UItemPlugin plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.projDamageKey = new org.bukkit.NamespacedKey(plugin, "uitem_proj_damage_bonus");
        this.projSpeedKey = new org.bukkit.NamespacedKey(plugin, "uitem_proj_speed_bonus");
    }

    /** Starts the once-per-second Health Regen / Mana Regen tick. Folia-safe: per-player entity scheduler. */
    public void startRegenTask() {
        SchedulerUtil.runGlobalTimer(plugin, 20, 20, () -> {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                SchedulerUtil.runForEntity(plugin, player, () -> tickRegen(player));
            }
        });
    }

    private void tickRegen(Player player) {
        if (!player.isOnline()) return;

        double healthRegen = statManager.getStat(player, Stat.HEALTH_REGEN);
        if (healthRegen > 0) {
            var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double max = attr != null ? attr.getValue() : 20.0;
            double newHealth = Math.min(max, player.getHealth() + healthRegen);
            if (newHealth > player.getHealth()) player.setHealth(newHealth);
        }

        double manaRegen = statManager.getStat(player, Stat.MANA_REGEN);
        if (manaRegen > 0) {
            SkillsHook hook = plugin.getSkillsHookManager().getActive();
            if (hook.hasManaSupport()) {
                double current = hook.getMana(player);
                double max = hook.getMaxMana(player);
                if (current >= 0 && max >= 0 && current < max) {
                    hook.consumeMana(player, -Math.min(manaRegen, max - current));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Projectile damage bonus is stamped on launch — apply it to the base damage first.
        if (event.getDamager() instanceof Projectile projectile) {
            Double bonus = projectile.getPersistentDataContainer().get(projDamageKey, PersistentDataType.DOUBLE);
            if (bonus != null && bonus > 0) {
                event.setDamage(event.getDamage() * (1 + bonus / 100.0));
            }
        }

        Player attacker = resolveAttacker(event);
        double damage = event.getDamage();

        if (attacker != null) {
            double critChance = clamp(statManager.getStat(attacker, Stat.CRIT_CHANCE), 0, 100);
            if (critChance > 0 && ThreadLocalRandom.current().nextDouble(100) < critChance) {
                double critDamage = Math.max(0, statManager.getStat(attacker, Stat.CRIT_DAMAGE));
                damage *= 1 + (critDamage / 100.0);
            }
        }

        if (event.getEntity() instanceof Player victim) {
            double dodge = clamp(statManager.getStat(victim, Stat.DODGE_CHANCE), 0, 100);
            if (dodge > 0 && ThreadLocalRandom.current().nextDouble(100) < dodge) {
                event.setCancelled(true);
                return;
            }

            double defense = clamp(statManager.getStat(victim, Stat.DEFENSE), 0, 90);
            if (attacker != null) {
                double penetration = clamp(statManager.getStat(attacker, Stat.ARMOR_PENETRATION), 0, 100);
                defense = clamp(defense * (1 - penetration / 100.0), 0, 90);
            }
            if (defense > 0) {
                double trueDamagePct = attacker != null
                        ? clamp(statManager.getStat(attacker, Stat.TRUE_DAMAGE), 0, 100) : 0;
                double trueDamage = damage * (trueDamagePct / 100.0);
                double mitigable = damage - trueDamage;
                damage = trueDamage + mitigable * (1 - defense / 100.0);
            }
        }

        if (damage != event.getDamage()) {
            event.setDamage(damage);
        }

        if (attacker != null) {
            double lifesteal = Math.max(0, statManager.getStat(attacker, Stat.LIFESTEAL));
            if (lifesteal > 0) {
                var maxAttr = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double max = maxAttr != null ? maxAttr.getValue() : 20.0;
                double heal = Math.min(max, attacker.getHealth() + damage * (lifesteal / 100.0));
                if (heal > attacker.getHealth()) attacker.setHealth(heal);
            }
        }
    }

    /** Resolves the Player behind a hit — direct melee, or shooter of a projectile. */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /** Stamps projectile damage/speed bonuses from the launching item onto the projectile entity. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        Projectile projectile = event.getEntity();

        double damageBonus = Math.max(0, statManager.getStat(player, Stat.PROJECTILE_DAMAGE));
        if (damageBonus > 0) {
            projectile.getPersistentDataContainer().set(projDamageKey, PersistentDataType.DOUBLE, damageBonus);
        }

        double speedBonus = Math.max(0, statManager.getStat(player, Stat.PROJECTILE_SPEED));
        if (speedBonus > 0) {
            projectile.setVelocity(projectile.getVelocity().multiply(1 + speedBonus / 100.0));
        }
    }

    /**
     * Fortune Bonus: chance to duplicate each block drop when mining with a
     * tool that has the stat. No vanilla Attribute for this, so it's applied
     * by rolling per dropped item and cloning it in-place.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        double fortuneBonus = clamp(statManager.getStat(player, Stat.FORTUNE_BONUS), 0, 100);
        if (fortuneBonus <= 0) return;

        for (Item itemEntity : event.getItems()) {
            if (ThreadLocalRandom.current().nextDouble(100) >= fortuneBonus) continue;
            ItemStack drop = itemEntity.getItemStack();
            player.getWorld().dropItemNaturally(itemEntity.getLocation(), drop.clone());
        }
    }

    /** Double Drop Chance: chance to duplicate each mob loot drop, and XP Bonus on kill. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        double doubleDropChance = clamp(statManager.getStat(killer, Stat.DOUBLE_DROP_CHANCE), 0, 100);
        if (doubleDropChance > 0 && !event.getDrops().isEmpty()) {
            java.util.List<ItemStack> extra = new java.util.ArrayList<>();
            for (ItemStack drop : event.getDrops()) {
                if (ThreadLocalRandom.current().nextDouble(100) < doubleDropChance) {
                    extra.add(drop.clone());
                }
            }
            event.getDrops().addAll(extra);
        }

        double xpBonus = Math.max(0, statManager.getStat(killer, Stat.XP_BONUS));
        if (xpBonus > 0 && event.getDroppedExp() > 0) {
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * (1 + xpBonus / 100.0)));
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
