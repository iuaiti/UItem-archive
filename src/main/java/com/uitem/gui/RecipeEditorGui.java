package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.RecipeIngredientResolver;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.Messages;
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

    private static final int PICK_UITEM_SLOT = 15;
    private static final int SAVE_SHAPED_SLOT = 38;
    private static final int SAVE_SHAPELESS_SLOT = 40;
    private static final int CLEAR_SLOT = 42;

    private final UItemPlugin plugin;
    private final Player player;
    private final UItemData data;
    private Inventory inventory;
    private UItemRecipe existingRecipe;

    public RecipeEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.existingRecipe = plugin.getRecipeManager().getRecipe(data.getId());
        this.inventory = org.bukkit.Bukkit.createInventory(this, GUI_SIZE,
                TextUtil.parse("<gold>Recipe: <white>" + data.getId()));
        initialize();
    }

    private void initialize() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(" "));
            border.setItemMeta(meta);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, border.clone());
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
        inventory.setItem(RESULT_SLOT, preview);
        inventory.setItem(RESULT_SLOT + 9, createInfoItem(Material.KNOWLEDGE_BOOK,
                "<gold><bold>Huong dan</bold>",
                List.of(
                        Messages.recipeEditorGuide1(),
                        Messages.recipeEditorGuide2(),
                        Messages.recipeEditorGuide3(),
                        Messages.recipeEditorGuide4(),
                        "",
                        "<green>Shaped <gray>— giu hinh dang 3x3"
                )));

        inventory.setItem(PICK_UITEM_SLOT, createInfoItem(Material.CHEST,
                Messages.recipePickerButton(),
                List.of(
                        Messages.recipePickerButtonDesc(),
                        "<gray>Item UItem lam NL"
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

        if (existingRecipe != null) {
            List<String> statusLore = new ArrayList<>();
            statusLore.add("<gray>Type: " + (existingRecipe.isShaped() ? "Shaped" : "Shapeless"));
            statusLore.add("<gray>Amount: " + existingRecipe.getResultAmount());
            appendUsedAsIngredient(statusLore);
            inventory.setItem(44, createInfoItem(Material.LIME_DYE,
                    "<green><bold>" + Messages.guiRecipeHas() + "</bold>", statusLore));
        } else {
            List<String> chainLore = new ArrayList<>();
            chainLore.add("<gray>" + Messages.guiRecipeNone());
            appendUsedAsIngredient(chainLore);
            if (chainLore.size() > 1) {
                inventory.setItem(44, createInfoItem(Material.PAPER,
                        "<yellow>Duoc dung lam NL", chainLore));
            }
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
            player.sendMessage(TextUtil.success(Messages.recipeEditorDeleted(data.getId())));
            refresh();
            return;
        }
        if (slot == PICK_UITEM_SLOT) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RecipeIngredientPickerGui(plugin, player, this, data.getId(), 0).open();
            return;
        }
        if (slot == 36) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemEditorGui(plugin, player, data).open();
            return;
        }
    }

    /**
     * Them 1 item UItem vao o nguyen lieu trong (tu picker).
     *
     * @return true neu da dat vao o trong hoac stack
     */
    public boolean addIngredientFromPicker(String uitemId) {
        if (uitemId == null || uitemId.isBlank()) {
            return false;
        }
        if (uitemId.equalsIgnoreCase(data.getId())) {
            player.sendMessage(TextUtil.error(Messages.recipePickerSelfItemError()));
            return false;
        }
        com.uitem.UItemData ingData = plugin.getItemManager().getItem(uitemId);
        if (ingData == null) {
            player.sendMessage(TextUtil.error(Messages.recipePickerItemNotFound(uitemId)));
            return false;
        }
        ItemStack stack = plugin.getItemManager().buildItemStack(ingData);
        stack.setAmount(1);
        return placeIngredient(stack);
    }

    private boolean placeIngredient(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        UItemRecipe.Ingredient incoming = resolveIngredient(stack);
        if (incoming == null) {
            player.sendMessage(TextUtil.error(Messages.recipePickerInvalidIngredient()));
            return false;
        }
        if (incoming.getUitemId() != null && incoming.getUitemId().equalsIgnoreCase(data.getId())) {
            player.sendMessage(TextUtil.error(Messages.recipePickerSelfItemError()));
            return false;
        }
        if (plugin.getPotionManager().isUPotion(stack)) {
            player.sendMessage(TextUtil.error(Messages.recipePickerNoPotionIngredient()));
            return false;
        }
        for (int craftSlot : CRAFT_SLOTS) {
            ItemStack existing = inventory.getItem(craftSlot);
            if (existing == null || existing.getType().isAir()) {
                inventory.setItem(craftSlot, stack.clone());
                return true;
            }
            UItemRecipe.Ingredient existingIng = resolveIngredient(existing);
            if (existingIng != null && existingIng.equals(incoming)) {
                existing.setAmount(existing.getAmount() + 1);
                return true;
            }
        }
        return false;
    }

    private UItemRecipe.Ingredient resolveIngredient(ItemStack item) {
        return RecipeIngredientResolver.fromStack(plugin, item);
    }

    private void appendUsedAsIngredient(List<String> lore) {
        List<String> usedIn = plugin.getRecipeManager().findRecipesUsingIngredient(data.getId());
        if (usedIn.isEmpty()) {
            return;
        }
        lore.add("");
        lore.add("<gray>Dung lam NL cho:");
        int show = Math.min(4, usedIn.size());
        for (int i = 0; i < show; i++) {
            lore.add("<white>• " + usedIn.get(i));
        }
        if (usedIn.size() > show) {
            lore.add("<dark_gray>... +" + (usedIn.size() - show) + " recipe");
        }
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
            player.sendMessage(TextUtil.error(Messages.recipeEditorEmptySlots()));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            return;
        }

        UItemRecipe recipe = new UItemRecipe(data.getId(), 1);

        if (shaped) {
            saveShaped(recipe, matrix);
        } else {
            saveShapeless(recipe, matrix);
        }

        plugin.getRecipeManager().setRecipe(data.getId(), recipe);
        plugin.getRecipeManager().saveRecipesAsync();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        player.sendMessage(TextUtil.success(Messages.recipeEditorSaved(shaped, data.getId())));
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
                        UItemRecipe.Ingredient ing = resolveIngredient(item);
                        if (ing == null) {
                            player.sendMessage(TextUtil.error(Messages.recipePickerInvalidIngredient()));
                            return;
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
                UItemRecipe.Ingredient ing = resolveIngredient(item);
                if (ing == null) {
                    player.sendMessage(TextUtil.error(Messages.recipePickerInvalidIngredient()));
                    return;
                }
                ings.add(ing);
            }
        }

        recipe.setIngredients(ings);
    }

    private char findMatchingChar(ItemStack item, Map<Character, UItemRecipe.Ingredient> map) {
        for (Map.Entry<Character, UItemRecipe.Ingredient> entry : map.entrySet()) {
            if (entry.getValue().matches(item)) {
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

    /** Quay lai editor sau khi chon NL. */
    public void reopenAfterPicker() {
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
