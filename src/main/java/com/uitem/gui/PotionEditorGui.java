package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.potion.PotionKind;
import com.uitem.potion.UPotionData;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PotionEditorGui extends AbstractGui {

    private final UPotionData data;

    public PotionEditorGui(UItemPlugin plugin, Player player, UPotionData data) {
        super(plugin, player, 54, GuiTheme.titleEdit(data.getId()));
        this.data = data;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        ItemStack preview = plugin.getPotionManager().buildPreview(data);
        setItem(4, preview);

        setItem(10, createItem(Material.NAME_TAG, Messages.potionFieldName(),
                List.of(
                        "<gray>Hiện tại: " + data.getDisplayName(),
                        "<yellow>Click để đổi tên (MiniMessage)"
                )), ev -> plugin.getChatInputManager().requestInput(player,
                Messages.potionPromptName(), input -> {
                    data.setDisplayName(input);
                    saveAndReopen();
                }));

        setItem(11, createItem(Material.WRITABLE_BOOK, Messages.potionFieldLore(),
                List.of(
                        "<gray>Dòng: <white>" + data.getLore().size(),
                        "<yellow>Click để thêm 1 dòng lore",
                        "<gray>Gõ 'clear' để xóa hết lore"
                )), ev -> plugin.getChatInputManager().requestInput(player,
                Messages.potionPromptLore(), input -> {
                    if (input.equalsIgnoreCase("clear")) {
                        data.setLore(new ArrayList<>());
                    } else {
                        List<String> lore = new ArrayList<>(data.getLore());
                        lore.add(input);
                        data.setLore(lore);
                    }
                    saveAndReopen();
                }));

        setItem(12, createItem(data.getKind().material(), Messages.potionFieldKind(),
                List.of(
                        "<gray>Loai: <white>" + data.getKind().name(),
                        "<yellow>Click <gray>doi (Drink/Splash/Lingering)"
                )), ev -> {
            data.setKind(data.getKind().next());
            saveAndReopen();
        });

        int r = (data.getColorRgb() >> 16) & 0xFF;
        int g = (data.getColorRgb() >> 8) & 0xFF;
        int b = data.getColorRgb() & 0xFF;
        setItem(13, createItem(Material.RED_DYE, Messages.potionFieldColor(),
                List.of(
                        "<gray>RGB: <white>" + r + ", " + g + ", " + b,
                        "<gray>Hex: <white>#" + String.format("%06X", data.getColorRgb()),
                        "<yellow>Click: nhập #RRGGBB hoặc r,g,b"
                )), ev -> plugin.getChatInputManager().requestInput(player,
                Messages.potionPromptColor(), input -> {
                    int parsed = parseColorInput(input, data.getColorRgb());
                    data.setColorRgb(parsed);
                    saveAndReopen();
                }));

        setItem(14, createItem(Material.BLAZE_POWDER, Messages.potionFieldEffects(),
                List.of(
                        "<gray>Số hiệu ứng: <white>" + data.getEffects().size(),
                        "<yellow>Click để quản lý hiệu ứng"
                )), ev -> new PotionEffectsGui(plugin, player, data).open());

        setItem(15, createItem(
                data.isGlowing() ? Material.GLOWSTONE_DUST : Material.GUNPOWDER,
                Messages.potionFieldGlow(),
                List.of(
                        "<gray>Trạng thái: " + (data.isGlowing() ? "<green>BẬT" : "<red>TẮT"),
                        "<yellow>Click để bật/tắt"
                )), ev -> {
            data.setGlowing(!data.isGlowing());
            saveAndReopen();
        });

        setItem(16, createItem(
                data.isAbilitiesEnabled() && !data.getAbilities().isEmpty()
                        ? Material.SPLASH_POTION : Material.POTION,
                "<gold><bold>Kỹ năng / Trigger</bold>",
                List.of(
                        "<gray>Trạng thái: " + (data.isAbilitiesEnabled() ? "<green>BẬT" : "<red>TẮT"),
                        "<gray>Số rule: <white>" + data.getAbilities().size(),
                        "",
                        "<yellow>Click <gray>mở studio kỹ năng potion",
                        "<gray>Cầm tay đánh, uống, bị đánh, phản sát..."
                )), ev -> AbilityListGui.forPotion(plugin, player, data).open());

        setItem(22, GuiTheme.actionPrimary(Material.LIME_CONCRETE, Messages.potionSave(),
                "<gray>Lưu vào plugins/UItem/potions/"), ev -> {
            plugin.getPotionManager().save(data);
            player.sendMessage(TextUtil.success(Messages.potionSaved(data.getId())));
            GuiFeedback.success(player);
            saveAndReopen();
        });

        setItem(45, createHomeButton(), ev -> new PotionHubGui(plugin, player).open());
        setItem(46, createBackButton(), ev -> new PotionListGui(plugin, player).open());
        setItem(49, GuiTheme.actionSecondary(Material.ENDER_PEARL, Messages.potionGiveSelf(),
                "<gray>Nhấn thử 1 bình để test"), ev -> {
            ItemStack potion = plugin.getPotionManager().buildItemStack(data);
            for (ItemStack left : player.getInventory().addItem(potion).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            player.sendMessage(TextUtil.success(Messages.potionGivenSelf(data.getId())));
        });
    }

    private void saveAndReopen() {
        plugin.getPotionManager().register(data);
        refresh();
    }

    private static int parseColorInput(String input, int fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String s = input.trim();
        if (s.startsWith("#")) {
            try {
                return Integer.parseInt(s.substring(1), 16) & 0xFFFFFF;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        if (s.contains(",")) {
            String[] p = s.split(",");
            if (p.length >= 3) {
                try {
                    int r = Integer.parseInt(p[0].trim());
                    int g = Integer.parseInt(p[1].trim());
                    int b = Integer.parseInt(p[2].trim());
                    return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }
}