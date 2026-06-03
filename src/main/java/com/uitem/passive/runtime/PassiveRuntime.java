package com.uitem.passive.runtime;

import com.uitem.UItemPlugin;
import com.uitem.passive.PassiveDefinition;
import com.uitem.passive.trigger.TriggerContext;

import java.util.concurrent.ThreadLocalRandom;

public final class PassiveRuntime {

    private final UItemPlugin plugin;
    private final ConditionEvaluator conditionEvaluator;
    private final ActionExecutor actionExecutor;
    private final CooldownTracker cooldownTracker;

    public PassiveRuntime(UItemPlugin plugin, CooldownTracker cooldownTracker) {
        this.plugin = plugin;
        this.cooldownTracker = cooldownTracker;
        this.conditionEvaluator = new ConditionEvaluator(cooldownTracker);
        this.actionExecutor = new ActionExecutor(plugin);
    }

    public void execute(PassiveDefinition passive, TriggerContext ctx) {
        if (!passive.enabled()) {
            return;
        }
        if (passive.trigger().requiresPlayer() && ctx.player() == null) {
            return;
        }

        var adv = passive.advanced();
        if (adv.globalChancePercent() < 100) {
            if (ThreadLocalRandom.current().nextDouble(100.0) >= adv.globalChancePercent()) {
                return;
            }
        }

        if (ctx.player() != null) {
            int cd = adv.cooldownSeconds();
            if (!cooldownTracker.isReady(ctx.player().getUniqueId(), passive.id(), cd)) {
                return;
            }
        }

        if (!conditionEvaluator.evaluate(passive.conditions(), ctx, passive.id())) {
            return;
        }

        if (ctx.player() != null) {
            cooldownTracker.markFired(ctx.player().getUniqueId(), passive.id());
        }

        Runnable run = () -> actionExecutor.executeAll(ctx, passive.actions());
        int delay = adv.delayTicks();
        if (delay > 0) {
            com.uitem.util.SchedulerUtil.runLater(plugin, run, delay);
        } else {
            run.run();
        }
    }
}