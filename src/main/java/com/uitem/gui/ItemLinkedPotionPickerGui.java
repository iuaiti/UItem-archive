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
 * Chọn potion UItem để gắn vào item (linked-potion), không phải nguyên liệu recipe.
 */
public class ItemLinkedPotionPickerGui extends AbstractGui {

    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final UItemData itemData;
    private int page;

    public ItemLinkedPotionPickerGui(UItemPlugin plugin, Player player, UItemData itemData, int page) {
        super(plugin, player, 54, GuiTheme.title(Messages.itemLinkedPotionPickerTitle()));
        this.itemData = itemData;
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, GuiTheme.header(Material.BREWING_STAND, Messages.itemLinkedPotionPickerTitle(),
                List.of(Messages.itemLinkedPotionPickerDesc(),
                        "<gray>Item: <white>" + itemData.getId())));

        List<UPotionData> potions = new ArrayList<>(plugin.getPotionManager().getAll().values());
        potions.sort(Comparator.comparing(UPotionData::getId, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) potions.size() / PER_PAGE));
        page = Math.min(page, totalPages - 1);

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            int slot = SLOTS[i];
            if (idx >= potions.size()) {
                setItem(slot, null);
                continue;
            }
            UPotionData potion = potions.get(idx);
            ItemStack preview = plugin.getPotionManager().buildPreview(potion);
            List<String> lore = new ArrayList<>();
            lore.add(Messages.itemEditorLinkedPotionClick());
            lore.add(Messages.recipePickerPotionKindLine(potion.getKind().name()));
            setItem(slot, preview, ev -> attach(potion.getId()));
        }

        if (page > 0) {
            setItem(45, createPreviousButton(), ev -> new ItemLinkedPotionPickerGui(plugin, player, itemData, page - 1).open());
        }
        setItem(49, createBackButton(), ev -> new ItemEditorGui(plugin, player, itemData).open());
        if (page < totalPages - 1) {
            setItem(53, createNextButton(), ev -> new ItemLinkedPotionPickerGui(plugin, player, itemData, page + 1).open());
        }
    }

    private void attach(String potionId) {
        if (plugin.getPotionManager().get(potionId) == null) {
            player.sendMessage(TextUtil.error(Messages.recipePickerPotionNotFound(potionId)));
            return;
        }
        itemData.setLinkedPotionId(potionId);
        plugin.getItemManager().registerItem(itemData);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveAllNow(plugin.getItemManager());
        player.sendMessage(TextUtil.success(Messages.itemEditorLinkedPotionAttached(potionId)));
        GuiFeedback.success(player);
        new ItemEditorGui(plugin, player, itemData).open();
    }
}