# Hệ Thống Potion - Quick Start Guide

## 🚀 Bắt Đầu Nhanh

### Cách 1: Thêm Potion Qua GUI (Dễ Nhất)

1. **Vào Item Editor**
   - `/uitem list`
   - Click item muốn chỉnh
   - Click slot 26 "Kỹ Năng & Passive"

2. **Tạo hoặc mở skill**
   - Click "Thêm Skill Mới" (slot 40) HOẶC click skill có sẵn

3. **Thêm Potion**
   - Click slot 45 **"⚡ Potion Presets"**
   - Chọn potion effect (VD: STRENGTH, SPEED)
   - Nhập duration (giây) → amplifier (0-5)
   - ✅ Done!

### Cách 2: Dùng Preset Combo (Nhanh Nhất)

1. Tại Skill Editor → Click slot 46 **"🍶 Potion Combos"**
2. Chọn combo (Warrior Buff, Mage Buff, v.v.)
3. ✅ Tất cả effect thêm 1 lần!

---

## 📋 Potion Effects Phổ Biến

```
BUFF:
  STRENGTH      → Tăng sát thương
  SPEED         → Tăng tốc độ
  HASTE         → Tăng tốc khai thác
  REGENERATION  → Hồi máu từ từ
  RESISTANCE    → Giảm sát thương
  JUMP_BOOST    → Nhảy cao hơn
  INSTANT_HEALTH → Hồi máu tức thì

DEBUFF:
  WEAKNESS      → Giảm sát thương
  SLOWNESS      → Chậm lại
  POISON        → Sát thương từ từ
  BLINDNESS     → Mù mắt
```

---

## ⚙️ Amplifier & Duration

| Level | Amplifier | Tên |
|-------|-----------|-----|
| I | 0 | Cơ bản |
| II | 1 | Cấp 2 |
| III | 2 | Cấp 3 |
| IV | 3 | Cấp 4 |
| V | 4 | Cấp 5 |
| VI | 5 | Cấp 6 |

**Duration**: Nhập số giây (VD: 30 = 30 giây)

---

## 🎯 Ví Dụ Thực Tế

### Item: "Vòng Sức Mạnh"
- **Skill**: RIGHT_CLICK, 30s cooldown
- **Action 1**: GIVE_POTION STRENGTH amplifier=1 duration=20
- **Action 2**: ACTION_BAR "+Sức Mạnh cho 20 giây!"
- **Action 3**: PLAY_SOUND "ENTITY_EXPERIENCE_ORB_PICKUP"

### Item: "Giáp Người Lính"
- **Skill**: WHILE_WEARING, period=4s (aura)
- **Action**: GIVE_POTION RESISTANCE amplifier=0 duration=5
- → Cứ 4 giây, player nhận Resistance I cho 5 giây

---

## 🎨 GUI Layout

```
Skill Editor (SkillEditorGui):

[Header]
┌─────────────────────────────────┐
│ Skill: fireball_skill │ Trigger  │
├─────────────────────────────────┤
│ [Trigger] [Cooldown] [Chance]   │
├─────────────────────────────────┤
│ [Cancel] [Actions Count]        │
├─────────────────────────────────┤
│ [Action 1] [Action 2] [Action 3]│
├─────────────────────────────────┤
│ [Cond: Chance] [⚡ Potions] [🍶 Combos]
└─────────────────────────────────┘

Slot 45: ⚡ Potion Presets → Browse all potion effects
Slot 46: 🍶 Potion Combos  → Choose preset combos
```

---

## 🔧 YAML Format

```yaml
my_item:
  material: DIAMOND_SWORD
  display-name: "<red>Sword of Power"
  
  skills:
    buff_skill:
      trigger: RIGHT_CLICK
      cooldown: 20
      display-name: "<red>Power Buff"
      actions:
        a0:
          type: GIVE_POTION
          effect: STRENGTH
          duration: 20
          amplifier: 1
        a1:
          type: ACTION_BAR
          text: "<red>Strength II for 20s!"
```

---

## ✅ Checklist

- [ ] Click "Kỹ Năng & Passive" (slot 26)
- [ ] Tạo skill mới hoặc chọn skill có sẵn
- [ ] Click "⚡ Potion Presets" (slot 45) HOẶC "🍶 Potion Combos" (slot 46)
- [ ] Chọn effect/combo
- [ ] Nhập duration & amplifier
- [ ] ✅ Save & Test!

---

## 📝 Mẹo

1. **Amplifier cao = effect mạnh**
   - STRENGTH II (amplifier=1) > STRENGTH I (amplifier=0)

2. **Duration phải hợp lý**
   - Short skill: 5-15s
   - Long buff: 30-60s
   - Aura: bằng interval (4-5s)

3. **Combo nên cân bằng**
   - Không quá 3-4 effect
   - Mix buff + thế sẽ tạo effect mạnh

4. **Cooldown quan trọng**
   - Right-click skill: 15-60s
   - Aura: 0 (rely on period)

---

## 🆘 Troubleshooting

**Q: Potion không áp dụng?**
- A: Kiểm tra effect name đúng format (UPPERCASE)
- A: Kiểm tra skill trigger phù hợp
- A: Kiểm tra cooldown/condition

**Q: Effect tắt quá nhanh?**
- A: Tăng duration
- A: Nếu dùng aura, tăng interval (period-ticks)

**Q: Thêm mấy effect vào 1 combo?**
- A: Tối đa 3-4 effect (tránh spam effect)

---

**Created**: 2026-06-06  
**For**: UItem Plugin v1.0.0+
