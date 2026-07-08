package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import com.uitem.search.SearchManager;
import java.util.Comparator;
import java.util.List;

/**
 * GUI showing the list of all custom items + create new (via Wizard).
 */
public class ItemListGui extends AbstractGui {

    private int page;
    private static final int DEFAULT_ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ItemListGui(UItemPlugin plugin, Player player) { this(plugin, player, 0); }

    public ItemListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>UItem Editor</bold></gradient>");
        this.page = page;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.NETHER_STAR,
                "<gradient:#ff6b6b:#ffd93d><bold>UItem Editor</bold></gradient>",
                List.of(
                        "<gray>Manage all custom items",
                        "",
                        "<yellow>Total items: <white>" + plugin.getItemManager().getAllItems().size(),
                        "<yellow>Page: <white>" + (page + 1)
                )));

        if (plugin.getConfig().getBoolean("gui.search.enabled", true)
                && player.hasPermission("uitem.search")) {
            setItem(48, createItem(Material.MAGENTA_GLAZED_TERRACOTTA,
                    "<aqua><bold>Search Item</bold>",
                    List.of(
                            "<gray>Enter a keyword to filter the list",
                            "<gray>Type 'clear' to clear the filter"
                    )), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter search keyword (<gray>or 'clear' to clear):",
                        input -> {
                            SearchManager sm = plugin.getSearchManager();
                            if (input.equalsIgnoreCase("clear")) sm.clearQuery(player);
                            else sm.setQuery(player, input);
                            new ItemListGui(plugin, player, page).open();
                        });
            });
        }

        UItemManager manager = plugin.getItemManager();
        List<UItemData> allItems = new ArrayList<>(manager.getAllItems().values());
        allItems.sort(Comparator.comparing(UItemData::getId, String.CASE_INSENSITIVE_ORDER));

        int configuredItemsPerPage = plugin.getConfig().getInt("gui.items-per-page", DEFAULT_ITEMS_PER_PAGE);
        int itemsPerPage = Math.max(1, Math.min(configuredItemsPerPage, ITEM_SLOTS.length));

        SearchManager searchMgr = plugin.getSearchManager();
        String query = searchMgr.getQuery(player);
        if (query != null && !query.isBlank()) {
            String lower = query.toLowerCase();
            allItems.removeIf(data -> {
                if (data.getId().toLowerCase().contains(lower)) return false;
                String disp = data.getDisplayName();
                return disp == null || !disp.toLowerCase().contains(lower);
            });
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / itemsPerPage));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < allItems.size()) {
                UItemData data = allItems.get(dataIndex);
                setItem(ITEM_SLOTS[i], manager.buildPreviewItemStack(data), event -> {
                    if (event.getClick() == ClickType.LEFT) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                        new ItemEditorGui(plugin, player, data).openAsChildOf(this);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.5f);
                        new ConfirmDeleteGui(plugin, player, data, this.page).openAsChildOf(this);
                    }
                });
            }
        }

        // Create new item — via 4-step Wizard
        setItem(49, createItem(Material.NETHER_STAR,
                "<green><bold>+ Create New Item (Wizard)</bold>",
                List.of(
                        "<gray>4 steps to create a UItem:",
                        "<gray>Category → Provider → Item → Setup",
                        "",
                        "<yellow>Click to start"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            // Wizard creates the item with generated ID then opens editor directly.
            new CreateWizardGui(plugin, player).openAsChildOf(this);
        });

        if (player.hasPermission("uitem.give")) {
            setItem(51, createItem(Material.CHEST,
                    "<gold><bold>📦 Give Multiple Items</bold>",
                    List.of(
                            "<gray>Give multiple items at once",
                            "",
                            "<yellow>Click to open"
                    )), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new BulkGiveGui(plugin, player).openAsChildOf(this);
            });
        }

        if (page > 0) {
            setItem(46, createPreviousButton(), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                page--;
                refresh();
            });
        }
        if (page < totalPages - 1) {
            setItem(52, createNextButton(), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                page++;
                refresh();
            });
        }
    }
}
