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

public class RecipeDetailGui extends AbstractGui {

    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int SLOT_ARROW1 = 14;
    private static final int SLOT_ARROW2 = 15;
    private static final int SLOT_RESULT = 23;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_CRAFT = 44;

    private final String itemId;
    private final String backCategory;
    private final String backSubCategory;

    public RecipeDetailGui(UItemPlugin plugin, Player player, String itemId) {
        this(plugin, player, itemId, null, null);
    }

    public RecipeDetailGui(UItemPlugin plugin, Player player, String itemId, String backCategory, String backSubCategory) {
        super(plugin, player, 45, GuiConfig.text("recipe-detail.title", "<gold>Recipe: <white>{id}", Map.of("id", itemId)));
        this.itemId = itemId;
        this.backCategory = backCategory;
        this.backSubCategory = backSubCategory;
    }

    @Override
    protected void setup() {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack inner = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                setItem(i, border);
            } else {
                setItem(i, inner);
            }
        }

        UItemRecipe recipe = plugin.getRecipeManager().getRecipe(itemId);
        UItemData data = plugin.getItemManager().getItem(itemId);

        if (recipe == null || data == null) {
            setItem(22, createItem(
                    GuiConfig.material("recipe-detail.not-found.material", Material.BARRIER),
                    GuiConfig.text("recipe-detail.not-found.name", "<red>Not found")));
            setItem(SLOT_BACK, createBackButton(), ev -> returnToRecipeList());
            return;
        }

        String type = recipe.isShaped()
                ? GuiConfig.text("recipe-detail.type-shaped", "Shaped 3x3")
                : GuiConfig.text("recipe-detail.type-shapeless", "Shapeless");
        String headerName = (data.getDisplayName() != null)
                ? data.getDisplayName()
                : "<gold><bold>" + itemId + "</bold>";
        setItem(4, createItem(
                GuiConfig.material("recipe-detail.header.material", Material.KNOWLEDGE_BOOK),
                headerName,
                GuiConfig.lore("recipe-detail.header.lore", List.of(
                        "<gray>{type}",
                        "<gray>Output: x{amount}"
                ), Map.of("type", type, "amount", String.valueOf(recipe.getResultAmount())))));

        if (recipe.isShaped()) {
            ItemStack[] matrix = recipe.toMatrix();
            Map<Character, UItemRecipe.Ingredient> ingMap = recipe.getIngredientMap();
            for (int i = 0; i < 9; i++) {
                ItemStack ing = matrix[i];
                if (ing != null && !ing.getType().isAir()) {
                    String name = findIngredientName(ing, ingMap);
                    setItem(GRID[i], addLore(ing.clone(), "<gray>" + name));
                }
            }
        } else {
            List<UItemRecipe.Ingredient> ings = recipe.getIngredients();
            for (int i = 0; i < Math.min(ings.size(), 9); i++) {
                ItemStack ing = ings.get(i).toItemStack();
                if (ing != null && !ing.getType().isAir()) {
                    String name;
                    if (ings.get(i).getUitemId() != null) {
                        UItemData ingData = plugin.getItemManager().getItem(ings.get(i).getUitemId());
                        name = (ingData != null && ingData.getDisplayName() != null)
                                ? stripMiniMessage(ingData.getDisplayName())
                                : ings.get(i).getUitemId();
                    } else {
                        name = formatMat(ing.getType());
                    }
                    setItem(GRID[i], addLore(ing.clone(), "<gray>" + name));
                }
            }
        }

        String arrowName = GuiConfig.text("recipe-detail.arrow", "<yellow>→");
        setItem(SLOT_ARROW1, createItem(Material.ARROW, arrowName));
        setItem(SLOT_ARROW2, createItem(Material.ARROW, arrowName));

        boolean showId = player.isOp() || player.hasPermission("uitem.id");
        ItemStack result = plugin.getItemManager().buildPreviewItemStack(data, showId);
        result.setAmount(recipe.getResultAmount());
        setItem(SLOT_RESULT, result);

        setItem(SLOT_BACK, createBackButton(), ev -> returnToRecipeList());

        setItem(SLOT_CRAFT, createItem(
                GuiConfig.material("recipe-detail.craft-button.material", Material.CRAFTING_TABLE),
                GuiConfig.text("recipe-detail.craft-button.name", "<green><bold>Craft</bold>"),
                GuiConfig.lore("recipe-detail.craft-button.lore", List.of("<gray>Open crafting table"))), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CraftingGui(plugin, player).open();
        });
    }

    private String findIngredientName(ItemStack item, Map<Character, UItemRecipe.Ingredient> map) {
        for (Map.Entry<Character, UItemRecipe.Ingredient> e : map.entrySet()) {
            if (e.getValue().matches(item)) {
                UItemRecipe.Ingredient ing = e.getValue();
                if (ing.getUitemId() != null) {
                    UItemData ingData = plugin.getItemManager().getItem(ing.getUitemId());
                    if (ingData != null && ingData.getDisplayName() != null) {
                        return stripMiniMessage(ingData.getDisplayName());
                    }
                    return ing.getUitemId();
                }
                if (ing.getMaterial() != null) return formatMat(ing.getMaterial());
            }
        }
        return formatMat(item.getType());
    }

    private void returnToRecipeList() {
        if (hasBackAction()) {
            goBack();
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        new RecipeListGui(plugin, player, 0, backCategory, backSubCategory).open();
    }

    private ItemStack addLore(ItemStack item, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(TextUtil.parse(line));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatMat(Material mat) {
        String s = mat.name().toLowerCase().replace("_", " ");
        String[] w = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String x : w) {
            if (x.length() > 0)
                sb.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String stripMiniMessage(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.replaceAll("<[^>]+>", "").trim();
    }
}
