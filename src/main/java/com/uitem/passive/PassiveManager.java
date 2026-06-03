package com.uitem.passive;

import com.uitem.UItemPlugin;
import com.uitem.passive.storage.PassiveStorage;
import com.uitem.passive.trigger.TriggerType;
import com.uitem.passive.runtime.CooldownTracker;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PassiveManager {

    private final UItemPlugin plugin;
    private final PassiveStorage storage;
    private final CooldownTracker cooldownTracker = new CooldownTracker();
    private final Map<String, PassiveDefinition> byId = new LinkedHashMap<>();
    private final Map<TriggerType, List<PassiveDefinition>> byTrigger = new EnumMap<>(TriggerType.class);
    private final Map<UUID, PassiveEditorSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public PassiveManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.storage = new PassiveStorage(plugin);
        for (TriggerType t : TriggerType.values()) {
            byTrigger.put(t, new ArrayList<>());
        }
    }

    public PassiveStorage storage() {
        return storage;
    }

    public CooldownTracker cooldownTracker() {
        return cooldownTracker;
    }

    public void loadAll() {
        byId.clear();
        for (List<PassiveDefinition> list : byTrigger.values()) {
            list.clear();
        }
        for (PassiveDefinition def : storage.loadAll()) {
            register(def, false);
        }
        plugin.getLogger().info("[UItem] Loaded " + byId.size() + " passives.");
    }

    public void register(PassiveDefinition def, boolean persist) {
        byId.put(def.id(), def);
        rebuildTriggerIndex();
        if (persist) {
            storage.save(def);
        }
    }

    public void unregister(String id, boolean deleteFile) {
        byId.remove(id);
        rebuildTriggerIndex();
        if (deleteFile) {
            storage.delete(id);
        }
    }

    private void rebuildTriggerIndex() {
        for (List<PassiveDefinition> list : byTrigger.values()) {
            list.clear();
        }
        for (PassiveDefinition def : byId.values()) {
            if (def.enabled()) {
                byTrigger.get(def.trigger()).add(def);
            }
        }
    }

    public List<PassiveDefinition> byTrigger(TriggerType type) {
        return Collections.unmodifiableList(byTrigger.getOrDefault(type, List.of()));
    }

    public PassiveDefinition get(String id) {
        return byId.get(id);
    }

    public boolean has(String id) {
        return byId.containsKey(id);
    }

    public Collection<PassiveDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public PassiveEditorSession session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), u -> new PassiveEditorSession(player));
    }

    public PassiveEditorSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void clearSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public PassiveDefinition duplicate(String sourceId, String newId) {
        PassiveDefinition src = get(sourceId);
        if (src == null) return null;
        PassiveDefinition copy = src.copy();
        // id is final - create new
        PassiveDefinition neu = new PassiveDefinition(newId);
        neu.setDisplayName(src.displayName() + " (copy)");
        neu.setEnabled(src.enabled());
        neu.setTrigger(src.trigger());
        neu.setConditions(src.conditions().copy());
        neu.setActions(new ArrayList<>(src.actions().stream().map(ActionEntry::copy).toList()));
        neu.setAdvanced(src.advanced().copy());
        register(neu, true);
        return neu;
    }
}