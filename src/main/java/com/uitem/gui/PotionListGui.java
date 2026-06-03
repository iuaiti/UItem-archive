package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.potion.UPotionData;
import com.uitem.search.SearchManager;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PotionListGui extends AbstractGui {

    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private int page;

    public PotionListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public PotionListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, GuiTheme.title(Messages.potionListTitle()));
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);
        List<UPotionData> all = new ArrayList<>(plugin.getPotionManager().getAll().values());
        all.sort(Comparator.comparing(UPotionData::getId));

        SearchManager searchMgr = plugin.getSearchManager();
        String query = searchMgr.getQuery(player, SearchManager.Scope.POTION);
        if (query != null && !query.isBlank()) {
            all.removeIf(p -> !matchesPotion(p, query));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, GuiTheme.header(org.bukkit.Material.BREWING_STAND, Messages.potionListTitle(),
                List.of(
                        "<gray>Trai: sua | Phai: xoa",
                        "<yellow>Trang: <white>" + (page + 1) + "<gray>/<white>" + totalPages,
                        "<yellow>Tong: <white>" + all.size()
                                + (query != null && !query.isBlank() ? " <dark_gray>(loc: " + query + ")" : "")
                )));

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            int slot = SLOTS[i];
            if (idx >= all.size()) {
                setItem(slot, null);
                continue;
            }
            UPotionData data = all.get(idx);
            ItemStack preview = plugin.getPotionManager().buildPreview(data);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Loai: <white>" + data.getKind().name());
            lore.add("<gray>Hieu ung: <white>" + data.getEffects().size());
            lore.add("<yellow>Trai-click <gray>sua");
            setItem(slot, preview, ev -> handleClick(ev, data));
        }

        String searchPerm = plugin.getConfig().getString("permissions.search", "uitem.search");
        if (plugin.getConfig().getBoolean("gui.search.enabled", true)
                && player.hasPermission(searchPerm)) {
            setItem(48, GuiTheme.actionSecondary(org.bukkit.Material.SPYGLASS,
                    Messages.guiSearchButton(),
                    Messages.guiSearchHint1(),
                    Messages.guiSearchHint2()), ev -> {
                GuiFeedback.click(player);
                plugin.getChatInputManager().requestInput(player,
                        Messages.guiSearchPrompt(),
                        input -> {
                            if (input.equalsIgnoreCase("clear")) {
                                searchMgr.clearQuery(player, SearchManager.Scope.POTION);
                            } else {
                                searchMgr.setQuery(player, SearchManager.Scope.POTION, input);
                            }
                            new PotionListGui(plugin, player, 0).open();
                        });
            });
        }

        if (page > 0) {
            setItem(45, createPreviousButton(), ev -> new PotionListGui(plugin, player, page - 1).open());
        }
        setItem(49, createBackButton(), ev -> new PotionHubGui(plugin, player).open());
        if (page < totalPages - 1) {
            setItem(53, createNextButton(), ev -> new PotionListGui(plugin, player, page + 1).open());
        }
    }

    private static boolean matchesPotion(UPotionData data, String query) {
        if (data == null || query == null || query.isBlank()) {
            return true;
        }
        String lower = query.toLowerCase().trim();
        if (data.getId().toLowerCase().contains(lower)) {
            return true;
        }
        if (data.getKind().name().toLowerCase().contains(lower)) {
            return true;
        }
        return TextUtil.stripMiniMessageTags(data.getDisplayName()).contains(lower);
    }

    private void handleClick(InventoryClickEvent ev, UPotionData data) {
        if (ev.getClick() == ClickType.LEFT) {
            GuiFeedback.click(player);
            new PotionEditorGui(plugin, player, data).open();
        } else if (ev.getClick() == ClickType.RIGHT) {
            GuiFeedback.click(player, false);
            new PotionConfirmDeleteGui(plugin, player, data, page).open();
        }
    }
}