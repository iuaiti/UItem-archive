package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.potion.PotionEffectTypes;
import com.uitem.potion.UPotionData;
import com.uitem.potion.UPotionEffectEntry;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PotionEffectPickerGui extends AbstractGui {

    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final UPotionData data;
    private int page;

    public PotionEffectPickerGui(UItemPlugin plugin, Player player, UPotionData data, int page) {
        super(plugin, player, 54, GuiTheme.title(Messages.potionPickerTitle()));
        this.data = data;
        this.page = page;
    }

    public PotionEffectPickerGui(UItemPlugin plugin, Player player, UPotionData data) {
        this(plugin, player, data, 0);
    }

    @Override
    protected void setup() {
        fillBorder(6);
        List<String> keys = PotionEffectTypes.allKeys();
        int totalPages = Math.max(1, (int) Math.ceil((double) keys.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        setItem(4, GuiTheme.header(Material.SPIDER_EYE, Messages.potionPickerTitle(),
                List.of(
                        "<yellow>Trang: <white>" + (page + 1) + "<gray>/<white>" + totalPages,
                        "<gray>Click để thêm (mặc định 10s, cấp 1)"
                )));

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            int slot = SLOTS[i];
            if (idx >= keys.size()) {
                setItem(slot, null);
                continue;
            }
            String key = keys.get(idx);
            setItem(slot, GuiTheme.button(Material.POTION,
                    "<white>" + PotionEffectTypes.displayName(key),
                    List.of("<gray>Key: <dark_gray>" + key)), ev -> {
                GuiFeedback.click(player);
                data.getEffects().add(new UPotionEffectEntry(key, 200, 0));
                plugin.getPotionManager().register(data);
                player.sendMessage(TextUtil.success(Messages.potionEffectAdded(key)));
                new PotionEffectsGui(plugin, player, data).open();
            });
        }

        if (page > 0) {
            setItem(45, createPreviousButton(), ev -> new PotionEffectPickerGui(plugin, player, data, page - 1).open());
        }
        setItem(49, createBackButton(), ev -> new PotionEffectsGui(plugin, player, data).open());
        if (page < totalPages - 1) {
            setItem(53, createNextButton(), ev -> new PotionEffectPickerGui(plugin, player, data, page + 1).open());
        }
    }
}