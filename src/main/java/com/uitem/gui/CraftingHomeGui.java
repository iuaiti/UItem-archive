package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Main hub for players to view recipes and open the custom crafting table. */
public class CraftingHomeGui extends AbstractGui {

    private static final int[] FEATURED_SLOTS = {29, 30, 31, 32, 33};

    public CraftingHomeGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 45, GuiConfig.text("crafting-home.title", "<gradient:#ff6b6b:#ffd93d><bold>UItem Crafting</bold></gradient>"));
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
        fillBorder(5);

        int recipeCount = plugin.getRecipeManager().getAllRecipes().size();
        Map<String, String> placeholders = Map.of("recipes", String.valueOf(recipeCount));
        setItem(4, createItem(
                GuiConfig.material("crafting-home.header.material", Material.CRAFTING_TABLE),
                GuiConfig.text("crafting-home.header.name", "<gradient:#ff6b6b:#ffd93d><bold>Custom Crafting Home</bold></gradient>", placeholders),
                GuiConfig.lore("crafting-home.header.lore", List.of(
                        "<gray>Recipes loaded: <white>{recipes}",
                        "<gray>Create custom UItems from recipes",
                        "<dark_gray>/crafting list, /crafting view <id>, /crafting craft"
                ), placeholders)));

        setItem(20, configuredItem("crafting-home.buttons.view-recipes", Material.KNOWLEDGE_BOOK,
                "<gold><bold>View Recipes</bold>", List.of(
                        "<gray>Browse every UItem recipe",
                        "<gray>Click an item to inspect its ingredients",
                        "",
                        "<yellow>Click to open recipe list"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RecipeListGui(plugin, player).openAsChildOf(this);
        });

        setItem(22, configuredItem("crafting-home.buttons.crafting-table", Material.CRAFTING_TABLE,
                "<green><bold>Open Crafting Table</bold>", List.of(
                        "<gray>Place materials or UItems in the 3x3 grid",
                        "<gray>The result preview appears on the right",
                        "<gray>Select x1, x8, x64 or MAX before crafting",
                        "",
                        "<yellow>Click to craft"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            new CraftingGui(plugin, player).open();
        });

        setItem(24, configuredItem("crafting-home.buttons.how-it-works", Material.WRITABLE_BOOK,
                "<aqua><bold>How It Works</bold>", List.of(
                        "<gray>Recipes may use vanilla materials or UItems",
                        "<gray>Shaped recipes must match the exact 3x3 layout",
                        "<gray>Shapeless recipes can be placed anywhere",
                        "<gray>Closing the crafting table safely returns inputs"
                )));

        renderFeaturedRecipes();
    }

    private void renderFeaturedRecipes() {
        List<Map.Entry<String, UItemRecipe>> recipes = new ArrayList<>(plugin.getRecipeManager().getAllRecipes().entrySet());
        recipes.sort(Comparator.comparing(Map.Entry::getKey));

        if (recipes.isEmpty()) {
            setItem(31, configuredItem("crafting-home.buttons.no-recipes", Material.BARRIER,
                    "<red>No Recipes Loaded", List.of("<gray>Create recipes in the editor or recipes/*.yml")));
            return;
        }

        setItem(27, configuredItem("crafting-home.buttons.featured-header", Material.BOOKSHELF,
                "<gold><bold>Featured Recipes</bold>", List.of("<gray>Quick preview of the first recipes")));

        int max = Math.min(FEATURED_SLOTS.length, recipes.size());
        for (int i = 0; i < max; i++) {
            Map.Entry<String, UItemRecipe> entry = recipes.get(i);
            String itemId = entry.getKey();
            UItemRecipe recipe = entry.getValue();
            var data = plugin.getItemManager().getItem(itemId);
            ItemStack item = data != null
                    ? plugin.getItemManager().buildPreviewItemStack(data, player.isOp() || player.hasPermission("uitem.id"))
                    : createItem(Material.BARRIER, "<red>Missing item: <white>" + itemId);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<net.kyori.adventure.text.Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(TextUtil.parse(""));
                lore.add(TextUtil.parse("<gold>" + (recipe.isShaped() ? "Shaped" : "Shapeless") + " <dark_gray>| <gold>x" + recipe.getResultAmount()));
                lore.add(TextUtil.parse("<yellow>Click to view recipe"));
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            setItem(FEATURED_SLOTS[i], item, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new RecipeDetailGui(plugin, player, itemId).openAsChildOf(this);
            });
        }

        if (recipes.size() > FEATURED_SLOTS.length) {
            setItem(35, configuredItem("crafting-home.buttons.more-recipes", Material.ARROW,
                    "<yellow>More Recipes", List.of("<gray>There are {more} more recipes", "<yellow>Click to browse all"),
                    Map.of("more", String.valueOf(recipes.size() - FEATURED_SLOTS.length))), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new RecipeListGui(plugin, player).openAsChildOf(this);
            });
        }
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
