package com.uitem.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemAbilityCooldown {

    private final Map<UUID, Map<String, Long>> expiry = new ConcurrentHashMap<>();

    public boolean isReady(UUID playerId, String itemId, ItemAbilityRule rule) {
        if (rule.getCooldownSeconds() <= 0) {
            return true;
        }
        Map<String, Long> perItem = expiry.get(playerId);
        if (perItem == null) {
            return true;
        }
        Long until = perItem.get(key(itemId, rule));
        return until == null || System.currentTimeMillis() >= until;
    }

    public void mark(UUID playerId, String itemId, ItemAbilityRule rule) {
        if (rule.getCooldownSeconds() <= 0) {
            return;
        }
        expiry.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(key(itemId, rule), System.currentTimeMillis() + rule.getCooldownSeconds() * 1000L);
    }

    private static String key(String itemId, ItemAbilityRule rule) {
        return itemId + ":" + rule.getTrigger().name() + ":" + rule.getRuleId();
    }
}