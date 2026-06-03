package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.RecipeDisplayUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
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

    public RecipeDetailGui(UItemPlugin plugin, Player player, String itemId) {
        super(plugin, player, 45, GuiTheme.title("Recipe: " + itemId));
        this.itemId = itemId;
    }

    @Override
    protected void setup() {
        for (int i = 0; i < 45; i++) {
            setItem(i, GuiTheme.border());
        }
        setItem(2, GuiTheme.accent());
        setItem(6, GuiTheme.accent());
        setItem(38, GuiTheme.accent());
        setItem(42, GuiTheme.accent());

        UItemRecipe recipe = plugin.getRecipeManager().getRecipe(itemId);
        UItemData data = plugin.getItemManager().getItem(itemId);

        if (recipe == null || data == null) {
            setItem(22, GuiTheme.button(Material.BARRIER, "<red>Not found", List.of()));
            setItem(SLOT_BACK, createBackButton(), ev -> new RecipeListGui(plugin, player).open());
            return;
        }

        List<String> infoLore = new ArrayList<>();
        infoLore.add("<gray>" + (recipe.isShaped() ? "Shaped 3x3" : "Shapeless"));
        infoLore.add("<gray>Output: x" + recipe.getResultAmount());
        infoLore.add("");
        infoLore.add("<gray>Ingredients:");
        infoLore.addAll(RecipeDisplayUtil.describeIngredients(recipe));

        setItem(4, GuiTheme.header(Material.KNOWLEDGE_BOOK, itemId, TextUtil.expandLoreLines(infoLore)));

        if (recipe.isShaped()) {
            ItemStack[] matrix = recipe.toMatrix();
            Map<Character, UItemRecipe.Ingredient> ingMap = recipe.getIngredientMap();
            for (int i = 0; i < 9; i++) {
                ItemStack ing = matrix[i];
                if (ing != null && !ing.getType().isAir()) {
                    String name = findIngredientName(ing, ingMap);
                    setItem(GRID[i], addLore(ing.clone(), "<gray>" + name));
                } else {
                    setItem(GRID[i], null);
                }
            }
        } else {
            List<UItemRecipe.Ingredient> ings = recipe.getIngredients();
            for (int i = 0; i < 9; i++) {
                if (i < ings.size()) {
                    ItemStack ing = ings.get(i).toItemStack();
                    if (ing != null && !ing.getType().isAir()) {
                        String name = ings.get(i).getUitemId() != null ? ings.get(i).getUitemId()
                                : formatMat(ing.getType());
                        setItem(GRID[i], addLore(ing.clone(), "<gray>" + name));
                    }
                } else {
                    setItem(GRID[i], null);
                }
            }
        }

        setItem(SLOT_ARROW1, GuiTheme.button(Material.SPECTRAL_ARROW, "<yellow>→", List.of()));
        setItem(SLOT_ARROW2, GuiTheme.button(Material.SPECTRAL_ARROW, "<yellow>→", List.of()));

        ItemStack result = plugin.getItemManager().buildItemStack(data);
        result.setAmount(recipe.getResultAmount());
        setItem(SLOT_RESULT, result);

        setItem(SLOT_BACK, createBackButton(), ev -> {
            GuiFeedback.click(player);
            new RecipeListGui(plugin, player).open();
        });

        if (player.hasPermission("craft") || player.hasPermission("uitem.craft")) {
            setItem(SLOT_CRAFT, GuiTheme.actionPrimary(Material.CRAFTING_TABLE,
                    "Craft",
                    "<gray>Mo ban che tao"), ev -> {
                GuiFeedback.click(player);
                new CraftingGui(plugin, player).open();
            });
        }
    }

    private String findIngredientName(ItemStack item, Map<Character, UItemRecipe.Ingredient> map) {
        for (Map.Entry<Character, UItemRecipe.Ingredient> e : map.entrySet()) {
            if (e.getValue().matches(item)) {
                UItemRecipe.Ingredient ing = e.getValue();
                if (ing.getUitemId() != null) return ing.getUitemId();
                if (ing.getMaterial() != null) return formatMat(ing.getMaterial());
            }
        }
        return formatMat(item.getType());
    }

    private ItemStack addLore(ItemStack item, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            else lore = new ArrayList<>(TextUtil.withLeadingSpacer(lore));
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
            if (!x.isEmpty())
                sb.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}