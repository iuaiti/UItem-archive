package com.uitem.passive.runtime;

import com.uitem.UItemPlugin;
import com.uitem.passive.ActionEntry;
import com.uitem.passive.trigger.TriggerContext;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ActionExecutor {

    private final UItemPlugin plugin;

    public ActionExecutor(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void executeAll(TriggerContext ctx, List<ActionEntry> actions) {
        long delay = 0;
        for (ActionEntry entry : actions) {
            delay += entry.delayTicks();
            long runDelay = delay;
            Runnable task = () -> executeOne(ctx, entry);
            if (runDelay <= 0) {
                task.run();
            } else {
                SchedulerUtil.runLater(plugin, task, runDelay);
            }
        }
    }

    private void executeOne(TriggerContext ctx, ActionEntry entry) {
        Map<String, Object> p = entry.params();
        switch (entry.type()) {
            case SEND_MESSAGE -> message(ctx, p);
            case EXECUTE_COMMAND -> command(ctx, p);
            case GIVE_ITEM -> giveItem(ctx, p);
            case REMOVE_ITEM -> removeItem(ctx, p);
            case HEAL -> heal(ctx, p);
            case DAMAGE -> damage(ctx, p);
            case ADD_EXPERIENCE -> exp(ctx, p);
            case POTION_EFFECT -> potion(ctx, p);
            case SOUND -> sound(ctx, p);
            case PARTICLE -> particle(ctx, p);
            case TELEPORT -> teleport(ctx, p);
            case LIGHTNING -> lightning(ctx, p);
            case FIREWORK -> firework(ctx);
            case BROADCAST -> broadcast(p);
            case ACTION_BAR -> actionBar(ctx, p);
            case TITLE -> title(ctx, p);
        }
    }

    private void message(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        pl.sendMessage(TextUtil.parse(replace(str(p.get("message")), pl)));
    }

    private void command(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        String cmd = replace(str(p.get("command")), pl);
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        String as = str(p.get("as")).toLowerCase(Locale.ROOT);
        switch (as) {
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            case "op" -> {
                if (pl != null) {
                    boolean wasOp = pl.isOp();
                    pl.setOp(true);
                    pl.performCommand(cmd);
                    pl.setOp(wasOp);
                }
            }
            default -> {
                if (pl != null) {
                    pl.performCommand(cmd);
                }
            }
        }
    }

    private void giveItem(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        Material mat = material(p.get("material"));
        int amount = intVal(p.get("amount"), 1);
        pl.getInventory().addItem(new ItemStack(mat, Math.min(64, Math.max(1, amount))));
    }

    private void removeItem(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        Material mat = material(p.get("material"));
        int amount = intVal(p.get("amount"), 1);
        pl.getInventory().removeItem(new ItemStack(mat, amount));
    }

    private void heal(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        double amt = dbl(p.get("amount"));
        double max = 20.0;
        var maxAttr = pl.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr != null) {
            max = maxAttr.getValue();
        }
        pl.setHealth(Math.min(max, pl.getHealth() + amt));
    }

    private void damage(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        pl.damage(dbl(p.get("amount")));
    }

    private void exp(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        if (bool(p.get("levels"))) {
            pl.setLevel(pl.getLevel() + intVal(p.get("amount"), 1));
        } else {
            pl.giveExp(intVal(p.get("amount"), 10));
        }
    }

    private void potion(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        PotionEffectType type = PotionEffectType.getByName(str(p.get("effect")).toUpperCase(Locale.ROOT));
        if (type == null) return;
        int sec = intVal(p.get("duration"), 30);
        int amp = intVal(p.get("amplifier"), 0);
        pl.addPotionEffect(new PotionEffect(type, sec * 20, amp));
    }

    private void sound(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        try {
            Sound s = Sound.valueOf(str(p.get("sound")).toUpperCase(Locale.ROOT));
            pl.playSound(pl.getLocation(), s, (float) dbl(p.get("volume")), (float) dbl(p.get("pitch")));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void particle(TriggerContext ctx, Map<String, Object> p) {
        Location loc = ctx.location();
        if (loc == null || loc.getWorld() == null) return;
        try {
            Particle part = Particle.valueOf(str(p.get("particle")).toUpperCase(Locale.ROOT));
            loc.getWorld().spawnParticle(part, loc, 20);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void teleport(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        World w = Bukkit.getWorld(str(p.get("world")));
        if (w == null) return;
        pl.teleport(new Location(w, dbl(p.get("x")), dbl(p.get("y")), dbl(p.get("z"))));
    }

    private void lightning(TriggerContext ctx, Map<String, Object> p) {
        Location loc = ctx.location();
        if (loc == null || loc.getWorld() == null) return;
        if (bool(p.get("at_target"))) {
            loc.getWorld().strikeLightning(loc);
        } else if (ctx.player() != null) {
            loc.getWorld().strikeLightning(ctx.player().getLocation());
        }
    }

    private void firework(TriggerContext ctx) {
        Player pl = ctx.player();
        if (pl == null) return;
        pl.getWorld().spawn(pl.getLocation(), org.bukkit.entity.Firework.class);
    }

    private void broadcast(Map<String, Object> p) {
        Bukkit.broadcast(TextUtil.parse(str(p.get("message"))));
    }

    private void actionBar(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        pl.sendActionBar(TextUtil.parse(replace(str(p.get("message")), pl)));
    }

    private void title(TriggerContext ctx, Map<String, Object> p) {
        Player pl = ctx.player();
        if (pl == null) return;
        Title.Times times = Title.Times.times(
                Duration.ofMillis(intVal(p.get("fade_in"), 10) * 50L),
                Duration.ofMillis(intVal(p.get("stay"), 40) * 50L),
                Duration.ofMillis(intVal(p.get("fade_out"), 10) * 50L));
        pl.showTitle(Title.title(
                TextUtil.parse(replace(str(p.get("title")), pl)),
                TextUtil.parse(replace(str(p.get("subtitle")), pl)),
                times));
    }

    private static String replace(String s, Player pl) {
        if (s == null) return "";
        return s.replace("%player%", pl.getName());
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
            return Double.parseDouble(str(o));
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
}