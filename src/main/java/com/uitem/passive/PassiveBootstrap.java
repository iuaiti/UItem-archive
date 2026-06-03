package com.uitem.passive;

import com.uitem.UItemPlugin;
import com.uitem.passive.runtime.CooldownTracker;
import com.uitem.passive.runtime.PassiveDispatcher;
import com.uitem.passive.runtime.PassiveRuntime;

public final class PassiveBootstrap {

    private final PassiveManager manager;
    private final PassiveRuntime runtime;
    private final PassiveDispatcher dispatcher;

    public PassiveBootstrap(UItemPlugin plugin) {
        this.manager = new PassiveManager(plugin);
        CooldownTracker tracker = manager.cooldownTracker();
        this.runtime = new PassiveRuntime(plugin, tracker);
        this.dispatcher = new PassiveDispatcher(plugin, manager, runtime);
    }

    public void enable(UItemPlugin plugin) {
        manager.storage().ensureFolder();
        manager.loadAll();
        plugin.getServer().getPluginManager().registerEvents(dispatcher, plugin);
    }

    public PassiveManager manager() {
        return manager;
    }
}