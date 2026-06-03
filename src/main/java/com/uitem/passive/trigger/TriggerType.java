package com.uitem.passive.trigger;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.List;
import java.util.Locale;

public enum TriggerType {
    BLOCK_BREAK("block_break", "Khi dap block", "Kich hoat moi khi nguoi choi dap block", Material.IRON_PICKAXE, BlockBreakEvent.class, true),
    BLOCK_PLACE("block_place", "Khi dat block", "Kich hoat moi khi nguoi choi dat block", Material.GRASS_BLOCK, BlockPlaceEvent.class, true),
    ENTITY_DAMAGE("entity_damage", "Khi gay sat thuong", "Khi nguoi choi gay hoac nhan sat thuong", Material.IRON_SWORD, EntityDamageEvent.class, false),
    ENTITY_KILL("entity_kill", "Khi giet entity", "Khi nguoi choi giet mob hoac nguoi choi", Material.DIAMOND_SWORD, EntityDeathEvent.class, true),
    PLAYER_INTERACT("player_interact", "Khi tuong tac", "Click block hoac item", Material.OAK_BUTTON, PlayerInteractEvent.class, true),
    PLAYER_JOIN("player_join", "Khi vao server", "Khi nguoi choi join", Material.EMERALD, PlayerJoinEvent.class, true),
    PLAYER_QUIT("player_quit", "Khi roi server", "Khi nguoi choi quit", Material.BARRIER, PlayerQuitEvent.class, true),
    PLAYER_COMMAND("player_command", "Khi dung lenh", "Truoc khi lenh duoc thuc thi", Material.COMMAND_BLOCK, PlayerCommandPreprocessEvent.class, true),
    PLAYER_CHAT("player_chat", "Khi chat", "Khi gui tin nhan chat", Material.WRITABLE_BOOK, AsyncChatEvent.class, true),
    CONSUME_ITEM("consume_item", "Khi an/uong", "An thuc an hoac uong thuoc", Material.COOKED_BEEF, PlayerItemConsumeEvent.class, true),
    FISH("fish", "Khi cau ca", "Bat ca hoac item tu cau", Material.FISHING_ROD, PlayerFishEvent.class, true),
    CRAFT("craft", "Khi che tao", "Che tao trong ban crafting", Material.CRAFTING_TABLE, CraftItemEvent.class, true),
    ENCHANT("enchant", "Khi enchant", "Enchant item tai ban", Material.ENCHANTING_TABLE, EnchantItemEvent.class, true);

    private final String key;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final Class<? extends Event> eventClass;
    private final boolean requiresPlayer;

    TriggerType(String key, String displayName, String description, Material icon,
                Class<? extends Event> eventClass, boolean requiresPlayer) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.eventClass = eventClass;
        this.requiresPlayer = requiresPlayer;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Material icon() {
        return icon;
    }

    public Class<? extends Event> eventClass() {
        return eventClass;
    }

    public boolean requiresPlayer() {
        return requiresPlayer;
    }

    public static TriggerType fromKey(String key) {
        if (key == null) {
            return BLOCK_BREAK;
        }
        String norm = key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (TriggerType t : values()) {
            if (t.key.equals(norm) || t.name().equalsIgnoreCase(norm)) {
                return t;
            }
        }
        return BLOCK_BREAK;
    }

    public static List<TriggerType> all() {
        return List.of(values());
    }
}