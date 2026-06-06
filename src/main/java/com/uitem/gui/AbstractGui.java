package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
     * Fill border với glass pane. (nicer dark theme)
     */
    protected void fillBorder(int rows) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                setItem(i, border);
            }
        }
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
                meta.lore(lore.stream().map(TextUtil::parse).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Tạo button quay lại.
     */
    protected ItemStack createBackButton() {
        return createItem(Material.ARROW, Messages.guiBack(), List.of(Messages.guiBackDesc()));
    }

    /**
     * Tạo button trang trước.
     */
    protected ItemStack createPreviousButton() {
        return createItem(Material.ARROW, "<yellow>◄ Previous page");
    }

    /**
     * Tạo button trang sau.
     */
    protected ItemStack createNextButton() {
        return createItem(Material.ARROW, "<yellow>Next page ►");
    }
}
