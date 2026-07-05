package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecipeListGui extends AbstractGui {

    private int page;
    private final String categoryFilter;
    private final String subCategoryFilter;
    // 4 rows × 7 columns (skip first and last column as border) = 28 slots
    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public RecipeListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0, null, null);
    }

    public RecipeListGui(UItemPlugin plugin, Player player, int page) {
        this(plugin, player, page, null, null);
    }

    public RecipeListGui(UItemPlugin plugin, Player player, int page, String categoryFilter, String subCategoryFilter) {
        super(plugin, player, 54, title(categoryFilter, subCategoryFilter));
        this.page = page;
        this.categoryFilter = normalize(categoryFilter);
        this.subCategoryFilter = normalize(subCategoryFilter);
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.craft")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.KNOWLEDGE_BOOK,
                "<gradient:#ff6b6b:#ffd93d><bold>Recipe List</bold></gradient>",
                List.of(
                        filterLabel(),
                        "<gray>Click a recipe to view details"
                )));

        List<Map.Entry<String, UItemRecipe>> list = filteredRecipes();

        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;

            Map.Entry<String, UItemRecipe> entry = list.get(idx);
            String id = entry.getKey();
            UItemRecipe recipe = entry.getValue();
            UItemData data = plugin.getItemManager().getItem(id);

            String type = recipe.isShaped() ? "Shaped" : "Shapeless";
            ItemStack item;
            if (data != null) {
                boolean showId = player.isOp() || player.hasPermission("uitem.id");
                item = plugin.getItemManager().buildPreviewItemStack(data, showId);
                var meta = item.getItemMeta();
                if (meta != null) {
                    var lore = meta.lore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(TextUtil.parse(""));
                    lore.add(TextUtil.parse("<gold>" + type + " <dark_gray>| <gold>x" + recipe.getResultAmount()));
                    lore.add(TextUtil.parse("<yellow>Click to view the recipe"));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
            } else {
                item = createItem(Material.BARRIER, "<red>" + id,
                        List.of("<gray>Item deleted"));
            }

            String fid = id;
            setItem(SLOTS[i], item, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new RecipeDetailGui(plugin, player, fid, categoryFilter, subCategoryFilter).openAsChildOf(this);
            });
        }

        setItem(49, createItem(Material.CRAFTING_TABLE,
                "<green><bold>Craft</bold>",
                List.of("<gray>Open crafting table")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CraftingGui(plugin, player).open();
        });

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page - 1, categoryFilter, subCategoryFilter).open(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page + 1, categoryFilter, subCategoryFilter).open(); } : null);
    }

    private List<Map.Entry<String, UItemRecipe>> filteredRecipes() {
        List<Map.Entry<String, UItemRecipe>> list = new ArrayList<>();
        for (Map.Entry<String, UItemRecipe> entry : plugin.getRecipeManager().getAllRecipes().entrySet()) {
            UItemData data = plugin.getItemManager().getItem(entry.getKey());
            if (matchesFilter(data)) {
                list.add(entry);
            }
        }
        return list;
    }

    private boolean matchesFilter(UItemData data) {
        if (categoryFilter == null && subCategoryFilter == null) return true;
        if (data == null) return false;
        if (categoryFilter != null && !categoryFilter.equals(normalize(data.getCategory()))) return false;
        return subCategoryFilter == null || subCategoryFilter.equals(normalize(data.getSubCategory()));
    }

    private String filterLabel() {
        if (categoryFilter == null && subCategoryFilter == null) return "<gray>Filter: <white>All recipes";
        if (subCategoryFilter != null) return "<gray>Filter: <white>" + prettify(categoryFilter) + " / " + prettify(subCategoryFilter);
        return "<gray>Filter: <white>" + prettify(categoryFilter);
    }

    private static String title(String category, String subCategory) {
        String cat = normalize(category);
        String sub = normalize(subCategory);
        if (cat == null && sub == null) return "<gradient:#ff6b6b:#ffd93d>Recipe List</gradient>";
        return "<gradient:#ff6b6b:#ffd93d>Recipes: </gradient><white>" + prettify(cat) + (sub != null ? " / " + prettify(sub) : "");
    }

    private static String normalize(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String prettify(String raw) {
        if (raw == null) return "All";
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
