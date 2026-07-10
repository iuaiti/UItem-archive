# ⚡ UItem — Hướng dẫn sử dụng

> **Plugin:** UItem v1.0.0  
> **Tác giả:** iuaititi  
> **API:** Paper 1.21.4+  
> **Folia:** ✔ Hỗ trợ

---

## 📦 Cài đặt

1. Tải `UItem.jar` cho vào thư mục `plugins/`
2. Khởi động lại server
3. File cấu hình được tạo tự động tại `plugins/UItem/`

**Phụ thuộc tùy chọn** (nếu có sẽ tự động hook):
- [Oraxen](https://www.spigotmc.org/resources/72448/)
- [ItemsAdder](https://www.spigotmc.org/resources/73355/)

---

## 🎮 Lệnh

### `/uitem` (bí danh: `/ui`)

| Lệnh | Mô tả | Quyền |
|------|-------|-------|
| `/uitem give <id> [player] [số lượng]` | Give item cho player | `uitem.give` |
| `/uitem editor` | Mở GUI editor | `uitem.editor` |
| `/uitem list` | Danh sách item trong chat | `uitem.editor` |
| `/uitem reload` | Reload plugin | `uitem.reload` |
| `/uitem craft` | Mở bàn chế tạo | * |
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
| `uitem.reload` | op | Reload plugin |
| `uitem.search` | op | Tìm kiếm item trong GUI |
| `uitem.craft` | op | Mở bàn chế tạo |

---

## 🛠 Tính năng

### Item Properties

Mỗi custom item có thể có:
- **Source** — item gốc (`vanilla:diamond_sword`, `ia:namespace:item`, `oraxen:item`)
- **Display Name** — tên hiển thị (hỗ trợ MiniMessage)
- **Lore** — mô tả (MiniMessage)
- **Enchantments** — 38 loại enchant
- **Item Flags** — 8 loại flag
- **Unbreakable** — vĩnh viễn
- **Glowing** — hiệu ứng phát sáng
- **Max Durability** — độ bền tối đa
- **Attributes** — chỉ số (damage, armor, speed...)
- **Rarity** — độ hiếm (COMMON → ULTRA)
- **skills** — hệ thống **Skill & Passive native** (giống ExecutableItems nhưng dễ và ổn định hơn)
- **Recipe** — công thức chế tạo (shaped/shapeless)

### Hệ thống Recipe

- Shaped (có hình dạng) & Shapeless (không hình dạng)
- Nguyên liệu có thể là item vanilla hoặc UItem ID
- Chế tạo 1 / 8 / 64 / MAX
- Tự động tiêu hao nguyên liệu

### Hỗ trợ Folia

Plugin chạy ổn định trên cả Paper, Spigot và Folia nhờ `SchedulerUtil`.

---

## ⚡ Hệ thống Skill & Passive Native (Mới)

UItem có hệ thống **skill/passive tích hợp sẵn**, mạnh mẽ, dễ dùng và ổn định hơn ExecutableItems rất nhiều:

- Không cần plugin nặng bên ngoài.
- Cấu hình qua GUI trực quan + YAML sạch.
- Cooldown lưu PersistentDataContainer (giữ sau restart/logout).
- Hỗ trợ Folia tốt.
- Dễ mở rộng.

### Trigger phổ biến

| Trigger              | Loại     | Mô tả                              |
|----------------------|----------|------------------------------------|
| RIGHT_CLICK          | Active   | Click phải                         |
| SHIFT_RIGHT_CLICK    | Active   | Shift + click phải                 |
| HIT                  | Passive  | Đánh trúng mục tiêu                |
| TAKE_DAMAGE          | Passive  | Bị đánh                            |
| EQUIP / WHILE_HOLDING| Passive  | Khi mặc/cầm (có PERIODIC)          |
| PERIODIC             | Passive  | Chạy lặp lại theo period-ticks     |

### Ví dụ config skill (trong items/*.yml)

```yaml
items:
  ruby_sword:
    material: DIAMOND_SWORD
    skills:
      fireball:
        trigger: RIGHT_CLICK
        cooldown: 6.0
        cancel-event: true
        display-name: "<red>Fireball"
        actions:
          a0:
            type: PLAY_SOUND
            sound: ENTITY_BLAZE_SHOOT
            volume: 1.0
            pitch: 1.0
          a1:
            type: SHOOT_PROJECTILE
            projectile: SMALL_FIREBALL
            speed: 1.6
            damage: 7.0
      lifesteal:
        trigger: HIT
        cooldown: 0
        chance: 40
        actions:
          a0:
            type: HEAL
            amount: 2.5
```

**Mở editor:** `/uitem editor` → chọn item → nhấn nút **Skills & Passives**.

Các action hỗ trợ sẵn: MESSAGE, ACTION_BAR, PLAY_SOUND, GIVE_POTION, HEAL/DAMAGE, LAUNCH, SHOOT_PROJECTILE, CONSOLE_COMMAND, PLAYER_COMMAND, STRIKE_LIGHTNING, CONSUME_DURABILITY...

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
    source: "vanilla:diamond_sword"
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
- Source, Name, Lore, Enchant, Flags
- Unbreakable, Glowing, Durability
- Attributes, Rarity
- Crafting Recipe

---

## 🔧 Yêu cầu

- **Server:** Paper 1.21.4+ (hoặc Spigot, Folia)
- **Java:** 21
- **Plugin phụ thuộc (optional):** Oraxen, ItemsAdder
