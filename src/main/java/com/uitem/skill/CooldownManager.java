package com.uitem.skill;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns for UItem skills.
 * 
 * - Uses a fast in-memory map for runtime.
 * - Stores last-used time in player PDC to persist across logout/restart (much more stable than many other plugins).
 * - PDC key: uitem_cd_<itemId>_<skillId>
 */
public final class CooldownManager {

    private final NamespacedKey pluginKeyBase;

    // memory cache: playerUUID -> ( "itemId:skillId" -> lastUsedMillis )
    private final Map<UUID, Map<String, Long>> lastUsed = new ConcurrentHashMap<>();

    public CooldownManager(org.bukkit.plugin.Plugin plugin) {
        this.pluginKeyBase = new NamespacedKey(plugin, "uitem_cd");
    }

    private String makeKey(String itemId, String skillId) {
        return (itemId == null ? "null" : itemId.toLowerCase(Locale.ROOT)) + ":" + (skillId == null ? "null" : skillId.toLowerCase(Locale.ROOT));
    }

    private NamespacedKey getPdcKey(String itemId, String skillId) {
        // Limit key length (PDC key max ~256)
        String shortKey = "cd_" + safe(itemId) + "_" + safe(skillId);
        if (shortKey.length() > 80) shortKey = shortKey.substring(0, 80);
        return new NamespacedKey(pluginKeyBase.getNamespace(), shortKey);
    }

    private String safe(String s) {
        return s == null ? "x" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    /**
     * Check if the skill is ready (off cooldown).
     * Returns true if it can be used.
     */
    public boolean isReady(Player player, String itemId, String skillId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return true;
        if (player == null) return true;

        long now = System.currentTimeMillis();
        long cdMillis = (long) (cooldownSeconds * 1000);

        Map<String, Long> playerMap = lastUsed.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        String key = makeKey(itemId, skillId);
        Long last = playerMap.get(key);

        // If not in memory yet -> try loading from PDC (first join or after restart)
        if (last == null) {
            try {
                NamespacedKey pdcKey = getPdcKey(itemId, skillId);
                if (player.getPersistentDataContainer().has(pdcKey, PersistentDataType.LONG)) {
                    last = player.getPersistentDataContainer().get(pdcKey, PersistentDataType.LONG);
                    if (last != null) playerMap.put(key, last);
                }
            } catch (Exception ignored) {}
        }

        if (last == null) return true;
        return (now - last) >= cdMillis;
    }

    /**
     * Returns the remaining time (seconds), 0 if ready.
     */
    public double getRemainingSeconds(Player player, String itemId, String skillId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return 0;
        if (player == null) return 0;

        long now = System.currentTimeMillis();
        long cdMillis = (long) (cooldownSeconds * 1000);

        Map<String, Long> playerMap = lastUsed.get(player.getUniqueId());
        String key = makeKey(itemId, skillId);
        Long last = (playerMap != null) ? playerMap.get(key) : null;

        if (last == null) {
            try {
                NamespacedKey pdcKey = getPdcKey(itemId, skillId);
                if (player.getPersistentDataContainer().has(pdcKey, PersistentDataType.LONG)) {
                    last = player.getPersistentDataContainer().get(pdcKey, PersistentDataType.LONG);
                }
            } catch (Exception ignored) {}
        }

        if (last == null) return 0;
        long elapsed = now - last;
        if (elapsed >= cdMillis) return 0;
        return (cdMillis - elapsed) / 1000.0;
    }

    /**
     * Mark the skill as just used (start cooldown).
     */
    public void markUsed(Player player, String itemId, String skillId) {
        if (player == null) return;
        long now = System.currentTimeMillis();

        Map<String, Long> playerMap = lastUsed.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        String key = makeKey(itemId, skillId);
        playerMap.put(key, now);

        // Save to PDC for persistence
        try {
            NamespacedKey pdcKey = getPdcKey(itemId, skillId);
            player.getPersistentDataContainer().set(pdcKey, PersistentDataType.LONG, now);
        } catch (Exception ignored) {}
    }

    /**
     * Clear cooldown for one skill (reset).
     */
    public void reset(Player player, String itemId, String skillId) {
        if (player == null) return;
        Map<String, Long> playerMap = lastUsed.get(player.getUniqueId());
        if (playerMap != null) {
            playerMap.remove(makeKey(itemId, skillId));
        }
        try {
            NamespacedKey pdcKey = getPdcKey(itemId, skillId);
            player.getPersistentDataContainer().remove(pdcKey);
        } catch (Exception ignored) {}
    }

    /**
     * Clear all cooldowns for a player (on logout or admin reset).
     */
    public void clearPlayer(Player player) {
        if (player == null) return;
        lastUsed.remove(player.getUniqueId());
        // Don't wipe the whole PDC (too heavy + unnecessary). PDC entries expire over time.
    }

    /**
     * Cleanup when a player quits.
     */
    public void onPlayerQuit(Player player) {
        clearPlayer(player);
    }
}
