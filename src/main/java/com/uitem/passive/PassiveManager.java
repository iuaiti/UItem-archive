package com.uitem.passive;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PassiveManager {

    private final UItemPlugin plugin;
    private final UItemManager itemManager;
    private final NamespacedKey uitemKey;
    private final MiniMessage miniMessage;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public PassiveManager(UItemPlugin plugin, UItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.uitemKey = new NamespacedKey(plugin, "uitem_id");
        this.miniMessage = MiniMessage.miniMessage();
    }

    public String getItemId(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(uitemKey, PersistentDataType.STRING);
    }

    public List<PassiveData> getPassivesForItem(String id) {
        if (id == null) return List.of();
        UItemData data = itemManager.getItem(id);
        if (data == null) return List.of();
        List<PassiveData> passives = data.getPassives();
        return passives != null ? passives : List.of();
    }

    public void executePassives(Player player, ItemStack item, String trigger, Map<String, Object> context) {
        String itemId = getItemId(item);
        if (itemId == null) return;
        executePassives(player, itemId, trigger, context);
    }

    public void executePassives(Player player, String itemId, String trigger, Map<String, Object> context) {
        List<PassiveData> passives = getPassivesForItem(itemId);
        if (passives.isEmpty()) return;

        for (PassiveData passive : passives) {
            if (!passive.getTrigger().equalsIgnoreCase(trigger)) continue;
            if (!checkCooldown(player, passive)) continue;
            if (!checkConditions(player, passive, context)) continue;
            executeAction(player, passive, context);
            applyCooldown(player, passive);
        }
    }

    private boolean checkCooldown(Player player, PassiveData passive) {
        if (passive.getCooldown() <= 0) return true;
        String key = player.getUniqueId() + "_" + passive.getType() + "_" + passive.getTrigger();
        Long lastUse = cooldowns.get(key);
        if (lastUse == null) return true;
        return (System.currentTimeMillis() - lastUse) >= (passive.getCooldown() * 1000L);
    }

    private void applyCooldown(Player player, PassiveData passive) {
        if (passive.getCooldown() <= 0) return;
        String key = player.getUniqueId() + "_" + passive.getType() + "_" + passive.getTrigger();
        cooldowns.put(key, System.currentTimeMillis());
    }

    private boolean checkConditions(Player player, PassiveData passive, Map<String, Object> context) {
        Map<String, Object> conds = passive.getConditions();
        if (conds == null || conds.isEmpty()) return true;

        if (conds.containsKey("chance")) {
            double chance = ((Number) conds.get("chance")).doubleValue();
            if (Math.random() * 100 > chance) return false;
        }

        if (conds.containsKey("health_below")) {
            double healthBelow = ((Number) conds.get("health_below")).doubleValue();
            if (player.getHealth() >= healthBelow) return false;
        }

        if (conds.containsKey("sneaking") && ((boolean) conds.get("sneaking")) != player.isSneaking()) {
            return false;
        }

        if (conds.containsKey("sprinting") && ((boolean) conds.get("sprinting")) != player.isSprinting()) {
            return false;
        }

        if (conds.containsKey("world")) {
            String worldName = conds.get("world").toString();
            if (!player.getWorld().getName().equalsIgnoreCase(worldName)) return false;
        }

        if (conds.containsKey("biome")) {
            String biomeName = conds.get("biome").toString();
            String currentBiome = player.getLocation().getBlock().getBiome().getKey().getKey();
            if (!currentBiome.equalsIgnoreCase(biomeName)) return false;
        }

        if (conds.containsKey("time")) {
            long time = player.getWorld().getTime();
            String timeCondition = conds.get("time").toString().toLowerCase(Locale.ROOT);
            switch (timeCondition) {
                case "day" -> { if (time < 0 || time > 12000) return false; }
                case "night" -> { if (time >= 12000 && time <= 18000) return false; }
                case "dawn" -> { if (time < 22000 || time > 24000) return false; }
                case "dusk" -> { if (time < 12000 || time > 14000) return false; }
            }
        }

        if (conds.containsKey("weather")) {
            World world = player.getWorld();
            String weather = conds.get("weather").toString().toLowerCase(Locale.ROOT);
            switch (weather) {
                case "clear" -> { if (world.hasStorm() || world.isThundering()) return false; }
                case "rain" -> { if (!world.hasStorm()) return false; }
                case "thunder" -> { if (!world.isThundering()) return false; }
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private void executeAction(Player player, PassiveData passive, Map<String, Object> context) {
        String type = passive.getType().toUpperCase(Locale.ROOT);
        Map<String, Object> conds = passive.getConditions();

        LivingEntity target = null;
        if (context != null) {
            Object targetObj = context.get("target");
            if (targetObj instanceof LivingEntity le) target = le;
        }

        switch (type) {
            case "LIGHTNING" -> {
                Location loc = target != null ? target.getLocation() : player.getTargetBlock(null, 50).getLocation().add(0.5, 1, 0.5);
                player.getWorld().strikeLightningEffect(loc);
            }
            case "FIRE" -> {
                int ticks = conds != null && conds.containsKey("ticks") ? ((Number) conds.get("ticks")).intValue() : 40;
                if (target != null) target.setFireTicks(ticks);
                else player.setFireTicks(ticks);
            }
            case "FREEZE" -> {
                int ticks = conds != null && conds.containsKey("ticks") ? ((Number) conds.get("ticks")).intValue() : 100;
                if (target != null) target.setFreezeTicks(ticks);
                else player.setFreezeTicks(ticks);
            }
            case "POTION" -> {
                if (conds != null && conds.containsKey("effect")) {
                    String effectName = conds.get("effect").toString();
                    int duration = conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 100;
                    int amplifier = conds.containsKey("amplifier") ? ((Number) conds.get("amplifier")).intValue() : 0;
                    PotionEffectType effectType = PotionEffectType.getByKey(NamespacedKey.fromString(effectName.toLowerCase(Locale.ROOT)));
                    if (effectType != null) {
                        LivingEntity applyTo = conds.containsKey("self") && !((boolean) conds.get("self")) ? target : player;
                        if (applyTo != null) {
                            applyTo.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, true, true));
                        }
                    }
                }
            }
            case "HEAL" -> {
                double amount = conds != null && conds.containsKey("amount") ? ((Number) conds.get("amount")).doubleValue() : 4.0;
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
            }
            case "FEED" -> {
                int food = conds != null && conds.containsKey("food") ? ((Number) conds.get("food")).intValue() : 6;
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + food));
            }
            case "SATURATION" -> {
                float sat = conds != null && conds.containsKey("amount") ? ((Number) conds.get("amount")).floatValue() : 5;
                player.setSaturation(Math.min(20, player.getSaturation() + sat));
            }
            case "SPEED" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 200;
                int amp = conds != null && conds.containsKey("amplifier") ? ((Number) conds.get("amplifier")).intValue() : 1;
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, amp, false, true, true));
            }
            case "JUMP" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 200;
                int amp = conds != null && conds.containsKey("amplifier") ? ((Number) conds.get("amplifier")).intValue() : 1;
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, dur, amp, false, true, true));
            }
            case "GLOW" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 100;
                int amp = conds != null && conds.containsKey("amplifier") ? ((Number) conds.get("amplifier")).intValue() : 0;
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, dur, amp, false, true, true));
            }
            case "INVISIBILITY" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 200;
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, dur, 0, false, true, true));
            }
            case "KNOCKBACK" -> {
                double force = conds != null && conds.containsKey("force") ? ((Number) conds.get("force")).doubleValue() : 1.5;
                if (target != null) {
                    Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(force);
                    knockback.setY(0.4);
                    target.setVelocity(knockback);
                }
            }
            case "DAMAGE_MULTIPLIER" -> {
                // Handled in PassiveListener via context
                if (context != null) context.put("damage_multiplier",
                        conds != null && conds.containsKey("multiplier") ? ((Number) conds.get("multiplier")).doubleValue() : 1.5);
            }
            case "LIFE_STEAL" -> {
                double percent = conds != null && conds.containsKey("percent") ? ((Number) conds.get("percent")).doubleValue() : 10;
                if (context != null && context.containsKey("damage")) {
                    double damage = ((Number) context.get("damage")).doubleValue();
                    double heal = damage * percent / 100.0;
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
                }
            }
            case "CRIT" -> {
                // Force critical hit handled via context
                if (context != null) context.put("force_crit", true);
            }
            case "AUTO_SMELT" -> {
                if (context != null) context.put("auto_smelt", true);
            }
            case "XP_BOOST" -> {
                double multiplier = conds != null && conds.containsKey("multiplier") ? ((Number) conds.get("multiplier")).doubleValue() : 2.0;
                if (context != null) context.put("xp_boost", multiplier);
            }
            case "REPAIR" -> {
                int amount = conds != null && conds.containsKey("amount") ? ((Number) conds.get("amount")).intValue() : 10;
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && item.getType() != Material.AIR && item.getItemMeta() instanceof Damageable damageable) {
                    int newDamage = Math.max(0, damageable.getDamage() - amount);
                    damageable.setDamage(newDamage);
                    item.setItemMeta((ItemMeta) damageable);
                }
            }
            case "NIGHT_VISION" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 600;
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, dur, 0, false, true, true));
            }
            case "WATER_BREATHING" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 600;
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, dur, 0, false, true, true));
            }
            case "FIRE_RESISTANCE" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 600;
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, dur, 0, false, true, true));
            }
            case "ABSORPTION" -> {
                int dur = conds != null && conds.containsKey("duration") ? ((Number) conds.get("duration")).intValue() : 200;
                int amp = conds != null && conds.containsKey("amplifier") ? ((Number) conds.get("amplifier")).intValue() : 0;
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, dur, amp, false, true, true));
            }
            case "PARTICLE" -> {
                if (conds != null && conds.containsKey("particle")) {
                    String particleName = conds.get("particle").toString();
                    int count = conds.containsKey("count") ? ((Number) conds.get("count")).intValue() : 10;
                    try {
                        org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
                        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.05);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case "SOUND" -> {
                if (conds != null && conds.containsKey("sound")) {
                    String soundName = conds.get("sound").toString();
                    try {
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                        float volume = conds.containsKey("volume") ? ((Number) conds.get("volume")).floatValue() : 1.0f;
                        float pitch = conds.containsKey("pitch") ? ((Number) conds.get("pitch")).floatValue() : 1.0f;
                        player.getWorld().playSound(player.getLocation(), sound, volume, pitch);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case "MESSAGE" -> {
                if (conds != null && conds.containsKey("message")) {
                    String msg = conds.get("message").toString();
                    player.sendMessage(miniMessage.deserialize(msg));
                }
            }
            case "TELEPORT" -> {
                if (conds != null && conds.containsKey("location")) {
                    String locStr = conds.get("location").toString();
                    String[] parts = locStr.split(",");
                    if (parts.length >= 3) {
                        try {
                            double x = Double.parseDouble(parts[0].trim());
                            double y = Double.parseDouble(parts[1].trim());
                            double z = Double.parseDouble(parts[2].trim());
                            player.teleportAsync(new Location(player.getWorld(), x, y, z));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            case "EXPLODE" -> {
                float power = conds != null && conds.containsKey("power") ? ((Number) conds.get("power")).floatValue() : 2.0f;
                boolean breakBlocks = conds == null || !conds.containsKey("break-blocks") || ((boolean) conds.get("break-blocks"));
                player.getWorld().createExplosion(player.getLocation(), power, false, breakBlocks);
            }
            case "IGNITE_NEARBY" -> {
                int radius = conds != null && conds.containsKey("radius") ? ((Number) conds.get("radius")).intValue() : 3;
                int fireTicks = conds != null && conds.containsKey("ticks") ? ((Number) conds.get("ticks")).intValue() : 40;
                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity le && !le.equals(player)) {
                        le.setFireTicks(fireTicks);
                    }
                }
            }
            case "PULL_ENTITY" -> {
                double force = conds != null && conds.containsKey("force") ? ((Number) conds.get("force")).doubleValue() : 1.5;
                if (target != null) {
                    Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(force);
                    pull.setY(0.3);
                    target.setVelocity(pull);
                }
            }
            case "PUSH_ENTITY" -> {
                double force = conds != null && conds.containsKey("force") ? ((Number) conds.get("force")).doubleValue() : 1.5;
                if (target != null) {
                    Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(force);
                    push.setY(0.3);
                    target.setVelocity(push);
                }
            }
        }
    }

    public String getPassiveDescription(PassiveData passive) {
        String typeName = formatTypeName(passive.getType());
        StringBuilder sb = new StringBuilder();
        sb.append("<gray>✦ ").append(typeName);
        Map<String, Object> conds = passive.getConditions();
        if (conds != null) {
            if (conds.containsKey("chance")) {
                sb.append(" <dark_gray>(").append(conds.get("chance")).append("%)");
            }
            if (conds.containsKey("ticks") && "FIRE".equalsIgnoreCase(passive.getType())) {
                sb.append(" <red>").append(conds.get("ticks") + " ticks");
            }
        }
        if (passive.getCooldown() > 0) {
            sb.append(" <dark_gray>⏱").append(passive.getCooldown()).append("s");
        }
        return sb.toString();
    }

    private String formatTypeName(String type) {
        if (type == null || type.isEmpty()) return "";
        String lower = type.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "lightning" -> "<gold>⚡ Lightning";
            case "fire" -> "<red>🔥 Fire";
            case "freeze" -> "<aqua>❄ Freeze";
            case "potion" -> "<light_purple>🧪 Potion";
            case "heal" -> "<green>❤ Heal";
            case "feed" -> "<gold>🍖 Feed";
            case "saturation" -> "<gold>🥩 Saturation";
            case "speed" -> "<white>💨 Speed";
            case "jump" -> "<green>🦘 Jump";
            case "glow" -> "<yellow>✨ Glow";
            case "invisibility" -> "<gray>👻 Invisibility";
            case "knockback" -> "<red>💥 Knockback";
            case "damage_multiplier" -> "<red>⚔ Damage Multiplier";
            case "life_steal" -> "<dark_red>💀 Life Steal";
            case "crit" -> "<gold>✦ Critical";
            case "auto_smelt" -> "<gold>🔥 Auto Smelt";
            case "xp_boost" -> "<green>✨ XP Boost";
            case "repair" -> "<aqua>🔧 Repair";
            case "night_vision" -> "<dark_green>👁 Night Vision";
            case "water_breathing" -> "<blue>🌊 Water Breathing";
            case "fire_resistance" -> "<gold>🔥 Fire Resistance";
            case "absorption" -> "<gold>🛡 Absorption";
            case "particle" -> "<light_purple>✨ Particle";
            case "sound" -> "<gray>🔔 Sound";
            case "message" -> "<white>💬 Message";
            case "teleport" -> "<dark_purple>🌀 Teleport";
            case "explode" -> "<red>💥 Explode";
            case "ignite_nearby" -> "<red>🔥 Ignite Nearby";
            case "pull_entity" -> "<dark_aqua>🔄 Pull Entity";
            case "push_entity" -> "<aqua>➡ Push Entity";
            default -> "<gray>" + type;
        };
    }
}
