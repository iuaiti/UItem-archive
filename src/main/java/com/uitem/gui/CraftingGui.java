package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.recipe.RecipeManager;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CraftingGui implements InventoryHolder {

    private static final int[] CRAFT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int GUI_SIZE = 45;

    private static final int CRAFT_1_SLOT = 36;
    private static final int CRAFT_8_SLOT = 37;
    private static final int CRAFT_64_SLOT = 38;
    private static final int CRAFT_MAX_SLOT = 39;

    private static final Set<UUID> openCraftingGuis = ConcurrentHashMap.newKeySet();

    private final UItemPlugin plugin;
    private final Player player;
    private final RecipeManager recipeManager;
    private Inventory inventory;
    private int craftAmount = 1;

    public CraftingGui(UItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.recipeManager = plugin.getRecipeManager();
        String titleRaw = plugin.getConfig().getString("craft.gui-title", "");
        if (titleRaw == null || titleRaw.isBlank()) {
            titleRaw = Messages.craftGuiTitle();
        }
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, TextUtil.parse(titleRaw));
        initialize();
    }

    private void initialize() {
        GuiTheme.frameCraftingInventory(inventory, GUI_SIZE, CRAFT_SLOTS,
                RESULT_SLOT, CRAFT_1_SLOT, CRAFT_8_SLOT, CRAFT_64_SLOT, CRAFT_MAX_SLOT);
        updateCraftButtons();
    }

    private void updateCraftButtons() {
        int selected = craftAmount;

        inventory.setItem(CRAFT_1_SLOT, createAmountButton(Material.LIME_DYE, "1",
                List.of(Messages.craftAmount1(), selected == 1 ? Messages.craftSelected() : Messages.craftClickToSelect())));
        inventory.setItem(CRAFT_8_SLOT, createAmountButton(Material.LIGHT_BLUE_DYE, "8",
                List.of(Messages.craftAmount8(), selected == 8 ? Messages.craftSelected() : Messages.craftClickToSelect())));
        inventory.setItem(CRAFT_64_SLOT, createAmountButton(Material.CYAN_DYE, "64",
                List.of(Messages.craftAmount64(), selected == 64 ? Messages.craftSelected() : Messages.craftClickToSelect())));
        inventory.setItem(CRAFT_MAX_SLOT, createAmountButton(Material.MAGENTA_DYE, "MAX",
                List.of(Messages.craftAmountMax(), selected == 999 ? Messages.craftSelected() : Messages.craftClickToSelect())));
    }

    private ItemStack createAmountButton(Material mat, String label, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse("<white>x" + label));
            meta.lore(TextUtil.buildLore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack[] getMatrix() {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = inventory.getItem(CRAFT_SLOTS[i]);
        }
        return matrix;
    }

    private void updateResult() {
        ItemStack[] matrix = getMatrix();
        boolean empty = Arrays.stream(matrix).allMatch(
                item -> item == null || item.getType().isAir());
        if (empty) {
            inventory.setItem(RESULT_SLOT, null);
            return;
        }

        ItemStack result = recipeManager.matchRecipe(matrix);
        if (result != null) {
            int maxCanCraft = maxCraftable(matrix);
            if (craftAmount == 999) {
                result.setAmount(Math.min(result.getMaxStackSize(), Math.min(64, maxCanCraft)));
            } else {
                result.setAmount(Math.min(result.getMaxStackSize(), Math.min(craftAmount, maxCanCraft)));
            }
        }
        inventory.setItem(RESULT_SLOT, result);
    }

    private int maxCraftable(ItemStack[] matrix) {
        UItemRecipe recipe = findMatchingRecipe(matrix);
        if (recipe == null) return 0;

        int max = 64;
        if (recipe.isShaped()) {
            for (Map.Entry<Character, UItemRecipe.Ingredient> e : recipe.getIngredientMap().entrySet()) {
                char ch = e.getKey();
                UItemRecipe.Ingredient ing = e.getValue();
                int available = 0;
                for (int i = 0; i < 9; i++) {
                    int r = i / 3, c = i % 3;
                    if (recipe.getShape().get(r).charAt(c) == ch) {
                        ItemStack item = matrix[i];
                        if (item != null && !item.getType().isAir()) {
                            available = Math.max(available, item.getAmount());
                        }
                    }
                }
                int needed = ing.getAmount();
                int canCraft = needed > 0 ? available / needed : 64;
                max = Math.min(max, canCraft);
            }
        } else {
            List<UItemRecipe.Ingredient> ings = recipe.getIngredients();
            List<ItemStack> nonAir = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                if (matrix[i] != null && !matrix[i].getType().isAir()) {
                    nonAir.add(matrix[i]);
                }
            }
            List<UItemRecipe.Ingredient> remaining = new ArrayList<>(ings);
            for (ItemStack item : nonAir) {
                UItemRecipe.Ingredient matched = null;
                for (UItemRecipe.Ingredient ing : remaining) {
                    if (ing.matches(item)) {
                        matched = ing;
                        break;
                    }
                }
                if (matched != null) {
                    remaining.remove(matched);
                    int ingAmount = matched.getAmount();
                    int canCraft = ingAmount > 0 ? item.getAmount() / ingAmount : 64;
                    max = Math.min(max, canCraft);
                }
            }
        }

        return Math.max(0, max);
    }

    private UItemRecipe findMatchingRecipe(ItemStack[] matrix) {
        for (UItemRecipe recipe : plugin.getRecipeManager().getAllRecipes().values()) {
            if (recipe.matches(matrix)) {
                return recipe;
            }
        }
        return null;
    }

    private void takeResult() {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType().isAir()) return;

        int toCraft = result.getAmount();
        int crafted = 0;

        while (crafted < toCraft) {
            ItemStack[] matrix = getMatrix();
            UItemRecipe recipe = findMatchingRecipe(matrix);
            if (recipe == null || !consumeOneCraft(recipe)) {
                break;
            }
            crafted++;
        }

        if (crafted > 0) {
            ItemStack toGive = result.clone();
            toGive.setAmount(crafted);
            for (ItemStack leftover : player.getInventory().addItem(toGive).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
            GuiFeedback.success(player);
        }

        updateCraftButtons();
        updateResult();
    }

    private boolean consumeOneCraft(UItemRecipe recipe) {
        ItemStack[] matrix = getMatrix();
        if (!recipe.matches(matrix)) {
            return false;
        }

        if (recipe.isShaped()) {
            List<String> rows = recipe.getShape();
            if (rows.size() != 3) {
                return false;
            }
            for (int r = 0; r < 3; r++) {
                String row = rows.get(r);
                if (row == null || row.length() != 3) {
                    return false;
                }
                for (int c = 0; c < 3; c++) {
                    int idx = r * 3 + c;
                    char ch = row.charAt(c);
                    if (ch == ' ') {
                        continue;
                    }
                    UItemRecipe.Ingredient ing = recipe.getIngredientMap().get(ch);
                    if (ing == null) {
                        return false;
                    }
                    ItemStack item = inventory.getItem(CRAFT_SLOTS[idx]);
                    if (item == null || !ing.matches(item)) {
                        return false;
                    }
                    int left = item.getAmount() - ing.getAmount();
                    if (left <= 0) {
                        inventory.setItem(CRAFT_SLOTS[idx], null);
                    } else {
                        item.setAmount(left);
                    }
                }
            }
            return true;
        }

        List<UItemRecipe.Ingredient> remaining = new ArrayList<>(recipe.getIngredients());
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(CRAFT_SLOTS[i]);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            boolean matched = false;
            for (Iterator<UItemRecipe.Ingredient> it = remaining.iterator(); it.hasNext(); ) {
                UItemRecipe.Ingredient ing = it.next();
                if (ing.matches(item)) {
                    int left = item.getAmount() - ing.getAmount();
                    if (left <= 0) {
                        inventory.setItem(CRAFT_SLOTS[i], null);
                    } else {
                        item.setAmount(left);
                    }
                    it.remove();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            takeResult();
            return;
        }

        if (slot == CRAFT_1_SLOT) {
            event.setCancelled(true);
            craftAmount = 1;
            updateCraftButtons();
            updateResult();
            GuiFeedback.click(player);
            return;
        }

        if (slot == CRAFT_8_SLOT) {
            event.setCancelled(true);
            craftAmount = 8;
            updateCraftButtons();
            updateResult();
            GuiFeedback.click(player);
            return;
        }

        if (slot == CRAFT_64_SLOT) {
            event.setCancelled(true);
            craftAmount = 64;
            updateCraftButtons();
            updateResult();
            GuiFeedback.click(player);
            return;
        }

        if (slot == CRAFT_MAX_SLOT) {
            event.setCancelled(true);
            craftAmount = 999;
            updateCraftButtons();
            updateResult();
            GuiFeedback.click(player);
            return;
        }

        if (slot >= 0 && slot < GUI_SIZE) {
            boolean isCraftSlot = false;
            for (int cs : CRAFT_SLOTS) {
                if (cs == slot) { isCraftSlot = true; break; }
            }
            if (isCraftSlot) {
                SchedulerUtil.runSync(plugin, player, this::updateResult);
                return;
            }
            event.setCancelled(true);
            GuiFeedback.click(player, false);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (slot < GUI_SIZE) {
                if (slot == RESULT_SLOT) {
                    event.setCancelled(true);
                    return;
                }
                SchedulerUtil.runSync(plugin, player, this::updateResult);
                return;
            }
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            for (int cs : CRAFT_SLOTS) {
                ItemStack existing = inventory.getItem(cs);
                if (existing == null || existing.getType().isAir()) {
                    ItemStack one = clicked.clone();
                    one.setAmount(1);
                    inventory.setItem(cs, one);
                    clicked.setAmount(clicked.getAmount() - 1);
                    SchedulerUtil.runSync(plugin, player, this::updateResult);
                    return;
                }
            }
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE) {
                if (slot == RESULT_SLOT || slot == CRAFT_1_SLOT || slot == CRAFT_8_SLOT
                        || slot == CRAFT_64_SLOT || slot == CRAFT_MAX_SLOT) {
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

    private void returnItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    public void onClose() {
        // Bug fix: Only return items from craft input slots, NOT the result preview slot.
        // The result slot contains a preview of what would be crafted — it was never
        // actually crafted, so returning it would allow item duplication.
        for (int slot : CRAFT_SLOTS) {
            returnItem(inventory.getItem(slot));
            inventory.setItem(slot, null);
        }
        // Clear result slot WITHOUT returning it to prevent item duplication
        inventory.setItem(RESULT_SLOT, null);
    }

    public void open() {
        if (!player.hasPermission("craft") && !player.hasPermission("uitem.craft")) {
            player.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }
        openCraftingGuis.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    public static boolean isCraftingOpen(Player player) {
        return openCraftingGuis.contains(player.getUniqueId());
    }

    public static void removeCraftingPlayer(Player player) {
        if (player == null) {
            // Bulk cleanup on plugin disable — clear all tracked players
            openCraftingGuis.clear();
        } else {
            openCraftingGuis.remove(player.getUniqueId());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
