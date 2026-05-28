package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI chỉnh sửa một custom item.
 * Hiển thị tất cả thuộc tính có thể chỉnh sửa.
 */
public class ItemEditorGui extends AbstractGui {

    private final UItemData data;

    public ItemEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54,
                "<gradient:#ff6b6b:#ffd93d>Chỉnh sửa: </gradient><white>" + data.getId());
        this.data = data;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // === Preview item (slot 4) ===
        ItemStack preview = plugin.getItemManager().buildItemStack(data);
        setItem(4, preview);

        // === Material (slot 10) - Mở Material Picker ===
        setItem(10, createItem(Material.CRAFTING_TABLE,
                "<gold><bold>Material</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + data.getMaterial().name(),
                        "",
                        "<yellow>Click để mở danh sách material",
                        "<gray>Xem tất cả blocks, items, tools, armor..."
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new MaterialPickerGui(plugin, player, data, null).open();
        });

        // === Display Name (slot 11) ===
        setItem(11, createItem(Material.NAME_TAG,
                "<gold><bold>Display Name</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.getDisplayName() != null ? data.getDisplayName() : "<gray>Chưa đặt"),
                        "",
                        "<yellow>Click để thay đổi tên",
                        "<gray>Hỗ trợ MiniMessage format",
                        "<gray>Ví dụ: <red>Ruby <gold>Sword"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập tên hiển thị <gray>(hỗ trợ MiniMessage):",
                    input -> {
                        data.setDisplayName(input);
                        saveAndReopen();
                        player.sendMessage(TextUtil.success("Đã đổi tên thành: " + input));
                    });
        });

        // === Lore (slot 12) ===
        setItem(12, createItem(Material.WRITABLE_BOOK,
                "<gold><bold>Lore</bold>",
                List.of(
                        "<gray>Số dòng: <white>" + data.getLore().size(),
                        "",
                        "<yellow>Click để chỉnh sửa lore",
                        "<gray>Mở Lore Editor GUI"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LoreEditorGui(plugin, player, data).open();
        });

        // === Enchantments (slot 13) ===
        setItem(13, createItem(Material.ENCHANTING_TABLE,
                "<gold><bold>Enchantments</bold>",
                List.of(
                        "<gray>Số enchant: <white>" + data.getEnchantments().size(),
                        "",
                        enchantmentSummary(),
                        "",
                        "<yellow>Click để chỉnh sửa enchantments"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EnchantmentGui(plugin, player, data).open();
        });

        // === Flags (slot 14) ===
        setItem(14, createItem(Material.WHITE_BANNER,
                "<gold><bold>Item Flags</bold>",
                List.of(
                        "<gray>Số flags: <white>" + data.getFlags().size(),
                        "",
                        flagsSummary(),
                        "",
                        "<yellow>Click để chỉnh sửa flags"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new FlagGui(plugin, player, data).open();
        });

        // === Custom Model Data (slot 15) ===
        setItem(15, createItem(Material.PAINTING,
                "<gold><bold>Custom Model Data</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + (data.getCustomModelData() > 0 ? data.getCustomModelData() : "Không"),
                        "",
                        "<yellow>Click để thay đổi",
                        "<gray>Nhập số (-1 để tắt)"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập số Custom Model Data <gray>(-1 để tắt):",
                    input -> {
                        try {
                            int cmd = Integer.parseInt(input.trim());
                            data.setCustomModelData(cmd);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Custom Model Data = " + cmd));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Số không hợp lệ!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Unbreakable (slot 16) ===
        setItem(16, createItem(
                data.isUnbreakable() ? Material.BEDROCK : Material.COBBLESTONE,
                "<gold><bold>Unbreakable</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.isUnbreakable() ? "<green>BẬT" : "<red>TẮT"),
                        "",
                        "<yellow>Click để bật/tắt"
                )), event -> {
            data.setUnbreakable(!data.isUnbreakable());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isUnbreakable() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // === Glowing (slot 19) ===
        setItem(19, createItem(
                data.isGlowing() ? Material.GLOWSTONE : Material.STONE,
                "<gold><bold>Glowing</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.isGlowing() ? "<green>BẬT" : "<red>TẮT"),
                        "",
                        "<yellow>Click để bật/tắt",
                        "<gray>Thêm hiệu ứng sáng cho item"
                )), event -> {
            data.setGlowing(!data.isGlowing());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isGlowing() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // === Attributes (slot 20) ===
        setItem(20, createItem(Material.IRON_SWORD,
                "<gold><bold>Attributes</bold>",
                List.of(
                        "<gray>Số attributes: <white>" + data.getAttributes().size(),
                        "",
                        "<yellow>Click để thêm attribute",
                        "<gray>Format: attribute amount operation slot",
                        "<gray>VD: generic_attack_damage 15 ADD_NUMBER HAND"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập attribute <gray>(format: attribute amount operation slot)\n" +
                            "<gray>VD: generic_attack_damage 15 ADD_NUMBER HAND\n" +
                            "<gray>Nhập 'clear' để xóa tất cả attributes:",
                    input -> {
                        if (input.equalsIgnoreCase("clear")) {
                            data.getAttributes().clear();
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Đã xóa tất cả attributes!"));
                            return;
                        }
                        String[] parts = input.trim().split("\\s+");
                        if (parts.length < 4) {
                            player.sendMessage(TextUtil.error("Format: attribute amount operation slot"));
                            new ItemEditorGui(plugin, player, data).open();
                            return;
                        }
                        try {
                            String attr = parts[0];
                            double amount = Double.parseDouble(parts[1]);
                            String operation = parts[2].toUpperCase();
                            String slot = parts[3].toUpperCase();
                            data.getAttributes().add(new UItemData.AttributeData(attr, amount, operation, slot));
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Đã thêm attribute " + attr + "!"));
                        } catch (Exception e) {
                            player.sendMessage(TextUtil.error("Lỗi: " + e.getMessage()));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Rarity (slot 21) ===
        String currentRarity = data.getRarity();
        String rarityDisplay = currentRarity != null ? currentRarity : "Không";
        setItem(21, createItem(Material.NETHER_STAR,
                "<gold><bold>Rarity</bold>",
                List.of(
                        "<gray>Hiện tại: " + (currentRarity != null ? RarityConfig.getRarityColor(currentRarity) + currentRarity : "<gray>Không"),
                        "",
                        "<yellow>Click để chọn rarity",
                        "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            StringBuilder msg = new StringBuilder("<gold>Các rarity có sẵn:<dark_gray> ");
            for (String r : RarityConfig.getKeys()) {
                msg.append("\n").append(RarityConfig.getRarityColor(r)).append("▸ ").append(r);
            }
            msg.append("\n\n<yellow>Nhập tên rarity (hoặc 'none' để bỏ):");
            plugin.getChatInputManager().requestInput(player,
                    msg.toString(),
                    input -> {
                        if (input.equalsIgnoreCase("none")) {
                            data.setRarity(null);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Đã bỏ rarity!"));
                            return;
                        }
                        String upper = input.toUpperCase().trim();
                        if (RarityConfig.getKeys().contains(upper)) {
                            data.setRarity(upper);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Đã đặt rarity = " + upper));
                        } else {
                            player.sendMessage(TextUtil.error("Rarity '" + input + "' không tồn tại!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Passives (slot 22) ===
        int passiveCount = data.getPassives() != null ? data.getPassives().size() : 0;
        setItem(22, createItem(Material.BLAZE_POWDER,
                "<gold><bold>⚡ Passives</bold>",
                List.of(
                        "<gray>Số passive: <white>" + passiveCount,
                        "",
                        "<yellow>Click để quản lý passives",
                        "<gray>Thêm/sửa/xóa kỹ năng passive",
                        "<gray>Trigger: HIT, DAMAGED, HOLD, EQUIP, BREAK_BLOCK, KILL"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new PassiveEditorGui(plugin, player, data).open();
        });

        // === Oraxen ID (slot 23) - chỉ hiện nếu Oraxen có ===
        if (plugin.getIntegrationManager().hasOraxen()) {
            setItem(23, createItem(Material.DIAMOND,
                    "<aqua><bold>Oraxen ID</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getOraxenId() != null ? data.getOraxenId() : "Không"),
                            "",
                            "<yellow>Click để đặt Oraxen item ID",
                            "<gray>Dùng item từ Oraxen làm base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Nhập Oraxen item ID <gray>(hoặc 'none' để xóa):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setOraxenId(null);
                            } else {
                                data.setOraxenId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === ItemsAdder ID (slot 24) - chỉ hiện nếu ItemsAdder có ===
        if (plugin.getIntegrationManager().hasItemsAdder()) {
            setItem(24, createItem(Material.EMERALD,
                    "<green><bold>ItemsAdder ID</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getItemsAdderId() != null ? data.getItemsAdderId() : "Không"),
                            "",
                            "<yellow>Click để đặt ItemsAdder item ID",
                            "<gray>Dùng item từ ItemsAdder làm base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Nhập ItemsAdder namespaced ID <gray>(hoặc 'none' để xóa):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setItemsAdderId(null);
                            } else {
                                data.setItemsAdderId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === Nút quay lại (slot 46) ===
        setItem(46, createBackButton(), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemListGui(plugin, player).open();
        });

        // === Nút SAVE (slot 49) ===
        setItem(49, createItem(Material.EMERALD,
                "<green><bold>💾 LƯU</bold>",
                List.of(
                        "<gray>Lưu tất cả thay đổi vào file",
                        "",
                        "<yellow>Click để lưu"
                )), event -> {
            plugin.getItemManager().registerItem(data);
            plugin.getResourcePackManager().syncCmdsToLiveItems();
            plugin.getStorageManager().saveAllNow(plugin.getItemManager());
            player.sendMessage(TextUtil.success("Đã lưu item '" + data.getId() + "'!"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            refresh();
        });

        // === Nút DELETE (slot 52) ===
        setItem(52, createItem(Material.BARRIER,
                "<red><bold>🗑 XÓA</bold>",
                List.of(
                        "<gray>Xóa item này",
                        "",
                        "<red>⚠ Click để xóa"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.5f);
            new ConfirmDeleteGui(plugin, player, data, 0).open();
        });
    }

    private void saveAndReopen() {
        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveAll(plugin.getItemManager());
        new ItemEditorGui(plugin, player, data).open();
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveAll(plugin.getItemManager());
        refresh();
    }

    private String enchantmentSummary() {
        if (data.getEnchantments().isEmpty()) return "<gray>Không có enchantment";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (var entry : data.getEnchantments().entrySet()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getEnchantments().size() - 3).append(" nữa)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(entry.getKey()).append(" ").append(entry.getValue());
            count++;
        }
        return sb.toString();
    }

    private String flagsSummary() {
        if (data.getFlags().isEmpty()) return "<gray>Không có flag";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String flag : data.getFlags()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getFlags().size() - 3).append(" nữa)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(flag);
            count++;
        }
        return sb.toString();
    }
}
