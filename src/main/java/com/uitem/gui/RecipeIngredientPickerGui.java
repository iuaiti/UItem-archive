package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chọn item UItem làm nguyên liệu recipe.
 */
public class RecipeIngredientPickerGui extends AbstractGui {

    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final RecipeEditorGui recipeEditor;
    private final String resultItemId;
    private int page;

    public RecipeIngredientPickerGui(UItemPlugin plugin, Player player, RecipeEditorGui recipeEditor,
                                     String resultItemId, int page) {
        super(plugin, player, 54, GuiTheme.title(Messages.recipePickerTitle()));
        this.recipeEditor = recipeEditor;
        this.resultItemId = resultItemId;
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        List<UItemData> items = new ArrayList<>(plugin.getItemManager().getAllItems().values());
        items.sort(Comparator.comparing(UItemData::getId, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, GuiTheme.header(Material.HOPPER, Messages.recipePickerTitle(),
                List.of(
                        Messages.recipePickerDesc(),
                        Messages.recipePickerResultLine(resultItemId),
                        Messages.recipePickerPageLine(page + 1, totalPages)
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
            lore.add(Messages.recipePickerIdLine(data.getId()));
            if (data.getId().equalsIgnoreCase(resultItemId)) {
                lore.add(Messages.recipePickerSelfItemHint());
            } else {
                lore.add(Messages.recipePickerClickAddItem());
            }
            setItem(slot, GuiTheme.glowing(
                    plugin.getItemManager().buildPreviewItemStack(data).getType(),
                    data.getDisplayName() != null ? data.getDisplayName() : data.getId(),
                    lore), ev -> pickItem(data.getId()));
        }

        if (page > 0) {
            setItem(45, createPreviousButton(), ev -> new RecipeIngredientPickerGui(
                    plugin, player, recipeEditor, resultItemId, page - 1).open());
        }
        setItem(49, createBackButton(), ev -> recipeEditor.reopenAfterPicker());
        if (page < totalPages - 1) {
            setItem(53, createNextButton(), ev -> new RecipeIngredientPickerGui(
                    plugin, player, recipeEditor, resultItemId, page + 1).open());
        }
    }

    private void pickItem(String id) {
        GuiFeedback.click(player);
        if (!recipeEditor.addIngredientFromPicker(id)) {
            return;
        }
        player.sendMessage(TextUtil.success(Messages.recipePickerAdded(id)));
        recipeEditor.reopenAfterPicker();
    }
}