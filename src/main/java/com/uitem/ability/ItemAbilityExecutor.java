package com.uitem.ability;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public final class ItemAbilityExecutor {

    private final UItemPlugin plugin;
    private final ItemAbilityCooldown cooldowns;

    public ItemAbilityExecutor(UItemPlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new ItemAbilityCooldown();
    }

    public void tryTrigger(AbilityContext ctx, ItemAbilityRule rule) {
        if (ctx == null || rule == null || rule.getActions().isEmpty()) {
            return;
        }
        AbilityHolder data = ctx.holder();
        if (!data.isAbilitiesEnabled()) {
            return;
        }
        Player player = ctx.player();
        if (!cooldowns.isReady(player.getUniqueId(), data.getAbilityId(), rule)) {
            return;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= rule.getChancePercent()) {
            return;
        }

        cooldowns.mark(player.getUniqueId(), data.getAbilityId(), rule);
        Runnable run = () -> {
            for (ItemAbilityAction action : rule.getActions()) {
                executeAction(ctx, action);
            }
        };
        SchedulerUtil.runForEntity(plugin, player, run);
    }

    private void executeAction(AbilityContext ctx, ItemAbilityAction action) {
        switch (action.getType()) {
            case DAMAGE -> applyDamage(ctx, action.paramDouble("amount", 5));
            case HEAL -> heal(ctx.player(), action.paramDouble("amount", 4));
            case POTION -> applyPotion(ctx, action);
            case LIGHTNING -> strikeLightning(ctx);
            case THORNS -> applyThorns(ctx, action.paramDouble("percent", 25));
            case KNOCKBACK -> applyKnockback(ctx, action.paramDouble("strength", 1.2));
            case MESSAGE -> ctx.player().sendMessage(TextUtil.parse(
                    action.param("text", "<green>Kích hoạt!")));
            case COMMAND -> runCommand(ctx, action.param("command", ""));
            default -> {
            }
        }
    }

    private void applyDamage(AbilityContext ctx, double amount) {
        Entity target = ctx.target();
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        SchedulerUtil.runForEntity(plugin, living, () ->
                living.damage(amount, ctx.player()));
    }

    private void heal(Player player, double amount) {
        double max = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20;
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    private void applyPotion(AbilityContext ctx, ItemAbilityAction action) {
        PotionEffectType type = resolveEffect(action.param("effect", "regeneration"));
        if (type == null) {
            return;
        }
        int duration = action.paramInt("duration", 5) * 20;
        int amp = action.paramInt("amplifier", 0);
        PotionEffect effect = new PotionEffect(type, duration, amp);
        String target = action.param("target", "SELF").toUpperCase();
        if (target.equals("TARGET") && ctx.target() instanceof LivingEntity living) {
            SchedulerUtil.runForEntity(plugin, living, () -> living.addPotionEffect(effect));
        } else {
            ctx.player().addPotionEffect(effect);
        }
    }

    private void strikeLightning(AbilityContext ctx) {
        Entity target = ctx.target();
        Location loc = target != null ? target.getLocation() : ctx.player().getLocation();
        SchedulerUtil.runAtLocation(plugin, loc, () -> {
            World world = loc.getWorld();
            if (world != null) {
                world.strikeLightningEffect(loc);
            }
        });
    }

    private void applyThorns(AbilityContext ctx, double percent) {
        if (ctx.damageAmount() <= 0 || !(ctx.target() instanceof LivingEntity attacker)) {
            return;
        }
        double reflect = ctx.damageAmount() * (percent / 100.0);
        if (reflect > 0) {
            SchedulerUtil.runForEntity(plugin, attacker, () ->
                    attacker.damage(reflect, ctx.player()));
        }
    }

    private void applyKnockback(AbilityContext ctx, double strength) {
        Entity target = ctx.target();
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        Vector dir = living.getLocation().toVector()
                .subtract(ctx.player().getLocation().toVector())
                .normalize()
                .multiply(strength);
        dir.setY(0.35);
        Vector velocity = dir;
        SchedulerUtil.runForEntity(plugin, living, () -> living.setVelocity(velocity));
    }

    private void runCommand(AbilityContext ctx, String cmd) {
        if (cmd == null || cmd.isBlank()) {
            return;
        }
        String parsed = cmd.replace("{player}", ctx.player().getName())
                .replace("{item}", ctx.holder().getAbilityId())
                .replace("{uuid}", ctx.player().getUniqueId().toString());
        SchedulerUtil.runGlobal(plugin, () ->
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed));
    }

    private static PotionEffectType resolveEffect(String raw) {
        return com.uitem.potion.PotionEffectTypes.resolve(raw);
    }
}