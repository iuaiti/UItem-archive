package com.uitem.passive.trigger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal runtime context — never shown in GUI.
 */
public final class TriggerContext {

    private final TriggerType triggerType;
    private final Event sourceEvent;
    @Nullable
    private final Player player;
    @Nullable
    private final World world;
    @Nullable
    private final Block block;
    @Nullable
    private final Entity entity;
    @Nullable
    private final ItemStack itemInHand;
    private final Map<String, Object> extras;

    private TriggerContext(Builder b) {
        this.triggerType = b.triggerType;
        this.sourceEvent = b.sourceEvent;
        this.player = b.player;
        this.world = b.world;
        this.block = b.block;
        this.entity = b.entity;
        this.itemInHand = b.itemInHand;
        this.extras = Map.copyOf(b.extras);
    }

    public TriggerType triggerType() {
        return triggerType;
    }

    public Event sourceEvent() {
        return sourceEvent;
    }

    @Nullable
    public Player player() {
        return player;
    }

    @Nullable
    public World world() {
        return world;
    }

    @Nullable
    public Block block() {
        return block;
    }

    @Nullable
    public Entity entity() {
        return entity;
    }

    @Nullable
    public ItemStack itemInHand() {
        return itemInHand;
    }

    public Map<String, Object> extras() {
        return extras;
    }

    @Nullable
    public Object extra(String key) {
        return extras.get(key);
    }

    @Nullable
    public Location location() {
        if (block != null) {
            return block.getLocation();
        }
        if (player != null) {
            return player.getLocation();
        }
        if (entity != null) {
            return entity.getLocation();
        }
        return null;
    }

    public static Builder builder(TriggerType type, Event event) {
        return new Builder(type, event);
    }

    public static final class Builder {
        private final TriggerType triggerType;
        private final Event sourceEvent;
        @Nullable
        private Player player;
        @Nullable
        private World world;
        @Nullable
        private Block block;
        @Nullable
        private Entity entity;
        @Nullable
        private ItemStack itemInHand;
        private final Map<String, Object> extras = new HashMap<>();

        public Builder(TriggerType triggerType, Event event) {
            this.triggerType = triggerType;
            this.sourceEvent = event;
        }

        public Builder player(@Nullable Player player) {
            this.player = player;
            return this;
        }

        public Builder world(@Nullable World world) {
            this.world = world;
            return this;
        }

        public Builder block(@Nullable Block block) {
            this.block = block;
            return this;
        }

        public Builder entity(@Nullable Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder itemInHand(@Nullable ItemStack itemInHand) {
            this.itemInHand = itemInHand;
            return this;
        }

        public Builder extra(String key, Object value) {
            extras.put(key, value);
            return this;
        }

        public TriggerContext build() {
            return new TriggerContext(this);
        }
    }
}