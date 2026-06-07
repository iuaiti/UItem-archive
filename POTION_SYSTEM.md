# Hệ Thống Potion Cho Item - UItem Plugin

## 📋 Tổng Quan

Plugin UItem giờ đây hỗ trợ **hệ thống potion effect tương đối hoàn chỉnh** cho các custom item. Các potion effect có thể được thêm vào item thông qua **skill system**:

- **GIVE_POTION**: Áp dụng potion effect khi skill trigger
- **REMOVE_POTION**: Xóa potion effect khi cần
- **Aura Potion**: Potion tự động khi holding/wearing item

---

## 🎮 Cách Sử Dụng Trong Game

### 1. **Thêm Potion Effect Vào Skill**

#### Cách 1: Qua GUI Potion Presets (Dễ Nhất)
1. Mở Item Editor → Click "Kỹ Năng & Passive"
2. Tạo hoặc chỉnh skill
3. Click button **"⚡ Potion Presets"** (slot 45)
4. Chọn potion effect từ danh sách
5. Nhập duration (giây) → amplifier (0-5) → thêm!

#### Cách 2: Potion Combo Presets
1. Tại Skill Editor → Click **"🍶 Potion Combos"** (slot 46)
2. Chọn preset combo (Warrior Buff, Mage Buff, v.v.)
3. Tất cả effect trong combo sẽ được thêm một lúc!

#### Cách 3: Manual - GIVE_POTION Action
1. Click button action **"GIVE_POTION"** (Material.POTION)
2. Nhập effect name (VD: STRENGTH, SPEED)
3. Nhập duration (seconds) → amplifier → xong!

---

## 📊 Hỗ Trợ Potion Effects

### Tất Cả Potion Types (1.21.x):

**Buff Effects:**
- `SPEED` - Tốc Độ (tăng tốc di chuyển)
- `HASTE` - Vội Vàng (tăng tốc khai thác)
- `STRENGTH` - Sức Mạnh (tăng sát thương)
- `INSTANT_HEALTH` - Hồi Máu Tức Thì
- `REGENERATION` - Tái Sinh (hồi máu từ từ)
- `RESISTANCE` - Kháng Cự (giảm sát thương)
- `FIRE_RESISTANCE` - Kháng Cự Lửa
- `WATER_BREATHING` - Hô Hấp Nước
- `INVISIBILITY` - Vô Hình
- `NIGHT_VISION` - Thị Lực Đêm
- `JUMP_BOOST` - Tăng Nhảy
- `HEALTH_BOOST` - Tăng Máu Tối Đa
- `SATURATION` - No Nê (hồi độ no ngay)
- `GLOWING` - Phát Sáng
- `CONDUIT_POWER` - Quyền Lực Conduit
- `SLOW_FALLING` - Rơi Chậm

**Debuff Effects:**
- `SLOWNESS` - Chậm Lại
- `MINING_FATIGUE` - Mệt Mỏi
- `INSTANT_DAMAGE` - Sát Thương Tức Thì
- `NAUSEA` - Buồn Nôn
- `BLINDNESS` - Mù Mắt
- `HUNGER` - Đói
- `WEAKNESS` - Yếu Ớt
- `POISON` - Độc (gây sát thương từ từ)
- `WITHER` - Héo Úa (sát thương, không drop loot)
- `LEVITATION` - Nổi Lơ Lửng
- `DARKNESS` - Bóng Tối

**Amplifier (Level):**
- 0 = Level I (cơ bản)
- 1 = Level II
- 2 = Level III
- ...v.v. (tối đa 5 = Level VI)

---

## 🎯 Preset Potion Combos

Có sẵn 10 preset combo phổ biến:

### 1. **Warrior Buff**
   - Strength II (30s)
   - Resistance I (30s)
   - → Dùng cho: Skill hit enemy

### 2. **Mage Buff**
   - Speed II (30s)
   - Haste I (30s)
   - → Dùng cho: Cast spell

### 3. **Healer Buff**
   - Regeneration II (30s)
   - Absorption I (30s)
   - → Dùng cho: Support player

### 4. **Speed Boost**
   - Speed III (20s)
   - → Dùng cho: Dash skill

### 5. **Night Vision**
   - Night Vision (60s)
   - → Dùng cho: Night activity

### 6. **Defensive Aura**
   - Resistance III (15s)
   - Fire Resistance (15s)
   - → Dùng cho: WHILE_HOLDING aura

### 7. **Damage Boost**
   - Strength III (15s)
   - → Dùng cho: Damage skill

### 8. **Healing Potion**
   - Instant Health III (immediate)
   - → Dùng cho: Heal on use

### 9. **Debuff: Weakness**
   - Weakness (20s)
   - → Dùng cho: Enemy debuff

### 10. **Debuff: Poison**
   - Poison II (10s)
   - → Dùng cho: Toxic skill

---

## 💻 Lập Trình - Thêm Potion Programmatically

### Ví Dụ 1: Tạo Potion Action
```java
import com.uitem.skill.SkillAction;

// Tạo action GIVE_POTION
SkillAction potionAction = SkillAction.givePotion("STRENGTH", 30, 1);
// Effect: Strength II cho 30 giây

skill.addAction(potionAction);
```

### Ví Dụ 2: Tạo Aura Potion (holding)
```java
import com.uitem.skill.PotionAuraManager;

// Tạo speed aura khi cầm item
Skill speedAura = PotionAuraManager.AuraPresets.speedAura("my_item");
itemData.getSkills().add(speedAura);
```

### Ví Dụ 3: Multi-Potion Aura
```java
import com.uitem.skill.PotionEffectHelper;

List<PotionEffectHelper.PotionEffectCombo> effects = List.of(
    new PotionEffectHelper.PotionEffectCombo("STRENGTH", 5, 1),
    new PotionEffectHelper.PotionEffectCombo("RESISTANCE", 5, 0)
);

Skill multiAura = PotionAuraManager.createMultiAuraSkill(
    "warrior_aura", 
    "<red>Warrior Aura",
    effects,
    5  // interval seconds
);
itemData.getSkills().add(multiAura);
```

### Ví Dụ 4: Thêm Preset Combo
```java
import com.uitem.skill.PotionEffectHelper;

// Lấy preset "Warrior Buff"
PotionEffectHelper.PotionPreset preset = 
    PotionEffectHelper.getPreset("Warrior Buff");

// Thêm toàn bộ preset vào skill
PotionEffectHelper.addPresetToSkill(skill, preset);
```

---

## 📁 Cấu Trúc YAML (items.yml)

Ví dụ item với potion effect trong YAML:

```yaml
epic_strength_ring:
  material: DIAMOND_RING
  display-name: "<red>Vòng Sức Mạnh"
  lore:
    - "<gray>Tăng sức mạnh khi cầm"
    - "<gray>+2 sát thương"
  
  # Skill system
  skills:
    strength_buff:
      trigger: RIGHT_CLICK
      cooldown: 30.0
      cancel-event: true
      display-name: "<red>Sức Mạnh Tức Thì"
      actions:
        # GIVE_POTION action
        a0:
          type: GIVE_POTION
          effect: STRENGTH
          duration: 30
          amplifier: 1  # Level II
        a1:
          type: ACTION_BAR
          text: "<red>+Sức Mạnh cho 30 giây!"
```

---

## 🛠️ Các Utility Classes

### 1. **PotionEffectRegistry** (`com.uitem.util.PotionEffectRegistry`)
```java
// Lấy tất cả potion effects
Map<String, PotionInfo> effects = PotionEffectRegistry.getAllEffects();

// Kiểm tra potion hợp lệ
boolean valid = PotionEffectRegistry.isValidEffect("STRENGTH");

// Lấy display name
PotionInfo info = PotionEffectRegistry.getInfo("STRENGTH");
System.out.println(info.displayName);  // "Sức Mạnh"
```

### 2. **PotionEffectHelper** (`com.uitem.skill.PotionEffectHelper`)
```java
// Lấy tất cả presets
Map<String, PotionPreset> presets = PotionEffectHelper.getAllPresets();

// Thêm preset vào skill
PotionEffectHelper.PotionPreset preset = 
    PotionEffectHelper.getPreset("Warrior Buff");
PotionEffectHelper.addPresetToSkill(skill, preset);
```

### 3. **PotionAuraManager** (`com.uitem.skill.PotionAuraManager`)
```java
// Tạo aura skill nhanh
Skill speedAura = PotionAuraManager.createAuraPotionSkill(
    "speed_aura", "<aqua>Speed Aura", "SPEED", 0, 5
);

// Dùng preset aura
Skill regenAura = PotionAuraManager.AuraPresets.regenAura("my_item");
```

---

## 🎨 GUI Navigation

### Skill Editor GUI Flow:
```
Item Editor
    ↓
Click "Kỹ Năng & Passive" (slot 26)
    ↓
Skill List GUI
    ↓
Click skill hoặc tạo skill mới
    ↓
Skill Editor GUI
    ├── Action Buttons (36-44): GIVE_POTION, REMOVE_POTION
    ├── Slot 45: ⚡ Potion Presets → PotionPresetGui
    ├── Slot 46: 🍶 Potion Combos → PotionPresetComboGui
    └── ...
```

---

## ⚙️ Cơ Chế Hoạt Động

### WHILE_HOLDING Potion Aura:
1. Player cầm item có skill WHILE_HOLDING
2. Hệ thống tick mỗi 5 tick (~0.25s)
3. Mỗi `periodTicks` tức là potion được áp dụng lại
4. Potion effect duy trì trong khoảng thời gian duration

**Ví dụ:**
- Skill có `periodTicks: 20` (1 giây)
- Action: GIVE_POTION SPEED duration=5 (5 giây)
- Mỗi 1 giây, player nhận Speed effect cho 5 giây
- → Effect duy trì liên tục khi đang cầm

### RIGHT_CLICK Potion:
1. Player right-click item
2. Skill trigger, cooldown check
3. GIVE_POTION action chạy
4. Player nhận potion effect
5. Cooldown trigger

---

## 🚀 Best Practices

1. **Amplifier & Duration:**
   - Buff: Duration 20-30s, Amplifier 0-2
   - Debuff: Duration 10-20s, Amplifier 0-1
   - Aura: Duration bằng interval, Amplifier 0-1

2. **Combo Design:**
   - Max 3-4 effects per combo (tránh spam)
   - Mix buff + debuff hợp lý
   - Interval tối thiểu 3-5s cho aura

3. **Cooldown:**
   - Active skill: 15-60s cooldown
   - Passive/aura: 0 cooldown (rely on interval)

4. **Performance:**
   - Dùng WHILE_HOLDING/WHILE_WEARING cẩn thận (mỗi player check periodic)
   - Tối đa ~5 aura skill per item
   - Amplifier cao = intensive effect, dùng sparingly

---

## 📝 Ghi Chú

- Tất cả potion effect types được lưu dưới dạng **CamelCase** (VD: `STRENGTH`, `SPEED`, `FIRE_RESISTANCE`)
- Duration là **giây** (chuyển sang ticks internally)
- Amplifier là **0-based** (0 = Level I, 1 = Level II, v.v.)
- Potion effect **infinite duration = 1000 ticks** (giới hạn Bukkit API)

---

## 📞 Hỗ Trợ

Nếu có vấn đề với potion effects:
1. Kiểm tra effect name đúng format (UPPERCASE)
2. Kiểm tra duration/amplifier hợp lệ
3. Kiểm tra skill trigger phù hợp
4. Xem console log cho error messages
