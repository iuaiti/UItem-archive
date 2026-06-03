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

import java.util.ArrayList;
import java.util.List;

public class PotionEffectsGui extends AbstractGui {

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final UPotionData data;

    public PotionEffectsGui(UItemPlugin plugin, Player player, UPotionData data) {
        super(plugin, player, 54, GuiTheme.title(Messages.potionEffectsTitle()));
        this.data = data;
    }

    @Override
    protected void setup() {
        fillBorder(6);
        setItem(4, GuiTheme.header(Material.FERMENTED_SPIDER_EYE, Messages.potionEffectsTitle(),
                List.of("<gray>Potion: <white>" + data.getId())));

        List<UPotionEffectEntry> effects = data.getEffects();
        for (int i = 0; i < SLOTS.length; i++) {
            int slot = SLOTS[i];
            if (i >= effects.size()) {
                setItem(slot, null);
                continue;
            }
            UPotionEffectEntry entry = effects.get(i);
            int index = i;
            setItem(slot, createItem(Material.POTION,
                    "<gold>" + PotionEffectTypes.displayName(entry.getTypeKey()),
                    List.of(
                            "<gray>Cap do: <white>" + entry.getLevelDisplay(),
                            "<gray>Thoi gian: <white>" + entry.getDurationSeconds() + "s",
                            "<yellow>Trai: sua thoi gian",
                            "<red>Phải: xóa hiệu ứng"
                    )), ev -> {
                if (ev.isRightClick()) {
                    effects.remove(index);
                    plugin.getPotionManager().register(data);
                    refresh();
                    player.sendMessage(TextUtil.success(Messages.potionEffectRemoved()));
                    return;
                }
                plugin.getChatInputManager().requestInput(player,
                        Messages.potionPromptDuration(),
                        input -> {
                            try {
                                int sec = Integer.parseInt(input.trim());
                                entry.setDurationSeconds(sec);
                                plugin.getPotionManager().register(data);
                                refresh();
                            } catch (NumberFormatException ex) {
                                player.sendMessage(TextUtil.error(Messages.potionInvalidNumber()));
                                open();
                            }
                        });
            });
        }

        setItem(31, GuiTheme.actionPrimary(Material.EMERALD, Messages.potionAddEffect(),
                "<gray>Chọn loại hiệu ứng"), ev -> {
            GuiFeedback.click(player);
            new PotionEffectPickerGui(plugin, player, data).open();
        });

        setItem(45, createBackButton(), ev -> new PotionEditorGui(plugin, player, data).open());
        setItem(49, GuiTheme.actionPrimary(Material.LIME_CONCRETE, Messages.potionSave(),
                "<gray>Lưu potion"), ev -> {
            plugin.getPotionManager().save(data);
            player.sendMessage(TextUtil.success(Messages.potionSaved(data.getId())));
            new PotionEditorGui(plugin, player, data).open();
        });
    }
}