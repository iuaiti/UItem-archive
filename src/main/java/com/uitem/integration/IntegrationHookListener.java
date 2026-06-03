package com.uitem.integration;

import com.uitem.UItemPlugin;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;

import java.util.Set;
import java.util.logging.Level;

/**
 * Re-detect integrations when related plugins enable or Nexo finishes loading items.
 */
public final class IntegrationHookListener implements Listener {

    private static final Set<String> WATCHED_PLUGINS = Set.of(
            "Oraxen", "Nexo", "ItemsAdder", "SCore", "ExecutableItems"
    );

    private final UItemPlugin plugin;

    public IntegrationHookListener(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);
        registerNexoItemsLoaded(pm);
    }

    @org.bukkit.event.EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        String name = event.getPlugin().getName();
        if (!WATCHED_PLUGINS.contains(name)) {
            return;
        }
        plugin.getLogger().info("[UItem] Detected " + name + " enabling — re-checking integrations...");
        plugin.getIntegrationManager().detectIntegrations();
        plugin.getLogger().info("[UItem] Integrations: " + plugin.getIntegrationManager().getStatus());
    }

    private void registerNexoItemsLoaded(PluginManager pm) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>)
                    Class.forName("com.nexomc.nexo.api.events.NexoItemsLoadedEvent");
            Listener listener = new Listener() {};
            EventExecutor executor = (l, event) -> {
                plugin.getLogger().info("[UItem] Nexo items loaded — refreshing integration hook...");
                plugin.getIntegrationManager().detectIntegrations();
            };
            pm.registerEvent(eventClass, listener, EventPriority.MONITOR, executor, plugin);
        } catch (ClassNotFoundException ignored) {
            // Nexo not on classpath — skip
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[UItem] Could not register NexoItemsLoadedEvent: " + e.getMessage());
        }
    }
}