package com.uitem.search;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Từ khóa tìm kiếm theo người chơi (Item Library / Potion List).
 */
public class SearchManager {

    public enum Scope {
        ITEM,
        POTION
    }

    private final Map<UUID, String> itemQueries = new ConcurrentHashMap<>();
    private final Map<UUID, String> potionQueries = new ConcurrentHashMap<>();

    public void setQuery(Player player, Scope scope, String query) {
        Map<UUID, String> map = scope == Scope.POTION ? potionQueries : itemQueries;
        if (query == null || query.isBlank()) {
            map.remove(player.getUniqueId());
        } else {
            map.put(player.getUniqueId(), query.toLowerCase().trim());
        }
    }

    public void clearQuery(Player player, Scope scope) {
        mapFor(scope).remove(player.getUniqueId());
    }

    public String getQuery(Player player, Scope scope) {
        return mapFor(scope).get(player.getUniqueId());
    }

    public void setQuery(Player player, String query) {
        setQuery(player, Scope.ITEM, query);
    }

    public void clearQuery(Player player) {
        clearQuery(player, Scope.ITEM);
    }

    public String getQuery(Player player) {
        return getQuery(player, Scope.ITEM);
    }

    private Map<UUID, String> mapFor(Scope scope) {
        return scope == Scope.POTION ? potionQueries : itemQueries;
    }
}