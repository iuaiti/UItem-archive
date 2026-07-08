package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Main hub for players to view recipes and open the custom crafting table. */
public class CraftingHomeGui extends AbstractGui {

    private static final String[] CATEGORIES = {"WEAPON", "ARMOR", "TOOL", "RANGED", "MAGIC", "CONSUMABLE", "MATERIAL", "MISC"};
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 19, 20, 21, 22};
    private static final String[] WEAPON_TYPES = {"SWORD", "AXE", "DAGGER", "SPEAR", "HAMMER", "STAFF"};
    private static final int[] WEAPON_SLOTS = {28, 29, 30, 31, 32, 33};
    private static final int SLOT_RECIPES = 37;
    private static final int SLOT_CRAFT = 40;

    public CraftingHomeGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiConfig.text("crafting-home.title", "<gradient:#ff6b6b:#ffd93d><bold>UItem Crafting</bold></gradient>"));
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
        Set<Integer> usedSlots = new HashSet<>();

        int recipeCount = plugin.getRecipeManager().getAllRecipes().size();
        Map<String, String> placeholders = Map.of("recipes", String.valueOf(recipeCount));
        int headerSlot = claimSlot(usedSlots, GuiConfig.slot("crafting-home.header.slot", 4, 54), 4);
        setItem(headerSlot, createItem(
                GuiConfig.material("crafting-home.header.material", Material.CRAFTING_TABLE),
                GuiConfig.text("crafting-home.header.name", "<gradient:#ff6b6b:#ffd93d><bold>Custom Crafting Hub</bold></gradient>", placeholders),
                GuiConfig.lore("crafting-home.header.lore", List.of(
                        "<gray>Recipes loaded: <white>{recipes}",
                        "<gray>Click a category or weapon type below"
                ), placeholders)));

        if (recipeCount == 0) {
            int noRecipesSlot = claimSlot(usedSlots, GuiConfig.slot("crafting-home.buttons.no-recipes.slot", 22, 54), 22);
            setItem(noRecipesSlot, configuredItem("crafting-home.buttons.no-recipes", Material.BARRIER,
                    "<red>No Recipes Loaded", List.of("<gray>Create recipes in the editor or recipes/*.yml")));
            return;
        }

        renderCategoryButtons(usedSlots);

        int recipesSlot = claimSlot(usedSlots, GuiConfig.slot("crafting-home.buttons.view-recipes.slot", SLOT_RECIPES, 54), SLOT_RECIPES);
        setItem(recipesSlot, configuredItem("crafting-home.buttons.view-recipes", Material.KNOWLEDGE_BOOK,
                "<gold><bold>All Recipes</bold>", List.of(
                        "<gray>Browse every UItem recipe",
                        "<gray>Click an item to inspect its ingredients",
                        "",
                        "<yellow>Click to open"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RecipeListGui(plugin, player).openAsChildOf(this);
        });

        int craftSlot = claimSlot(usedSlots, GuiConfig.slot("crafting-home.buttons.crafting-table.slot", SLOT_CRAFT, 54), SLOT_CRAFT);
        setItem(craftSlot, configuredItem("crafting-home.buttons.crafting-table", Material.CRAFTING_TABLE,
                "<green><bold>Open Crafting Table</bold>", List.of(
                        "<gray>Place materials or UItems in the 3x3 grid",
                        "<gray>The result preview appears on the right",
                        "",
                        "<yellow>Click to craft"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            new CraftingGui(plugin, player).open();
        });

        renderWeaponTypeButtons(usedSlots);
    }

    private void renderCategoryButtons(Set<Integer> usedSlots) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            String category = CATEGORIES[i];
            int count = countRecipes(category, null);
            String path = "crafting-home.categories." + category.toLowerCase();
            int slot = claimSlot(usedSlots, GuiConfig.slot(path + ".slot", CATEGORY_SLOTS[i], 54), CATEGORY_SLOTS[i]);
            if (slot < 0) continue;
            setItem(slot, configuredItem(path, defaultCategoryIcon(category),
                    "<aqua><bold>" + prettify(category) + "</bold>", List.of(
                            "<gray>Recipes: <white>{count}",
                            "<yellow>Click to browse"
                    ), Map.of("count", String.valueOf(count))), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new RecipeListGui(plugin, player, 0, category, null).openAsChildOf(this);
            });
        }
    }

    private void renderWeaponTypeButtons(Set<Integer> usedSlots) {
        for (int i = 0; i < WEAPON_TYPES.length; i++) {
            String type = WEAPON_TYPES[i];
            int count = countRecipes("WEAPON", type);
            String path = "crafting-home.weapon-types." + type.toLowerCase();
            int slot = claimSlot(usedSlots, GuiConfig.slot(path + ".slot", WEAPON_SLOTS[i], 54), WEAPON_SLOTS[i]);
            if (slot < 0) continue;
            setItem(slot, configuredItem(path, defaultWeaponIcon(type),
                    "<red><bold>" + prettify(type) + "</bold>", List.of(
                            "<gray>Weapon recipes: <white>{count}",
                            "<yellow>Click to browse"
                    ), Map.of("count", String.valueOf(count))), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new RecipeListGui(plugin, player, 0, "WEAPON", type).openAsChildOf(this);
            });
        }
    }

    private int claimSlot(Set<Integer> usedSlots, int requested, int fallback) {
        if (usedSlots.add(requested)) return requested;
        if (fallback >= 0 && fallback < 54 && usedSlots.add(fallback)) return fallback;
        for (int slot = 0; slot < 54; slot++) {
            if (usedSlots.add(slot)) return slot;
        }
        return -1;
    }

    private int countRecipes(String category, String subCategory) {
        int count = 0;
        for (Map.Entry<String, UItemRecipe> entry : plugin.getRecipeManager().getAllRecipes().entrySet()) {
            var data = plugin.getItemManager().getItem(entry.getKey());
            if (data == null) continue;
            if (category != null && !category.equalsIgnoreCase(String.valueOf(data.getCategory()))) continue;
            if (subCategory != null && !subCategory.equalsIgnoreCase(String.valueOf(data.getSubCategory()))) continue;
            count++;
        }
        return count;
    }

    private Material defaultCategoryIcon(String category) {
        return switch (category) {
            case "WEAPON" -> Material.DIAMOND_SWORD;
            case "ARMOR" -> Material.DIAMOND_CHESTPLATE;
            case "TOOL" -> Material.DIAMOND_PICKAXE;
            case "RANGED" -> Material.BOW;
            case "MAGIC" -> Material.BLAZE_ROD;
            case "CONSUMABLE" -> Material.GOLDEN_APPLE;
            case "MATERIAL" -> Material.DIAMOND;
            default -> Material.CHEST;
        };
    }

    private Material defaultWeaponIcon(String type) {
        return switch (type) {
            case "SWORD" -> Material.DIAMOND_SWORD;
            case "AXE" -> Material.DIAMOND_AXE;
            case "DAGGER" -> Material.IRON_SWORD;
            case "SPEAR" -> Material.TRIDENT;
            case "HAMMER" -> Material.MACE;
            case "STAFF" -> Material.BLAZE_ROD;
            default -> Material.IRON_SWORD;
        };
    }

    private String prettify(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private ItemStack configuredItem(String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore) {
        return configuredItem(path, fallbackMaterial, fallbackName, fallbackLore, Map.of());
    }

    private ItemStack configuredItem(String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore, Map<String, String> placeholders) {
        return createItem(
                GuiConfig.material(path + ".material", fallbackMaterial),
                GuiConfig.text(path + ".name", fallbackName, placeholders),
                GuiConfig.lore(path + ".lore", fallbackLore, placeholders)
        );
    }
}
