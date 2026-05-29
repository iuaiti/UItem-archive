package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.recipe.RecipeManager;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CraftingGui implements InventoryHolder {

    private static final int[] CRAFT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int GUI_SIZE = 45;

    private static final int CRAFT_1_SLOT = 36;
    private static final int CRAFT_8_SLOT = 37;
    private static final int CRAFT_64_SLOT = 38;
    private static final int CRAFT_MAX_SLOT = 39;

    private static final Set<UUID> openCraftingGuis = new HashSet<>();

    private final UItemPlugin plugin;
    private final Player player;
    private final RecipeManager recipeManager;
    private Inventory inventory;
    private int craftAmount = 1;

    public CraftingGui(UItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.recipeManager = plugin.getRecipeManager();
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
                TextUtil.parse("<gradient:#ff6b6b:#ffd93d>Crafting</gradient>"));
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
        inventory.setItem(RESULT_SLOT, null);

        updateCraftButtons();
    }

    private void updateCraftButtons() {
        int selected = craftAmount;

        inventory.setItem(CRAFT_1_SLOT, createAmountButton(Material.LIME_DYE, "1",
                List.of("<gray>Craft 1 item", selected == 1 ? "<green>✔ Selected" : "<yellow>Click to select")));
        inventory.setItem(CRAFT_8_SLOT, createAmountButton(Material.LIGHT_BLUE_DYE, "8",
                List.of("<gray>Craft 8 items", selected == 8 ? "<green>✔ Selected" : "<yellow>Click to select")));
        inventory.setItem(CRAFT_64_SLOT, createAmountButton(Material.CYAN_DYE, "64",
                List.of("<gray>Craft 64 items", selected == 64 ? "<green>✔ Selected" : "<yellow>Click to select")));
        inventory.setItem(CRAFT_MAX_SLOT, createAmountButton(Material.MAGENTA_DYE, "MAX",
                List.of("<gray>Craft as many as possible", selected == 999 ? "<green>✔ Selected" : "<yellow>Click to select")));
    }

    private ItemStack createAmountButton(Material mat, String label, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse("<gold><bold>Craft x" + label + "</bold>"));
            meta.lore(lore.stream().map(TextUtil::parse).toList());
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

        ItemStack[] matrix = getMatrix();
        UItemRecipe recipe = findMatchingRecipe(matrix);
        if (recipe == null) return;

        int toCraft = result.getAmount();
        int crafted = 0;

        while (crafted < toCraft) {
            if (!recipe.matches(matrix)) break;

            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(CRAFT_SLOTS[i]);
                if (item != null && !item.getType().isAir()) {
                    item.setAmount(item.getAmount() - 1);
                    if (item.getAmount() <= 0) {
                        inventory.setItem(CRAFT_SLOTS[i], null);
                    }
                }
            }
            crafted++;
        }

        if (crafted > 0) {
            ItemStack toGive = result.clone();
            toGive.setAmount(crafted);
            for (ItemStack leftover : player.getInventory().addItem(toGive).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.5f);
        }

        updateCraftButtons();
        updateResult();
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
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == CRAFT_8_SLOT) {
            event.setCancelled(true);
            craftAmount = 8;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == CRAFT_64_SLOT) {
            event.setCancelled(true);
            craftAmount = 64;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == CRAFT_MAX_SLOT) {
            event.setCancelled(true);
            craftAmount = 999;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot >= 0 && slot < GUI_SIZE) {
            boolean isCraftSlot = false;
            for (int cs : CRAFT_SLOTS) {
                if (cs == slot) { isCraftSlot = true; break; }
            }
            if (isCraftSlot) {
                SchedulerUtil.runSync(plugin, this::updateResult);
                return;
            }
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            SchedulerUtil.runSync(plugin, this::updateResult);
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

    public void onClose() {
        for (int slot : CRAFT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
                inventory.setItem(slot, null);
            }
        }
    }

    public void open() {
        openCraftingGuis.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    public static boolean isCraftingOpen(Player player) {
        return openCraftingGuis.contains(player.getUniqueId());
    }

    public static void removeCraftingPlayer(Player player) {
        openCraftingGuis.remove(player.getUniqueId());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
