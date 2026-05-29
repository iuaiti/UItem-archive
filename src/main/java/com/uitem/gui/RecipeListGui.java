package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeListGui extends AbstractGui {

    private int page;

    private static final int ITEMS_PER_PAGE = 7;
    private static final int[] RECIPE_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    public RecipeListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public RecipeListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d>Recipe List</gradient>");
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.KNOWLEDGE_BOOK,
                "<gradient:#ff6b6b:#ffd93d><bold>All Crafting Recipes</bold></gradient>",
                List.of(
                        "<gray>Browse all available recipes",
                        "<yellow>Click a recipe <gray>to view details",
                        "",
                        "<green>Open crafting: <white>Click button below"
                )));

        Map<String, UItemRecipe> allRecipes = plugin.getRecipeManager().getAllRecipes();
        List<Map.Entry<String, UItemRecipe>> recipeList = new ArrayList<>(allRecipes.entrySet());

        int totalPages = Math.max(1, (int) Math.ceil((double) recipeList.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < RECIPE_SLOTS.length; i++) {
            int idx = startIndex + i;
            if (idx >= recipeList.size()) break;

            Map.Entry<String, UItemRecipe> entry = recipeList.get(idx);
            String itemId = entry.getKey();
            UItemRecipe recipe = entry.getValue();
            UItemData data = plugin.getItemManager().getItem(itemId);

            ItemStack displayItem;
            if (data != null) {
                displayItem = plugin.getItemManager().buildPreviewItemStack(data);
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<net.kyori.adventure.text.Component> lore = meta.lore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(net.kyori.adventure.text.Component.empty());
                    lore.add(TextUtil.parse("<gold>Recipe: <white>" + (recipe.isShaped() ? "Shaped" : "Shapeless")));
                    lore.add(TextUtil.parse("<gold>Output: <white>x" + recipe.getResultAmount()));
                    lore.add(net.kyori.adventure.text.Component.empty());
                    lore.add(TextUtil.parse("<dark_gray>Ingredients:"));
                    if (recipe.isShaped()) {
                        for (String row : recipe.getShape()) {
                            StringBuilder rowStr = new StringBuilder("<dark_gray>  ");
                            for (char c : row.toCharArray()) {
                                if (c == ' ') {
                                    rowStr.append("<gray>⬜ ");
                                } else {
                                    rowStr.append("<gold>⬛ ");
                                }
                            }
                            lore.add(TextUtil.parse(rowStr.toString()));
                        }
                        for (Map.Entry<Character, UItemRecipe.Ingredient> ingEntry : recipe.getIngredientMap().entrySet()) {
                            UItemRecipe.Ingredient ing = ingEntry.getValue();
                            String ingName = ing.getUitemId() != null ? ing.getUitemId() :
                                    ing.getMaterial() != null ? ing.getMaterial().name() : "?";
                            lore.add(TextUtil.parse("<gray>  " + ingEntry.getKey() + ": <white>" + ingName + " <dark_gray>x" + ing.getAmount()));
                        }
                    } else {
                        for (UItemRecipe.Ingredient ing : recipe.getIngredients()) {
                            String ingName = ing.getUitemId() != null ? ing.getUitemId() :
                                    ing.getMaterial() != null ? ing.getMaterial().name() : "?";
                            lore.add(TextUtil.parse("<gray>  • <white>" + ingName + " <dark_gray>x" + ing.getAmount()));
                        }
                    }
                    meta.lore(lore);
                    displayItem.setItemMeta(meta);
                }
            } else {
                displayItem = createItem(Material.BARRIER,
                        "<red>" + itemId + " <dark_gray>(deleted)",
                        List.of("<gray>This item no longer exists"));
            }

            setItem(RECIPE_SLOTS[i], displayItem);
        }

        // Craft button
        setItem(49, createItem(Material.CRAFTING_TABLE,
                "<green><bold>Open Crafting Table</bold>",
                List.of("<gray>Click to open crafting GUI")), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CraftingGui(plugin, player).open();
        });

        // Navigation
        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page - 1).open();
                } : null);

        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page + 1).open();
                } : null);

        setItem(45, createItem(Material.ARROW,
                "<yellow>← Back to Editor",
                List.of("<gray>Click to go back")), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemListGui(plugin, player).open();
        });
    }
}
