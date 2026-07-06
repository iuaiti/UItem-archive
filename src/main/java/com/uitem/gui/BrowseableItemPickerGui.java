package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.provider.BrowseableProvider;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Wizard Step 3 (browseable): shows the list of all items from a provider
 * (ItemsAdder/Oraxen/vanilla all share this) with search + pagination.
 * Click -> callback receives the source string "namespace:id".
 */
public class BrowseableItemPickerGui extends AbstractGui {

    private final BrowseableProvider provider;
    private final Consumer<String> callback;
    private int page;
    private String filter;
    private List<String> ids;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public BrowseableItemPickerGui(UItemPlugin plugin, Player player, BrowseableProvider provider,
                                    Consumer<String> callback) {
        this(plugin, player, provider, callback, 0, null);
    }

    public BrowseableItemPickerGui(UItemPlugin plugin, Player player, BrowseableProvider provider,
                                    Consumer<String> callback, int page, String filter) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>" + provider.getDisplayName() + "</bold> <gray>→ Choose Item");
        this.provider = provider;
        this.callback = callback;
        this.page = page;
        this.filter = filter;
        this.ids = loadIds();
    }

    private List<String> loadIds() {
        List<String> all = new ArrayList<>(provider.listIds());
        all.sort(Comparator.nullsFirst(String::compareTo));
        if (filter != null && !filter.isBlank()) {
            String lower = filter.toLowerCase(Locale.ROOT);
            all.removeIf(id -> id == null || !id.toLowerCase(Locale.ROOT).contains(lower));
        }
        return all;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.COMPASS,
                "<gold><bold>" + provider.getDisplayName() + " Items</bold>",
                List.of(
                        "<gray>Total: <white>" + ids.size(),
                        "<gray>Page: <white>" + (page + 1),
                        filter != null ? "<gray>Filter: <white>" + filter : ""
                )));

        setItem(53, createItem(Material.OAK_SIGN,
                "<gold><bold>Search</bold>",
                List.of(filter != null ? "<gray>Filter: <white>" + filter : "<gray>Click to search by id")),
                ev -> {
                    player.closeInventory();
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Enter a keyword (Enter to clear filter):",
                            input -> {
                                String f = input.trim().isEmpty() ? null : input.trim();
                                new BrowseableItemPickerGui(plugin, player, provider, callback, 0, f).open();
                            });
                });

        int totalPages = Math.max(1, (int) Math.ceil((double) ids.size() / ITEMS_PER_PAGE));
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = startIndex + i;
            if (idx >= ids.size()) break;
            String rawId = ids.get(idx);
            if (rawId == null) continue;

            String displayId = displayId(rawId);
            String source = sourceId(rawId);

            ItemStack preview;
            try {
                preview = provider.buildPreview(rawId);
            } catch (Exception e) {
                preview = null;
            }
            if (preview == null || preview.getType().isAir()) {
                preview = createItem(Material.PAPER, "<white>" + displayId,
                        previewLore(rawId, source, true));
            } else {
                ItemMeta meta = preview.getItemMeta();
                if (meta != null) {
                    var lore = meta.lore();
                    if (lore == null) lore = new java.util.ArrayList<>();
                    for (String line : previewLore(rawId, source, false)) {
                        lore.add(TextUtil.parse(line));
                    }
                    meta.lore(lore);
                    preview.setItemMeta(meta);
                }
            }
            setItem(ITEM_SLOTS[i], preview, ev -> {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                player.sendMessage(TextUtil.success("Selected " + provider.getDisplayName() + " item: " + rawId));
                if (callback != null) callback.accept(source);
            });
        }

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { page--; refresh(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { page++; refresh(); } : null);

        setItem(45, createBackButton(), ev -> goBack());
    }

    private String sourceId(String rawId) {
        if (rawId == null) return provider.getNamespace() + ":unknown";
        if (provider.getNamespace().equals("ia") && rawId.contains(":")) {
            return provider.getNamespace() + ":" + rawId;
        }
        return provider.getNamespace() + ":" + displayId(rawId);
    }

    private List<String> previewLore(String rawId, String source, boolean missingPreview) {
        List<String> lore = new ArrayList<>();
        if (missingPreview) {
            lore.add("<red>No preview available (provider returned null)");
        }
        lore.add("<dark_gray>──────────────");

        String providerNs = provider.getNamespace();
        if (providerNs.equals("ia")) {
            lore.add("<gray>Provider: <white>ItemsAdder");
            lore.add("<gray>Namespace: <white>" + innerNamespace(rawId));
            lore.add("<gray>Item ID: <white>" + displayId(rawId));
        } else if (providerNs.equals("oraxen")) {
            lore.add("<gray>Provider: <white>Oraxen");
            lore.add("<gray>Item ID: <white>" + displayId(rawId));
        } else {
            lore.add("<gray>Source: <white>" + source);
        }

        lore.add("<yellow>Click to select");
        return lore;
    }

    private static String innerNamespace(String rawId) {
        if (rawId == null) return "unknown";
        int idx = rawId.indexOf(':');
        return idx < 0 ? "itemsadder" : rawId.substring(0, idx);
    }

    private static String displayId(String full) {
        if (full == null) return "unknown";
        int idx = full.lastIndexOf(':');
        return idx < 0 ? full : full.substring(idx + 1);
    }
}
