package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractGui implements InventoryHolder {

    /**
     * The last UItem GUI each player opened.
     *
     * <p>Needed because chat prompts close the inventory: by the time a prompt is
     * cancelled there is no open inventory left to read the holder from, so
     * {@code getOpenInventory().getTopInventory().getHolder()} returns null and
     * "cancel" used to dead-end in chat. This map survives the close, so cancel
     * can always put the player back where they were.
     *
     * <p>Cleared on quit by {@link #forget(Player)}.
     */
    private static final Map<UUID, AbstractGui> lastOpened = new ConcurrentHashMap<>();

    protected final UItemPlugin plugin;
    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;

    private Runnable backAction;

    public AbstractGui(UItemPlugin plugin, Player player, int size, String title) {
        this.plugin = plugin;
        this.player = player;
        this.clickHandlers = new HashMap<>();
        this.inventory = Bukkit.createInventory(this, size,
                TextUtil.parse(title));
    }

    /** The GUI this player last had open, or null. Used for chat-prompt cancel. */
    public static AbstractGui lastOpenedFor(Player player) {
        return player != null ? lastOpened.get(player.getUniqueId()) : null;
    }

    /** Drop the remembered GUI for a player (call on quit). */
    public static void forget(Player player) {
        if (player != null) lastOpened.remove(player.getUniqueId());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        setup();
        lastOpened.put(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    /**
     * Open this GUI as a child of the given parent.
     * The back button will reopen the parent GUI.
     */
    public void openAsChildOf(AbstractGui parent) {
        if (parent != null) {
            this.backAction = () -> parent.reopen();
        }
        open();
    }

    /**
     * Open this GUI as root (no parent, close on back).
     */
    public void openAsRoot() {
        this.backAction = null;
        open();
    }

    public void refresh() {
        clickHandlers.clear();
        inventory.clear();
        setup();
    }

    /**
     * Clear, setup again, and reopen the inventory.
     * Used by goBack() so the parent GUI is displayed correctly.
     */
    public void reopen() {
        clickHandlers.clear();
        inventory.clear();
        setup();
        lastOpened.put(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    protected abstract void setup();

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(event);
        }
    }

    public void handleBottomInventoryClick(InventoryClickEvent event) {
        event.setCancelled(false);
    }

    protected void setBackAction(Runnable backAction) {
        this.backAction = backAction;
    }

    protected boolean hasBackAction() {
        return backAction != null;
    }

    /**
     * The parent-reopening action, or null when this GUI is a root.
     *
     * <p>Public so sibling GUIs can forward the parent link when they replace
     * themselves with a fresh instance (rename, re-filter, ...) — without this
     * the new instance becomes a root and its Back button closes the inventory
     * instead of returning to the parent.
     */
    public Runnable getBackAction() {
        return backAction;
    }

    public void goBack() {
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        if (backAction != null) {
            // Do NOT null backAction here. This instance can be reopened later
            // (a child GUI calling parent.reopen(), or a chat prompt returning),
            // and clearing the link turned its Back button into "close inventory".
            backAction.run();
        } else {
            player.closeInventory();
        }
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        } else {
            clickHandlers.remove(slot);
        }
    }

    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
        clickHandlers.remove(slot);
    }

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

    protected ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

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

    protected ItemStack createBackButton() {
        return createItem(
                GuiConfig.material("back-button.material", Material.ARROW),
                GuiConfig.text("back-button.name", "<yellow>\u2190 Back"),
                GuiConfig.lore("back-button.lore", List.of("<gray>Click to go back")));
    }

    protected ItemStack createPreviousButton() {
        return createItem(
                GuiConfig.material("prev-page.material", Material.ARROW),
                GuiConfig.text("prev-page.name", "<yellow>\u25c0 Previous page"));
    }

    protected ItemStack createNextButton() {
        return createItem(
                GuiConfig.material("next-page.material", Material.ARROW),
                GuiConfig.text("next-page.name", "<yellow>Next page \u25b6"));
    }
}
