# ⚡ UItem — Custom Item Engine

> **Version:** 1.0.0  
> **Author:** iuaititi  
> **API:** Paper **1.21.4 – 1.26.2** (Java 21)
> **Folia:** ✔ Fully supported (`SchedulerUtil` — entity / region / global schedulers)
> **License:** **All Rights Reserved** — [LICENSE](LICENSE) · [Terms (VI)](docs/GIAY-PHEP.md)

📖 **Documentation:** [Full guide (Vietnamese)](docs/HUONG-DAN.md) · [Docs index](docs/README.md)

---

## 📦 Installation

1. Place `UItem.jar` into your `plugins/` folder
2. Restart the server
3. Configuration files are auto-generated in `plugins/UItem/`

**Optional dependencies** (auto-hooked if present):
- [Oraxen](https://www.spigotmc.org/resources/72448/)
- [ItemsAdder](https://www.spigotmc.org/resources/73355/)
- [ExecutableItems](https://www.spigotmc.org/resources/77578/)

---

## 🎮 Commands

### `/uitem` (alias: `/ui`)

| Command | Description | Permission |
|---------|-------------|------------|
| `/uitem give <id> [player] [amount]` | Give item to player | `uitem.give` |
| `/uitem editor` | Open GUI editor | `uitem.editor` |
| `/uitem list` | List items in chat | `uitem.editor` |
| `/uitem reload` | Reload plugin | `uitem.reload` |
| `/uitem rp generate` | Generate resource pack | `uitem.reload` |
| `/uitem rp export` | Export resource pack zip | `uitem.reload` |
| `/uitem rp apply` | Apply pack to player | `uitem.reload` |
| `/uitem rp status` | View RP status | `uitem.reload` |
| `/uitem craft` | Open crafting table | * |
| `/uitem check` | Check held item | * |
| `/uitem passive` | Open Passive Studio (GUI) | `uitem.passive` |
| `/uitem help` | Show help | * |

### `/crafting` (alias: `/craft`)

| Command | Description | Permission |
|---------|-------------|------------|
| `/crafting` | Open recipe list | `uitem.craft` |
| `/crafting craft` | Open crafting table | `uitem.craft` |

---

## 🔐 Permissions

| Node | Default | Description |
|------|---------|-------------|
| `uitem.admin` | op | Full access (includes all children) |
| `uitem.give` | op | Give items |
| `uitem.editor` | op | Open GUI editor |
| `uitem.reload` | op | Reload + resource pack management |
| `uitem.search` | op | Search items in GUI |
| `uitem.craft` | op | Open crafting table |
| `uitem.passive` | op | Passive Studio |
| `uitem.passive.create` | op | Create passives |
| `uitem.passive.edit` | op | Edit passives |
| `uitem.passive.delete` | op | Delete passives |

---

## 🛠 Features

### Item Properties

Each custom item supports:
- **Material** — base vanilla material (e.g. DIAMOND_SWORD)
- **Display Name** — MiniMessage format
- **Lore** — MiniMessage format
- **Enchantments** — 38 types supported
- **Item Flags** — 8 flags available
- **Custom Model Data** — for texture packs
- **Unbreakable** — never breaks
- **Glowing** — enchantment glint effect
- **Max Durability** — custom durability cap
- **Attributes** — damage, armor, speed modifiers
- **Rarity** — COMMON → ULTRA (6 tiers)
- **oraxen-id** — Oraxen integration
- **itemsadder-id** — ItemsAdder integration
- **executableitems-id** — ExecutableItems integration
- **Recipe** — shaped / shapeless crafting

### Auto Resource Pack

- Auto-generates model JSONs + textures
- Auto-assigns CustomModelData
- Built-in HTTP server to host the pack
- ZIP export with SHA-1 hash
- Auto-apply to players on join

### Recipe System

- Shaped & Shapeless recipes
- Ingredients: vanilla materials OR UItem IDs
- Craft: 1 / 8 / 64 / MAX
- Auto-consumes ingredients

### Folia Support

Runs on Paper, Spigot, and **Folia**. On Folia, player GUIs, item give, resource packs, and abilities use each player's **entity scheduler**; console commands and file I/O use global/async schedulers.

---

## ⚡ ExecutableItems Integration

When an item has `executableitems-id`:

```yaml
items:
  ruby_sword:
    material: DIAMOND_SWORD
    display-name: "<red>Ruby Sword"
    executableitems-id: "Excalibur"
```

- UItem delegates item building to ExecutableItems API
- EI handles all activators / skills / passives automatically
- UItem manages only the item registry, not skills

### Developer API

```java
UItemAPI api = UItemAPI.get();

boolean hasEI = api.hasExecutableItems();

String eiId = api.getExecutableItemsIdByUItemId("ruby_sword");

Optional<ItemStack> item = api.buildExecutableItemsItem("ruby_sword");

String id = api.getExecutableItemsId(player.getItemInHand());
```

---

## 📝 Public API

### Getting the API

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
api.registerItem(new UItemData("my_item"), sender);
api.removeItem("my_item", sender);
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
    event.setAmount(5);
    event.setItemStack(newItem);
}
@EventHandler
public void onUItemInteract(UItemInteractEvent event) { }
@EventHandler
public void onUItemPickup(UItemPickupEvent event) { }
@EventHandler
public void onUItemEquip(UItemEquipEvent event) { }
```

---

## ⚙️ Config (config.yml)

### Resource Pack

```yaml
resourcepack:
  auto-generate: true
  auto-apply: true
  auto-host: true
  server-port: 9732
  kick-on-fail: true
  url: ""
  pack-format: 34
  namespace: "uitem"
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

## 📁 items.yml Structure

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

Use `/ui editor` to open the item management GUI.

### ItemListGui
- Lists all items (28 per page)
- Left-click → edit item
- Right-click → delete item
- Search button → filter by name
- Create button → new item

### ItemEditorGui
Edit all properties of a single item:
- Material, Name, Lore, Enchantments, Flags
- CMD, Unbreakable, Glowing, Durability
- Attributes, Rarity
- Oraxen / ItemsAdder / ExecutableItems ID
- Crafting Recipe

---

## 🔧 Requirements

- **Server:** Paper **1.21.4 – 1.26.2** (Spigot/Folia also work via `SchedulerUtil`; equip abilities use Paper events when available)
- **Java:** 21
- **Optional plugins:** Oraxen, ItemsAdder, ExecutableItems

---

## ⚡ Passive Studio

GUI-based automations (trigger → conditions → actions). No config editing required.

- Open: `/uitem passive`
- Guide: [docs/HUONG-DAN.md § Passive Studio](docs/HUONG-DAN.md#6-passive-studio-hành-động-tự-động)

---

## © License — All Rights Reserved

Copyright (c) 2026 **iuaititi**. All rights reserved.

- You may run UItem on servers you own or manage.
- You may **not** redistribute, modify, decompile, or resell the plugin.
- Full terms: [LICENSE](LICENSE) · Vietnamese summary: [docs/GIAY-PHEP.md](docs/GIAY-PHEP.md)
- Report violations: huy594481@gmail.com
