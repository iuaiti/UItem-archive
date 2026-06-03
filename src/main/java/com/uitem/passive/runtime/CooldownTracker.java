package com.uitem.passive.runtime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownTracker {

    private final Map<String, Long> lastFireMs = new ConcurrentHashMap<>();

    public boolean isReady(UUID playerId, String passiveId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return true;
        }
        String key = playerId + ":" + passiveId;
        Long last = lastFireMs.get(key);
        if (last == null) {
            return true;
        }
        return System.currentTimeMillis() - last >= cooldownSeconds * 1000L;
    }

    public void markFired(UUID playerId, String passiveId) {
        lastFireMs.put(playerId + ":" + passiveId, System.currentTimeMillis());
    }

    public boolean isReadyCondition(UUID playerId, String passiveId, String conditionKey, int seconds) {
        if (seconds <= 0) {
            return true;
        }
        String key = playerId + ":c:" + passiveId + ":" + conditionKey;
        Long last = lastFireMs.get(key);
        if (last == null) {
            return true;
        }
        return System.currentTimeMillis() - last >= seconds * 1000L;
    }

    public void markCondition(UUID playerId, String passiveId, String conditionKey) {
        lastFireMs.put(playerId + ":c:" + passiveId + ":" + conditionKey, System.currentTimeMillis());
    }

    public void clearPlayer(UUID playerId) {
        lastFireMs.keySet().removeIf(k -> k.startsWith(playerId.toString()));
    }
}