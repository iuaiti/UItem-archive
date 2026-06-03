# UItem — Hướng dẫn sử dụng (GUI-first)

> **Phiên bản:** 1.0.0 · **Tác giả:** iuaititi · **Paper** 1.21.4 – 1.26.2 · **Folia** ✔ · **Java** 21  
> **Giấy phép:** [All Rights Reserved](../LICENSE) — xem [GIAY-PHEP.md](GIAY-PHEP.md)

Tài liệu này hướng dẫn **cách dùng trong game**, ưu tiên **GUI**, không bắt buộc sửa file config tay.

---

## Mục lục

1. [Cài đặt](#1-cài-đặt)
2. [Vào menu chính](#2-vào-menu-chính)
3. [Tạo Custom Item](#3-tạo-custom-item)
4. [Resource Pack](#4-resource-pack)
5. [Recipe & Crafting](#5-recipe--crafting)
6. [Kỹ năng / Trigger (item & potion)](#6-kỹ-năng--trigger-item--potion)
7. [Potion Studio](#7-potion-studio)
8. [Quyền (Permissions)](#8-quyền-permissions)
9. [Lệnh nhanh](#9-lệnh-nhanh)
10. [Cấu trúc thư mục](#10-cấu-trúc-thư-mục)
11. [Xử lý lỗi thường gặp](#11-xử-lý-lỗi-thường-gặp)

---

## 1. Cài đặt

1. Build hoặc lấy file `UItem.jar` (chỉ dùng `plugin.yml`, **không** đặt `paper-plugin.yml` trong JAR).
2. Copy vào `plugins/`.
3. Khởi động server (Paper **1.21.4 – 1.26.2** hoặc **Folia**, Java 21).
4. Thư mục tự tạo: `plugins/UItem/`.

**Tùy chọn (tự nhận nếu có):** Oraxen, Nexo, ItemsAdder, ExecutableItems.

---

## 2. Vào menu chính

| Cách | Lệnh / Thao tác |
|------|------------------|
| Menu Studio | `/uitem editor` hoặc `/ui editor` |
| Potion Studio | `/uitem potion` (hub → nút Brewing Stand) |
| Craft | `/craft` hoặc `/uitem craft` (cần quyền `craft`) |

Hub hiển thị: Item Library, Recipes, Resource Pack, Crafting, Integrations, Potion Studio, tạo item mới.

---

## 3. Tạo Custom Item

### Bước 1 — Tạo ID

Hub → **+ New Item** → nhập ID (chữ thường, `a-z`, `0-9`, `_`).

### Bước 2 — Chỉnh thuộc tính (Item Editor)

| Nút | Chức năng |
|-----|-----------|
| Material, Name, Lore | Hiển thị cơ bản |
| Enchant / Flags / CMD | Enchant, ẩn tooltip, texture |
| Attributes, Rarity | Chỉ số, độ hiếm |
| Oraxen / Nexo / IA / EI ID | Lấy model từ plugin ngoài |
| Recipe | Công thức chế tạo (có thể dùng **UItem potion** làm nguyên liệu) |
| **Ky nang / Trigger** | Kỹ năng khi đánh, click, mặc đồ, … |

Lưu → file `plugins/UItem/items/<id>.yml`.

---

## 4. Resource Pack

- Hub → **Resource Pack** hoặc `/uitem rp gui`
- **Build** — generate + export zip
- **Send all** — gửi pack cho người online
- Texture: `plugins/UItem/textures/` · Import zip: `packs/import/`

Cấu hình: `config.yml` → `resourcepack.*` (`pack-format: -1` = tự khớp phiên bản server).

---

## 5. Recipe & Crafting

1. **Item Editor** → mục recipe (hoặc Recipe list từ hub).
2. Đặt nguyên liệu trên lưới 3×3; chọn **UItem**, **potion**, hoặc material vanilla.
3. Player mở `/craft` để chế tạo (GUI tiêu đề vanilla, không lộ ID item).

File lưu: `plugins/UItem/recipes/<id>.yml`

---

## 6. Kỹ năng / Trigger (item & potion)

Gắn trực tiếp vào **từng item** hoặc **potion** (không còn Passive Studio riêng).

### Mở editor

- **Item:** Item Editor → **Ky nang / Trigger**
- **Potion:** Potion Editor → mục kỹ năng tương tự

### Mỗi rule gồm

| Thành phần | Mô tả |
|------------|--------|
| **Trigger** | ON_HIT, ON_KILL, ON_DAMAGE_TAKEN, ON_RIGHT/LEFT_CLICK, ON_EQUIP, ON_DRINK (potion) |
| **Vị trí** | Tay chính, tay phụ, giáp, … |
| **Cooldown / Chance %** | Giới hạn tần suất |
| **Hành động** | DAMAGE, HEAL, POTION, LIGHTNING, THORNS, KNOCKBACK, MESSAGE, COMMAND |

### YAML (trong file item/potion)

```yaml
abilities:
  enabled: true
  rules:
    - trigger: ON_HIT
      slot: EITHER_HAND
      cooldown: 2
      chance: 100
      actions:
        a0:
          type: DAMAGE
          params:
            amount: "5"
```

Sau khi sửa tay: `/uitem reload`.

**ExecutableItems:** nếu item có `executableitems-id`, kỹ năng phức tạp nên để EI xử lý.

---

## 7. Potion Studio

| Lệnh | Mô tả |
|------|--------|
| `/uitem potion` | Hub potion |

- Tạo / sửa potion uống, splash, lingering
- Hiệu ứng, màu, lore, kỹ năng ON_DRINK
- Dùng làm **nguyên liệu recipe** cho item khác

File: `plugins/UItem/potions/<id>.yml`

---

## 8. Quyền (Permissions)

| Quyền | Mô tả |
|--------|--------|
| `uitem.admin` | Toàn quyền (OP mặc định) |
| `uitem.editor` | Mở editor item |
| `uitem.give` | Give item / potion |
| `uitem.reload` | Reload + RP |
| `uitem.search` | Tìm trong Item Library |
| `uitem.potion` | Potion Studio |
| `uitem.potion.create` / `.edit` / `.delete` | CRUD potion |
| `craft` / `uitem.craft` | Bàn craft |

LuckPerms ví dụ:

```
/lp user <player> permission set uitem.editor true
/lp user <player> permission set craft true
```

---

## 9. Lệnh nhanh

```
/uitem help
/uitem list
/uitem reload
/uitem potion
/uitem rp build
/craft
```

---

## 10. Cấu trúc thư mục

```
plugins/UItem/
├── config.yml
├── items/           # Mỗi item một file .yml (abilities: trong file item)
├── potions/         # Custom potions
├── recipes/
├── textures/        # PNG cho resource pack
├── packs/import/    # Zip pack ngoài
└── resourcepack/    # Pack đã generate
```

---

## 11. Xử lý lỗi thường gặp

| Triệu chứng | Gợi ý |
|-------------|--------|
| Plugin không enable, lỗi `getCommand` | Xóa `paper-plugin.yml` khỏi JAR; chỉ dùng `plugin.yml` |
| Không mở được GUI | Quyền `uitem.editor` / `uitem.potion` |
| Kỹ năng không chạy | `/uitem reload`; bật `abilities.enabled: true`; kiểm tra cooldown/chance |
| Resource pack không tải | `/uitem rp status`; set `resourcepack.url` hoặc bật auto-host |
| Build lỗi Java | Server cần **Java 21** |
| Chat input bị kẹt | Gõ `cancel` để hủy nhập |

---

© 2026 iuaititi · [LICENSE](../LICENSE)