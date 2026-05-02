# UItem Folia Fixlog

## Đã sửa (round 1 - bug fixes)
- Thêm `folia-supported: true` vào `plugin.yml`.
- Thay `BukkitRunnable.runTask(...)` trong `ChatInputManager` bằng entity scheduler.
- Thêm `SchedulerUtil` để gom các scheduler API hiện đại của Paper/Folia.
- Đổi hệ thống save sang snapshot + async save coalescing.
- Dùng `saveAllNow(...)` cho các thao tác quan trọng.
- Deep clone `AttributeData` để tránh race khi snapshot/save async.
- Chống NPE cho item/material/id null trong `UItemManager`.
- Sắp xếp danh sách item theo ID và clamp page.
- Fix mẫu `magic_apple.display-name` bị thiếu thẻ đóng MiniMessage.
- Thêm log trạng thái scheduler khi plugin bật.
- Ghép ResourcePackManager vào project.
- Fix đúng đường dẫn resource pack.
- pack.mcmeta pack_format được lấy từ config (mặc định 34).
- Thêm lệnh /uitem rp.

## Đã sửa (round 2 - bug fixes tiếp theo)
- Bug A: config.yml — `auto-detect` thụt lề sai (nằm ở root thay vì trong `integration:`).
- Bug B: UItemCommand — give command thất bại âm thầm khi amount không hợp lệ.
- Bug C: ItemEditorGui — enchantmentSummary()/flagsSummary() nhúng `\n` vào 1 lore entry duy nhất.
- Bug D: UItemPlugin.onEnable() — exportZip() block main thread, chuyển sang async.
- Bug E: ResourcePackManager — config không được reload sau /uitem reload, thêm reloadFromConfig().

## Thêm mới (round 3 - Public API + Custom Events)

### Public API — com.uitem.api.UItemAPI
- Static facade duy nhất cho external developer.
- Khởi tạo qua UItemPlugin.onEnable(), hủy qua onDisable().
- Methods: getItem, getAllItems, hasItem, buildItem, isUItem, getUItemId,
  getUItemData, registerItem (fires event), removeItem (fires event),
  giveItem (fires event), getUItemKey, getPlugin.

### Custom Events — com.uitem.api.events.*
- UItemEvent           — base class (có getItemData())
- UItemCancellableEvent — base class cho event có thể cancel
- UItemCreateEvent     — tạo item mới, cancellable, có getCreator()
- UItemDeleteEvent     — xóa item, cancellable, có getDeleter()
- UItemGiveEvent       — give item, cancellable + modifiable (setAmount, setItemStack)
- UItemInteractEvent   — player click với UItem, cancellable
- UItemPickupEvent     — player nhặt UItem, cancellable
- UItemEquipEvent      — player mặc/tháo UItem, cancellable, isEquipping()

### UItemActionListener — com.uitem.listener.*
- Bắt PlayerInteractEvent → fire UItemInteractEvent
- Bắt EntityPickupItemEvent → fire UItemPickupEvent
- Bắt PlayerArmorChangeEvent (Paper 1.21) → fire UItemEquipEvent

### Tích hợp event vào flow hiện tại
- /uitem give → UItemGiveEvent (có thể cancel hoặc modify amount)
- GUI tạo item mới → UItemCreateEvent (có thể cancel)
- GUI xóa item → UItemDeleteEvent (có thể cancel, báo lỗi nếu bị chặn)
- API removeItem → UItemDeleteEvent
- API registerItem (item mới) → UItemCreateEvent

## Lưu ý
- Source đã được chỉnh theo API scheduler hiện đại của Paper/Folia 1.21.
- Bạn chỉ cần mở bằng IDE hoặc thêm Gradle Wrapper rồi build lại.
