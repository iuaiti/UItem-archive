package com.uitem.passive;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PassiveManager {

    private static PassiveManager instance;

    private final UItemPlugin plugin;
    private final UItemManager itemManager;
    private final NamespacedKey uitemKey;
    private final MiniMessage miniMessage;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public PassiveManager(UItemPlugin plugin, UItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.uitemKey = itemManager.getUItemKey();
        this.miniMessage = MiniMessage.miniMessage();
        instance = this;
    }

    public static PassiveManager getInstance() {
        return instance;
    }

    public static void clearInstance() {
        instance = null;
    }

    private boolean isDebug() {
        return plugin.getConfig().getBoolean("passive.debug", false)
                || plugin.getConfig().getBoolean("debug", false);
    }

    public String getItemId(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc == null) {
            if (isDebug()) {
                plugin.getLogger().warning("[Passive] PDC is null for item: " + item.getType());
            }
            return null;
        }

        String id = pdc.get(uitemKey, PersistentDataType.STRING);
        if (id == null && isDebug()) {
            plugin.getLogger().warning("[Passive] No uitem_id PDC tag found on " + item.getType()
                    + "! Item may have been obtained via /give or Creative menu instead of /uitem give.");
        }
        return id;
    }

    public List<PassiveData> getPassivesForItem(String id) {
        if (id == null) return List.of();
        UItemData data = itemManager.getItem(id);
        if (data == null) {
            plugin.getLogger().warning("[Passive] Không tìm thấy item '" + id + "' trong manager! Danh sách items: " + itemManager.getAllItems().keySet());
            return List.of();
        }
        List<PassiveData> passives = data.getPassives();
        if ((passives == null || passives.isEmpty()) && isDebug()) {
            plugin.getLogger().info("[Passive] Item '" + id + "' has no passives defined.");
        }
        return passives != null ? passives : List.of();
    }

    public void executePassives(Player player, ItemStack item, String trigger, Map<String, Object> context) {
        try {
            executePassives(player, item, PassiveTrigger.valueOf(trigger.toUpperCase(Locale.ROOT)), context);
        } catch (IllegalArgumentException e) {
            if (isDebug()) {
                plugin.getLogger().warning("[Passive] Invalid trigger string: " + trigger);
            }
        }
    }

    public void executePassives(Player player, ItemStack item, PassiveTrigger trigger, Map<String, Object> context) {
        String itemId = getItemId(item);
        if (itemId == null) {
            if (isDebug()) {
                plugin.getLogger().info("[Passive] No uitem_id in PDC for " + player.getName()
                        + " trigger=" + trigger + " item=" + item.getType());
            }
            return;
        }
        if (isDebug()) {
            plugin.getLogger().info("[Passive] Trigger " + trigger + " for " + player.getName() + " item=" + itemId);
        }
        executePassives(player, itemId, trigger, context);
    }

    public void executePassives(Player player, String itemId, String triggerStr, Map<String, Object> context) {
        try {
            executePassives(player, itemId, PassiveTrigger.valueOf(triggerStr.toUpperCase(Locale.ROOT)), context);
        } catch (IllegalArgumentException e) {
            if (isDebug()) {
                plugin.getLogger().warning("[Passive] Invalid trigger string: " + triggerStr);
            }
        }
    }

    public void executePassives(Player player, String itemId, PassiveTrigger trigger, Map<String, Object> context) {
        boolean debug = isDebug();
        List<PassiveData> passives = getPassivesForItem(itemId);
        if (passives.isEmpty()) {
            if (debug) {
                plugin.getLogger().info("[Passive] No passives for " + itemId + " trigger=" + trigger);
            }
            return;
        }

        if (debug) {
            plugin.getLogger().info("[Passive] Executing " + passives.size() + " passive(s) for "
                    + player.getName() + " item=" + itemId + " trigger=" + trigger);
        }

        for (PassiveData passive : passives) {
            try {
                PassiveTrigger passiveTrigger;
                try {
                    passiveTrigger = PassiveTrigger.valueOf(passive.getTrigger().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (passiveTrigger != trigger) continue;
                if (context == null) context = new HashMap<>();
                if (!checkCooldown(player, itemId, passive)) {
                    if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": on cooldown");
                    continue;
                }
                if (!checkConditions(player, passive, context)) {
                    if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": conditions not met");
                    continue;
                }
                if (debug) plugin.getLogger().info("[Passive] EXECUTE " + passive.getType() + " for " + player.getName());
                executeAction(player, passive, context);
                applyCooldown(player, itemId, passive);
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing passive " + passive.getType()
                        + " for item " + itemId + " on player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean checkCooldown(Player player, String itemId, PassiveData passive) {
        if (passive.getCooldown() <= 0) return true;
        String key = player.getUniqueId() + "_" + itemId + "_" + passive.getType() + "_" + passive.getTrigger();
        Long lastUse = cooldowns.get(key);
        if (lastUse == null) {
            lastUse = loadCooldownFromPDC(player, passive);
            if (lastUse != null) cooldowns.put(key, lastUse);
        }
        if (lastUse == null) return true;
        return (System.currentTimeMillis() - lastUse) >= (passive.getCooldown() * 1000L);
    }

    private void applyCooldown(Player player, String itemId, PassiveData passive) {
        if (passive.getCooldown() <= 0) return;
        String key = player.getUniqueId() + "_" + itemId + "_" + passive.getType() + "_" + passive.getTrigger();
        long now = System.currentTimeMillis();
        cooldowns.put(key, now);
        saveCooldownToPDC(player, passive, now);
    }

    private NamespacedKey cooldownKey(Player player, PassiveData passive) {
        String cdKey = "uitem_cd_" + passive.getType() + "_" + passive.getTrigger();
        return new NamespacedKey(plugin, cdKey);
    }

    private void saveCooldownToPDC(Player player, PassiveData passive, long timestamp) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(cooldownKey(player, passive), PersistentDataType.LONG, timestamp);
    }

    public void clearCooldownsFor(Player player) {
        String prefix = player.getUniqueId() + "_";
        cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private Long loadCooldownFromPDC(Player player, PassiveData passive) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (pdc.has(cooldownKey(player, passive), PersistentDataType.LONG)) {
            return pdc.get(cooldownKey(player, passive), PersistentDataType.LONG);
        }
        return null;
    }

    private double parseDouble(Map<String, Object> conds, String key, double defaultVal) {
        Object val = conds.get(key);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    private int parseInt(Map<String, Object> conds, String key, int defaultVal) {
        Object val = conds.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    private boolean parseBoolean(Map<String, Object> conds, String key, boolean defaultVal) {
        Object val = conds.get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        if (val instanceof Number n) return n.intValue() != 0;
        return defaultVal;
    }

    private boolean checkConditions(Player player, PassiveData passive, Map<String, Object> context) {
        Map<String, Object> conds = passive.getConditions();
        if (conds == null || conds.isEmpty()) return true;
        boolean debug = isDebug();

        if (conds.containsKey("chance")) {
            double chance = parseDouble(conds, "chance", 100.0);
            if (Math.random() * 100 > chance) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": chance failed (" + chance + "%)");
                return false;
            }
        }

        if (conds.containsKey("health_below")) {
            double healthBelow = parseDouble(conds, "health_below", player.getMaxHealth());
            if (player.getHealth() >= healthBelow) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": health below (" + player.getHealth() + " >= " + healthBelow + ")");
                return false;
            }
        }

        if (conds.containsKey("sneaking")) {
            if (parseBoolean(conds, "sneaking", false) != player.isSneaking()) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": sneaking mismatch");
                return false;
            }
        }

        if (conds.containsKey("sprinting")) {
            if (parseBoolean(conds, "sprinting", false) != player.isSprinting()) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": sprinting mismatch");
                return false;
            }
        }

        if (conds.containsKey("world")) {
            String worldName = conds.get("world").toString();
            if (!player.getWorld().getName().equalsIgnoreCase(worldName)) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": world mismatch");
                return false;
            }
        }

        if (conds.containsKey("biome")) {
            String biomeName = conds.get("biome").toString().toLowerCase(Locale.ROOT);
            String currentBiome = player.getLocation().getBlock().getBiome().getKey().toString();
            String configBiome = biomeName.contains(":") ? biomeName : "minecraft:" + biomeName;
            if (!currentBiome.equalsIgnoreCase(configBiome)) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": biome mismatch");
                return false;
            }
        }

        if (conds.containsKey("time")) {
            long time = player.getWorld().getTime();
            String timeCondition = conds.get("time").toString().toLowerCase(Locale.ROOT);
            boolean timeMatch = switch (timeCondition) {
                case "day" -> time >= 0 && time <= 12000;
                case "night" -> time >= 13000;
                case "dawn" -> time >= 22000 || time <= 2000;
                case "dusk" -> time >= 12000 && time <= 14000;
                default -> true;
            };
            if (!timeMatch) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": time mismatch (" + timeCondition + ", world time=" + time + ")");
                return false;
            }
        }

        if (conds.containsKey("weather")) {
            World world = player.getWorld();
            String weather = conds.get("weather").toString().toLowerCase(Locale.ROOT);
            boolean weatherMatch = switch (weather) {
                case "clear" -> !world.hasStorm() && !world.isThundering();
                case "rain" -> world.hasStorm();
                case "thunder" -> world.isThundering();
                default -> true;
            };
            if (!weatherMatch) {
                if (debug) plugin.getLogger().fine("[Passive] Skip " + passive.getType() + ": weather mismatch (" + weather + ")");
                return false;
            }
        }

        return true;
    }

    private boolean condBoolean(Map<String, Object> conds, String key) {
        if (conds == null || !conds.containsKey(key)) return false;
        return parseBoolean(conds, key, false);
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
                        boolean self = !conds.containsKey("self") || condBoolean(conds, "self");
                        LivingEntity applyTo = self ? player : target;
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
                double multiplier = conds != null && conds.containsKey("multiplier")
                        ? ((Number) conds.get("multiplier")).doubleValue() : 1.5;
                context.compute("damage_multiplier", (k, v) -> v != null ? ((Number) v).doubleValue() * multiplier : multiplier);
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
                boolean breakBlocks = conds == null || !conds.containsKey("break-blocks") || condBoolean(conds, "break-blocks");
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
            case "TNT_MINER" -> {
                if (context != null && context.containsKey("block")) {
                    org.bukkit.block.Block block = (org.bukkit.block.Block) context.get("block");
                    float power = conds != null && conds.containsKey("power") ? ((Number) conds.get("power")).floatValue() : 2.0f;
                    boolean breakBlocks = conds == null || !conds.containsKey("break-blocks") || condBoolean(conds, "break-blocks");
                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    player.setInvulnerable(true);
                    player.getWorld().createExplosion(loc, power, false, breakBlocks);
                    SchedulerUtil.runSync(plugin, () -> player.setInvulnerable(false));
                }
            }
            case "TREE_FELLER" -> {
                if (context != null && context.containsKey("block")) {
                    org.bukkit.block.Block block = (org.bukkit.block.Block) context.get("block");
                    int maxLogs = conds != null && conds.containsKey("max_logs") ? ((Number) conds.get("max_logs")).intValue() : 200;
                    if (isLog(block.getType())) {
                        Set<org.bukkit.block.Block> logs = new HashSet<>();
                        ArrayDeque<org.bukkit.block.Block> queue = new ArrayDeque<>();
                        queue.add(block);
                        while (!queue.isEmpty() && logs.size() < maxLogs) {
                            org.bukkit.block.Block current = queue.poll();
                            if (!isLog(current.getType())) continue;
                            if (!logs.add(current)) continue;
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dy = -1; dy <= 1; dy++) {
                                    for (int dz = -1; dz <= 1; dz++) {
                                        if (dx == 0 && dy == 0 && dz == 0) continue;
                                        org.bukkit.block.Block neighbor = current.getRelative(dx, dy, dz);
                                        if (isLog(neighbor.getType()) && !logs.contains(neighbor)) {
                                            queue.add(neighbor);
                                        }
                                    }
                                }
                            }
                        }
                        ItemStack tool = player.getInventory().getItemInMainHand();
                        for (org.bukkit.block.Block log : logs) {
                            log.setType(Material.AIR);
                            for (ItemStack drop : log.getDrops(tool)) {
                                log.getWorld().dropItemNaturally(log.getLocation(), drop);
                            }
                        }
                    }
                }
            }
            case "MACE_LIGHTNING" -> {
                Location loc = target != null ? target.getLocation() : player.getTargetBlock(null, 50).getLocation().add(0.5, 1, 0.5);
                player.getWorld().strikeLightningEffect(loc);
                if (target != null) {
                    target.getWorld().strikeLightning(target.getLocation());
                }
            }
            case "MACE_TNT" -> {
                Location loc = target != null ? target.getLocation() : player.getLocation();
                float power = conds != null && conds.containsKey("power") ? ((Number) conds.get("power")).floatValue() : 2.0f;
                boolean breakBlocks = conds == null || !conds.containsKey("break-blocks") || condBoolean(conds, "break-blocks");
                player.getWorld().createExplosion(loc, power, false, breakBlocks);
            }
        }
    }

    private boolean isLog(Material material) {
        return switch (material) {
            case OAK_LOG, OAK_WOOD, STRIPPED_OAK_LOG, STRIPPED_OAK_WOOD,
                 SPRUCE_LOG, SPRUCE_WOOD, STRIPPED_SPRUCE_LOG, STRIPPED_SPRUCE_WOOD,
                 BIRCH_LOG, BIRCH_WOOD, STRIPPED_BIRCH_LOG, STRIPPED_BIRCH_WOOD,
                 JUNGLE_LOG, JUNGLE_WOOD, STRIPPED_JUNGLE_LOG, STRIPPED_JUNGLE_WOOD,
                 ACACIA_LOG, ACACIA_WOOD, STRIPPED_ACACIA_LOG, STRIPPED_ACACIA_WOOD,
                 DARK_OAK_LOG, DARK_OAK_WOOD, STRIPPED_DARK_OAK_LOG, STRIPPED_DARK_OAK_WOOD,
                 MANGROVE_LOG, MANGROVE_WOOD, STRIPPED_MANGROVE_LOG, STRIPPED_MANGROVE_WOOD,
                 CHERRY_LOG, CHERRY_WOOD, STRIPPED_CHERRY_LOG, STRIPPED_CHERRY_WOOD,
                 CRIMSON_STEM, STRIPPED_CRIMSON_STEM, CRIMSON_HYPHAE, STRIPPED_CRIMSON_HYPHAE,
                 WARPED_STEM, STRIPPED_WARPED_STEM, WARPED_HYPHAE, STRIPPED_WARPED_HYPHAE -> true;
            default -> false;
        };
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
            case "tnt_miner" -> "<red>💥 TNT Miner";
            case "tree_feller" -> "<green>🌲 Tree Feller";
            case "mace_lightning" -> "<gold>⚡ Mace Lightning";
            case "mace_tnt" -> "<red>💥 Mace TNT";
            default -> "<gray>" + type;
        };
    }
}
