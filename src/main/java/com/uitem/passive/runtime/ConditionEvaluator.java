package com.uitem.passive.runtime;

import com.uitem.passive.ConditionEntry;
import com.uitem.passive.ConditionGroup;
import com.uitem.passive.LogicMode;
import com.uitem.passive.condition.ConditionType;
import com.uitem.passive.trigger.TriggerContext;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ConditionEvaluator {

    private final CooldownTracker cooldownTracker;

    public ConditionEvaluator(CooldownTracker cooldownTracker) {
        this.cooldownTracker = cooldownTracker;
    }

    public boolean evaluate(ConditionGroup group, TriggerContext ctx, String passiveId) {
        if (group.entries().isEmpty()) {
            return true;
        }
        return switch (group.mode()) {
            case ALL -> group.entries().stream().allMatch(e -> testOne(e, ctx, passiveId));
            case ANY -> group.entries().stream().anyMatch(e -> testOne(e, ctx, passiveId));
            case NOT -> group.entries().stream().noneMatch(e -> testOne(e, ctx, passiveId));
        };
    }

    private boolean testOne(ConditionEntry entry, TriggerContext ctx, String passiveId) {
        return switch (entry.type()) {
            case WORLD -> world(entry.params(), ctx);
            case BIOME -> biome(entry.params(), ctx);
            case PERMISSION -> permission(entry.params(), ctx);
            case ITEM_IN_HAND -> itemInHand(entry.params(), ctx);
            case ARMOR -> armor(entry.params(), ctx);
            case HEALTH_BELOW -> healthBelow(entry.params(), ctx);
            case HEALTH_ABOVE -> healthAbove(entry.params(), ctx);
            case FOOD_LEVEL -> foodLevel(entry.params(), ctx);
            case BLOCK_TYPE -> blockType(entry.params(), ctx);
            case ENTITY_TYPE -> entityType(entry.params(), ctx);
            case CHANCE -> chance(entry.params());
            case COOLDOWN -> cooldown(entry.params(), ctx, passiveId);
            case SNEAKING -> sneaking(entry.params(), ctx);
            case FLYING -> flying(entry.params(), ctx);
            case WEATHER -> weather(entry.params(), ctx);
            case TIME -> time(entry.params(), ctx);
            case GAME_MODE -> gameMode(entry.params(), ctx);
        };
    }

    private boolean world(Map<String, Object> p, TriggerContext ctx) {
        World w = ctx.world();
        if (w == null) return false;
        String want = str(p.get("world"));
        return w.getName().equalsIgnoreCase(want);
    }

    private boolean biome(Map<String, Object> p, TriggerContext ctx) {
        var loc = ctx.location();
        if (loc == null) return false;
        String want = str(p.get("biome")).toUpperCase(Locale.ROOT);
        try {
            return loc.getBlock().getBiome().name().equalsIgnoreCase(want)
                    || loc.getBlock().getBiome().getKey().getKey().equalsIgnoreCase(want.replace("minecraft:", ""));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean permission(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        return pl.hasPermission(str(p.get("permission")));
    }

    private boolean itemInHand(Map<String, Object> p, TriggerContext ctx) {
        ItemStack hand = ctx.itemInHand();
        if (hand == null || hand.getType().isAir()) return false;
        Material want = material(p.get("material"));
        return hand.getType() == want;
    }

    private boolean armor(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        String slot = str(p.get("slot")).toLowerCase(Locale.ROOT);
        Material want = material(p.get("material"));
        ItemStack piece = switch (slot) {
            case "helmet", "head" -> pl.getInventory().getHelmet();
            case "chest", "chestplate" -> pl.getInventory().getChestplate();
            case "legs", "leggings" -> pl.getInventory().getLeggings();
            case "boots", "feet" -> pl.getInventory().getBoots();
            default -> pl.getInventory().getChestplate();
        };
        return piece != null && piece.getType() == want;
    }

    private boolean healthBelow(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        return pl.getHealth() < dbl(p.get("value"));
    }

    private boolean healthAbove(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        return pl.getHealth() > dbl(p.get("value"));
    }

    private boolean foodLevel(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        int level = pl.getFoodLevel();
        return level >= intVal(p.get("min"), 0) && level <= intVal(p.get("max"), 20);
    }

    private boolean blockType(Map<String, Object> p, TriggerContext ctx) {
        Block b = ctx.block();
        if (b == null) return false;
        return b.getType() == material(p.get("block"));
    }

    private boolean entityType(Map<String, Object> p, TriggerContext ctx) {
        Entity e = ctx.entity();
        if (e == null) return false;
        EntityType want = parseEntityType(p.get("entity"));
        return e.getType() == want;
    }

    private boolean chance(Map<String, Object> p) {
        double percent = dbl(p.get("percent"));
        if (percent >= 100) return true;
        if (percent <= 0) return false;
        return ThreadLocalRandom.current().nextDouble(100.0) < percent;
    }

    private boolean cooldown(Map<String, Object> p, TriggerContext ctx, String passiveId) {
        Player pl = ctx.player();
        if (pl == null) return false;
        int sec = intVal(p.get("seconds"), 0);
        String key = "cond";
        if (!cooldownTracker.isReadyCondition(pl.getUniqueId(), passiveId, key, sec)) {
            return false;
        }
        cooldownTracker.markCondition(pl.getUniqueId(), passiveId, key);
        return true;
    }

    private boolean sneaking(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        boolean req = bool(p.get("required"));
        return pl.isSneaking() == req;
    }

    private boolean flying(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        boolean req = bool(p.get("required"));
        return pl.isFlying() == req;
    }

    private boolean weather(Map<String, Object> p, TriggerContext ctx) {
        World w = ctx.world();
        if (w == null) return false;
        String weather = str(p.get("weather")).toLowerCase(Locale.ROOT);
        return switch (weather) {
            case "rain" -> w.hasStorm() && !w.isThundering();
            case "thunder" -> w.isThundering();
            default -> !w.hasStorm() && !w.isThundering();
        };
    }

    private boolean time(Map<String, Object> p, TriggerContext ctx) {
        World w = ctx.world();
        if (w == null) return false;
        long time = w.getTime() % 24000;
        int from = intVal(p.get("from"), 0);
        int to = intVal(p.get("to"), 24000);
        if (from <= to) {
            return time >= from && time <= to;
        }
        return time >= from || time <= to;
    }

    private boolean gameMode(Map<String, Object> p, TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return false;
        try {
            GameMode gm = GameMode.valueOf(str(p.get("mode")).toUpperCase(Locale.ROOT));
            return pl.getGameMode() == gm;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static int intVal(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(str(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double dbl(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(str(o).replace("%", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean bool(Object o) {
        if (o instanceof Boolean b) return b;
        return Boolean.parseBoolean(str(o));
    }

    private static Material material(Object o) {
        try {
            return Material.valueOf(str(o).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    private static EntityType parseEntityType(Object o) {
        try {
            return EntityType.valueOf(str(o).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return EntityType.ZOMBIE;
        }
    }
}