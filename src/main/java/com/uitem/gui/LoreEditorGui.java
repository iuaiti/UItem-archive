package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI chỉnh sửa lore (mô tả) của item.
 * Hiển thị từng dòng lore, cho phép thêm/sửa/xóa/di chuyển.
 */
public class LoreEditorGui extends AbstractGui {

    private final UItemData data;
    private int page;
    private int selectedLine; // -1 = không chọn dòng nào

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public LoreEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, "<gold><bold>Lore Editor</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
        this.page = 0;
        this.selectedLine = -1;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Title
        setItem(4, createItem(Material.WRITABLE_BOOK,
                "<gold><bold>Lore Editor</bold>",
                List.of(
                        "<gray>Chỉnh sửa mô tả item",
                        "",
                        "<yellow>Click trái: <white>Sửa dòng",
                        "<yellow>Click phải: <white>Xóa dòng",
                        "<yellow>Shift+Click: <white>Chèn dòng trống phía trên"
                )));

        List<String> lore = data.getLore();
        int totalPages = Math.max(1, (int) Math.ceil((double) lore.size() / ITEMS_PER_PAGE));

        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int lineIndex = startIndex + i;
            if (lineIndex >= lore.size()) break;

            String line = lore.get(lineIndex);
            int finalLineIndex = lineIndex;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse("<white>Dòng " + (lineIndex + 1)));
                meta.lore(List.of(
                        TextUtil.parse("<gray>Nội dung:"),
                        TextUtil.parse(line.isEmpty() ? "<dark_gray>(trống)" : line),
                        TextUtil.parse(""),
                        TextUtil.parse("<yellow>Click trái: <white>Sửa"),
                        TextUtil.parse("<red>Click phải: <white>Xóa"),
                        TextUtil.parse("<aqua>Shift+Click: <white>Chèn dòng trống")
                ));
                item.setItemMeta(meta);
            }

            setItem(ITEM_SLOTS[i], item, event -> {
                if (event.getClick() == ClickType.LEFT) {
                    // Sửa dòng
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Nhập nội dung mới cho dòng " + (finalLineIndex + 1) + " <gray>(hỗ trợ MiniMessage):",
                            input -> {
                                if (finalLineIndex < data.getLore().size()) {
                                    data.getLore().set(finalLineIndex, input);
                                    plugin.getItemManager().registerItem(data);
                                    plugin.getStorageManager().saveAll(plugin.getItemManager());
                                }
                                new LoreEditorGui(plugin, player, data).open();
                            });
                } else if (event.getClick() == ClickType.RIGHT) {
                    // Xóa dòng
                    if (finalLineIndex < data.getLore().size()) {
                        data.getLore().remove(finalLineIndex);
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveAll(plugin.getItemManager());
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    refresh();
                } else if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    // Chèn dòng trống phía trên
                    if (finalLineIndex <= data.getLore().size()) {
                        data.getLore().add(finalLineIndex, "");
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveAll(plugin.getItemManager());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    refresh();
                }
            });
        }

        // Nút THÊM DÒNG MỚI (slot 49)
        setItem(49, createItem(Material.EMERALD,
                "<green><bold>+ Thêm dòng mới</bold>",
                List.of(
                        "<gray>Thêm dòng mới vào cuối lore"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập nội dung dòng mới <gray>(hỗ trợ MiniMessage):",
                    input -> {
                        data.getLore().add(input);
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveAll(plugin.getItemManager());
                        new LoreEditorGui(plugin, player, data).open();
                    });
        });

        // Nút XÓA TẤT CẢ (slot 47)
        setItem(47, createItem(Material.BARRIER,
                "<red><bold>Xóa tất cả lore</bold>",
                List.of(
                        "<gray>Xóa toàn bộ dòng lore"
                )), event -> {
            data.getLore().clear();
            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveAll(plugin.getItemManager());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
            refresh();
        });

        // Trang trước
        if (page > 0) {
            setItem(46, createPreviousButton(), event -> {
                page--;
                refresh();
            });
        }

        // Trang sau
        if (page < totalPages - 1) {
            setItem(52, createNextButton(), event -> {
                page++;
                refresh();
            });
        }

        // Quay lại
        setItem(51, createBackButton(), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemEditorGui(plugin, player, data).open();
        });
    }
}
