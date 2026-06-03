package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.search.ItemSearchUtil;
import com.uitem.search.SearchManager;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ItemListGui extends AbstractGui {

    private int page;
    private static final int DEFAULT_ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ItemListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public ItemListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, GuiTheme.title("Item Library"));
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        UItemManager manager = plugin.getItemManager();
        List<UItemData> allItems = new ArrayList<>(manager.getAllItems().values());
        allItems.sort(Comparator.comparing(UItemData::getId, String.CASE_INSENSITIVE_ORDER));

        int itemsPerPage = plugin.getConfig().getInt("gui.items-per-page", DEFAULT_ITEMS_PER_PAGE);

        SearchManager searchMgr = plugin.getSearchManager();
        String query = searchMgr.getQuery(player, SearchManager.Scope.ITEM);
        if (query != null && !query.isBlank()) {
            allItems.removeIf(data -> !ItemSearchUtil.matches(data, query));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / itemsPerPage));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, GuiTheme.header(Material.CHEST, "Item Library",
                List.of(
                        "<gray>Trai: <white>Sua  <gray>|  Phai: <white>Xoa",
                        "<yellow>Trang: <white>" + (page + 1) + "<gray>/<white>" + totalPages,
                        "<yellow>Tong: <white>" + allItems.size() + " <gray>item"
                        + (query != null && !query.isBlank() ? " <dark_gray>(loc: " + query + ")" : "")
                )));

        int startIndex = page * itemsPerPage;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < allItems.size()) {
                UItemData data = allItems.get(dataIndex);
                ItemStack preview = manager.buildPreviewItemStack(data);
                setItem(ITEM_SLOTS[i], preview, event -> handleItemClick(event, data));
            } else {
                setItem(ITEM_SLOTS[i], null);
            }
        }

        String searchPerm = plugin.getConfig().getString("permissions.search", "uitem.search");
        if (plugin.getConfig().getBoolean("gui.search.enabled", true)
                && player.hasPermission(searchPerm)) {
            setItem(48, GuiTheme.actionSecondary(Material.SPYGLASS,
                    Messages.guiSearchButton(),
                    Messages.guiSearchHint1(),
                    Messages.guiSearchHint2(),
                    query != null && !query.isBlank()
                            ? "<gray>Đang lọc: <white>" + query : "<gray>Tìm: id, tên, rarity, material"), event -> {
                GuiFeedback.click(player);
                plugin.getChatInputManager().requestInput(player,
                        Messages.guiSearchPrompt(),
                        input -> {
                            if (input.equalsIgnoreCase("clear")) {
                                searchMgr.clearQuery(player, SearchManager.Scope.ITEM);
                            } else {
                                searchMgr.setQuery(player, SearchManager.Scope.ITEM, input);
                            }
                            new ItemListGui(plugin, player, 0).open();
                        });
            });
        }

        setItem(49, GuiTheme.actionPrimary(Material.EMERALD,
                "+ Create",
                Messages.guiCreateHint(),
                Messages.guiCreateHint2()), event -> {
            GuiFeedback.success(player);
            plugin.getChatInputManager().requestInput(player,
                    Messages.guiCreatePrompt(),
                    input -> promptCreateItem(input));
        });

        setItem(45, createHomeButton(), event -> {
            GuiFeedback.click(player);
            new MainHubGui(plugin, player).open();
        });

        if (page > 0) {
            setItem(46, createPreviousButton(), event -> {
                GuiFeedback.click(player);
                new ItemListGui(plugin, player, page - 1).open();
            });
        } else {
            setItem(46, GuiTheme.disabledNav());
        }

        if (page < totalPages - 1) {
            setItem(52, createNextButton(), event -> {
                GuiFeedback.click(player);
                new ItemListGui(plugin, player, page + 1).open();
            });
        } else {
            setItem(52, GuiTheme.disabledNav());
        }
    }

    private void handleItemClick(InventoryClickEvent event, UItemData data) {
        if (event.getClick() == ClickType.LEFT) {
            GuiFeedback.click(player);
            new ItemEditorGui(plugin, player, data).open();
        } else if (event.getClick() == ClickType.RIGHT) {
            GuiFeedback.click(player, false);
            new ConfirmDeleteGui(plugin, player, data, page).open();
        }
    }

    private void promptCreateItem(String input) {
        UItemManager manager = plugin.getItemManager();
        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (id.isEmpty()) {
            player.sendMessage(TextUtil.error(Messages.invalidId()));
            new ItemListGui(plugin, player, page).open();
            return;
        }
        if (manager.hasItem(id)) {
            player.sendMessage(TextUtil.error(Messages.itemAlreadyExists(id)));
            new ItemListGui(plugin, player, page).open();
            return;
        }
        UItemData newData = new UItemData(id);
        boolean created = com.uitem.api.UItemAPI.get().registerItem(newData, player);
        if (!created) {
            player.sendMessage(TextUtil.error(Messages.cannotCreateBlocked(id)));
            new ItemListGui(plugin, player, page).open();
            return;
        }
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        player.sendMessage(TextUtil.success(Messages.createdItem(id)));
        new ItemEditorGui(plugin, player, newData).open();
    }
}