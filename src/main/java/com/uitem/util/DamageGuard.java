package com.uitem.util;

import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Re-entrancy guard shared by every UItem listener that can deal damage from
 * inside a damage event (skill {@code DAMAGE} actions, {@code REFLECT},
 * {@code EXTRA_DAMAGE}, ...).
 *
 * <p>Without a guard the chain {@code HIT -> DAMAGE action -> target.damage()
 * -> EntityDamageByEntityEvent -> HIT -> ...} recurses until the stack blows.
 *
 * <p>The state is held in a {@link ThreadLocal} rather than a plain field for
 * two reasons:
 * <ul>
 *   <li>On Folia each region ticks on its own thread, so a shared boolean would
 *       both race and let one region silently swallow another region's
 *       triggers.</li>
 *   <li>Bukkit event dispatch is synchronous, so the whole recursive chain
 *       always runs on the thread that opened the guard — a ThreadLocal is
 *       therefore exactly the right scope.</li>
 * </ul>
 *
 * <p>The guard is keyed by the <em>damager</em>, not global: player A hitting a
 * mob must never suppress player B's unrelated hit in the same tick.
 */
public final class DamageGuard {

    private static final ThreadLocal<Set<UUID>> ACTIVE = ThreadLocal.withInitial(HashSet::new);

    private DamageGuard() {
    }

    /**
     * Try to enter the guarded section for {@code damager}.
     *
     * @return {@code true} when the caller acquired the guard and MUST call
     *         {@link #exit(Entity)} in a {@code finally} block; {@code false}
     *         when this damager is already being processed further up the
     *         stack, in which case the caller must return immediately.
     */
    public static boolean enter(Entity damager) {
        if (damager == null) return true;
        return ACTIVE.get().add(damager.getUniqueId());
    }

    /** Leave the guarded section opened by {@link #enter(Entity)}. */
    public static void exit(Entity damager) {
        if (damager == null) return;
        Set<UUID> active = ACTIVE.get();
        active.remove(damager.getUniqueId());
        // Don't leak the (usually empty) set into the thread's TLS map forever.
        if (active.isEmpty()) ACTIVE.remove();
    }

    /** True when {@code damager} is currently inside a guarded section. */
    public static boolean isProcessing(Entity damager) {
        return damager != null && ACTIVE.get().contains(damager.getUniqueId());
    }
}
