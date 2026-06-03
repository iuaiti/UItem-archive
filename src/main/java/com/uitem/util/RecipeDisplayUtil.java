package com.uitem.util;

import com.uitem.recipe.UItemRecipe;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats recipe info as multiple lore lines (one ingredient per line).
 */
public final class RecipeDisplayUtil {

    private RecipeDisplayUtil() {
    }

    /**
     * Lore lines for /craft list and recipe tooltips — each ingredient on its own line.
     */
    public static List<String> craftSummaryLore(UItemRecipe recipe) {
        List<String> lines = new ArrayList<>();
        String type = recipe.isShaped() ? "Shaped" : "Shapeless";
        lines.add("<gold>" + type + " <dark_gray>| <gold>Output: x" + recipe.getResultAmount());
        lines.add("");
        lines.add("<gray>Ingredients:");
        lines.addAll(describeIngredients(recipe));
        lines.add("");
        lines.add("<yellow>Click to view recipe");
        return lines;
    }

    public static List<String> describeIngredients(UItemRecipe recipe) {
        List<String> lines = new ArrayList<>();
        if (recipe.isShaped()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (UItemRecipe.Ingredient ing : recipe.getIngredientMap().values()) {
                if (ing == null || ing.isAir()) continue;
                String label = formatIngredient(ing);
                counts.merge(label, 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                String suffix = e.getValue() > 1 ? " <dark_gray>x" + e.getValue() : "";
                lines.add("<dark_gray>• <white>" + e.getKey() + suffix);
            }
        } else {
            for (UItemRecipe.Ingredient ing : recipe.getIngredients()) {
                if (ing == null || ing.isAir()) continue;
                lines.add("<dark_gray>• <white>" + formatIngredient(ing));
            }
        }
        if (lines.isEmpty()) {
            lines.add("<dark_gray>• <red>(none)");
        }
        return lines;
    }

    public static String formatIngredient(UItemRecipe.Ingredient ing) {
        if (ing.getPotionId() != null) {
            return "<light_purple>potion:" + ing.getPotionId() + (ing.getAmount() > 1 ? " x" + ing.getAmount() : "");
        }
        if (ing.getUitemId() != null) {
            return ing.getUitemId() + (ing.getAmount() > 1 ? " x" + ing.getAmount() : "");
        }
        if (ing.getMaterial() != null) {
            return formatMaterial(ing.getMaterial()) + (ing.getAmount() > 1 ? " x" + ing.getAmount() : "");
        }
        return "?";
    }

    private static String formatMaterial(Material mat) {
        String s = mat.name().toLowerCase().replace('_', ' ');
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }
}