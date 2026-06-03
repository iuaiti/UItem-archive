# ⚡ UItem — Hướng dẫn sử dụng

> **Plugin:** UItem v1.0.0  
> **Tác giả:** iuaititi  
> **API:** Paper **1.21.4 – 1.26.2** (Java 21)
> **Folia:** ✔ Hỗ trợ đầy đủ (entity / region / global scheduler qua `SchedulerUtil`)
> **Giấy phép:** **All Rights Reserved** — [LICENSE](LICENSE) · [Tóm tắt](docs/GIAY-PHEP.md)

📖 **Hướng dẫn đầy đủ (khuyên dùng):** [docs/HUONG-DAN.md](docs/HUONG-DAN.md)

---

## 📦 Cài đặt

1. Tải `UItem.jar` cho vào thư mục `plugins/`
2. Khởi động lại server
3. File cấu hình được tạo tự động tại `plugins/UItem/`

**Phụ thuộc tùy chọn** (nếu có sẽ tự động hook):
- [Oraxen](https://www.spigotmc.org/resources/72448/)
- [ItemsAdder](https://www.spigotmc.org/resources/73355/)
- [ExecutableItems](https://www.spigotmc.org/resources/77578/)

---

## 🎮 Lệnh

### `/uitem` (bí danh: `/ui`)

| Lệnh | Mô tả | Quyền |
|------|-------|-------|
| `/uitem give <id> [player] [số lượng]` | Give item cho player | `uitem.give` |
| `/uitem editor` | Mở GUI editor | `uitem.editor` |
| `/uitem list` | Danh sách item trong chat | `uitem.editor` |
| `/uitem reload` | Reload plugin | `uitem.reload` |
| `/uitem rp generate` | Tạo resource pack | `uitem.reload` |
| `/uitem rp export` | Export resource pack zip | `uitem.reload` |
| `/uitem rp apply` | Apply pack cho player | `uitem.reload` |
| `/uitem rp status` | Xem trạng thái RP | `uitem.reload` |
| `/uitem craft` | Mở bàn chế tạo | `craft` / `uitem.craft` |
| `/uitem potion` | Potion Studio | `uitem.potion` |
| `/uitem check` | Kiểm tra item đang cầm | * |
| `/uitem help` | Xem hướng dẫn | * |

### `/crafting` (bí danh: `/craft`)

| Lệnh | Mô tả | Quyền |
|------|-------|-------|
| `/crafting` | Mở danh sách recipe | `uitem.craft` |
| `/crafting craft` | Mở bàn chế tạo | `uitem.craft` |

---

## 🔐 Quyền (Permissions)

| Node | Mặc định | Mô tả |
|------|----------|-------|
| `uitem.admin` | op | Toàn quyền (bao gồm các quyền con) |
| `uitem.give` | op | Give item |
| `uitem.editor` | op | Mở GUI editor |
| `uitem.reload` | op | Reload + quản lý resource pack |
| `uitem.search` | op | Tìm kiếm item trong GUI |
| `uitem.craft` | op | Mở bàn chế tạo |
| `uitem.potion` | op | Potion Studio |
| `uitem.potion.create` | op | Tạo potion |
| `uitem.potion.edit` | op | Sửa potion |
| `uitem.potion.delete` | op | Xóa potion |

---

## 🛠 Tính năng

### Item Properties

Mỗi custom item có thể có:
- **Material** — chất liệu gốc (VD: DIAMOND_SWORD)
- **Display Name** — tên hiển thị (hỗ trợ MiniMessage)
- **Lore** — mô tả (MiniMessage)
- **Enchantments** — 38 loại enchant
- **Item Flags** — 8 loại flag
- **Custom Model Data** — cho texture pack
- **Unbreakable** — vĩnh viễn
- **Glowing** — hiệu ứng phát sáng
- **Max Durability** — độ bền tối đa
- **Attributes** — chỉ số (damage, armor, speed...)
- **Rarity** — độ hiếm (COMMON → ULTRA)
- **oraxen-id / nexo-id** — Oraxen, Nexo
- **itemsadder-id** — ItemsAdder
- **executableitems-id** — ExecutableItems (kỹ năng nâng cao do EI)
- **abilities** — kỹ năng trigger tích hợp sẵn
- **Recipe** — công thức chế tạo (shaped/shapeless; có thể dùng potion làm NL)

### Resource Pack tự động

- Tự động sinh model JSON + texture
- Gán CustomModelData tự động
- Host HTTP server tích hợp sẵn
- Export ZIP + SHA-1 hash
- Auto-apply cho player khi join

### Hệ thống Recipe

- Shaped (có hình dạng) & Shapeless (không hình dạng)
- Nguyên liệu có thể là item vanilla hoặc UItem ID
- Chế tạo 1 / 8 / 64 / MAX
- Tự động tiêu hao nguyên liệu

### Hỗ trợ Folia

Plugin chạy ổn định trên **Paper, Spigot và Folia**. Trên Folia: GUI, give item, resource pack, kỹ năng item/potion đều chạy trên **entity scheduler** của từng người chơi; lệnh console và I/O dùng global/async scheduler.

---

## ⚡ ExecutableItems Integration

Khi có `executableitems-id` trong item:

```yaml
items:
  ruby_sword:
    material: DIAMOND_SWORD
    display-name: "<red>Ruby Sword"
    executableitems-id: "Excalibur"
```

- Khi build item, UItem sẽ gọi EI API để lấy ItemStack gốc
- EI tự xử lý toàn bộ activators / skills / passives
- UItem chỉ quản lý item, không can thiệp vào skill

### API cho developer

```java
UItemAPI api = UItemAPI.get();

// Check EI có available không
boolean hasEI = api.hasExecutableItems();

// Lấy EI ID từ UItem ID
String eiId = api.getExecutableItemsIdByUItemId("ruby_sword");

// Build ItemStack từ EI
Optional<ItemStack> item = api.buildExecutableItemsItem("ruby_sword");

// Lấy EI ID từ ItemStack đang cầm
String id = api.getExecutableItemsId(player.getItemInHand());
```

---

## 📝 Public API (cho developer)

### Lấy API instance

```java
if (UItemAPI.isAvailable()) {
    UItemAPI api = UItemAPI.get();
}
```

### Item Lookup

```java
Optional<UItemData> data = api.getItem("ruby_sword");
Map<String, UItemData> all = api.getAllItems();
boolean exists = api.hasItem("ruby_sword");
```

### Build ItemStack

```java
Optional<ItemStack> item = api.buildItem("ruby_sword");
ItemStack item2 = api.buildItem(data);
```

### Identify Item

```java
boolean isUItem = api.isUItem(itemStack);
Optional<String> id = api.getUItemId(itemStack);
Optional<UItemData> data = api.getUItemData(itemStack);
```

### Management

```java
// Tạo item mới (fire UItemCreateEvent)
api.registerItem(new UItemData("my_item"), sender);

// Xóa item (fire UItemDeleteEvent)
api.removeItem("my_item", sender);

// Give item cho player (fire UItemGiveEvent)
api.giveItem(player, "ruby_sword", 1, sender);
```

### Events

```java
@EventHandler
public void onUItemCreate(UItemCreateEvent event) { }
@EventHandler
public void onUItemDelete(UItemDeleteEvent event) { }
@EventHandler
public void onUItemGive(UItemGiveEvent event) {
    event.setAmount(5); // modify amount
    event.setItemStack(newItem); // modify item
}
@EventHandler
public void onUItemInteract(UItemInteractEvent event) { }
@EventHandler
public void onUItemPickup(UItemPickupEvent event) { }
@EventHandler
public void onUItemEquip(UItemEquipEvent event) { }
```

---

## ⚙️ Cấu hình (config.yml)

### Resource Pack

```yaml
resourcepack:
  auto-generate: true       # Tự sinh RP khi start
  auto-apply: true          # Tự apply cho player
  auto-host: true           # Tự host HTTP server
  server-port: 9732         # Cổng HTTP server
  kick-on-fail: true        # Kick nếu không tải được RP
  url: ""                   # URL RP tùy chỉnh
  pack-format: -1          # -1 = tu dong theo phien ban server (1.21.4→34, 1.26.x→64)
  namespace: "uitem"        # Namespace
  output-zip: "UItem-ResourcePack.zip"
```

### Rarity

```yaml
rarity:
  COMMON:
    color: "<gray>"
    prefix: ""
    lore: []
  UNCOMMON:
    color: "<green>"
    prefix: "<green>"
    lore: ["<dark_gray>──────────────", "<green>Uncommon"]
  RARE:
    color: "<blue>"
    prefix: "<blue>"
    lore: ["<dark_gray>──────────────", "<blue>Rare"]
  EPIC:
    color: "<dark_purple>"
    prefix: "<dark_purple>"
    lore: ["<dark_gray>──────────────", "<dark_purple>Epic"]
  LEGENDARY:
    color: "<gold>"
    prefix: "<gold>"
    lore: ["<dark_gray>──────────────", "<gold>Legendary"]
  ULTRA:
    color: "<red>"
    prefix: "<red>"
    lore: ["<dark_gray>──────────────", "<red>✦ ULTRA ✦"]
```

---

## 📁 items.yml — Cấu trúc

```yaml
items:
  ruby_sword:
    material: DIAMOND_SWORD
    display-name: "<gradient:#ff0000:#ff6600><bold>Ruby Sword</bold></gradient>"
    lore:
      - "<gray>A legendary sword forged from Ruby"
      - ""
      - "<gold>✦ Legendary"
    enchantments:
      sharpness: 5
      unbreaking: 3
      fire_aspect: 2
    flags:
      - HIDE_ENCHANTS
      - HIDE_ATTRIBUTES
    custom-model-data: 10001
    unbreakable: true
    glowing: false
    max-durability: -1
    attributes:
      attr_0:
        attribute: GENERIC_ATTACK_DAMAGE
        amount: 15.0
        operation: ADD_NUMBER
        slot: HAND
    rarity: EPIC
    oraxen-id: ""
    itemsadder-id: ""
    executableitems-id: ""
```

---

## 🖥 GUI Editor

Dùng `/ui editor` để mở GUI quản lý item.

### ItemListGui
- Danh sách tất cả items (28 item / trang)
- Click trái → chỉnh sửa
- Click phải → xóa
- Nút Search → lọc theo tên
- Nút Create → tạo item mới

### ItemEditorGui
Chỉnh sửa tất cả thuộc tính của 1 item:
- Material, Name, Lore, Enchant, Flags
- CMD, Unbreakable, Glowing, Durability
- Attributes, Rarity
- Oraxen/ItemsAdder/ExecutableItems ID
- Crafting Recipe

---

## ⚡ Kỹ năng / Trigger (item & potion)

Gắn rule trực tiếp vào item hoặc potion qua GUI (**Ky nang / Trigger** trong Item Editor).

- Trigger: đánh, giết, bị đánh, click, mặc đồ, uống potion
- Hành động: damage, heal, potion, sét, phản damage, knockback, message, lệnh console
- Chi tiết: **[docs/HUONG-DAN.md](docs/HUONG-DAN.md)** (mục 6–7)

## 🧪 Potion Studio

- `/uitem potion` — tạo potion custom, hiệu ứng, kỹ năng ON_DRINK
- Dùng potion làm nguyên liệu recipe cho item khác

---

## 🔧 Yêu cầu

- **Server:** Paper **1.21.4 – 1.26.2** (Spigot/Folia qua `SchedulerUtil`; ky nang ON_EQUIP uu tien su kien Paper)
- **Java:** 21
- **Plugin phụ thuộc (optional):** Oraxen, ItemsAdder, ExecutableItems

---

## © Bản quyền — All Rights Reserved

© 2026 **iuaititi**. Mọi quyền được bảo lưu.

- Được cài trên server bạn quản trị.
- **Không** chia sẻ `.jar`, sửa code, decompile, hoặc bán lại plugin.
- Chi tiết: [LICENSE](LICENSE) · [docs/GIAY-PHEP.md](docs/GIAY-PHEP.md)
- Báo leak: huy594481@gmail.com
