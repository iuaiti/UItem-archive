package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RecipeEditorGui implements InventoryHolder {

    private static final int[] CRAFT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int GUI_SIZE = 45;

    private static final int SAVE_SHAPED_SLOT = 38;
    private static final int SAVE_SHAPELESS_SLOT = 40;
    private static final int CLEAR_SLOT = 42;
    private static final int RESULT_AMOUNT_SLOT = 39;
    private static final int GIVE_CURRENT_SLOT = 41;  // Easy way to grab this custom item to use as ingredient in other recipes

    private final UItemPlugin plugin;
    private final Player player;
    private final UItemData data;
    private Inventory inventory;
    private UItemRecipe existingRecipe;
    private int resultAmount = 1;

    public RecipeEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.existingRecipe = plugin.getRecipeManager().getRecipe(data.getId());
        if (this.existingRecipe != null) {
            this.resultAmount = Math.max(1, Math.min(64, this.existingRecipe.getResultAmount()));
        }
        this.inventory = org.bukkit.Bukkit.createInventory(this, GUI_SIZE,
                TextUtil.parse("<gold>Recipe: <white>" + data.getId()));
        initialize();
    }

    private void initialize() {
        // Prettier editor theme
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(" "));
            border.setItemMeta(meta);
        }
        ItemStack inner = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta im = inner.getItemMeta();
        if (im != null) {
            im.displayName(TextUtil.parse(" "));
            inner.setItemMeta(im);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                inventory.setItem(i, border.clone());
            } else {
                inventory.setItem(i, inner.clone());
            }
        }

        for (int slot : CRAFT_SLOTS) {
            inventory.setItem(slot, null);
        }

        if (existingRecipe != null && existingRecipe.isShaped()) {
            ItemStack[] matrix = existingRecipe.toMatrix();
            for (int i = 0; i < 9 && i < matrix.length; i++) {
                if (matrix[i] != null && !matrix[i].getType().isAir()) {
                    inventory.setItem(CRAFT_SLOTS[i], matrix[i].clone());
                }
            }
        } else if (existingRecipe != null && !existingRecipe.isShaped()) {
            List<UItemRecipe.Ingredient> ings = existingRecipe.getIngredients();
            for (int i = 0; i < Math.min(ings.size(), 9); i++) {
                ItemStack ingItem = ings.get(i).toItemStack();
                if (ingItem != null && !ingItem.getType().isAir()) {
                    inventory.setItem(CRAFT_SLOTS[i], ingItem);
                }
            }
        }

        ItemStack preview = plugin.getItemManager().buildPreviewItemStack(data);
        preview.setAmount(resultAmount);
        inventory.setItem(RESULT_SLOT, preview);
        inventory.setItem(RESULT_SLOT + 9, createInfoItem(Material.KNOWLEDGE_BOOK,
                "<gold><bold>Guide</bold>",
                List.of(
                        "<gray>Place ingredients (vanilla or UItem) in left",
                        "<gray>Result: this item x" + resultAmount,
                        "<yellow>Tip: /uitem give <id> then drag here",
                        "<yellow>Stack size = ingredient amount needed",
                        "",
                        "<green>Shaped</green> <gray>save pattern | <aqua>Shapeless</aqua> <gray>any order"
                )));

        inventory.setItem(SAVE_SHAPED_SLOT, createInfoItem(Material.GREEN_STAINED_GLASS_PANE,
                "<green><bold>Save Shaped</bold>",
                List.of("<gray>Save shaped recipe (3x3)")));
        inventory.setItem(SAVE_SHAPELESS_SLOT, createInfoItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "<aqua><bold>Save Shapeless</bold>",
                List.of("<gray>Save shapeless recipe")));
        inventory.setItem(CLEAR_SLOT, createInfoItem(Material.RED_STAINED_GLASS_PANE,
                "<red><bold>Delete Recipe</bold>",
                List.of("<gray>Delete current recipe")));

        // Result amount editor button
        inventory.setItem(RESULT_AMOUNT_SLOT, createInfoItem(Material.GOLD_INGOT,
                "<gold><bold>Result Amount: x" + resultAmount + "</bold>",
                List.of(
                        "<gray>Click to change output count",
                        "<gray>Current: <white>" + resultAmount,
                        "<yellow>1 - 64 items per craft"
                )));

        // Quick "get this item" button - makes using custom UItem as ingredient for other recipes very easy
        inventory.setItem(GIVE_CURRENT_SLOT, createInfoItem(Material.ENDER_CHEST,
                "<aqua><bold>Give 1x This Item</bold>",
                List.of(
                        "<gray>Receive a copy of this custom item",
                        "<yellow>Drag it into the grid above to use as ingredient",
                        "<gray>Stack size you place = required amount in recipe",
                        "<gray>Perfect for making recipes that use other custom items"
                )));

        if (existingRecipe != null) {
            inventory.setItem(44, createInfoItem(Material.LIME_DYE,
                    "<green><bold>Has recipe</bold>",
                    List.of(
                            "<gray>Type: " + (existingRecipe.isShaped() ? "Shaped" : "Shapeless"),
                            "<gray>Amount: " + existingRecipe.getResultAmount()
                    )));
        }

        int backSlot = 36;
        inventory.setItem(backSlot, createInfoItem(Material.ARROW,
                "<yellow>← Back",
                List.of("<gray>Click to go back")));

    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= GUI_SIZE) return;

        boolean isCraftSlot = false;
        for (int cs : CRAFT_SLOTS) {
            if (cs == slot) { isCraftSlot = true; break; }
        }
        if (isCraftSlot) {
            return;
        }
        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (slot == SAVE_SHAPED_SLOT) {
            saveRecipe(true);
            return;
        }
        if (slot == SAVE_SHAPELESS_SLOT) {
            saveRecipe(false);
            return;
        }
        if (slot == CLEAR_SLOT) {
            plugin.getRecipeManager().removeRecipe(data.getId());
            plugin.getRecipeManager().saveRecipesAsync();
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
            player.sendMessage(TextUtil.success("Deleted recipe for '" + data.getId() + "'!"));
            refresh();
            return;
        }
        if (slot == RESULT_AMOUNT_SLOT) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Enter result amount (1-64):",
                    input -> {
                        try {
                            int amt = Integer.parseInt(input.trim());
                            if (amt < 1 || amt > 64) {
                                player.sendMessage(TextUtil.error("Amount must be 1-64!"));
                            } else {
                                resultAmount = amt;
                                player.sendMessage(TextUtil.success("Result amount set to x" + amt));
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Invalid number!"));
                        }
                        // Update display in place to preserve placed ingredients in editor grid
                        updateResultAmountDisplay();
                    });
            return;
        }

        if (slot == GIVE_CURRENT_SLOT) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            ItemStack one = plugin.getItemManager().buildItemStack(data);
            one.setAmount(1);
            var leftovers = player.getInventory().addItem(one);
            leftovers.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            player.sendMessage(TextUtil.success("Gave you 1x '" + data.getId() + "'. Drag into the crafting grid to use as ingredient for another item!"));
            return;
        }
        if (slot == 36) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemEditorGui(plugin, player, data).open();
            return;
        }
    }

    private void updateResultAmountDisplay() {
        // Update preview amount
        ItemStack preview = inventory.getItem(RESULT_SLOT);
        if (preview != null) {
            preview.setAmount(resultAmount);
            inventory.setItem(RESULT_SLOT, preview);
        }
        // Update the amount button
        inventory.setItem(RESULT_AMOUNT_SLOT, createInfoItem(Material.GOLD_INGOT,
                "<gold><bold>Result Amount: x" + resultAmount + "</bold>",
                List.of(
                        "<gray>Click to change output count",
                        "<gray>Current: <white>" + resultAmount,
                        "<yellow>1 - 64 items per craft"
                )));
        // Update guide
        inventory.setItem(RESULT_SLOT + 9, createInfoItem(Material.KNOWLEDGE_BOOK,
                "<gold><bold>Guide</bold>",
                List.of(
                        "<gray>Place ingredients (vanilla or UItem) in left",
                        "<gray>Result: this item x" + resultAmount,
                        "<yellow>Tip: /uitem give <id> then drag here",
                        "<yellow>Stack size = ingredient amount needed",
                        "",
                        "<green>Shaped</green> <gray>save pattern | <aqua>Shapeless</aqua> <gray>any order"
                )));
    }

    public void handleDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE) {
                if (slot == RESULT_SLOT) {
                    event.setCancelled(true);
                    return;
                }
                boolean isCraft = false;
                for (int cs : CRAFT_SLOTS) {
                    if (cs == slot) { isCraft = true; break; }
                }
                if (!isCraft) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void saveRecipe(boolean shaped) {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = inventory.getItem(CRAFT_SLOTS[i]);
        }

        boolean empty = Arrays.stream(matrix).allMatch(
                item -> item == null || item.getType().isAir());
        if (empty) {
            player.sendMessage(TextUtil.error("Please place ingredients in the crafting slots!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            return;
        }

        UItemRecipe recipe = new UItemRecipe(data.getId(), resultAmount);

        if (shaped) {
            saveShaped(recipe, matrix);
        } else {
            saveShapeless(recipe, matrix);
        }

        plugin.getRecipeManager().setRecipe(data.getId(), recipe);
        plugin.getRecipeManager().saveRecipesAsync();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        player.sendMessage(TextUtil.success("Saved recipe " + (shaped ? "shaped" : "shapeless") + " for '" + data.getId() + "'!"));
        refresh();
    }

    private void saveShaped(UItemRecipe recipe, ItemStack[] matrix) {
        recipe.setShaped(true);
        List<String> shape = new ArrayList<>();
        Map<Character, UItemRecipe.Ingredient> ingMap = new HashMap<>();
        char nextChar = 'A';

        for (int r = 0; r < 3; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < 3; c++) {
                ItemStack item = matrix[r * 3 + c];
                if (item == null || item.getType().isAir()) {
                    row.append(' ');
                } else {
                    char match = findMatchingChar(item, ingMap);
                    if (match == 0) {
                        match = nextChar++;
                        UItemRecipe.Ingredient ing;
                        if (isUItem(item)) {
                            String uitemId = getUItemId(item);
                            ing = new UItemRecipe.Ingredient(uitemId, item.getAmount());
                        } else {
                            ing = new UItemRecipe.Ingredient(item.getType(), item.getAmount());
                        }
                        ingMap.put(match, ing);
                    }
                    row.append(match);
                }
            }
            shape.add(row.toString());
        }

        recipe.setShape(shape);
        recipe.setIngredientMap(ingMap);
    }

    private void saveShapeless(UItemRecipe recipe, ItemStack[] matrix) {
        recipe.setShaped(false);
        List<UItemRecipe.Ingredient> ings = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            ItemStack item = matrix[i];
            if (item != null && !item.getType().isAir()) {
                UItemRecipe.Ingredient ing;
                if (isUItem(item)) {
                    ing = new UItemRecipe.Ingredient(getUItemId(item), item.getAmount());
                } else {
                    ing = new UItemRecipe.Ingredient(item.getType(), item.getAmount());
                }
                ings.add(ing);
            }
        }

        recipe.setIngredients(ings);
    }

    private char findMatchingChar(ItemStack item, Map<Character, UItemRecipe.Ingredient> map) {
        for (Map.Entry<Character, UItemRecipe.Ingredient> entry : map.entrySet()) {
            UItemRecipe.Ingredient ing = entry.getValue();
            // Require exact match on type/uitem AND the exact amount (stack size placed = consume amount for that position)
            // This allows same material with *different* per-position amounts (they get distinct letters)
            boolean typeMatch = false;
            if (ing.getUitemId() != null) {
                String id = getUItemId(item);
                typeMatch = ing.getUitemId().equals(id);
            } else {
                typeMatch = ing.getMaterial() == item.getType();
            }
            if (typeMatch && ing.getAmount() == item.getAmount()) {
                return entry.getKey();
            }
        }
        return 0;
    }

    private boolean isUItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getItemManager().getUItemKey(),
                        org.bukkit.persistence.PersistentDataType.STRING);
    }

    private String getUItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getItemManager().getUItemKey(),
                        org.bukkit.persistence.PersistentDataType.STRING);
    }

    private void refresh() {
        ItemStack[] savedItems = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            savedItems[i] = inventory.getItem(CRAFT_SLOTS[i]);
        }

        this.existingRecipe = plugin.getRecipeManager().getRecipe(data.getId());
        if (this.existingRecipe != null) {
            this.resultAmount = Math.max(1, Math.min(64, this.existingRecipe.getResultAmount()));
        }
        initialize();

        for (int i = 0; i < 9; i++) {
            if (savedItems[i] != null && !savedItems[i].getType().isAir()) {
                inventory.setItem(CRAFT_SLOTS[i], savedItems[i]);
            }
        }
    }

    private ItemStack createInfoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            if (lore != null) {
                meta.lore(lore.stream().map(TextUtil::parse).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Called when the GUI is closed — returns craft slot items to the player
     * to prevent permanent item loss.
     */
    public void onClose(Player player) {
        for (int slot : CRAFT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                for (ItemStack leftover : player.getInventory().addItem(item.clone()).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                inventory.setItem(slot, null);
            }
        }
    }
}
