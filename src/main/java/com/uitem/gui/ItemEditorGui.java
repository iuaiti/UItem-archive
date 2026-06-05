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

        // === Material (slot 10) - Open Material Picker ===
        setItem(10, createItem(Material.CRAFTING_TABLE,
                "<gold><bold>Vật Liệu</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + (data.getMaterial() != null ? data.getMaterial().name() : "STONE"),
                        "",
                        "<yellow>Click để mở danh sách vật liệu",
                        "<gray>Xem tất cả block, item, công cụ, giáp..."
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new MaterialPickerGui(plugin, player, data, null).open();
        });

        // === Display Name (slot 11) ===
        setItem(11, createItem(Material.NAME_TAG,
                "<gold><bold>Tên Hiển Thị</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.getDisplayName() != null ? data.getDisplayName() : "<gray>Chưa đặt"),
                        "",
                        "<yellow>Click để đổi tên",
                        "<gray>Hỗ trợ định dạng MiniMessage",
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
                        "<yellow>Click để chỉnh lore",
                        "<gray>Mở GUI Chỉnh Lore"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LoreEditorGui(plugin, player, data).open();
        });

        // === Enchantments (slot 13) ===
        setItem(13, createItem(Material.ENCHANTING_TABLE,
                "<gold><bold>Phù Phép</bold>",
                List.of(
                        "<gray>Phù phép: <white>" + data.getEnchantments().size(),
                        "",
                        enchantmentSummary(),
                        "",
                        "<yellow>Click để chỉnh phù phép"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EnchantmentGui(plugin, player, data).open();
        });

        // === Flags (slot 14) ===
        setItem(14, createItem(Material.WHITE_BANNER,
                "<gold><bold>Cờ Item (Flags)</bold>",
                List.of(
                        "<gray>Cờ: <white>" + data.getFlags().size(),
                        "",
                        flagsSummary(),
                        "",
                        "<yellow>Click để chỉnh cờ"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new FlagGui(plugin, player, data).open();
        });

        // === Custom Model Data (slot 15) ===
        setItem(15, createItem(Material.PAINTING,
                "<gold><bold>Custom Model Data</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + (data.getCustomModelData() > 0 ? data.getCustomModelData() : "Không có"),
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

        // === Custom Model (for resource pack, slot 18) - allows mapping any model file (Oraxen style)
        String currentModel = data.getModel();
        setItem(18, createItem(Material.BOOK,
                "<aqua><bold>Custom Model (RP)</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + (currentModel != null ? currentModel : "Không có (dùng item ID)"),
                        "",
                        "<yellow>Click để đặt tên/path model tùy chỉnh",
                        "<gray>ví dụ 'my_sword' hoặc 'uitem:item/library/sword'",
                        "<gray>hoặc 'itemsadder:item/some_ia_model' cho model ItemsAdder",
                        "<gray>Thả file .json (bất kỳ tên) vào plugins/UItem/models/",
                        "<gray>Tốt để copy-paste model ngoài từ IA/Oraxen"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập tên model tùy chỉnh (hoặc 'none' để xóa):",
                    input -> {
                        String val = input.trim();
                        if (val.equalsIgnoreCase("none") || val.isEmpty()) {
                            data.setModel(null);
                            player.sendMessage(TextUtil.success("Đã xóa custom model (sẽ dùng item ID)."));
                        } else {
                            data.setModel(val);
                            player.sendMessage(TextUtil.success("Đã đặt custom model: " + val));
                        }
                        saveAndReopen();
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

        // === Durability (slot 17) ===
        setItem(17, createItem(Material.ANVIL,
                "<gold><bold>Độ Bền</bold>",
                List.of(
                        "<gray>Độ bền tối đa: <white>" + (data.getMaxDurability() > 0 ? data.getMaxDurability() : "Mặc định"),
                        "",
                        "<yellow>Click để đặt độ bền tối đa",
                        "<gray>Nhập -1 cho độ bền mặc định vật liệu",
                        "<gray>Nhập 0 cho không có thanh độ bền (như không thể phá)",
                        "<gray>Hoặc nhập số > 0"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập độ bền tối đa <gray>(-1 = mặc định, 0 = không thanh, >0 = tùy chỉnh):",
                    input -> {
                        try {
                            int dur = Integer.parseInt(input.trim());
                            data.setMaxDurability(dur);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Độ bền tối đa = " + (dur > 0 ? dur : (dur == 0 ? "Không thanh" : "Mặc định"))));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Số không hợp lệ!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Glowing (slot 19) ===
        setItem(19, createItem(
                data.isGlowing() ? Material.GLOWSTONE : Material.STONE,
                "<gold><bold>Phát Sáng</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.isGlowing() ? "<green>BẬT" : "<red>TẮT"),
                        "",
                        "<yellow>Click để bật/tắt",
                        "<gray>Thêm hiệu ứng phát sáng cho item"
                )), event -> {
            data.setGlowing(!data.isGlowing());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isGlowing() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // === Attributes (slot 20) ===
        setItem(20, createItem(Material.IRON_SWORD,
                "<gold><bold>Thuộc Tính (Attributes)</bold>",
                List.of(
                        "<gray>Thuộc tính: <white>" + data.getAttributes().size(),
                        "",
                        "<yellow>Click để thêm thuộc tính",
                        "<gray>Định dạng: attribute amount operation slot",
                        "<gray>Ví dụ: generic_attack_damage 15 ADD_NUMBER HAND"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập thuộc tính <gray>(định dạng: attribute amount operation slot)\n" +
                            "<gray>Ví dụ: generic_attack_damage 15 ADD_NUMBER HAND\n" +
                            "<gray>Gõ 'clear' để xóa hết thuộc tính:",
                    input -> {
                        if (input.equalsIgnoreCase("clear")) {
                            data.getAttributes().clear();
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Đã xóa tất cả thuộc tính!"));
                            return;
                        }
                        String[] parts = input.trim().split("\\s+");
                        if (parts.length < 4) {
                            player.sendMessage(TextUtil.error("Định dạng: attribute amount operation slot"));
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
                            player.sendMessage(TextUtil.success("Đã thêm thuộc tính " + attr + "!"));
                        } catch (Exception e) {
                            player.sendMessage(TextUtil.error("Lỗi: " + e.getMessage()));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Rarity (slot 21) ===
        String currentRarity = data.getRarity();
        String rarityDisplay = currentRarity != null ? currentRarity : "Không có";
        setItem(21, createItem(Material.NETHER_STAR,
                "<gold><bold>Độ Hiếm (Rarity)</bold>",
                List.of(
                        "<gray>Hiện tại: " + (currentRarity != null ? RarityConfig.getRarityColor(currentRarity) + currentRarity : "<gray>Không có"),
                        "",
                        "<yellow>Click để chọn độ hiếm",
                        "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            StringBuilder msg = new StringBuilder("<gold>Các độ hiếm có sẵn:<dark_gray> ");
            for (String r : RarityConfig.getKeys()) {
                msg.append("\n").append(RarityConfig.getRarityColor(r)).append("▸ ").append(r);
            }
            msg.append("\n\n<yellow>Nhập tên độ hiếm (hoặc 'none' để xóa):");
            plugin.getChatInputManager().requestInput(player,
                    msg.toString(),
                    input -> {
                        if (input.equalsIgnoreCase("none")) {
                            data.setRarity(null);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Đã xóa độ hiếm!"));
                            return;
                        }
                        String upper = input.toUpperCase().trim();
                        if (RarityConfig.getKeys().contains(upper)) {
                            data.setRarity(upper);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Đã đặt độ hiếm = " + upper));
                        } else {
                            player.sendMessage(TextUtil.error("Độ hiếm '" + input + "' không tồn tại!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === ExecutableItems ID (slot 22) - only show if ExecutableItems is present ===
        if (plugin.getIntegrationManager().hasExecutableItems()) {
            setItem(22, createItem(Material.BLAZE_POWDER,
                    "<gold><bold>⚡ ExecutableItems</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getExecutableItemsId() != null ? data.getExecutableItemsId() : "Không có"),
                            "",
                            "<yellow>Click để đặt ID ExecutableItems",
                            "<gray>Kỹ năng/Passive sẽ do EI xử lý"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Nhập ID item ExecutableItems <gray>(hoặc 'none' để xóa):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setExecutableItemsId(null);
                            } else {
                                data.setExecutableItemsId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === Oraxen ID (slot 23) - only show if Oraxen is present ===
        if (plugin.getIntegrationManager().hasOraxen()) {
            setItem(23, createItem(Material.DIAMOND,
                    "<aqua><bold>Oraxen ID</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getOraxenId() != null ? data.getOraxenId() : "Không có"),
                            "",
                            "<yellow>Click để đặt ID item Oraxen",
                            "<gray>Dùng item từ Oraxen làm base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Nhập ID item Oraxen <gray>(hoặc 'none' để xóa):",
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

        // === ItemsAdder ID (slot 24) - only show if ItemsAdder is present ===
        if (plugin.getIntegrationManager().hasItemsAdder()) {
            setItem(24, createItem(Material.EMERALD,
                    "<green><bold>ItemsAdder ID</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getItemsAdderId() != null ? data.getItemsAdderId() : "Không có"),
                            "",
                            "<yellow>Click để đặt ID item ItemsAdder",
                            "<gray>Dùng item từ ItemsAdder làm base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Nhập ID namespaced ItemsAdder <gray>(hoặc 'none' để xóa):",
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

        // === Recipe (slot 25) - Open Recipe Editor ===
        boolean hasRecipe = plugin.getRecipeManager().hasRecipe(data.getId());
        setItem(25, createItem(
                hasRecipe ? Material.CRAFTING_TABLE : Material.FURNACE,
                "<gold><bold>Công Thức Chế Tạo</bold>",
                List.of(
                        hasRecipe ? "<green>Đã có công thức" : "<red>Chưa có công thức",
                        "",
                        "<yellow>Click để chỉnh công thức",
                        "<gray>Shaped / Shapeless + nguyên liệu UItem tùy chỉnh",
                        "<gray>Support output x1-x64 per craft"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RecipeEditorGui(plugin, player, data).open();
        });

        // === Hiệu Ứng (phản đòn / sát thương thêm khi hit/damage/equip/fullset/die) slot 26 ===
        int effCount = (data.getEffects() != null) ? data.getEffects().size() : 0;
        setItem(26, createItem(
                effCount > 0 ? Material.BOOK : Material.PAPER,
                "<light_purple><bold>Hiệu Ứng Đã Gắn</bold>",
                List.of(
                        "<gray>Hiệu ứng: <white>" + effCount,
                        "",
                        "<yellow>Click để mở GUI Chỉnh Hiệu Ứng",
                        "<gray>Chọn trigger (HIT, DAMAGE, DIE, EQUIP, FULL_SET)",
                        "<gray>Chọn loại REFLECT hoặc EXTRA_DAMAGE",
                        "<gray>Đặt tỷ lệ + giá trị; tùy chọn target khi HIT",
                        "<gray>FULL_SET kích hoạt khi mặc đủ 4 món giáp"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EffectEditorGui(plugin, player, data).open();
        });

        // === Back button (slot 46) ===
        setItem(46, createBackButton(), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemListGui(plugin, player).open();
        });

        // === SAVE button (slot 49) ===
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

        // === DELETE button (slot 52) ===
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
        plugin.getStorageManager().saveAllNow(plugin.getItemManager());
        new ItemEditorGui(plugin, player, data).open();
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveAllNow(plugin.getItemManager());
        refresh();
    }

    private String enchantmentSummary() {
        if (data.getEnchantments().isEmpty()) return "<gray>Không có phù phép";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (var entry : data.getEnchantments().entrySet()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getEnchantments().size() - 3).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(entry.getKey()).append(" ").append(entry.getValue());
            count++;
        }
        return sb.toString();
    }

    private String flagsSummary() {
        if (data.getFlags().isEmpty()) return "<gray>Không có cờ";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String flag : data.getFlags()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getFlags().size() - 3).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(flag);
            count++;
        }
        return sb.toString();
    }
}
