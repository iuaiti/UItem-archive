package com.uitem.skill;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Thực thi skill/passive.
 * 
 * Thiết kế ổn định:
 * - Mọi action được bọc try/catch riêng lẻ.
 * - Placeholder đơn giản: %player%, %target%, %x%, %y%, %z%, %health%
 * - Dùng SchedulerUtil (Folia-safe).
 * - Không block main thread lâu.
 */
public final class SkillExecutor {

    private final UItemPlugin plugin;
    private final CooldownManager cooldowns;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SkillExecutor(UItemPlugin plugin, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
    }

    public CooldownManager getCooldowns() {
        return cooldowns;
    }

    // =========================================================
    // Main entry: thực thi danh sách skill phù hợp trigger
    // =========================================================

    /**
     * Chạy tất cả skill của item khớp trigger.
     * Trả về true nếu có ít nhất 1 skill được thực thi (và có thể cancel event).
     */
    public boolean executeSkills(Player player, String itemId, List<Skill> skills,
                                 SkillTrigger trigger, LivingEntity target) {
        if (skills == null || skills.isEmpty()) return false;

        boolean anyExecuted = false;
        boolean shouldCancel = false;

        for (Skill skill : skills) {
            if (skill.getTrigger() != trigger) continue;

            if (!checkChance(skill)) continue;
            if (!checkConditions(player, skill, target)) continue;
            if (!cooldowns.isReady(player, itemId, skill.getId(), skill.getCooldown())) continue;

            // Execute actions
            boolean ok = executeSkillActions(player, itemId, skill, target);
            if (ok) {
                cooldowns.markUsed(player, itemId, skill.getId());
                anyExecuted = true;
                if (skill.isCancelEvent() && trigger.isActive()) {
                    shouldCancel = true;
                }
            }
        }

        if (shouldCancel) {
            // Caller sẽ quyết định cancel event dựa trên return
        }
        return anyExecuted;
    }

    private boolean checkChance(Skill skill) {
        double ch = skill.getChance();
        if (ch >= 100) return true;
        if (ch <= 0) return false;
        return ThreadLocalRandom.current().nextDouble(100) <= ch;
    }

    private boolean checkConditions(Player player, Skill skill, LivingEntity target) {
        if (skill.getConditions() == null || skill.getConditions().isEmpty()) return true;

        for (SkillCondition cond : skill.getConditions()) {
            if (!evaluateCondition(player, cond, target)) return false;
        }
        return true;
    }

    private boolean evaluateCondition(Player player, SkillCondition cond, LivingEntity target) {
        try {
            return switch (cond.getType()) {
                case ALWAYS -> true;
                case CHANCE -> ThreadLocalRandom.current().nextDouble(100) <= cond.getDouble("value", 100);
                case PERMISSION -> {
                    String perm = cond.getString("permission", "");
                    yield perm.isEmpty() || player.hasPermission(perm);
                }
                case HEALTH_MIN -> player.getHealth() >= cond.getDouble("value", 0);
                case HEALTH_MAX -> player.getHealth() <= cond.getDouble("value", 20);
                case FOOD_MIN -> player.getFoodLevel() >= cond.getInt("value", 0);
                case SNEAKING -> player.isSneaking();
                case NOT_SNEAKING -> !player.isSneaking();
                case IN_WORLD -> {
                    String w = cond.getString("world", "");
                    yield w.isEmpty() || player.getWorld().getName().equalsIgnoreCase(w);
                }
                case HAS_OFFHAND -> {
                    ItemStack off = player.getInventory().getItemInOffHand();
                    yield off != null && !off.getType().isAir();
                }
                default -> true;
            };
        } catch (Exception e) {
            plugin.getLogger().warning("[Skill] Condition error: " + e.getMessage());
            return false;
        }
    }

    private boolean executeSkillActions(Player player, String itemId, Skill skill, LivingEntity target) {
        if (skill.getActions() == null || skill.getActions().isEmpty()) return true;

        boolean anySuccess = false;

        for (SkillAction action : skill.getActions()) {
            try {
                boolean ran = runSingleAction(player, target, action, skill);
                if (ran) anySuccess = true;
            } catch (Exception ex) {
                plugin.getLogger().warning("[Skill] Action failed for " + skill.getId() + " / " + action.getType() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return anySuccess;
    }

    // =========================================================
    // Thực thi 1 action
    // =========================================================

    private boolean runSingleAction(Player player, LivingEntity target, SkillAction action, Skill skill) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        switch (action.getType()) {
            case NOTHING -> { return true; }

            case MESSAGE -> {
                String text = replacePlaceholders(action.getStringParam("text", ""), player, target);
                player.sendMessage(mm.deserialize(text));
                return true;
            }

            case ACTION_BAR -> {
                String text = replacePlaceholders(action.getStringParam("text", ""), player, target);
                player.sendActionBar(mm.deserialize(text));
                return true;
            }

            case TITLE -> {
                String title = replacePlaceholders(action.getStringParam("title", ""), player, target);
                String sub = replacePlaceholders(action.getStringParam("subtitle", ""), player, target);
                int fadeIn = action.getIntParam("fadeIn", 10);
                int stay = action.getIntParam("stay", 40);
                int fadeOut = action.getIntParam("fadeOut", 10);
                player.showTitle(net.kyori.adventure.title.Title.title(
                        mm.deserialize(title),
                        mm.deserialize(sub),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(fadeIn * 50L),
                                java.time.Duration.ofMillis(stay * 50L),
                                java.time.Duration.ofMillis(fadeOut * 50L)
                        )
                ));
                return true;
            }

            case PLAY_SOUND -> {
                String snd = action.getStringParam("sound", "UI_BUTTON_CLICK");
                float vol = (float) action.getDoubleParam("volume", 1.0);
                float pitch = (float) action.getDoubleParam("pitch", 1.0);
                try {
                    Sound sound = Sound.valueOf(snd.toUpperCase(Locale.ROOT));
                    player.playSound(loc, sound, vol, pitch);
                } catch (IllegalArgumentException e) {
                    world.playSound(loc, snd, vol, pitch); // custom sound support
                }
                return true;
            }

            case SPAWN_PARTICLE -> {
                String p = action.getStringParam("particle", "FLAME");
                int count = action.getIntParam("count", 10);
                double off = action.getDoubleParam("offset", 0.3);
                try {
                    Particle particle = Particle.valueOf(p.toUpperCase(Locale.ROOT));
                    world.spawnParticle(particle, loc.clone().add(0, 1, 0), count, off, off, off, 0.01);
                } catch (Exception ignored) {}
                return true;
            }

            case DAMAGE -> {
                double amt = action.getDoubleParam("amount", 4.0);
                if (target != null && target.isValid()) {
                    target.damage(amt, player);
                }
                return true;
            }

            case HEAL -> {
                double amt = action.getDoubleParam("amount", 4.0);
                double newHp = Math.min(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), player.getHealth() + amt);
                player.setHealth(newHp);
                return true;
            }

            case FEED -> {
                int amt = action.getIntParam("amount", 4);
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + amt));
                return true;
            }

            case SET_FIRE -> {
                int sec = action.getIntParam("seconds", 4);
                player.setFireTicks(sec * 20);
                return true;
            }

            case LAUNCH -> {
                double x = action.getDoubleParam("x", 0);
                double y = action.getDoubleParam("y", 1.2);
                double z = action.getDoubleParam("z", 0);
                Vector v = new Vector(x, y, z);
                if (action.getBooleanParam("relative", true)) {
                    v = player.getLocation().getDirection().multiply(x).setY(y);
                }
                player.setVelocity(v);
                return true;
            }

            case TELEPORT -> {
                // relative or absolute
                double x = action.getDoubleParam("x", 0);
                double y = action.getDoubleParam("y", 0);
                double z = action.getDoubleParam("z", 0);
                if (action.getBooleanParam("relative", true)) {
                    player.teleport(loc.clone().add(x, y, z));
                } else {
                    World w = Bukkit.getWorld(action.getStringParam("world", loc.getWorld().getName()));
                    if (w != null) player.teleport(new Location(w, x, y, z));
                }
                return true;
            }

            case STRIKE_LIGHTNING -> {
                if (target != null && target.isValid()) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(5, player);
                } else {
                    world.strikeLightningEffect(loc);
                }
                return true;
            }

            case SHOOT_PROJECTILE -> {
                String proj = action.getStringParam("projectile", "FIREBALL").toUpperCase(Locale.ROOT);
                double speed = action.getDoubleParam("speed", 1.5);
                double dmg = action.getDoubleParam("damage", 6.0);

                Projectile pr;
                switch (proj) {
                    case "ARROW" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.ARROW);
                    case "SMALL_FIREBALL", "FIREBALL" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.SMALL_FIREBALL);
                    case "WITHER_SKULL" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.WITHER_SKULL);
                    case "TRIDENT" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.TRIDENT);
                    default -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.SMALL_FIREBALL);
                }
                pr.setShooter(player);
                pr.setVelocity(player.getLocation().getDirection().multiply(speed));
                if (pr instanceof AbstractArrow aa) aa.setDamage(dmg);
                else if (pr instanceof Fireball fb) fb.setYield((float) Math.min(4, dmg / 2.0));
                return true;
            }

            case PLAYER_COMMAND -> {
                String cmd = replacePlaceholders(action.getStringParam("command", ""), player, target);
                if (!cmd.isEmpty()) {
                    player.performCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
                }
                return true;
            }

            case CONSOLE_COMMAND -> {
                String cmd = replacePlaceholders(action.getStringParam("command", ""), player, target);
                if (!cmd.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.startsWith("/") ? cmd.substring(1) : cmd);
                }
                return true;
            }

            case CONSUME_DURABILITY -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d && !hand.getType().isAir()) {
                    int amt = action.getIntParam("amount", 1);
                    int newDmg = Math.min(hand.getType().getMaxDurability(), d.getDamage() + amt);
                    d.setDamage(newDmg);
                    hand.setItemMeta(d);
                    if (newDmg >= hand.getType().getMaxDurability() && !action.getBooleanParam("keep_broken", false)) {
                        player.getInventory().setItemInMainHand(null);
                    }
                }
                return true;
            }

            case CONSUME_ITEM -> {
                // Consume 1 from main hand (simple)
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (!hand.getType().isAir()) {
                    hand.setAmount(hand.getAmount() - 1);
                    if (hand.getAmount() <= 0) player.getInventory().setItemInMainHand(null);
                }
                return true;
            }

            case GIVE_POTION -> {
                String effName = action.getStringParam("effect", "SPEED").toUpperCase(Locale.ROOT);
                int duration = action.getIntParam("duration", 10); // seconds
                int amp = action.getIntParam("amplifier", 0);
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effName.toLowerCase(Locale.ROOT)));
                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, duration * 20, amp, false, true, true));
                }
                return true;
            }

            case REMOVE_POTION -> {
                String effName = action.getStringParam("effect", "").toUpperCase(Locale.ROOT);
                if (effName.isEmpty()) {
                    player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
                } else {
                    PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effName.toLowerCase(Locale.ROOT)));
                    if (type != null) player.removePotionEffect(type);
                }
                return true;
            }

            case GIVE_ITEM -> {
                // Simple: give vanilla material (advanced: support uitem later via API)
                String mat = action.getStringParam("material", "DIAMOND");
                int amt = action.getIntParam("amount", 1);
                try {
                    Material m = Material.valueOf(mat.toUpperCase(Locale.ROOT));
                    player.getInventory().addItem(new ItemStack(m, amt));
                } catch (Exception ignored) {}
                return true;
            }

            default -> { return false; }
        }
    }

    // =========================================================
    // Placeholders
    // =========================================================

    private String replacePlaceholders(String input, Player player, LivingEntity target) {
        if (input == null) return "";
        String out = input;
        out = out.replace("%player%", player.getName());
        out = out.replace("%display%", player.getDisplayName());
        out = out.replace("%health%", String.format("%.1f", player.getHealth()));
        out = out.replace("%x%", String.valueOf((int) player.getLocation().getX()));
        out = out.replace("%y%", String.valueOf((int) player.getLocation().getY()));
        out = out.replace("%z%", String.valueOf((int) player.getLocation().getZ()));
        out = out.replace("%world%", player.getWorld().getName());

        if (target != null) {
            out = out.replace("%target%", target instanceof Player p ? p.getName() : target.getType().name());
            out = out.replace("%target_health%", String.format("%.1f", target.getHealth()));
        } else {
            out = out.replace("%target%", "mục tiêu");
            out = out.replace("%target_health%", "?");
        }
        return out;
    }

    // =========================================================
    // Periodic support (gọi từ listener)
    // =========================================================

    /**
     * Chạy skill PERIODIC / WHILE_* cho player.
     * Được gọi định kỳ từ SkillListener.
     */
    public void tickPeriodicSkills(Player player, String itemId, List<Skill> skills, SkillTrigger triggerType) {
        if (skills == null) return;
        for (Skill skill : skills) {
            if (skill.getTrigger() != triggerType) continue;
            if (!cooldowns.isReady(player, itemId, skill.getId(), skill.getCooldown())) continue;
            // For periodic we use cooldown field as "minimum interval"
            if (!checkChance(skill)) continue;
            if (!checkConditions(player, skill, null)) continue;

            boolean ok = executeSkillActions(player, itemId, skill, null);
            if (ok) {
                cooldowns.markUsed(player, itemId, skill.getId());
            }
        }
    }
}
