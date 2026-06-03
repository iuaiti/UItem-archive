package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RarityPickerGui extends AbstractGui {

    private final UItemData data;

    public RarityPickerGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, GuiTheme.title("Do hiem — " + data.getId()));
        this.data = data;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        String current = data.getRarity();
        setItem(4, GuiTheme.header(Material.NETHER_STAR, Messages.guiRarityField(),
                List.of(
                        "<gray>Hiện tại: " + (current != null
                                ? RarityConfig.getRarityColor(current) + current
                                : "<gray>" + Messages.guiRarityNone()),
                        "<yellow>Click một độ hiếm bên dưới"
                )));

        List<String> keys = new ArrayList<>(RarityConfig.getKeys());
        keys.sort(Comparator.comparing(String::toString));
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < slots.length && i < keys.size(); i++) {
            String key = keys.get(i);
            RarityConfig.RarityEntry entry = RarityConfig.get(key);
            Material icon = rarityIcon(key);
            List<String> lore = new ArrayList<>();
            if (entry != null && entry.lore() != null) {
                lore.addAll(entry.lore());
            }
            lore.add("");
            if (key.equalsIgnoreCase(current)) {
                lore.add("<green>Đang chọn");
            } else {
                lore.add("<yellow>Click để chọn");
            }
            setItem(slots[i], createItem(icon,
                    (entry != null ? entry.color() : "<white>") + "<bold>" + key + "</bold>",
                    lore), ev -> {
                data.setRarity(key);
                plugin.getItemManager().registerItem(data);
                player.sendMessage(TextUtil.success(Messages.guiRaritySet(key)));
                GuiFeedback.click(player);
                new ItemEditorGui(plugin, player, data).open();
            });
        }

        setItem(40, createItem(Material.BARRIER, "<red><bold>Xóa độ hiếm</bold>",
                List.of("<gray>Đặt về không có rarity")), ev -> {
            data.setRarity(null);
            plugin.getItemManager().registerItem(data);
            player.sendMessage(TextUtil.success(Messages.guiRarityRemoved()));
            new ItemEditorGui(plugin, player, data).open();
        });

        setItem(46, createBackButton(), ev -> new ItemEditorGui(plugin, player, data).open());
    }

    private static Material rarityIcon(String key) {
        return switch (key.toUpperCase()) {
            case "UNCOMMON" -> Material.LIME_DYE;
            case "RARE" -> Material.LAPIS_LAZULI;
            case "EPIC" -> Material.AMETHYST_SHARD;
            case "LEGENDARY" -> Material.GOLD_INGOT;
            case "ULTRA" -> Material.NETHER_STAR;
            default -> Material.GRAY_DYE;
        };
    }
}