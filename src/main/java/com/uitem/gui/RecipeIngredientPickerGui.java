package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.potion.UPotionData;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chọn item hoặc potion UItem làm nguyên liệu recipe.
 */
public class RecipeIngredientPickerGui extends AbstractGui {

    public enum Mode {
        ITEM, POTION
    }

    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final RecipeEditorGui recipeEditor;
    private final String resultItemId;
    private final Mode mode;
    private int page;

    public RecipeIngredientPickerGui(UItemPlugin plugin, Player player, RecipeEditorGui recipeEditor,
                                     String resultItemId, Mode mode, int page) {
        super(plugin, player, 54, GuiTheme.title(
                mode == Mode.POTION ? Messages.recipePickerPotionTitle() : Messages.recipePickerTitle()));
        this.recipeEditor = recipeEditor;
        this.resultItemId = resultItemId;
        this.mode = mode;
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        if (mode == Mode.ITEM) {
            setupItemMode();
        } else {
            setupPotionMode();
        }

        setItem(48, mode == Mode.ITEM
                ? GuiTheme.glowing(Material.CHEST, Messages.recipePickerTabItems(), List.of("<green>Dang chon"))
                : GuiTheme.button(Material.CHEST, Messages.recipePickerTabItems(), List.of("<yellow>Click chuyen")),
                ev -> new RecipeIngredientPickerGui(plugin, player, recipeEditor, resultItemId, Mode.ITEM, 0).open());

        setItem(50, mode == Mode.POTION
                ? GuiTheme.glowing(Material.BREWING_STAND, Messages.recipePickerTabPotions(), List.of("<green>Dang chon"))
                : GuiTheme.button(Material.BREWING_STAND, Messages.recipePickerTabPotions(), List.of("<yellow>Click chuyen")),
                ev -> new RecipeIngredientPickerGui(plugin, player, recipeEditor, resultItemId, Mode.POTION, 0).open());

        if (page > 0) {
            setItem(45, createPreviousButton(), ev -> new RecipeIngredientPickerGui(
                    plugin, player, recipeEditor, resultItemId, mode, page - 1).open());
        }
        setItem(49, createBackButton(), ev -> recipeEditor.open());
    }

    private void setupItemMode() {
        List<UItemData> items = new ArrayList<>(plugin.getItemManager().getAllItems().values());
        items.sort(Comparator.comparing(UItemData::getId, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, GuiTheme.header(Material.HOPPER, Messages.recipePickerTitle(),
                List.of(
                        Messages.recipePickerDesc(),
                        "<gray>Ket qua item: <white>" + resultItemId,
                        "<yellow>Trang: <white>" + (page + 1) + "<gray>/<white>" + totalPages
                )));

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            int slot = SLOTS[i];
            if (idx >= items.size()) {
                setItem(slot, null);
                continue;
            }
            UItemData data = items.get(idx);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>ID: <white>" + data.getId());
            if (data.getId().equalsIgnoreCase(resultItemId)) {
                lore.add("<red>Khong dung chinh item ket qua");
            } else {
                lore.add("<yellow>Click → them NL");
            }
            setItem(slot, GuiTheme.glowing(
                    plugin.getItemManager().buildPreviewItemStack(data).getType(),
                    data.getDisplayName() != null ? data.getDisplayName() : data.getId(),
                    lore), ev -> pickItem(data.getId()));
        }
        if (page < totalPages - 1) {
            setItem(53, createNextButton(), ev -> new RecipeIngredientPickerGui(
                    plugin, player, recipeEditor, resultItemId, mode, page + 1).open());
        }
    }

    private void setupPotionMode() {
        List<UPotionData> potions = new ArrayList<>(plugin.getPotionManager().getAll().values());
        potions.sort(Comparator.comparing(UPotionData::getId, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) potions.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, GuiTheme.header(Material.BREWING_STAND, Messages.recipePickerPotionTitle(),
                List.of(
                        Messages.recipePickerPotionDesc(),
                        "<gray>Ket qua item: <white>" + resultItemId,
                        "<yellow>Trang: <white>" + (page + 1) + "<gray>/<white>" + totalPages
                )));

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            int slot = SLOTS[i];
            if (idx >= potions.size()) {
                setItem(slot, null);
                continue;
            }
            UPotionData data = potions.get(idx);
            ItemStack preview = plugin.getPotionManager().buildPreview(data);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>ID: <white>" + data.getId());
            lore.add("<gray>Loai: <white>" + data.getKind().name());
            lore.add("<yellow>Click → them NL potion");
            setItem(slot, preview, ev -> pickPotion(data.getId()));
        }
        if (page < totalPages - 1) {
            setItem(53, createNextButton(), ev -> new RecipeIngredientPickerGui(
                    plugin, player, recipeEditor, resultItemId, mode, page + 1).open());
        }
    }

    private void pickItem(String id) {
        GuiFeedback.click(player);
        if (id.equalsIgnoreCase(resultItemId)) {
            player.sendMessage(TextUtil.error(Messages.recipePickerSelfError()));
            return;
        }
        if (!recipeEditor.addIngredientFromPicker(id)) {
            player.sendMessage(TextUtil.error(Messages.recipePickerFull()));
            return;
        }
        player.sendMessage(TextUtil.success(Messages.recipePickerAdded(id)));
        recipeEditor.open();
    }

    private void pickPotion(String id) {
        GuiFeedback.click(player);
        if (!recipeEditor.addPotionFromPicker(id)) {
            player.sendMessage(TextUtil.error(Messages.recipePickerFull()));
            return;
        }
        player.sendMessage(TextUtil.success(Messages.recipePickerPotionAdded(id)));
        recipeEditor.open();
    }
}