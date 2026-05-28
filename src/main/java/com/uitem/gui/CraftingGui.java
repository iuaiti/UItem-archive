package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.recipe.RecipeManager;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
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

import java.util.Arrays;
import java.util.List;

public class CraftingGui implements InventoryHolder {

    private static final int[] CRAFT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int GUI_SIZE = 45;

    private final UItemPlugin plugin;
    private final Player player;
    private final RecipeManager recipeManager;
    private Inventory inventory;

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
            meta.displayName(Component.text(" "));
            border.setItemMeta(meta);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, border.clone());
        }

        for (int slot : CRAFT_SLOTS) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(RESULT_SLOT, null);
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
        inventory.setItem(RESULT_SLOT, result);
    }

    private void takeResult() {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType().isAir()) return;

        ItemStack[] matrix = getMatrix();

        ItemStack match = recipeManager.matchRecipe(matrix);
        if (match == null) return;

        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(CRAFT_SLOTS[i]);
            if (item != null && !item.getType().isAir()) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    inventory.setItem(CRAFT_SLOTS[i], null);
                }
            }
        }

        for (ItemStack leftover : player.getInventory().addItem(match.clone()).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.5f);
        updateResult();
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            takeResult();
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

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
