# UItem — Hướng dẫn sử dụng (GUI-first)

> **Phiên bản:** 1.0.0 · **Tác giả:** iuaititi · **Paper** 1.21.4+ · **Java** 21  
> **Giấy phép:** [All Rights Reserved](../LICENSE) — xem [GIAY-PHEP.md](GIAY-PHEP.md)

Tài liệu này hướng dẫn **cách dùng trong game**, ưu tiên **GUI**, không bắt buộc sửa file config tay.

---

## Mục lục

1. [Cài đặt](#1-cài-đặt)
2. [Vào menu chính](#2-vào-menu-chính)
3. [Tạo Custom Item](#3-tạo-custom-item)
4. [Resource Pack](#4-resource-pack)
5. [Recipe & Crafting](#5-recipe--crafting)
6. [Passive Studio](#6-passive-studio-hành-động-tự-động)
7. [Quyền (Permissions)](#7-quyền-permissions)
8. [Lệnh nhanh](#8-lệnh-nhanh)
9. [Cấu trúc thư mục](#9-cấu-trúc-thư-mục)
10. [Xử lý lỗi thường gặp](#10-xử-lý-lỗi-thường-gặp)

---

## 1. Cài đặt

1. Build hoặc lấy file `UItem.jar`.
2. Copy vào `plugins/`.
3. Khởi động server (Paper **1.21.4 – 1.26.2** hoặc **Folia** cùng dải phiên bản, Java 21).
4. Thư mục tự tạo: `plugins/UItem/`.

**Tùy chọn (tự nhận nếu có):** Oraxen, ItemsAdder, ExecutableItems.

---

## 2. Vào menu chính

| Cách | Lệnh / Thao tác |
|------|------------------|
| Menu Studio | `/uitem editor` hoặc `/ui editor` |
| Passive | `/uitem passive` hoặc nút **Passive Studio** trong hub |
| Craft | `/craft` (cần quyền `craft`) |

Hub hiển thị: Item Library, Recipes, Resource Pack, Crafting, **Passive Studio**, tạo item mới.

---

## 3. Tạo Custom Item

### Bước 1 — Tạo ID

1. Mở hub → **+ New Item** (hoặc Item Library → tạo mới).
2. Chat nhập ID: chỉ `a-z`, `0-9`, `_` (ví dụ: `ruby_sword`).

### Bước 2 — Chỉnh trong Item Editor

| Nút GUI | Ý nghĩa |
|---------|---------|
| Material | Chọn block/item gốc |
| Name / Lore | MiniMessage (`<green>`, `<bold>`, gradient…) |
| Enchant / Flags | Phù phép, ẩn tooltip |
| CMD | Custom Model Data (texture pack) |
| Attributes | Sát thương, giáp, tốc độ… |
| Rarity | Độ hiếm hiển thị |

Lưu tự động vào `plugins/UItem/items/<id>.yml` (không cần sửa tay).

### Bước 3 — Give thử

```
/uitem give ruby_sword
/uitem give ruby_sword TenNguoiChoi 1
```

---

## 4. Resource Pack

### Cách nhanh (GUI)

1. Hub → **Resource Pack** (hoặc `/uitem rp gui`).
2. **BUILD PACK** — generate + merge + export zip.
3. Bật trong `config.yml`:
   - `resourcepack.auto-host: true` — host local
   - `resourcepack.auto-apply: true` — gửi pack khi join

### Texture

- Đặt PNG: `plugins/UItem/textures/<ten>.png`
- Pack ngoài: `plugins/UItem/packs/import/*.zip`

### Lệnh

| Lệnh | Việc làm |
|------|----------|
| `/uitem rp build` | Build đầy đủ |
| `/uitem rp export` | Xuất zip |
| `/uitem rp sendall` | Gửi cho online |
| `/uitem rp status` | Xem URL, hash, CMD |

---

## 5. Recipe & Crafting

1. **Item Editor** → mục recipe (hoặc Recipe list từ hub).
2. Đặt nguyên liệu trên lưới 3×3, chọn kết quả.
3. Player mở `/craft` để craft (cần quyền).

File lưu: `plugins/UItem/recipes/<id>.yml`

---

## 6. Passive Studio (hành động tự động)

Passive = **Khi nào** + **Điều kiện** + **Làm gì**.  
Toàn bộ tạo bằng **wizard 5 bước**, không cần học YAML.

### Mở Passive Studio

- `/uitem passive`
- Hub → **Passive Studio** (BEACON)

### Wizard 5 bước

```
① Khi nào  →  ② Điều kiện  →  ③ Hành động  →  ④ Xem trước  →  ⑤ Lưu
```

#### Bước 1 — Khi nào kích hoạt?

Chọn một trigger, ví dụ:

| Trigger | Ý nghĩa |
|---------|---------|
| Khi đập block | Đập block |
| Khi đặt block | Đặt block |
| Khi giết entity | Kill mob/player |
| Khi vào server | Join |
| Khi chat / lệnh | Chat hoặc command |
| Khi câu / craft / enchant | … |

#### Bước 2 — Điều kiện (tùy chọn)

- Nút **+** → chọn loại (World, Block, Chance %, Permission, Máu, …).
- Click từng dòng để **sửa giá trị** (chat hoặc nút %).
- **Shift + click** → xóa điều kiện.
- **Nâng cao** → ALL / ANY / NOT, cooldown toàn passive, delay.

Không thêm điều kiện = **luôn chạy** khi trigger kích hoạt.

#### Bước 3 — Hành động

- Nút **+** → Give item, Heal, Message, Command, Sound, Title, …
- Thứ tự trên GUI = thứ tự chạy.

#### Bước 4 — Xem trước

Đọc tóm tắt dạng:

```
Khi: Đập block
Nếu (tất cả): Block = STONE, Tỉ lệ = 5%
Thì: Cho DIAMOND x1
```

#### Bước 5 — Lưu

- Nhập **ID** (vd: `stone_diamond`).
- Tùy chọn **tên hiển thị**.
- File tự lưu: `plugins/UItem/passives/<id>.yml`

### Ví dụ có sẵn

`stone_diamond` — đập **STONE**, **5%** nhận **DIAMOND** (tự copy lần đầu chạy).

### Quản lý danh sách

**Danh sách Passive**:

| Thao tác | Cách |
|----------|------|
| Sửa | Click trái |
| Xóa | Click phải → xác nhận |
| Nhân bản | Shift + click trái → nhập ID mới |

### Nâng cao (ẩn trong menu Nâng cao)

| Tính năng | Mô tả |
|-----------|--------|
| ALL / ANY / NOT | Cách gộp nhiều điều kiện |
| Cooldown (giây) | Chờ giữa mỗi lần chạy passive |
| Chance % toàn passive | Tỉ lệ trước khi check điều kiện |
| Delay (tick) | Trễ trước khi chạy chuỗi action |

### YAML (chỉ để đọc / backup)

GUI tự ghi, ví dụ:

```yaml
when:
  trigger: block_break
if:
  mode: ALL
  list:
    - type: block_type
      block: STONE
    - type: chance
      percent: 5
then:
  - type: give_item
    material: DIAMOND
    amount: 1
```

**Không bắt buộc** sửa file tay; sau khi sửa YAML, chạy `/uitem reload`.

---

## 7. Quyền (Permissions)

| Quyền | Mô tả |
|--------|--------|
| `uitem.admin` | Toàn quyền (OP mặc định) |
| `uitem.editor` | Mở editor item |
| `uitem.give` | Give item |
| `uitem.reload` | Reload + RP |
| `uitem.passive` | Passive Studio |
| `uitem.passive.create` | Tạo passive |
| `uitem.passive.edit` | Sửa passive |
| `uitem.passive.delete` | Xóa passive |
| `craft` / `uitem.craft` | Bàn craft |

LuckPerms ví dụ:

```
/lp user <player> permission set uitem.passive true
/lp user <player> permission set craft true
```

---

## 8. Lệnh nhanh

```
/uitem help
/uitem list
/uitem reload
/uitem passive
/uitem rp build
/craft
```

---

## 9. Cấu trúc thư mục

```
plugins/UItem/
├── config.yml
├── items/           # Mỗi item một file .yml
├── recipes/
├── passives/        # Passive Studio
├── textures/        # PNG cho resource pack
├── packs/import/    # Zip pack ngoài
└── resourcepack/    # Pack đã generate
```

---

## 10. Xử lý lỗi thường gặp

| Triệu chứng | Gợi ý |
|-------------|--------|
| Không mở được GUI | Kiểm tra quyền `uitem.editor` / `uitem.passive` |
| Passive không chạy | `/uitem reload`; xem `enabled: true`; tỉ lệ % / cooldown |
| Resource pack không tải | `/uitem rp status`; set `resourcepack.url` hoặc bật auto-host |
| Build lỗi Java | Server cần **Java 21** |
| Chat input bị kẹt | Gõ `cancel` để hủy nhập |

---

## Liên hệ & bản quyền

- **All Rights Reserved** — không redistribute file `.jar`.
- Báo leak: **huy594481@gmail.com**

*Bản tiếng Anh tóm tắt: [README.md](../README.md)*