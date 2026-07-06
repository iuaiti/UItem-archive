package com.uitem.gui;

import com.uitem.UItemData;
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
import java.util.concurrent.ConcurrentHashMap;

public class CraftingGui implements InventoryHolder {

    private static final int GUI_SIZE = 45;

    private static final int[] DEFAULT_CRAFT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};

    private static final Set<UUID> openCraftingGuis = ConcurrentHashMap.newKeySet();

    private final UItemPlugin plugin;
    private final Player player;
    private final RecipeManager recipeManager;
    private Inventory inventory;
    private int craftUnits = 1;  // number of recipe units (each may produce >1 result items)

    private final int[] craftSlots;
    private final int resultSlot;
    private final int craft1Slot;
    private final int craft8Slot;
    private final int craft64Slot;
    private final int craftMaxSlot;
    private final int homeSlot;
    private final int recipesSlot;
    private final int gridLabelSlot;
    private final int arrowLabelSlot;
    private final int resultLabelSlot;

    public CraftingGui(UItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.recipeManager = plugin.getRecipeManager();

        this.craftSlots = resolveCraftSlots();
        this.resultSlot = GuiConfig.slot("crafting-table.slots.result", 23);
        this.craft1Slot = GuiConfig.slot("crafting-table.slots.craft-1", 36);
        this.craft8Slot = GuiConfig.slot("crafting-table.slots.craft-8", 37);
        this.craft64Slot = GuiConfig.slot("crafting-table.slots.craft-64", 38);
        this.craftMaxSlot = GuiConfig.slot("crafting-table.slots.craft-max", 39);
        this.homeSlot = GuiConfig.slot("crafting-table.slots.home", 41);
        this.recipesSlot = GuiConfig.slot("crafting-table.slots.recipes", 42);
        this.gridLabelSlot = GuiConfig.slot("crafting-table.labels.grid.slot", 13);
        this.arrowLabelSlot = GuiConfig.slot("crafting-table.labels.arrow.slot", 22);
        this.resultLabelSlot = GuiConfig.slot("crafting-table.labels.result.slot", 24);

        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
                TextUtil.parse(GuiConfig.text("crafting-table.title", "<gradient:#ff6b6b:#ffd93d>Crafting</gradient>")));
        initialize();
    }

    private static int[] resolveCraftSlots() {
        List<Integer> configured = GuiConfig.slotList("crafting-table.slots.grid", null);
        if (configured == null || configured.size() != 9) return DEFAULT_CRAFT_SLOTS.clone();
        int[] out = new int[9];
        for (int i = 0; i < 9; i++) out[i] = configured.get(i);
        return out;
    }

    private void initialize() {
        ItemStack border = new ItemStack(GuiConfig.material("crafting-table.border.primary", Material.BROWN_STAINED_GLASS_PANE));
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(" "));
            border.setItemMeta(meta);
        }
        ItemStack accent = new ItemStack(GuiConfig.material("crafting-table.border.accent", Material.ORANGE_STAINED_GLASS_PANE));
        ItemMeta am = accent.getItemMeta();
        if (am != null) {
            am.displayName(TextUtil.parse(" "));
            accent.setItemMeta(am);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                inventory.setItem(i, border.clone());
            } else {
                inventory.setItem(i, accent.clone());
            }
        }

        for (int slot : craftSlots) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(resultSlot, null);

        inventory.setItem(gridLabelSlot, createLabel("crafting-table.labels.grid", Material.CRAFTING_TABLE, "<gold>Crafting Grid"));
        inventory.setItem(arrowLabelSlot, createLabel("crafting-table.labels.arrow", Material.ARROW, "<yellow>→"));
        inventory.setItem(resultLabelSlot, createLabel("crafting-table.labels.result", Material.KNOWLEDGE_BOOK, "<gold>Result"));

        inventory.setItem(homeSlot, createLabel("crafting-table.labels.home", Material.OAK_DOOR, "<gold>Home"));
        inventory.setItem(recipesSlot, createLabel("crafting-table.labels.recipes", Material.KNOWLEDGE_BOOK, "<yellow>Recipes"));

        updateCraftButtons();
    }

    private ItemStack createLabel(String path, Material fallbackMaterial, String fallbackName) {
        ItemStack item = new ItemStack(GuiConfig.material(path + ".material", fallbackMaterial));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(GuiConfig.text(path + ".name", fallbackName)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateCraftButtons() {
        int selected = craftUnits;
        ItemStack[] matrix = getMatrix();
        int maxU = maxCraftable(matrix);
        boolean hasRecipe = maxU > 0;

        inventory.setItem(craft1Slot, createAmountButton("crafting-table.buttons.craft-1", "1",
                "<gray>Craft 1 unit", selected == 1));
        inventory.setItem(craft8Slot, createAmountButton("crafting-table.buttons.craft-8", "8",
                "<gray>Craft 8 units", selected == 8));
        inventory.setItem(craft64Slot, createAmountButton("crafting-table.buttons.craft-64", "64",
                "<gray>Craft 64 units", selected == 64));
        inventory.setItem(craftMaxSlot, createAmountButton("crafting-table.buttons.craft-max", "MAX",
                hasRecipe
                        ? GuiConfig.text("crafting-table.buttons.max-with-recipe", "<gray>Craft as many as possible: x{count}", Map.of("count", String.valueOf(maxU)))
                        : GuiConfig.text("crafting-table.buttons.max-no-recipe", "<gray>No crafting recipe available"),
                selected == 999));
    }

    private ItemStack createAmountButton(String path, String label, String lorePrefix, boolean selected) {
        Material mat = GuiConfig.material(path + ".material", Material.LIME_DYE);
        String name = GuiConfig.text(path + ".name", "<gold><bold>Craft x" + label + "</bold>");
        String suffix = selected
                ? GuiConfig.text("crafting-table.buttons.selected", "<green>✔ Selected")
                : GuiConfig.text("crafting-table.buttons.click", "<yellow>Click to select");
        List<String> lore = GuiConfig.lore(path + ".lore", List.of(lorePrefix));
        lore.add(suffix);

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            meta.lore(lore.stream().map(TextUtil::parse).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack[] getMatrix() {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = inventory.getItem(craftSlots[i]);
        }
        return matrix;
    }

    private void updateResult() {
        ItemStack[] matrix = getMatrix();
        boolean empty = Arrays.stream(matrix).allMatch(
                item -> item == null || item.getType().isAir());
        if (empty) {
            inventory.setItem(resultSlot, null);
            return;
        }

        ItemStack baseResult = recipeManager.matchRecipe(matrix);
        if (baseResult != null) {
            UItemRecipe rec = findMatchingRecipe(matrix);
            int per = (rec != null ? rec.getResultAmount() : 1);
            int maxU = maxCraftable(matrix);
            int units = (craftUnits == 999 ? maxU : Math.min(craftUnits, maxU));
            int total = units * per;
            int showAmt = Math.min(baseResult.getMaxStackSize(), Math.min(64, total));

            // Clone for preview so we can safely add temporary "click to craft" lore without polluting the real item
            ItemStack preview = baseResult.clone();
            preview.setAmount(showAmt);

            ItemMeta pm = preview.getItemMeta();
            if (pm != null) {
                List<net.kyori.adventure.text.Component> lore = pm.lore() != null ? new ArrayList<>(pm.lore()) : new ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse(GuiConfig.text("crafting-table.result.click-to-craft", "<yellow>Click to craft x{amount}", Map.of("amount", String.valueOf(showAmt)))));
                lore.add(TextUtil.parse(GuiConfig.text("crafting-table.result.max-units", "<gray>Max units craftable: {max}", Map.of("max", String.valueOf(maxU)))));
                pm.lore(lore);
                preview.setItemMeta(pm);
            }
            inventory.setItem(resultSlot, preview);
        } else {
            inventory.setItem(resultSlot, null);
        }
    }

    private int maxCraftable(ItemStack[] matrix) {
        UItemRecipe recipe = findMatchingRecipe(matrix);
        if (recipe == null) return 0;

        int max = 64;
        if (recipe.isShaped()) {
            for (Map.Entry<Character, UItemRecipe.Ingredient> e : recipe.getIngredientMap().entrySet()) {
                char ch = e.getKey();
                UItemRecipe.Ingredient ing = e.getValue();
                int needed = ing.getAmount();
                int letterCan = Integer.MAX_VALUE;
                boolean hasPos = false;
                for (int i = 0; i < 9; i++) {
                    int r = i / 3, c = i % 3;
                    if (recipe.getShape().get(r).charAt(c) == ch) {
                        hasPos = true;
                        ItemStack item = matrix[i];
                        int avail = (item != null && !item.getType().isAir()) ? item.getAmount() : 0;
                        int can = needed > 0 ? avail / needed : 64;
                        letterCan = Math.min(letterCan, can);
                    }
                }
                if (hasPos) {
                    max = Math.min(max, Math.max(0, letterCan));
                }
            }
        } else {
            List<UItemRecipe.Ingredient> ings = recipe.getIngredients();
            List<ItemStack> nonAir = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                if (matrix[i] != null && !matrix[i].getType().isAir()) {
                    nonAir.add(matrix[i]);
                }
            }
            if (nonAir.size() != ings.size()) return 0;
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
            if (!remaining.isEmpty()) return 0;
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
        ItemStack result = inventory.getItem(resultSlot);
        if (result == null || result.getType().isAir()) return;

        // Clear immediately to prevent double-click / race condition giving duplicate items
        inventory.setItem(resultSlot, null);

        int totalItemsWanted = result.getAmount();

        // Determine per-unit output from current recipe (may be >1)
        ItemStack[] curMatrix = getMatrix();
        UItemRecipe rec = findMatchingRecipe(curMatrix);
        int per = (rec != null ? rec.getResultAmount() : 1);
        int unitsWanted = (per > 0 ? totalItemsWanted / per : totalItemsWanted);
        String resultItemId = (rec != null ? rec.getResultItemId() : null);

        int craftedUnits = 0;

        while (craftedUnits < unitsWanted) {
            ItemStack[] matrix = getMatrix();
            UItemRecipe recipe = findMatchingRecipe(matrix);
            if (recipe == null) break;

            if (recipe.isShaped()) {
                List<String> rows = recipe.getShape();
                if (rows.size() != 3) break;
                for (int i = 0; i < 9; i++) {
                    int r = i / 3, c = i % 3;
                    if (r >= rows.size() || c >= rows.get(r).length()) continue;
                    char ch = rows.get(r).charAt(c);
                    if (ch == ' ') continue;
                    UItemRecipe.Ingredient ing = recipe.getIngredientMap().get(ch);
                    if (ing == null) continue;
                    ItemStack item = inventory.getItem(craftSlots[i]);
                    if (item != null && !item.getType().isAir()) {
                        int deduct = ing.getAmount();
                        item.setAmount(item.getAmount() - deduct);
                        if (item.getAmount() <= 0) {
                            inventory.setItem(craftSlots[i], null);
                        }
                    }
                }
            } else {
                // Shapeless: re-match ings to current slots to deduct exact amounts
                List<Integer> nonAirIdx = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    ItemStack it = inventory.getItem(craftSlots[i]);
                    if (it != null && !it.getType().isAir()) nonAirIdx.add(i);
                }
                List<UItemRecipe.Ingredient> remaining = new ArrayList<>(recipe.getIngredients());
                for (int idx : nonAirIdx) {
                    ItemStack item = inventory.getItem(craftSlots[idx]);
                    if (item == null || item.getType().isAir()) continue;
                    UItemRecipe.Ingredient matched = null;
                    for (UItemRecipe.Ingredient ing : remaining) {
                        if (ing.matches(item)) {
                            matched = ing;
                            break;
                        }
                    }
                    if (matched != null) {
                        remaining.remove(matched);
                        int deduct = matched.getAmount();
                        item.setAmount(item.getAmount() - deduct);
                        if (item.getAmount() <= 0) {
                            inventory.setItem(craftSlots[idx], null);
                        }
                    }
                }
            }
            craftedUnits++;
        }

        if (craftedUnits > 0) {
            int actualItems = craftedUnits * per;
            // Re-build a clean result item (do NOT clone the preview which has extra "click to craft" lore)
            ItemStack toGive = null;
            UItemData giveData = plugin.getItemManager().getItem(resultItemId);
            if (giveData != null) {
                toGive = plugin.getItemManager().buildItemStack(giveData);
            }
            if (toGive != null) {
                toGive.setAmount(actualItems);
                for (ItemStack leftover : player.getInventory().addItem(toGive).values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.5f);
        }

        updateCraftButtons();
        updateResult();
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == resultSlot) {
            event.setCancelled(true);
            takeResult();
            return;
        }

        if (slot == craft1Slot) {
            event.setCancelled(true);
            craftUnits = 1;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == craft8Slot) {
            event.setCancelled(true);
            craftUnits = 8;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == craft64Slot) {
            event.setCancelled(true);
            craftUnits = 64;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == craftMaxSlot) {
            event.setCancelled(true);
            craftUnits = 999;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == homeSlot) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            SchedulerUtil.runSync(plugin, () -> new CraftingHomeGui(plugin, player).open());
            return;
        }

        if (slot == recipesSlot) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            SchedulerUtil.runSync(plugin, () -> new RecipeListGui(plugin, player).open());
            return;
        }

        if (slot >= 0 && slot < GUI_SIZE) {
            boolean isCraftSlot = false;
            for (int cs : craftSlots) {
                if (cs == slot) { isCraftSlot = true; break; }
            }
            if (isCraftSlot) {
                SchedulerUtil.runSync(plugin, () -> {
                    updateResult();
                    updateCraftButtons();
                });
                return;
            }
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // Shift-click from inside the GUI (craft/result slots) - cancel all
            if (slot < GUI_SIZE) {
                event.setCancelled(true);
                return;
            }
            // Shift-click from player inventory into the craft grid
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            // Fill ALL empty craft slots (not just the first one)
            int remaining = clicked.getAmount();
            for (int cs : craftSlots) {
                if (remaining <= 0) break;
                ItemStack existing = inventory.getItem(cs);
                if (existing == null || existing.getType().isAir()) {
                    ItemStack one = clicked.clone();
                    one.setAmount(1);
                    inventory.setItem(cs, one);
                    remaining--;
                }
            }

            // Update the player slot again - setAmount(0) is unreliable, use setItem directly
            if (remaining != clicked.getAmount()) {
                int clickedSlot = event.getSlot();
                if (remaining <= 0) {
                    player.getInventory().setItem(clickedSlot, null);
                } else {
                    clicked.setAmount(remaining);
                    player.getInventory().setItem(clickedSlot, clicked);
                }
                SchedulerUtil.runSync(plugin, () -> {
                    updateResult();
                    updateCraftButtons();
                });
            }
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE) {
                if (slot == resultSlot || slot == craft1Slot || slot == craft8Slot
                        || slot == craft64Slot || slot == craftMaxSlot) {
                    event.setCancelled(true);
                    return;
                }
                boolean isCraft = false;
                for (int cs : craftSlots) {
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
        for (int slot : craftSlots) {
            returnItem(inventory.getItem(slot));
            inventory.setItem(slot, null);
        }
        // Clear result slot WITHOUT returning it to prevent item duplication
        inventory.setItem(resultSlot, null);
    }

    public void open() {
        openCraftingGuis.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    public static boolean isCraftingOpen(Player player) {
        return openCraftingGuis.contains(player.getUniqueId());
    }

    public static void removeCraftingPlayer(Player player) {
        if (player == null) {
        // Bulk cleanup on plugin disable - clear all tracked players
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
