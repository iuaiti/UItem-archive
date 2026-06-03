package com.uitem.potion;

import com.uitem.UItemPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PotionManager {

    private final UItemPlugin plugin;
    private final PotionStorage storage;
    private final Map<String, UPotionData> potions;
    private final NamespacedKey potionKey;
    private final MiniMessage miniMessage;

    public PotionManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.storage = new PotionStorage(plugin);
        this.potions = new ConcurrentHashMap<>();
        this.potionKey = new NamespacedKey(plugin, "upotion_id");
        this.miniMessage = MiniMessage.miniMessage();
        storage.ensureDefaults();
        storage.loadAll(this);
    }

    public NamespacedKey getPotionKey() {
        return potionKey;
    }

    public PotionStorage storage() {
        return storage;
    }

    public void register(UPotionData data) {
        if (data == null || data.getId() == null || data.getId().isBlank()) {
            return;
        }
        potions.put(data.getId().toLowerCase(Locale.ROOT), data);
    }

    public void remove(String id) {
        if (id == null) {
            return;
        }
        String key = id.toLowerCase(Locale.ROOT);
        potions.remove(key);
        storage.deleteFile(key);
    }

    public UPotionData get(String id) {
        if (id == null) {
            return null;
        }
        return potions.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean has(String id) {
        return id != null && potions.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, UPotionData> getAll() {
        return Collections.unmodifiableMap(potions);
    }

    public void clearAll() {
        potions.clear();
    }

    public void reload() {
        storage.loadAll(this);
    }

    public void saveAll() {
        storage.saveAll(this);
    }

    public void save(UPotionData data) {
        register(data);
        storage.saveOne(data);
    }

    public ItemStack buildItemStack(UPotionData data) {
        if (data == null) {
            return new ItemStack(org.bukkit.Material.POTION);
        }
        ItemStack stack = new ItemStack(data.getKind().material());
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        int r = (data.getColorRgb() >> 16) & 0xFF;
        int g = (data.getColorRgb() >> 8) & 0xFF;
        int b = data.getColorRgb() & 0xFF;
        meta.setColor(Color.fromRGB(r, g, b));

        meta.clearCustomEffects();
        for (UPotionEffectEntry entry : data.getEffects()) {
            PotionEffectType type = PotionEffectTypes.resolve(entry.getTypeKey());
            PotionEffect effect = entry.toBukkit(type);
            if (effect != null) {
                meta.addCustomEffect(effect, true);
            }
        }

        if (data.getDisplayName() != null && !data.getDisplayName().isBlank()) {
            meta.displayName(miniMessage.deserialize(data.getDisplayName()));
        }
        if (data.getLore() != null && !data.getLore().isEmpty()) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : data.getLore()) {
                lore.add(miniMessage.deserialize(line));
            }
            meta.lore(lore);
        }

        com.uitem.util.ServerVersionUtil.applyHideAdditionalTooltip(meta);
        if (plugin.getConfig().getBoolean("display.hide-item-id-in-game", true)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (data.isGlowing()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.getPersistentDataContainer().set(potionKey, PersistentDataType.STRING, data.getId());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack buildPreview(UPotionData data) {
        return buildPreview(data, plugin.getConfig().getBoolean("display.show-id-in-editor-preview", true));
    }

    public ItemStack buildPreview(UPotionData data, boolean showAdminMeta) {
        ItemStack stack = buildItemStack(data);
        if (!showAdminMeta) {
            return stack;
        }
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        List<net.kyori.adventure.text.Component> lore = meta.lore();
        if (lore == null) {
            lore = new java.util.ArrayList<>();
        } else {
            lore = new java.util.ArrayList<>(com.uitem.util.TextUtil.withLeadingSpacer(lore));
        }
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(miniMessage.deserialize("<dark_gray>──────────────"));
        lore.add(miniMessage.deserialize("<gray>ID: <white>" + data.getId()));
        lore.add(miniMessage.deserialize("<dark_gray>──────────────"));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isUPotion(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(potionKey, PersistentDataType.STRING);
    }

    public String getIdFromItem(ItemStack item) {
        if (!isUPotion(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(potionKey, PersistentDataType.STRING);
    }

    /** Ap dung hieu ung tu UPotionData len nguoi choi (dung khi item co linked-potion). */
    public void applyEffectsToPlayer(Player player, UPotionData data) {
        if (player == null || data == null) {
            return;
        }
        for (UPotionEffectEntry entry : data.getEffects()) {
            PotionEffectType type = PotionEffectTypes.resolve(entry.getTypeKey());
            PotionEffect effect = entry.toBukkit(type);
            if (effect != null) {
                player.addPotionEffect(effect, true);
            }
        }
    }
}