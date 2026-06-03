package com.uitem.util;

import com.uitem.UItemPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Centralized message system with EN/VI language support.
 * Language controlled by config.yml → "language: en" or "language: vi"
 */
public final class Messages {

    private static UItemPlugin plugin;
    private static boolean isVietnamese;

    private Messages() {}

    public static void init(UItemPlugin pluginInstance) {
        plugin = pluginInstance;
        reload();
    }

    public static void reload() {
        if (plugin == null) { isVietnamese = false; return; }
        String lang = plugin.getConfig().getString("language", "en");
        isVietnamese = "vi".equalsIgnoreCase(lang) || "vn".equalsIgnoreCase(lang) || "vietnamese".equalsIgnoreCase(lang);
    }

    public static boolean isVietnamese() { return isVietnamese; }

    private static String get(String key, String en, String vi) {
        if (plugin != null) {
            FileConfiguration cfg = plugin.getConfig();
            // Check per-language override first
            String langKey = "messages." + (isVietnamese ? "vi." : "en.") + key;
            if (cfg.contains(langKey)) return cfg.getString(langKey);
            // Check generic key
            String path = "messages." + key;
            if (cfg.contains(path)) return cfg.getString(path);
        }
        return isVietnamese ? vi : en;
    }

    // ==================== Generic ====================
    public static String noPermission() { return get("no-permission", "You don't have permission to use this command!", "Bạn không có quyền sử dụng lệnh này!"); }
    public static String playersOnly() { return get("players-only", "This command can only be used by players!", "Lệnh này chỉ dành cho người chơi!"); }
    public static String playerNotFound(String name) { return get("player-not-found", "Player '" + name + "' is not online!", "Người chơi '" + name + "' không online!").replace("{player}", name); }
    public static String invalidCommand(String label) { return get("invalid-command", "Invalid command. Use /" + label + " help", "Lệnh không hợp lệ. Dùng /" + label + " help").replace("{label}", label); }

    // ==================== Give ====================
    public static String giveUsage() { return get("give-usage", "Usage: /uitem give <id> [player] [amount]", "Cách dùng: /uitem give <id> [người chơi] [số lượng]"); }
    public static String itemNotFound(String id) { return get("item-not-found", "Item '" + id + "' does not exist!", "Item '" + id + "' không tồn tại!").replace("{id}", id); }
    public static String gaveItem(String player, int amount, String itemId) { return get("gave-item", "Gave " + player + " x" + amount + " " + itemId, "Đã đưa cho " + player + " x" + amount + " " + itemId).replace("{player}", player).replace("{amount}", String.valueOf(amount)).replace("{item}", itemId); }
    public static String receivedItem(int amount, String itemId) { return get("received-item", "You received x" + amount + " " + itemId, "Bạn nhận được x" + amount + " " + itemId).replace("{amount}", String.valueOf(amount)).replace("{item}", itemId); }
    public static String cannotGive() { return get("cannot-give", "Cannot give item (may have been blocked by another plugin).", "Không thể đưa item (có thể bị chặn bởi plugin khác)."); }
    public static String consoleNeedPlayer() { return get("console-need-player", "Console must specify a player: /uitem give <id> <player> [amount]", "Console phải chỉ định người chơi: /uitem give <id> <người chơi> [số lượng]"); }
    public static String invalidAmount() { return get("invalid-amount", "Invalid amount! Must be an integer from 1 to 64.", "Số lượng không hợp lệ! Phải là số nguyên từ 1 đến 64."); }

    // ==================== Reload ====================
    public static String reloaded(int count) { return get("reloaded", "Reloaded UItem! Loaded " + count + " items.", "Đã reload UItem! Đã load " + count + " items.").replace("{count}", String.valueOf(count)); }
    public static String rpRegenerated() { return get("rp-regenerated", "Resource pack has been re-generated and exported.", "Resource pack đã được tạo lại và xuất ra."); }
    public static String rpGenerateFailed(String msg) { return get("rp-generate-failed", "Cannot generate/export resource pack: " + msg, "Không thể tạo/xuất resource pack: " + msg).replace("{error}", msg); }

    // ==================== Check ====================
    public static String notHoldingItem() { return get("not-holding-item", "You are not holding any item!", "Bạn không cầm item nào!"); }
    public static String validUItem() { return get("valid-uitem", "✔ Item is a valid UItem!", "✔ Item là UItem hợp lệ!"); }
    public static String notUItem() { return get("not-uitem", "✗ Item is NOT a valid UItem!", "✗ Item KHÔNG phải UItem hợp lệ!"); }
    public static String missingPDC() { return get("missing-pdc", "Missing PDC tag. This item was not obtained through /uitem give.", "Thiếu tag PDC. Item này không được lấy qua /uitem give."); }
    public static String useGiveToGet() { return get("use-give-to-get", "Use /uitem give <id> <player> to get properly tagged items.", "Dùng /uitem give <id> <người chơi> để lấy item có tag đúng."); }

    // ==================== Editor ====================
    public static String noItemsYet() { return get("no-items-yet", "No custom items yet. Use /uitem editor to create one.", "Chưa có custom item nào. Dùng /uitem editor để tạo."); }

    // ==================== Resource Pack ====================
    public static String rpHelpGenerate() { return get("rp-help-gen", "<gold>Generate resource pack: <white>/uitem rp generate", "<gold>Tạo resource pack: <white>/uitem rp generate"); }
    public static String rpHelpExport() { return get("rp-help-export", "<gold>Export zip: <white>/uitem rp export", "<gold>Xuất zip: <white>/uitem rp export"); }
    public static String rpHelpApply() { return get("rp-help-apply", "<gold>Apply to player: <white>/uitem rp apply <player> <url> [hash]", "<gold>Áp dụng cho người chơi: <white>/uitem rp apply <người chơi> <url> [hash]"); }
    public static String rpHelpStatus() { return get("rp-help-status", "<gold>Status: <white>/uitem rp status", "<gold>Trạng thái: <white>/uitem rp status"); }
    public static String rpGenerated() { return get("rp-generated", "Generated resource pack at data/resourcepack/", "Đã tạo resource pack tại data/resourcepack/"); }
    public static String rpExported(String path) { return get("rp-exported", "Exported zip: " + path, "Đã xuất zip: " + path).replace("{path}", path); }
    public static String rpExportFailed(String msg) { return get("rp-export-failed", "Export failed: " + msg, "Xuất thất bại: " + msg).replace("{error}", msg); }
    public static String rpApplyUsage() { return get("rp-apply-usage", "Usage: /uitem rp apply <player> <url> [hash]", "Cách dùng: /uitem rp apply <người chơi> <url> [hash]"); }
    public static String rpSent(String name) { return get("rp-sent", "Sent resource pack to " + name, "Đã gửi resource pack cho " + name).replace("{player}", name); }
    public static String rpUsage() { return get("rp-usage", "Usage: /uitem rp <generate|export|apply|status>", "Cách dùng: /uitem rp <generate|export|apply|status>"); }
    public static String rpStatusNotExported() { return get("rp-status-not-exported", "Not exported", "Chưa xuất"); }
    public static String rpStatusNone() { return get("rp-status-none", "None", "Không có"); }
    public static String rpKickMessage() { return get("rp-kick", "Unable to load resource packs, please rejoin", "Không tải được resource packs, vui lòng vào lại"); }
    public static String rpHelpBuild() { return get("rp-help-build", "<gold>Full build: <white>/uitem rp build", "<gold>Build đầy đủ: <white>/uitem rp build"); }
    public static String rpHelpSendAll() { return get("rp-help-sendall", "<gold>Send to all online: <white>/uitem rp sendall", "<gold>Gửi cho tất cả online: <white>/uitem rp sendall"); }
    public static String rpHelpGui() { return get("rp-help-gui", "<gold>Open RP panel: <white>/uitem rp gui", "<gold>Mở panel RP: <white>/uitem rp gui"); }
    public static String rpHelpMerge() { return get("rp-help-merge", "<gold>Import external packs: <white>/uitem rp merge", "<gold>Nhập pack ngoài: <white>/uitem rp merge"); }
    public static String rpSentAll() { return get("rp-sent-all", "Sent resource pack to all online players.", "Đã gửi resource pack cho tất cả người chơi online."); }
    public static String rpMerged(int files) { return get("rp-merged", "Imported " + files + " files from packs/import/", "Đã nhập " + files + " file từ packs/import/").replace("{count}", String.valueOf(files)); }
    public static String rpUsageFull() { return get("rp-usage-full", "Usage: /uitem rp <build|generate|export|sendall|apply|status|gui|merge>", "Cách dùng: /uitem rp <build|generate|export|sendall|apply|status|gui|merge>"); }

    public static String rpGuiItemsLoaded(int n) { return get("rp-gui-items", "<gray>Items in pack: <white>" + n, "<gray>Item trong pack: <white>" + n).replace("{count}", String.valueOf(n)); }
    public static String rpGuiCmdCount(int n) { return get("rp-gui-cmd", "<gray>CMD assignments: <white>" + n, "<gray>CMD đã gán: <white>" + n).replace("{count}", String.valueOf(n)); }
    public static String rpGuiHost(boolean on) { return get("rp-gui-host", "<gray>Auto-host: " + (on ? "<green>ON" : "<red>OFF"), "<gray>Tự host: " + (on ? "<green>BẬT" : "<red>TẮT")); }
    public static String rpGuiUrlLine(String url) { return get("rp-gui-url", "<gray>URL: <white>" + url, "<gray>URL: <white>" + url).replace("{url}", url); }
    public static String rpGuiBuildTitle() { return get("rp-gui-build-title", "BUILD PACK", "TẠO PACK"); }
    public static String rpGuiBuildDesc1() { return get("rp-gui-build-d1", "<gray>Generate + merge + export zip", "<gray>Tạo + gộp + xuất zip"); }
    public static String rpGuiBuildDesc2() { return get("rp-gui-build-d2", "<yellow>Like Oraxen / Nexo / ItemsAdder build", "<yellow>Giống build Oraxen / Nexo / ItemsAdder"); }
    public static String rpGuiSendAllTitle() { return get("rp-gui-send-title", "SEND TO ALL", "GỬI CHO TẤT CẢ"); }
    public static String rpGuiSendAllDesc1() { return get("rp-gui-send-d1", "<gray>Apply pack to online players", "<gray>Áp dụng pack cho người online"); }
    public static String rpGuiSendAllDesc2() { return get("rp-gui-send-d2", "<yellow>Requires URL or auto-host", "<yellow>Cần URL hoặc tự host"); }
    public static String rpGuiExportTitle() { return get("rp-gui-export-title", "EXPORT ZIP", "XUẤT ZIP"); }
    public static String rpGuiExportDesc1() { return get("rp-gui-export-d1", "<gray>Export without full rebuild", "<gray>Xuất zip không build lại"); }
    public static String rpGuiExportDesc2() { return get("rp-gui-export-d2", "<gray>Uses current pack folder", "<gray>Dùng thư mục pack hiện tại"); }
    public static String rpGuiFolderTitle() { return get("rp-gui-folder-title", "PACK FOLDER", "THƯ MỤC PACK"); }
    public static String rpGuiFolderHint() { return get("rp-gui-folder-hint", "Drop textures in textures/", "Đặt texture vào textures/"); }
    public static String rpGuiImportTitle() { return get("rp-gui-import-title", "IMPORT PACKS", "NHẬP PACK NGOÀI"); }
    public static String rpGuiImportHint() { return get("rp-gui-import-hint", "Place zips/folders in packs/import/", "Đặt zip/folder vào packs/import/"); }
    public static String rpGuiHelpTitle() { return get("rp-gui-help-title", "COMMANDS", "LỆNH"); }
    public static String rpGuiBuilding() { return get("rp-gui-building", "Building resource pack... please wait.", "Đang tạo resource pack... vui lòng chờ."); }
    public static String rpGenerating() { return get("rp-generating", "Generating resource pack... please wait.", "Đang tạo resource pack... vui lòng chờ."); }
    public static String rpUrlNotSet() { return get("rp-url-not-set", "No pack URL configured. Set resourcepack.url or enable auto-host.", "Chưa có URL pack. Đặt resourcepack.url hoặc bật auto-host."); }
    public static String rpApplied(String name) { return get("rp-applied", "Applied resource pack to " + name, "Đã áp dụng resource pack cho " + name).replace("{player}", name); }

    // ==================== GUI: Item Editor ====================
    public static String guiEditTitle(String id) { return get("gui-edit-title", "<gradient:#ff6b6b:#ffd93d>Edit: </gradient><white>" + id, "<gradient:#ff6b6b:#ffd93d>Sửa: </gradient><white>" + id).replace("{id}", id); }
    public static String guiEditorTitle() { return get("gui-editor-title", "<gradient:#ff6b6b:#ffd93d><bold>UItem Editor</bold></gradient>", "<gradient:#ff6b6b:#ffd93d><bold>Trình Sửa UItem</bold></gradient>"); }
    public static String guiManageItems() { return get("gui-manage-items", "<gray>Manage all custom items", "<gray>Quản lý tất cả custom items"); }
    public static String guiCreateNew() { return get("gui-create-new", "<green><bold>+ Create New Item</bold>", "<green><bold>+ Tạo Item Mới</bold>"); }
    public static String guiCreateHint() { return get("gui-create-hint", "<gray>Click to create a new custom item", "<gray>Nhấn để tạo custom item mới"); }
    public static String guiCreatePrompt() { return get("gui-create-prompt", "<yellow>Enter ID for new item <gray>(letters, numbers, underscores only):", "<yellow>Nhập ID cho item mới <gray>(chữ cái, số, dấu gạch dưới):"); }
    public static String guiCreateHint2() { return get("gui-create-hint2", "<yellow>You will be prompted to enter an ID", "<yellow>Bạn sẽ được yêu cầu nhập ID"); }
    public static String guiSearchButton() { return get("gui-search-btn", "<aqua><bold>Search Item</bold>", "<aqua><bold>Tìm Kiếm Item</bold>"); }
    public static String guiSearchHint1() { return get("gui-search-hint1", "<gray>Enter keyword to filter list", "<gray>Nhập từ khóa để lọc danh sách"); }
    public static String guiSearchHint2() { return get("gui-search-hint2", "<gray>Type 'clear' to clear filter", "<gray>Gõ 'clear' để xóa bộ lọc"); }
    public static String guiSearchPrompt() { return get("gui-search-prompt", "<yellow>Enter search keyword (<gray>or 'clear' to clear):", "<yellow>Nhập từ khóa tìm kiếm (<gray>hoặc 'clear' để xóa):"); }

    // ==================== GUI: Material Picker ====================
    public static String guiMaterialTitle() { return get("gui-mat-title", "<gold><bold>Choose Material</bold>", "<gold><bold>Chọn Vật Liệu</bold>"); }
    public static String guiMaterialSearch() { return get("gui-mat-search", "<gold><bold>Search</bold>", "<gold><bold>Tìm Kiếm</bold>"); }
    public static String guiMaterialSearchHint() { return get("gui-mat-search-hint", "<gray>Click to search material by name", "<gray>Nhấn để tìm vật liệu theo tên"); }
    public static String guiMaterialSelectHint() { return get("gui-mat-select", "<gray>Click to select this material", "<gray>Nhấn để chọn vật liệu này"); }
    public static String guiMaterialPrompt() { return get("gui-mat-prompt", "<yellow>Enter material name to search (Enter to clear filter):", "<yellow>Nhập tên vật liệu để tìm (Enter để xóa lọc):"); }

    // ==================== Item Editor fields ====================
    public static String guiMaterialField() { return get("gui-field-material", "<gold><bold>Material</bold>", "<gold><bold>Vật Liệu</bold>"); }
    public static String guiMaterialClick() { return get("gui-field-material-click", "<yellow>Click to open material list", "<yellow>Nhấn để mở danh sách vật liệu"); }
    public static String guiDisplayNameField() { return get("gui-field-name", "<gold><bold>Display Name</bold>", "<gold><bold>Tên Hiển Thị</bold>"); }
    public static String guiDisplayNameClick() { return get("gui-field-name-click", "<yellow>Click to change name", "<yellow>Nhấn để đổi tên"); }
    public static String guiDisplayNameSupport() { return get("gui-field-name-support", "<gray>Supports MiniMessage format", "<gray>Hỗ trợ định dạng MiniMessage"); }
    public static String guiDisplayNameExample() { return get("gui-field-name-example", "<gray>Example: <red>Ruby <gold>Sword", "<gray>Ví dụ: <red>Ruby <gold>Sword"); }
    public static String guiDisplayNamePrompt() { return get("gui-field-name-prompt", "<yellow>Enter display name <gray>(supports MiniMessage):", "<yellow>Nhập tên hiển thị <gray>(hỗ trợ MiniMessage):"); }
    public static String renamedTo(String name) { return get("renamed-to", "Renamed to: " + name, "Đã đổi tên thành: " + name).replace("{name}", name); }
    public static String guiLoreField() { return get("gui-field-lore", "<gold><bold>Lore</bold>", "<gold><bold>Mô Tả</bold>"); }
    public static String guiLoreClick() { return get("gui-field-lore-click", "<yellow>Click to edit lore", "<yellow>Nhấn để sửa mô tả"); }
    public static String guiLoreOpen() { return get("gui-field-lore-open", "<gray>Open Lore Editor GUI", "<gray>Mở trình sửa Lore"); }
    public static String guiEnchantsField() { return get("gui-field-ench", "<gold><bold>Enchantments</bold>", "<gold><bold>Phù Phép</bold>"); }
    public static String guiEnchantsClick() { return get("gui-field-ench-click", "<yellow>Click to edit enchantments", "<yellow>Nhấn để sửa phù phép"); }
    public static String guiFlagsField() { return get("gui-field-flags", "<gold><bold>Item Flags</bold>", "<gold><bold>Cờ Item</bold>"); }
    public static String guiFlagsClick() { return get("gui-field-flags-click", "<yellow>Click to edit flags", "<yellow>Nhấn để sửa cờ"); }
    public static String guiCmdField() { return get("gui-field-cmd", "<gold><bold>Custom Model Data</bold>", "<gold><bold>Custom Model Data</bold>"); }
    public static String guiCmdClick() { return get("gui-field-cmd-click", "<yellow>Click to change", "<yellow>Nhấn để thay đổi"); }
    public static String guiCmdHint() { return get("gui-field-cmd-hint", "<gray>Enter number (-1 to disable)", "<gray>Nhập số (-1 để tắt)"); }
    public static String guiCmdPrompt() { return get("gui-field-cmd-prompt", "<yellow>Enter Custom Model Data number <gray>(-1 to disable):", "<yellow>Nhập số Custom Model Data <gray>(-1 để tắt):"); }
    public static String guiUnbreakableField() { return get("gui-field-unbreak", "<gold><bold>Unbreakable</bold>", "<gold><bold>Không Thể Phá</bold>"); }
    public static String guiUnbreakableOn() { return get("gui-field-unbreak-on", "<green>ON", "<green>BẬT"); }
    public static String guiUnbreakableOff() { return get("gui-field-unbreak-off", "<red>OFF", "<red>TẮT"); }
    public static String guiUnbreakableToggle() { return get("gui-field-unbreak-toggle", "<yellow>Click to toggle", "<yellow>Nhấn để chuyển đổi"); }
    public static String guiDurabilityField() { return get("gui-field-dur", "<gold><bold>Durability</bold>", "<gold><bold>Độ Bền</bold>"); }
    public static String guiDurabilityClick() { return get("gui-field-dur-click", "<yellow>Click to set max durability", "<yellow>Nhấn để đặt độ bền tối đa"); }
    public static String guiDurabilityHint1() { return get("gui-field-dur-hint1", "<gray>Enter -1 for default material durability", "<gray>Nhập -1 cho độ bền mặc định"); }
    public static String guiDurabilityHint2() { return get("gui-field-dur-hint2", "<gray>Enter 0 for unbreakable-like (no damage bar)", "<gray>Nhập 0 để không có thanh hư hỏng"); }
    public static String guiDurabilityHint3() { return get("gui-field-dur-hint3", "<gray>Or enter any number > 0", "<gray>Hoặc nhập số > 0"); }
    public static String guiDurabilityPrompt() { return get("gui-field-dur-prompt", "<yellow>Enter max durability <gray>(-1 = default, 0 = no damage bar, >0 = custom):", "<yellow>Nhập độ bền tối đa <gray>(-1 = mặc định, 0 = không thanh, >0 = tùy chỉnh):"); }
    public static String durabilitySet(String status) { return get("durability-set", "Max durability = " + status, "Độ bền tối đa = " + status).replace("{status}", status); }
    public static String durabilityDefault() { return get("durability-default", "Default", "Mặc định"); }
    public static String durabilityNoBar() { return get("durability-no-bar", "No bar", "Không thanh"); }
    public static String guiGlowingField() { return get("gui-field-glow", "<gold><bold>Glowing</bold>", "<gold><bold>Phát Sáng</bold>"); }
    public static String guiGlowingToggle() { return get("gui-field-glow-toggle", "<yellow>Click to toggle", "<yellow>Nhấn để chuyển đổi"); }
    public static String guiGlowingHint() { return get("gui-field-glow-hint", "<gray>Add glowing effect to item", "<gray>Thêm hiệu ứng phát sáng cho item"); }
    public static String guiAttributesField() { return get("gui-field-attr", "<gold><bold>Attributes</bold>", "<gold><bold>Thuộc Tính</bold>"); }

    // ==================== GUI: Attributes ====================
    public static String guiAttributesClick() { return get("gui-attributes-click", "<yellow>Click to add attribute", "<yellow>Nhấn để thêm thuộc tính"); }
    public static String guiAttributesFormat() { return get("gui-attributes-format", "<gray>Format: attribute amount operation slot", "<gray>Định dạng: thuộc_tính giá_trị phép_toán vị_trí"); }
    public static String guiAttributesExample() { return get("gui-attributes-example", "<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND", "<gray>Ví dụ: generic_attack_damage 15 ADD_NUMBER HAND"); }
    public static String guiAttributesPrompt() { return get("gui-attributes-prompt", "<yellow>Enter attribute <gray>(format: attribute amount operation slot)\\n<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND\\n<gray>Type 'clear' to remove all attributes:", "<yellow>Nhập thuộc tính <gray>(định dạng: thuộc_tính giá_trị phép_toán vị_trí)\\n<gray>Ví dụ: generic_attack_damage 15 ADD_NUMBER HAND\\n<gray>Gõ 'clear' để xóa tất cả thuộc tính:"); }
    public static String guiAttributesCleared() { return get("gui-attributes-cleared", "Removed all attributes!", "Đã xóa tất cả thuộc tính!"); }
    public static String guiAttributesError() { return get("gui-attributes-error", "Format: attribute amount operation slot", "Định dạng: thuộc_tính giá_trị phép_toán vị_trí"); }
    public static String guiAttributesAdded(String attr) { return get("gui-attributes-added", "Added attribute " + attr + "!", "Đã thêm thuộc tính " + attr + "!").replace("{attr}", attr); }

    // ==================== GUI: Rarity ====================
    public static String guiRarityField() { return get("gui-rarity-field", "<gold><bold>Rarity</bold>", "<gold><bold>Độ Hiếm</bold>"); }
    public static String guiRarityNone() { return get("gui-rarity-none", "None", "Không có"); }
    public static String guiRarityClick() { return get("gui-rarity-click", "<yellow>Click to choose rarity", "<yellow>Nhấn để chọn độ hiếm"); }
    public static String guiRarityHints() { return get("gui-rarity-hints", "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA", "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA"); }
    public static String guiRarityListHeader() { return get("gui-rarity-list-header", "<gold>Available rarities:<dark_gray> ", "<gold>Các độ hiếm có sẵn:<dark_gray> "); }
    public static String guiRarityPrompt() { return get("gui-rarity-prompt", "<yellow>Enter rarity name (or 'none' to remove):", "<yellow>Nhập tên độ hiếm (hoặc 'none' để xóa):"); }
    public static String guiRarityRemoved() { return get("gui-rarity-removed", "Removed rarity!", "Đã xóa độ hiếm!"); }
    public static String guiRaritySet(String rarity) { return get("gui-rarity-set", "Set rarity = " + rarity, "Đã đặt độ hiếm = " + rarity).replace("{rarity}", rarity); }
    public static String guiRarityNotFound(String rarity) { return get("gui-rarity-not-found", "Rarity '" + rarity + "' does not exist!", "Độ hiếm '" + rarity + "' không tồn tại!").replace("{rarity}", rarity); }


    // ==================== GUI: ExecutableItems ====================
    public static String guiEIIdField() { return get("gui-eiid-field", "<gold><bold>⚡ ExecutableItems</bold>", "<gold><bold>⚡ ExecutableItems</bold>"); }
    public static String guiEIIdCurrent() { return get("gui-eiid-current", "None", "Không có"); }
    public static String guiEIIdClick() { return get("gui-eiid-click", "<yellow>Click to set ExecutableItems ID", "<yellow>Nhấn để đặt ID ExecutableItems"); }
    public static String guiEIIdHint() { return get("gui-eiid-hint", "<gray>Skills/Passives will be handled by EI", "<gray>Kỹ năng/Bị động sẽ được EI xử lý"); }
    public static String guiEIIdPrompt() { return get("gui-eiid-prompt", "<yellow>Enter ExecutableItems item ID <gray>(or 'none' to remove):", "<yellow>Nhập ID item ExecutableItems <gray>(hoặc 'none' để xóa):"); }

    // ==================== GUI: Oraxen ====================
    public static String guiOraxenField() { return get("gui-oraxen-field", "<aqua><bold>Oraxen ID</bold>", "<aqua><bold>Oraxen ID</bold>"); }
    public static String guiOraxenCurrent() { return get("gui-oraxen-current", "None", "Không có"); }
    public static String guiOraxenClick() { return get("gui-oraxen-click", "<yellow>Click to set Oraxen item ID", "<yellow>Nhấn để đặt ID item Oraxen"); }
    public static String guiOraxenHint() { return get("gui-oraxen-hint", "<gray>Use item from Oraxen as base", "<gray>Dùng item từ Oraxen làm gốc"); }
    public static String guiOraxenPrompt() { return get("gui-oraxen-prompt", "<yellow>Enter Oraxen item ID <gray>(or 'none' to remove):", "<yellow>Nhập ID item Oraxen <gray>(hoặc 'none' để xóa):"); }
    public static String guiOraxenPick() { return get("gui-oraxen-pick", "<aqua>Shift+Click: pick ID from hand", "<aqua>Shift+Click: lấy ID từ item trên tay"); }

    // ==================== GUI: Nexo ====================
    public static String guiNexoField() { return get("gui-nexo-field", "<light_purple><bold>Nexo ID</bold>", "<light_purple><bold>Nexo ID</bold>"); }
    public static String guiNexoClick() { return get("gui-nexo-click", "<yellow>Click to set Nexo item ID", "<yellow>Nhấn để đặt ID item Nexo"); }
    public static String guiNexoHint() { return get("gui-nexo-hint", "<gray>Use item from Nexo as base", "<gray>Dùng item từ Nexo làm gốc"); }
    public static String guiNexoPrompt() { return get("gui-nexo-prompt", "<yellow>Enter Nexo item ID <gray>(or 'none' to remove):", "<yellow>Nhập ID item Nexo <gray>(hoặc 'none' để xóa):"); }
    public static String guiNexoPick() { return get("gui-nexo-pick", "<light_purple>Shift+Click: pick ID from hand", "<light_purple>Shift+Click: lấy ID từ item trên tay"); }

    // ==================== GUI: ItemsAdder ====================
    public static String guiIaField() { return get("gui-ia-field", "<green><bold>ItemsAdder ID</bold>", "<green><bold>ItemsAdder ID</bold>"); }
    public static String guiIaCurrent() { return get("gui-ia-current", "None", "Không có"); }
    public static String guiIaClick() { return get("gui-ia-click", "<yellow>Click to set ItemsAdder item ID", "<yellow>Nhấn để đặt ID item ItemsAdder"); }
    public static String guiIaHint() { return get("gui-ia-hint", "<gray>Use item from ItemsAdder as base", "<gray>Dùng item từ ItemsAdder làm gốc"); }
    public static String guiIaPrompt() { return get("gui-ia-prompt", "<yellow>Enter ItemsAdder namespaced ID <gray>(or 'none' to remove):", "<yellow>Nhập ID định danh ItemsAdder <gray>(hoặc 'none' để xóa):"); }
    public static String guiIaPick() { return get("gui-ia-pick", "<green>Shift+Click: pick ID from hand", "<green>Shift+Click: lấy ID từ item trên tay"); }
    public static String guiEIIdPick() { return get("gui-eiid-pick", "<gold>Shift+Click: pick ID from hand", "<gold>Shift+Click: lấy ID từ item trên tay"); }
    public static String guiIntegrationShiftHint() { return get("gui-integration-shift", "<dark_gray>Shift+Click = lấy ID từ tay", "<dark_gray>Shift+Click = lấy ID từ tay"); }

    // ==================== GUI: Integrations hub ====================
    public static String guiIntegrationsTitle() { return get("gui-integrations-title", "Integrations", "Tích hợp"); }
    public static String guiIntegrationsSubtitle() { return get("gui-integrations-sub", "<gray>Oraxen, Nexo, ItemsAdder, ExecutableItems", "<gray>Oraxen, Nexo, ItemsAdder, ExecutableItems"); }
    public static String guiIntegrationsHub() { return get("gui-integrations-hub", "Integrations", "Tích hợp plugin"); }
    public static String guiIntegrationsHubDesc() { return get("gui-integrations-hub-d", "<gray>Oraxen / Nexo / ItemsAdder / EI", "<gray>Oraxen / Nexo / ItemsAdder / EI"); }
    public static String guiIntegrationsPriority() { return get("gui-integrations-priority", "Build priority", "Thứ tự ưu tiên build"); }
    public static String guiIntegrationsRescan() { return get("gui-integrations-rescan", "Rescan plugins", "Quét lại plugin"); }

    // ==================== Potion Studio ====================
    public static String potionHubTitle() {
        return get("potion-hub-title", "Potion Studio", "Studio Thuốc");
    }
    public static String potionHubSubtitle() {
        return get("potion-hub-sub", "<gray>Custom potions with effects & colors", "<gray>Tạo thuốc tùy chỉnh");
    }
    public static String potionListButton() {
        return get("potion-list-btn", "Potion List", "Danh sách Potion");
    }
    public static String potionListButtonDesc() {
        return get("potion-list-btn-d", "<gray>View and edit all potions", "<gray>Xem và sửa potion");
    }
    public static String potionCreateButton() {
        return get("potion-create-btn", "+ New Potion", "+ Tạo Potion");
    }
    public static String potionCreateButtonDesc() {
        return get("potion-create-btn-d", "<gray>Create a new custom potion", "<gray>Tạo potion mới");
    }
    public static String potionCreatePrompt() {
        return get("potion-create-prompt", "<yellow>Enter new potion ID:", "<yellow>Nhập ID potion mới:");
    }
    public static String potionInvalidId() {
        return get("potion-invalid-id", "Invalid potion ID!", "ID potion không hợp lệ!");
    }
    public static String potionExists(String id) {
        return get("potion-exists", "Potion '" + id + "' already exists!", "Potion '" + id + "' đã tồn tại!");
    }
    public static String potionCreated(String id) {
        return get("potion-created", "Created potion '" + id + "'!", "Đã tạo potion '" + id + "'!");
    }
    public static String potionListTitle() {
        return get("potion-list-title", "Potion List", "Danh sách Potion");
    }
    public static String potionDeleteTitle() {
        return get("potion-delete-title", "Delete Potion?", "Xóa Potion?");
    }
    public static String potionDeleteConfirm() {
        return get("potion-delete-yes", "Confirm Delete", "Xác nhận xóa");
    }
    public static String potionDeleteCancel() {
        return get("potion-delete-no", "Cancel", "Hủy");
    }
    public static String potionDeleted(String id) {
        return get("potion-deleted", "Deleted potion '" + id + "'", "Đã xóa potion '" + id + "'");
    }
    public static String potionFieldName() {
        return get("potion-field-name", "<gold><bold>Display Name</bold>", "<gold><bold>Tên hiển thị</bold>");
    }
    public static String potionFieldLore() {
        return get("potion-field-lore", "<gold><bold>Lore</bold>", "<gold><bold>Lore</bold>");
    }
    public static String potionFieldKind() {
        return get("potion-field-kind", "<aqua><bold>Potion Type</bold>", "<aqua><bold>Loại bình</bold>");
    }
    public static String potionFieldColor() {
        return get("potion-field-color", "<light_purple><bold>Color</bold>", "<light_purple><bold>Màu sắc</bold>");
    }
    public static String potionFieldEffects() {
        return get("potion-field-effects", "<green><bold>Effects</bold>", "<green><bold>Hiệu ứng</bold>");
    }
    public static String potionFieldGlow() {
        return get("potion-field-glow", "<yellow><bold>Glow</bold>", "<yellow><bold>Phát sáng</bold>");
    }
    public static String potionSave() {
        return get("potion-save", "SAVE", "LƯU");
    }
    public static String potionSaved(String id) {
        return get("potion-saved", "Saved potion '" + id + "'!", "Đã lưu potion '" + id + "'!");
    }
    public static String potionGiveSelf() {
        return get("potion-give-self", "Test (give self)", "Thử (tự give)");
    }
    public static String potionGivenSelf(String id) {
        return get("potion-given-self", "Received potion '" + id + "'", "Nhận potion '" + id + "'");
    }
    public static String potionPromptName() {
        return get("potion-prompt-name", "<yellow>Enter display name (MiniMessage):", "<yellow>Nhập tên (MiniMessage):");
    }
    public static String potionPromptLore() {
        return get("potion-prompt-lore", "<yellow>Enter lore line (or 'clear'):", "<yellow>Nhập dòng lore (hoặc 'clear'):");
    }
    public static String potionPromptColor() {
        return get("potion-prompt-color", "<yellow>Color #RRGGBB or r,g,b:", "<yellow>Màu #RRGGBB hoặc r,g,b:");
    }
    public static String potionEffectsTitle() {
        return get("potion-effects-title", "Potion Effects", "Hiệu ứng Potion");
    }
    public static String potionAddEffect() {
        return get("potion-add-effect", "+ Add Effect", "+ Thêm hiệu ứng");
    }
    public static String potionEffectRemoved() {
        return get("potion-effect-removed", "Effect removed.", "Đã xóa hiệu ứng.");
    }
    public static String potionEffectAdded(String type) {
        return get("potion-effect-added", "Added effect: " + type, "Đã thêm: " + type);
    }
    public static String potionPromptDuration() {
        return get("potion-prompt-duration", "<yellow>Duration in seconds:", "<yellow>Thời gian (giây):");
    }
    public static String potionInvalidNumber() {
        return get("potion-invalid-num", "Invalid number!", "Số không hợp lệ!");
    }
    public static String potionPickerTitle() {
        return get("potion-picker-title", "Pick Effect", "Chọn hiệu ứng");
    }

    // ==================== GUI: Recipe editor / chain ====================
    public static String recipeEditorGuide1() {
        return get("recipe-editor-g1", "<gray>Place ingredients in the left slots", "<gray>Đặt nguyên liệu vào ô bên trái");
    }
    public static String recipeEditorGuide2() {
        return get("recipe-editor-g2", "<gray>Ingredients: <white>Select Item", "<gray>NL: nút <white>Chọn Item");
    }
    public static String recipeEditorGuide3() {
        return get("recipe-editor-g3", "<gray>Or drag items from /uitem give", "<gray>Hoặc kéo item từ /uitem give");
    }
    public static String recipePickerButton() {
        return get("recipe-picker-btn", "<gold><bold>Select UItem</bold>", "<gold><bold>Chọn Item UItem</bold>");
    }
    public static String recipePickerButtonDesc() {
        return get("recipe-picker-btn-d", "<gray>Use another custom item as ingredient", "<gray>Dùng item UItem khác làm NL");
    }
    public static String recipePickerTitle() {
        return get("recipe-picker-title", "Recipe ingredients", "Nguyên liệu công thức");
    }
    public static String recipePickerDesc() {
        return get("recipe-picker-desc", "<gray>Click an item to add to the recipe", "<gray>Chọn item để thêm vào công thức");
    }
    /** @deprecated use {@link #recipePickerSelfItemError()} */
    public static String recipePickerSelfError() {
        return recipePickerSelfItemError();
    }
    public static String recipePickerSelfItemError() {
        return get("recipe-picker-self-item",
                "Cannot use this result item as its own ingredient.",
                "Không thể dùng chính item kết quả làm nguyên liệu!");
    }
    public static String recipePickerNoPotionIngredient() {
        return get("recipe-picker-no-potion",
                "Potion UItem cannot be a recipe ingredient. Link potion on the item in Item Editor.",
                "Potion UItem không làm NL công thức. Gắn potion ở Item Editor (Gắn Potion vào Item).");
    }
    public static String recipePickerFull() {
        return get("recipe-picker-full", "Crafting grid is full!", "Ô công thức đã đầy!");
    }
    public static String recipePickerAdded(String id) {
        return get("recipe-picker-added", "Added ingredient: " + id, "Đã thêm NL: " + id);
    }
    public static String recipePickerPotionButton() {
        return get("recipe-picker-potion-btn", "<light_purple><bold>Select Potion</bold>", "<light_purple><bold>Chọn Potion</bold>");
    }
    public static String recipePickerPotionButtonDesc() {
        return get("recipe-picker-potion-btn-d", "<gray>Potion as crafting ingredient", "<gray>Potion làm NL chế item");
    }
    public static String recipePickerPotionTitle() {
        return get("recipe-picker-potion-title", "Potion ingredients", "Nguyên liệu Potion");
    }
    public static String recipePickerPotionDesc() {
        return get("recipe-picker-potion-desc", "<gray>Click a potion to add (IDs may match item names)", "<gray>Chọn potion UItem (ID có thể trùng tên item)");
    }
    public static String recipePickerPotionAdded(String id) {
        return get("recipe-picker-potion-added", "Added potion ingredient: " + id, "Đã thêm potion: " + id);
    }
    public static String recipePickerPotionNotFound(String id) {
        return get("recipe-picker-potion-missing", "Potion '" + id + "' not found.", "Không tìm thấy potion '" + id + "'.");
    }
    public static String recipePickerItemNotFound(String id) {
        return get("recipe-picker-item-missing", "Item '" + id + "' not found.", "Không tìm thấy item '" + id + "'.");
    }
    public static String recipePickerInvalidIngredient() {
        return get("recipe-picker-invalid", "Invalid ingredient (use UItem or vanilla material).", "Nguyên liệu không hợp lệ (UItem hoặc material vanilla).");
    }
    public static String recipePickerTabItems() {
        return get("recipe-picker-tab-items", "Items", "Item");
    }
    public static String recipePickerTabPotions() {
        return get("recipe-picker-tab-potions", "Potions", "Potion");
    }
    public static String recipePickerTabSelected() {
        return get("recipe-picker-tab-on", "<green>Selected", "<green>Đang chọn");
    }
    public static String recipePickerTabSwitch() {
        return get("recipe-picker-tab-switch", "<yellow>Click to switch", "<yellow>Click để chuyển");
    }
    public static String recipePickerResultLine(String resultId) {
        return get("recipe-picker-result", "<gray>Result item: <white>" + resultId, "<gray>Item kết quả: <white>" + resultId);
    }
    public static String recipePickerPageLine(int page, int total) {
        return get("recipe-picker-page", "<yellow>Page: <white>" + page + "<gray>/<white>" + total,
                "<yellow>Trang: <white>" + page + "<gray>/<white>" + total);
    }
    public static String recipePickerIdLine(String id) {
        return get("recipe-picker-id", "<gray>ID: <white>" + id, "<gray>ID: <white>" + id);
    }
    public static String recipePickerSelfItemHint() {
        return get("recipe-picker-self-hint", "<red>Cannot use as ingredient (result item)", "<red>Không dùng làm NL (item kết quả)");
    }
    public static String recipePickerClickAddItem() {
        return get("recipe-picker-click-item", "<yellow>Click → add ingredient", "<yellow>Click → thêm NL");
    }
    public static String recipePickerClickAddPotion() {
        return get("recipe-picker-click-potion", "<yellow>Click → add potion", "<yellow>Click → thêm potion");
    }
    public static String recipePickerAlsoPotionHint(String id) {
        return get("recipe-picker-also-potion",
                "<aqua>ID <white>" + id + " <aqua>is also a potion — use <light_purple>Potions<aqua> tab for potion NL",
                "<aqua>ID <white>" + id + " <aqua>cũng là potion — dùng tab <light_purple>Potion<aqua> để làm NL thuốc");
    }
    public static String recipePickerPotionKindLine(String kind) {
        return get("recipe-picker-potion-kind", "<gray>Type: <white>" + kind, "<gray>Loại: <white>" + kind);
    }
    public static String recipeListPageLine(int page, int total, int count) {
        return get("recipe-list-page",
                "<yellow>Page: <white>" + page + "<gray>/<white>" + total + " <dark_gray>| <yellow>Total: <white>" + count,
                "<yellow>Trang: <white>" + page + "<gray>/<white>" + total + " <dark_gray>| <yellow>Tổng: <white>" + count);
    }
    public static String craftingUsage(String label) {
        return get("crafting-usage", "Usage: /" + label + " [list|craft]", "Cách dùng: /" + label + " [list|craft]");
    }
    public static String itemEditorNoPotions() {
        return get("item-editor-no-potions", "No UItem potions yet. Use /uitem potion to create one.", "Chưa có potion UItem. Dùng /uitem potion để tạo.");
    }

    public static String itemEditorLinkedPotionBtn() {
        return get("item-editor-linked-potion", "<light_purple><bold>Gắn Potion vào Item</bold>",
                "<light_purple><bold>Gắn Potion vào Item</bold>");
    }

    public static String itemEditorLinkedPotionDesc() {
        return get("item-editor-linked-potion-d",
                "<gray>Link a UItem potion — right-click item to apply its effects",
                "<gray>Gắn potion UItem — click phải item để nhận hiệu ứng");
    }

    public static String itemEditorLinkedPotionCurrent(String id) {
        return get("item-editor-linked-potion-current", "<gray>Linked: <light_purple>" + id,
                "<gray>Đang gắn: <light_purple>" + id);
    }

    public static String itemEditorLinkedPotionNone() {
        return get("item-editor-linked-potion-none", "<gray>No potion linked", "<gray>Chưa gắn potion");
    }

    public static String itemEditorLinkedPotionClick() {
        return get("item-editor-linked-potion-click", "<yellow>Click <gray>choose potion",
                "<yellow>Click <gray>chọn potion");
    }

    public static String itemEditorLinkedPotionClear() {
        return get("item-editor-linked-potion-clear", "<yellow>Shift+Click <gray>remove link",
                "<yellow>Shift+Click <gray>gỡ potion");
    }

    public static String itemEditorLinkedPotionAttached(String id) {
        return get("item-editor-linked-potion-attached", "Linked potion: " + id, "Đã gắn potion: " + id);
    }

    public static String itemEditorLinkedPotionRemoved() {
        return get("item-editor-linked-potion-removed", "Removed linked potion.", "Đã gỡ potion khỏi item.");
    }

    public static String itemLinkedPotionPickerTitle() {
        return get("item-linked-potion-picker-title", "Link Potion to Item", "Gắn Potion vào Item");
    }

    public static String itemLinkedPotionPickerDesc() {
        return get("item-linked-potion-picker-desc",
                "<gray>Click a potion to attach to this item",
                "<gray>Chọn potion để gắn vào item này");
    }

    public static String itemLinkedPotionUsed(String id) {
        return get("item-linked-potion-used", "Applied potion effects: " + id,
                "Đã áp hiệu ứng potion: " + id);
    }

    // ==================== GUI: Recipe ====================
    public static String guiRecipeField() { return get("gui-recipe-field", "<gold><bold>Crafting Recipe</bold>", "<gold><bold>Công Thức Chế Tạo</bold>"); }
    public static String guiRecipeHas() { return get("gui-recipe-has", "<green>Has recipe", "<green>Có công thức"); }
    public static String guiRecipeNone() { return get("gui-recipe-none", "<red>No recipe", "<red>Không có công thức"); }
    public static String guiRecipeClick() { return get("gui-recipe-click", "<yellow>Click to edit recipe", "<yellow>Nhấn để sửa công thức"); }
    public static String guiRecipeHint() { return get("gui-recipe-hint", "<gray>Set shaped/shapeless crafting recipe", "<gray>Đặt công thức chế tạo shaped/shapeless"); }

    // ==================== GUI: Save / Delete Buttons ====================
    public static String guiSaveButton() { return get("gui-save-button", "<green><bold>💾 SAVE</bold>", "<green><bold>💾 LƯU</bold>"); }
    public static String guiSaveDesc() { return get("gui-save-desc", "<gray>Save all changes to file", "<gray>Lưu tất cả thay đổi vào file"); }
    public static String guiSaveClick() { return get("gui-save-click", "<yellow>Click to save", "<yellow>Nhấn để lưu"); }
    public static String guiDeleteButton() { return get("gui-delete-button", "<red><bold>🗑 DELETE</bold>", "<red><bold>🗑 XÓA</bold>"); }
    public static String guiDeleteDesc() { return get("gui-delete-desc", "<gray>Delete this item", "<gray>Xóa item này"); }
    public static String guiDeleteWarn() { return get("gui-delete-warn", "<red>⚠ Click to delete", "<red>⚠ Nhấn để xóa"); }

    // ==================== Item CRUD Messages ====================
    public static String savedItem(String id) { return get("saved-item", "Saved item '" + id + "'!", "Đã lưu item '" + id + "'!").replace("{id}", id); }
    public static String invalidId() { return get("invalid-id", "Invalid ID!", "ID không hợp lệ!"); }
    public static String itemAlreadyExists(String id) { return get("item-already-exists", "Item '" + id + "' already exists!", "Item '" + id + "' đã tồn tại!").replace("{id}", id); }
    public static String cannotCreateBlocked(String id) { return get("cannot-create-blocked", "Cannot create item '" + id + "' (blocked by another plugin).", "Không thể tạo item '" + id + "' (bị chặn bởi plugin khác).").replace("{id}", id); }
    public static String createdItem(String id) { return get("created-item", "Created item '" + id + "'!", "Đã tạo item '" + id + "'!").replace("{id}", id); }
    public static String cannotDeleteBlocked(String id) { return get("cannot-delete-blocked", "Cannot delete item '" + id + "' (blocked by another plugin).", "Không thể xóa item '" + id + "' (bị chặn bởi plugin khác).").replace("{id}", id); }
    public static String deletedItem(String id) { return get("deleted-item", "Deleted item '" + id + "'!", "Đã xóa item '" + id + "'!").replace("{id}", id); }

    // ==================== Confirm Delete GUI ====================
    public static String confirmDeleteTitle() { return get("confirm-delete-title", "<red><bold>Confirm Delete Item", "<red><bold>Xác Nhận Xóa Item"); }
    public static String confirmDeleteDesc() { return get("confirm-delete-desc", "Delete item: ", "Xóa item: "); }
    public static String confirmDeleteWarn() { return get("confirm-delete-warn", "<red>⚠ This action cannot be undone!", "<red>⚠ Hành động này không thể hoàn tác!"); }
    public static String confirmDeleteYes() { return get("confirm-delete-yes", "<green><bold>CONFIRM DELETE", "<green><bold>XÁC NHẬN XÓA"); }
    public static String confirmDeleteNo() { return get("confirm-delete-no", "<red><bold>CANCEL", "<red><bold>HỦY"); }
    public static String confirmDeleteBack() { return get("confirm-delete-back", "<gray>Back to item list", "<gray>Quay lại danh sách item"); }


    // ==================== Navigation ====================
    public static String guiBack() { return get("gui-back", "<yellow>← Back", "<yellow>← Quay Lại"); }
    public static String guiBackDesc() { return get("gui-back-desc", "<gray>Click to go back", "<gray>Nhấn để quay lại"); }
    public static String guiPrevious() { return get("gui-previous", "<yellow>◄ Previous page", "<yellow>◄ Trang trước"); }
    public static String guiNext() { return get("gui-next", "<yellow>Next page ►", "<yellow>Trang sau ►"); }

    // ==================== Lore Editor ====================
    public static String loreEditorTitle(String id) { return get("lore-editor-title", "<gold><bold>Lore Editor</bold> <dark_gray>- <white>" + id, "<gold><bold>Trình Sửa Lore</bold> <dark_gray>- <white>" + id).replace("{id}", id); }
    public static String loreEditorHeader() { return get("lore-editor-header", "<gold><bold>Lore Editor</bold>", "<gold><bold>Trình Sửa Lore</bold>"); }
    public static String loreEditorDesc() { return get("lore-editor-desc", "<gray>Edit item description", "<gray>Sửa mô tả của item"); }
    public static String loreEditorLeftClick() { return get("lore-editor-left", "<yellow>Left click: <white>Edit line", "<yellow>Trái chuột: <white>Sửa dòng"); }
    public static String loreEditorRightClick() { return get("lore-editor-right", "<yellow>Right click: <white>Delete line", "<yellow>Phải chuột: <white>Xóa dòng"); }
    public static String loreEditorShiftClick() { return get("lore-editor-shift", "<yellow>Shift+Click: <white>Insert blank line above", "<yellow>Shift+Nhấn: <white>Chèn dòng trống phía trên"); }
    public static String loreEditorLine(int line) { return get("lore-editor-line", "Line " + line, "Dòng " + line).replace("{line}", String.valueOf(line)); }
    public static String loreEditorContent() { return get("lore-editor-content", "<gray>Content:", "<gray>Nội dung:"); }
    public static String loreEditorEmpty() { return get("lore-editor-empty", "<dark_gray>(empty)", "<dark_gray>(trống)"); }
    public static String loreEditorEdit() { return get("lore-editor-edit", "<yellow>Left click: <white>Edit", "<yellow>Trái chuột: <white>Sửa"); }
    public static String loreEditorDelete() { return get("lore-editor-delete", "<red>Right click: <white>Delete", "<red>Phải chuột: <white>Xóa"); }
    public static String loreEditorInsert() { return get("lore-editor-insert", "<aqua>Shift+Click: <white>Insert blank line", "<aqua>Shift+Nhấn: <white>Chèn dòng trống"); }
    public static String loreEditorPrompt(int line) { return get("lore-editor-prompt", "<yellow>Enter new content for line " + line + " <gray>(supports MiniMessage):", "<yellow>Nhập nội dung mới cho dòng " + line + " <gray>(hỗ trợ MiniMessage):").replace("{line}", String.valueOf(line)); }
    public static String loreEditorAddButton() { return get("lore-editor-add-btn", "<green><bold>+ Add new line</bold>", "<green><bold>+ Thêm dòng mới</bold>"); }
    public static String loreEditorAddDesc() { return get("lore-editor-add-desc", "<gray>Add a new line at the end of lore", "<gray>Thêm dòng mới vào cuối lore"); }
    public static String loreEditorAddPrompt() { return get("lore-editor-add-prompt", "<yellow>Enter new line content <gray>(supports MiniMessage):", "<yellow>Nhập nội dung dòng mới <gray>(hỗ trợ MiniMessage):"); }
    public static String loreEditorClearButton() { return get("lore-editor-clear-btn", "<red><bold>Clear all lore</bold>", "<red><bold>Xóa tất cả lore</bold>"); }
    public static String loreEditorClearDesc() { return get("lore-editor-clear-desc", "<gray>Delete all lore lines", "<gray>Xóa tất cả dòng lore"); }


    // ==================== Enchantment Editor ====================
    public static String enchEditorTitle(String id) { return get("ench-editor-title", "<light_purple><bold>Enchantments</bold> <dark_gray>- <white>" + id, "<light_purple><bold>Phù Phép</bold> <dark_gray>- <white>" + id).replace("{id}", id); }
    public static String enchEditorHeader() { return get("ench-editor-header", "<light_purple><bold>Enchantment Editor</bold>", "<light_purple><bold>Trình Sửa Phù Phép</bold>"); }
    public static String enchEditorDesc() { return get("ench-editor-desc", "<gray>Edit enchantments for this item", "<gray>Sửa phù phép cho item này"); }
    public static String enchEditorLeft() { return get("ench-editor-left", "<yellow>Left click: <white>Increase level", "<yellow>Trái chuột: <white>Tăng cấp"); }
    public static String enchEditorRight() { return get("ench-editor-right", "<yellow>Right click: <white>Decrease level", "<yellow>Phải chuột: <white>Giảm cấp"); }
    public static String enchEditorShift() { return get("ench-editor-shift", "<yellow>Shift+Click: <white>Remove entirely", "<yellow>Shift+Nhấn: <white>Xóa hoàn toàn"); }
    public static String enchEditorKey(String key) { return get("ench-editor-key", "Key: " + key, "Khóa: " + key).replace("{key}", key); }
    public static String enchEditorAdded(int level) { return get("ench-editor-added", "✔ Added - Level " + level, "✔ Đã thêm - Cấp " + level).replace("{level}", String.valueOf(level)); }
    public static String enchEditorNotAdded() { return get("ench-editor-not-added", "✘ Not added", "✘ Chưa thêm"); }

    // ==================== Flag Editor ====================
    public static String flagEditorTitle(String id) { return get("flag-editor-title", "<blue><bold>Item Flags</bold> <dark_gray>- <white>" + id, "<blue><bold>Cờ Item</bold> <dark_gray>- <white>" + id).replace("{id}", id); }
    public static String flagEditorOn() { return get("flag-editor-on", "<green>✔ ON", "<green>✔ BẬT"); }
    public static String flagEditorOff() { return get("flag-editor-off", "<red>✘ OFF", "<red>✘ TẮT"); }
    public static String flagEditorToggle() { return get("flag-editor-toggle", "<yellow>Click to toggle", "<yellow>Nhấn để chuyển đổi"); }


    // ==================== Recipe Editor ====================
    public static String recipeEditorTitle(String id) { return get("recipe-editor-title", "<gold>Recipe: <white>" + id, "<gold>Công Thức: <white>" + id).replace("{id}", id); }
    public static String recipeEditorGuide() { return get("recipe-editor-guide", "<gold><bold>Guide</bold>", "<gold><bold>Hướng Dẫn</bold>"); }
    public static String recipeEditorGuide4() { return get("recipe-editor-guide4", "<aqua>Shapeless: <gray>save without shape pattern", "<aqua>Shapeless: <gray>lưu không cần mẫu hình dạng"); }
    public static String recipeEditorSaveShaped() { return get("recipe-editor-save-shaped", "<green><bold>Save Shaped</bold>", "<green><bold>Lưu Shaped</bold>"); }
    public static String recipeEditorSaveShapedDesc() { return get("recipe-editor-save-shaped-desc", "<gray>Save shaped recipe (3x3)", "<gray>Lưu công thức shaped (3x3)"); }
    public static String recipeEditorSaveShapeless() { return get("recipe-editor-save-shapeless", "<aqua><bold>Save Shapeless</bold>", "<aqua><bold>Lưu Shapeless</bold>"); }
    public static String recipeEditorSaveShapelessDesc() { return get("recipe-editor-save-shapeless-desc", "<gray>Save shapeless recipe", "<gray>Lưu công thức shapeless"); }
    public static String recipeEditorDelete() { return get("recipe-editor-delete", "<red><bold>Delete Recipe</bold>", "<red><bold>Xóa Công Thức</bold>"); }
    public static String recipeEditorDeleteDesc() { return get("recipe-editor-delete-desc", "<gray>Delete current recipe", "<gray>Xóa công thức hiện tại"); }
    public static String recipeEditorHasRecipe() { return get("recipe-editor-has-recipe", "<green><bold>Has recipe</bold>", "<green><bold>Có công thức</bold>"); }
    public static String recipeEditorTypeShaped() { return get("recipe-editor-type-shaped", "Shaped", "Shaped"); }
    public static String recipeEditorTypeShapeless() { return get("recipe-editor-type-shapeless", "Shapeless", "Shapeless"); }
    public static String recipeEditorAmount(int amount) { return get("recipe-editor-amount", "Amount: " + amount, "Số lượng: " + amount).replace("{amount}", String.valueOf(amount)); }
    public static String recipeEditorEmptySlots() { return get("recipe-editor-empty-slots", "Please place ingredients in the crafting slots!", "Hãy đặt nguyên liệu vào ô chế tạo!"); }
    public static String recipeEditorSaved(boolean shaped, String id) { String type = shaped ? "shaped" : "shapeless"; return get("recipe-editor-saved", "Saved recipe " + type + " for '" + id + "'!", "Đã lưu công thức " + type + " cho '" + id + "'!").replace("{type}", type).replace("{id}", id); }
    public static String recipeEditorDeleted(String id) { return get("recipe-editor-deleted", "Deleted recipe for '" + id + "'!", "Đã xóa công thức cho '" + id + "'!").replace("{id}", id); }


    // ==================== Recipe List ====================
    public static String recipeListTitle() { return get("recipe-list-title", "<gradient:#ff6b6b:#ffd93d>Recipe List</gradient>", "<gradient:#ff6b6b:#ffd93d>Danh Sách Công Thức</gradient>"); }
    public static String recipeListHeader() { return get("recipe-list-header", "<gradient:#ff6b6b:#ffd93d><bold>Recipe List</bold></gradient>", "<gradient:#ff6b6b:#ffd93d><bold>Danh Sách Công Thức</bold></gradient>"); }
    public static String recipeListClick() { return get("recipe-list-click", "<gray>Click a recipe to view details", "<gray>Nhấn vào công thức để xem chi tiết"); }
    public static String recipeListCraft() { return get("recipe-list-craft", "<green><bold>Craft</bold>", "<green><bold>Chế Tạo</bold>"); }
    public static String recipeListCraftDesc() { return get("recipe-list-craft-desc", "<gray>Open crafting table", "<gray>Mở bàn chế tạo"); }
    public static String recipeListDeletedItem() { return get("recipe-list-deleted-item", "<gray>Item deleted", "<gray>Item đã bị xóa"); }

    // ==================== Recipe Detail ====================
    public static String recipeDetailTitle(String id) { return get("recipe-detail-title", "<gold>Recipe: <white>" + id, "<gold>Công Thức: <white>" + id).replace("{id}", id); }
    public static String recipeDetailNotFound() { return get("recipe-detail-not-found", "<red>Not found", "<red>Không tìm thấy"); }

    // ==================== Crafting GUI ====================
    public static String craftGuiTitle() {
        return get("craft-gui-title", "<white>Crafting Table", "<white>Bàn chế tạo");
    }
    public static String craftGuiHintPlace() {
        return get("craft-gui-hint-place", "<gray>Place ingredients on the left", "<gray>Đặt nguyên liệu bên trái");
    }
    public static String craftGuiHintTake() {
        return get("craft-gui-hint-take", "<gray>Click the result to take items", "<gray>Click kết quả để nhận đồ");
    }
    public static String craftAmount1() { return get("craft-amount-1", "<gray>Craft 1 item", "<gray>Chế tạo 1 item"); }
    public static String craftAmount8() { return get("craft-amount-8", "<gray>Craft 8 items", "<gray>Chế tạo 8 items"); }
    public static String craftAmount64() { return get("craft-amount-64", "<gray>Craft 64 items", "<gray>Chế tạo 64 items"); }
    public static String craftAmountMax() { return get("craft-amount-max", "<gray>Craft as many as possible", "<gray>Chế tạo nhiều nhất có thể"); }
    public static String craftSelected() { return get("craft-selected", "<green>✔ Selected", "<green>✔ Đã chọn"); }
    public static String craftClickToSelect() { return get("craft-click-to-select", "<yellow>Click to select", "<yellow>Nhấn để chọn"); }

    // ==================== Chat Input ====================
    public static String chatCancel() { return get("chat-cancel", "cancel", "hủy"); }
    public static String chatCancelHint() { return get("chat-cancel-hint", "Type 'cancel' to cancel", "Gõ 'cancel' để hủy"); }
    public static String invalidNumber() { return get("invalid-number", "Invalid number!", "Số không hợp lệ!"); }
    public static String materialSet(String material) { return get("material-set", "Material set to: " + material, "Đã đặt vật liệu thành: " + material).replace("{material}", material); }

    // ==================== Search ====================
    public static String searchNoPermission() { return get("search-no-permission", "You don't have permission to use search (uitem.search).", "Bạn không có quyền sử dụng tìm kiếm (uitem.search)."); }

    // ==================== Help ====================
    public static String helpTitle() { return get("help-title", "  <gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <gray>— Ultimate Item Plugin", "  <gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <gray>— Ultimate Item Plugin"); }
    public static String helpGive() { return get("help-give", "  <gold>/uitem give <id> [player] [amount] <dark_gray>— <gray>Give item", "  <gold>/uitem give <id> [người chơi] [số lượng] <dark_gray>— <gray>Đưa item"); }
    public static String helpEditor() { return get("help-editor", "  <gold>/uitem editor <dark_gray>— <gray>Open UItem Studio", "  <gold>/uitem editor <dark_gray>— <gray>Mở UItem Studio"); }
    public static String helpList() { return get("help-list", "  <gold>/uitem list <dark_gray>— <gray>List items", "  <gold>/uitem list <dark_gray>— <gray>Danh sách items"); }
    public static String helpRp() { return get("help-rp", "  <gold>/uitem rp <generate|export|apply|status> <dark_gray>— <gray>Manage resource pack", "  <gold>/uitem rp <generate|export|apply|status> <dark_gray>— <gray>Quản lý resource pack"); }
    public static String helpCheck() { return get("help-check", "  <gold>/uitem check <dark_gray>— <gray>Check if held item is a valid UItem", "  <gold>/uitem check <dark_gray>— <gray>Kiểm tra item có phải UItem hợp lệ"); }
    public static String helpCraft() { return get("help-craft", "  <gold>/uitem craft <dark_gray>— <gray>Open UItem crafting table", "  <gold>/uitem craft <dark_gray>— <gray>Mở bàn chế tạo UItem"); }
    public static String helpCraftingCmd() { return get("help-crafting-cmd", "  <gold>/craft <dark_gray>— <gray>Open UItem crafting table", "  <gold>/craft <dark_gray>— <gray>Mở bàn chế tạo UItem"); }
    public static String helpReload() { return get("help-reload", "  <gold>/uitem reload <dark_gray>— <gray>Reload plugin", "  <gold>/uitem reload <dark_gray>— <gray>Tải lại plugin"); }
    public static String helpHelp() { return get("help-help", "  <gold>/uitem help <dark_gray>— <gray>Show help", "  <gold>/uitem help <dark_gray>— <gray>Hiện trợ giúp"); }
    public static String helpUi() { return get("help-ui", "  <gold>/ui <dark_gray>— <gray>Short alias for /uitem", "  <gold>/ui <dark_gray>— <gray>Viết tắt của /uitem"); }

    // ==================== Additional feedback (for completeness) ====================
    public static String cmdSet(String value) { return get("cmd-set", "Custom Model Data = " + value, "Custom Model Data = " + value).replace("{value}", value); }
    public static String potionGiveSent(String player, int amount, String id) { return get("potion-give-sent", "Gave " + player + " x" + amount + " potion " + id, "Đã đưa cho " + player + " x" + amount + " potion " + id); }
    public static String potionGiveReceived(int amount, String id) { return get("potion-give-recv", "You received x" + amount + " potion " + id, "Bạn nhận được x" + amount + " potion " + id); }
}


