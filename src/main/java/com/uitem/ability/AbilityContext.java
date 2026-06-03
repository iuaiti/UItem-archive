package com.uitem.ability;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class AbilityContext {

    private final Player player;
    private final AbilityHolder holder;
    private final ItemTrigger trigger;
    private final Entity target;
    private final double damageAmount;

    public AbilityContext(Player player, AbilityHolder holder, ItemTrigger trigger, Entity target, double damageAmount) {
        this.player = player;
        this.holder = holder;
        this.trigger = trigger;
        this.target = target;
        this.damageAmount = damageAmount;
    }

    public Player player() {
        return player;
    }

    public AbilityHolder holder() {
        return holder;
    }

    public ItemTrigger trigger() {
        return trigger;
    }

    public Entity target() {
        return target;
    }

    public double damageAmount() {
        return damageAmount;
    }
}