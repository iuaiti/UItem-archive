package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class cho tất cả GUI trong UItem.
 * Cung cấp:
 * - Tạo inventory
 * - Map slot → click handler
 * - Helper methods tạo button
 * - Border fill
 */
public abstract class AbstractGui implements InventoryHolder {

    protected final UItemPlugin plugin;
    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;

    public AbstractGui(UItemPlugin plugin, Player player, int size, String title) {
        this.plugin = plugin;
        this.player = player;
        this.clickHandlers = new HashMap<>();
        this.inventory = Bukkit.createInventory(this, size,
                TextUtil.parse(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Mở GUI cho player. Gọi setup() rồi mở inventory.
     */
    public void open() {
        setup();
        player.openInventory(inventory);
    }

    /**
     * Refresh GUI (clear và setup lại).
     */
    public void refresh() {
        clickHandlers.clear();
        inventory.clear();
        setup();
    }

    /**
     * Method abstract — mỗi GUI phải implement để setup nội dung.
     */
    protected abstract void setup();

    /**
     * Xử lý click event. Gọi từ GuiListener.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < inventory.getSize()) {
            GuiFeedback.click(player, clickHandlers.containsKey(slot));
        }
        Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(event);
        }
    }

    // === Helper methods ===

    /**
     * Đặt item vào slot với click handler.
     */
    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
    }

    /**
     * Đặt item vào slot không có handler.
     */
    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    /**
     * Styled border (black frame + orange accents).
     */
    protected void fillBorder(int rows) {
        GuiTheme.fillBorder(this, rows);
    }

    /** Used by {@link GuiTheme} to place panes. */
    public void setItemPublic(int slot, ItemStack item) {
        setItem(slot, item);
    }

    /**
     * Tạo ItemStack đơn giản.
     */
    protected ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Tạo ItemStack với lore.
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            if (lore != null) {
                meta.lore(TextUtil.buildLore(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Tạo button quay lại.
     */
    protected ItemStack createBackButton() {
        return GuiTheme.navBack();
    }

    protected ItemStack createHomeButton() {
        return GuiTheme.navHome();
    }

    protected ItemStack createPreviousButton() {
        return GuiTheme.navPrevious();
    }

    protected ItemStack createNextButton() {
        return GuiTheme.navNext();
    }
}
