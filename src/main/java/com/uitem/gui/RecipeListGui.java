package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.RecipeDisplayUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeListGui extends AbstractGui {

    private int page;
    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public RecipeListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public RecipeListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, GuiTheme.title(Messages.recipeListTitle()));
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        List<Map.Entry<String, UItemRecipe>> list = new ArrayList<>(
                plugin.getRecipeManager().getAllRecipes().entrySet());

        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, GuiTheme.header(Material.KNOWLEDGE_BOOK, Messages.recipeListHeader(),
                List.of(
                        Messages.recipeListClick(),
                        Messages.recipeListPageLine(page + 1, totalPages, list.size()))));

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int slot = SLOTS[i];
            int idx = start + i;
            if (idx >= list.size()) {
                setItem(slot, null);
                continue;
            }

            Map.Entry<String, UItemRecipe> entry = list.get(idx);
            String id = entry.getKey();
            UItemRecipe recipe = entry.getValue();
            UItemData data = plugin.getItemManager().getItem(id);
            ItemStack item = buildCraftListItem(id, recipe, data);

            String fid = id;
            setItem(slot, item, ev -> {
                GuiFeedback.click(player);
                new RecipeDetailGui(plugin, player, fid).open();
            });
        }

        if (player.hasPermission("craft") || player.hasPermission("uitem.craft")) {
            setItem(49, GuiTheme.actionPrimary(Material.CRAFTING_TABLE,
                    Messages.recipeListCraft(),
                    Messages.recipeListCraftDesc()), ev -> {
                GuiFeedback.click(player);
                new CraftingGui(plugin, player).open();
            });
        }

        setItem(45, createHomeButton(), ev -> {
            GuiFeedback.click(player);
            new MainHubGui(plugin, player).open();
        });

        if (page > 0) {
            setItem(46, createPreviousButton(), ev -> {
                GuiFeedback.click(player);
                new RecipeListGui(plugin, player, page - 1).open();
            });
        } else {
            setItem(46, GuiTheme.disabledNav());
        }

        if (page < totalPages - 1) {
            setItem(52, createNextButton(), ev -> {
                GuiFeedback.click(player);
                new RecipeListGui(plugin, player, page + 1).open();
            });
        } else {
            setItem(52, GuiTheme.disabledNav());
        }
    }

    private ItemStack buildCraftListItem(String id, UItemRecipe recipe, UItemData data) {
        if (data == null) {
            return GuiTheme.button(Material.BARRIER, "<red>" + id,
                    List.of("<gray>Item đã bị xóa"));
        }
        ItemStack item = plugin.getItemManager().buildItemStack(data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(TextUtil.buildLore(RecipeDisplayUtil.craftSummaryLore(recipe)));
            item.setItemMeta(meta);
        }
        return item;
    }
}