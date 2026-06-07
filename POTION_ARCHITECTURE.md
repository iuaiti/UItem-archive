# 🗺️ Potion System - Complete Flowchart & Architecture

## 📊 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      UItem Plugin v1.0.0                         │
│                    Potion System Architecture                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ UTILITY LAYER                                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ PotionEffectRegistry (util)                              │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ • 30+ potion effects                                      │  │
│  │ • Display names (Vietnamese)                             │  │
│  │ • Descriptions                                           │  │
│  │ • Validation methods                                     │  │
│  │ • Pagination support                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ SKILL LAYER                                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ PotionEffectHelper (skill)                               │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ • 10 preset combos                                        │  │
│  │ • Combo management                                        │  │
│  │ • SkillAction creation                                   │  │
│  │ • Batch operations                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ PotionAuraManager (skill)                                │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ • Aura skill factory                                      │  │
│  │ • WHILE_HOLDING support                                  │  │
│  │ • WHILE_WEARING support                                  │  │
│  │ • 6 preset auras                                         │  │
│  │ • Multi-potion support                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Skill (existing)                                         │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ • Trigger (RIGHT_CLICK, WHILE_HOLDING, etc.)            │  │
│  │ • Cooldown                                                │  │
│  │ • Actions list                                            │  │
│  │ └─→ SkillAction (GIVE_POTION, REMOVE_POTION)            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ EXECUTOR LAYER                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ SkillExecutor (existing)                                 │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ • Execute skills based on trigger                        │  │
│  │ • Run actions (including GIVE_POTION)                    │  │
│  │ • Apply potion effects via Bukkit API                    │  │
│  │ • Manage cooldowns                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ UItemSkillListener (existing)                            │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ • Listen for skill triggers                              │  │
│  │ • Periodic ticker for aura skills                        │  │
│  │ • Event handling                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ GUI LAYER                                                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ SkillEditorGui (modified)                                │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ • Added slot 45: ⚡ Potion Presets button               │  │
│  │ • Added slot 46: 🍶 Potion Combos button                │  │
│  └──────────────────────────────────────────────────────────┘  │
│         │                          │                           │
│         ├──────────────────────────┼────────────────────────┐  │
│         ↓                          ↓                        ↓  │
│  ┌──────────────────┐    ┌──────────────────┐  (GIVE_POTION)  │
│  │ PotionPresetGui  │    │ PotionPreset     │  action button   │
│  ├──────────────────┤    │ ComboGui         │                  │
│  │ • Browse effects │    ├──────────────────┤                  │
│  │ • Pagination     │    │ • Browse combos  │                  │
│  │ • Duration/amp   │    │ • Pagination     │                  │
│  │ • Add to skill   │    │ • Add combo      │                  │
│  └──────────────────┘    └──────────────────┘                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 User Flow - Adding Potion to Item

```
START
  │
  ├─→ /uitem list
  │
  ├─→ Click Item (slot varies)
  │
  ├─→ Item Editor GUI opens
  │   └─→ Click slot 26 "Kỹ Năng & Passive"
  │
  ├─→ Skill List GUI opens
  │   ├─→ Option A: Click existing skill
  │   └─→ Option B: Click slot 40 "Thêm Skill Mới"
  │
  ├─→ Skill Editor GUI opens
  │   │
  │   ├─→ Option 1: Use Potion Presets
  │   │   │
  │   │   ├─→ Click slot 45 "⚡ Potion Presets"
  │   │   │
  │   │   ├─→ PotionPresetGui opens
  │   │   │   ├─→ Browse effects (with pagination)
  │   │   │   ├─→ Click effect
  │   │   │   │
  │   │   │   ├─→ Prompt: "Nhập duration (giây)"
  │   │   │   │   └─→ Enter: 20
  │   │   │   │
  │   │   │   ├─→ Prompt: "Nhập amplifier (0-5)"
  │   │   │   │   └─→ Enter: 1
  │   │   │   │
  │   │   │   ├─→ SkillAction created ✓
  │   │   │   │
  │   │   │   └─→ Back to Skill Editor
  │   │   │
  │   │   └─→ [GOTO: Skill Editor]
  │   │
  │   │
  │   ├─→ Option 2: Use Potion Combos
  │   │   │
  │   │   ├─→ Click slot 46 "🍶 Potion Combos"
  │   │   │
  │   │   ├─→ PotionPresetComboGui opens
  │   │   │   ├─→ Browse presets (Warrior Buff, etc.)
  │   │   │   ├─→ Click preset
  │   │   │   │
  │   │   │   ├─→ All effects added to skill ✓
  │   │   │   │   (e.g., STRENGTH + RESISTANCE)
  │   │   │   │
  │   │   │   └─→ Back to Skill Editor
  │   │   │
  │   │   └─→ [GOTO: Skill Editor]
  │   │
  │   │
  │   ├─→ Option 3: Manual GIVE_POTION Action
  │   │   │
  │   │   ├─→ Click slot 41 "GIVE_POTION" button
  │   │   │
  │   │   ├─→ Default action created
  │   │   │
  │   │   ├─→ Prompt: "Nhập potion effect"
  │   │   │   └─→ Enter: STRENGTH
  │   │   │
  │   │   ├─→ [GOTO: Skill Editor]
  │   │   │
  │   │   └─→ Prompt: "Nhập duration"
  │   │       └─→ Enter: 20
  │   │
  │   │
  │   └─→ [SKILL EDITOR]
  │       │
  │       ├─→ View actions added
  │       │   └─→ Action 1: GIVE_POTION (STRENGTH)
  │       │   └─→ Action 2: ACTION_BAR (optional message)
  │       │
  │       ├─→ Click Back (slot 53)
  │       │
  │       └─→ [BACK TO SKILL LIST]
  │
  ├─→ Skill List GUI
  │   ├─→ Skill listed with details
  │   └─→ Click Back (slot 53)
  │
  ├─→ Item Editor GUI
  │   ├─→ Skill count updated
  │   ├─→ Click "💾 SAVE" (slot 49)
  │   │
  │   └─→ Item saved to YAML ✓
  │
  ├─→ In-Game Test
  │   │
  │   ├─→ Player holds/uses item
  │   │
  │   ├─→ Trigger fires (RIGHT_CLICK, WHILE_HOLDING, etc.)
  │   │
  │   ├─→ SkillExecutor runs action
  │   │
  │   ├─→ Bukkit applies potion effect ✓
  │   │
  │   └─→ Player sees effect active!
  │
  └─→ END ✓
```

---

## 📈 Skill Trigger Flow

### Example 1: RIGHT_CLICK Skill

```
Player RIGHT_CLICK item with skill
        ↓
UItemSkillListener.onUItemInteract()
        ↓
Check trigger = RIGHT_CLICK
        ↓
SkillExecutor.executeSkills()
        ↓
Loop through skills
        ├─→ Check trigger match ✓
        ├─→ Check chance ✓
        ├─→ Check cooldown ✓
        ├─→ Check conditions ✓
        │
        └─→ executeSkillActions()
                ↓
            Loop through actions
                ├─→ ACTION 1: GIVE_POTION
                │   └─→ Apply potion via Bukkit API ✓
                │
                ├─→ ACTION 2: PLAY_SOUND
                │   └─→ Play sound ✓
                │
                └─→ ACTION 3: ACTION_BAR
                    └─→ Send action bar message ✓
        ↓
Mark skill as used (cooldown)
        ↓
Return true (event executed)
        ↓
LisListener cancels interact event (if cancelEvent = true)
        ↓
Effect applied to player! ✓
```

### Example 2: WHILE_HOLDING Aura

```
Main ticker runs every 5 ticks (~0.25s)
        ↓
For each online player:
        ├─→ Get main hand item
        │
        ├─→ UItemSkillListener.checkAndTickHolding()
        │
        ├─→ Get item's skills
        │
        ├─→ Filter skills with WHILE_HOLDING trigger
        │
        └─→ SkillExecutor.tickPeriodicSkills()
                ↓
            For each WHILE_HOLDING skill:
                ├─→ Check cooldown (0, always ready)
                ├─→ Check periodTicks (if time elapsed)
                │
                └─→ executeSkillActions()
                        ↓
                    Loop through actions
                        └─→ GIVE_POTION (SPEED, 5s)
                            └─→ Apply to player ✓
        ↓
Every 4-5 seconds (based on periodTicks):
        └─→ Potion refreshed ✓
        
Result: Player has continuous Speed effect while holding! ✓
```

---

## 🎮 Real-World Example: Strength Ring

```yaml
epic_strength_ring:
  material: DIAMOND_RING
  display-name: "<red>Vòng Sức Mạnh"
  lore:
    - "<gray>Tăng sức mạnh khi cầm"
    - "<gray>+2 sát thương"
  
  skills:
    strength_buff:
      trigger: RIGHT_CLICK
      cooldown: 30.0
      cancel-event: true
      display-name: "<red>Buff Sức Mạnh"
      actions:
        a0:
          type: GIVE_POTION
          effect: STRENGTH
          duration: 20
          amplifier: 1
        a1:
          type: ACTION_BAR
          text: "<red>Strength II cho 20 giây!"
        a2:
          type: PLAY_SOUND
          sound: ENTITY_EXPERIENCE_ORB_PICKUP
          volume: 1.0
          pitch: 1.2
```

**Workflow:**
1. Player has ring in hand
2. Player RIGHT_CLICK
3. Skill triggers (cooldown: 30s)
4. GIVE_POTION action: Player gets STRENGTH II for 20s
5. ACTION_BAR: Red message displayed
6. PLAY_SOUND: Pickup sound plays
7. Player deals +20% damage for 20 seconds!
8. Must wait 30s to use again

---

## 🧮 Data Flow Diagram

```
┌──────────────────┐
│  Player Action   │
│ (RIGHT_CLICK)    │
└────────┬─────────┘
         │
         ↓
┌──────────────────────────────┐
│  Bukkit Event                │
│  (PlayerInteractEvent)       │
└────────┬─────────────────────┘
         │
         ↓
┌──────────────────────────────┐
│  UItemSkillListener          │
│  (onUItemInteract)           │
└────────┬─────────────────────┘
         │
         ↓
┌──────────────────────────────┐
│  Get UItemData from item     │
│  skill list from item data   │
└────────┬─────────────────────┘
         │
         ↓
┌──────────────────────────────┐
│  SkillExecutor.executeSkills │
│  (player, skills, trigger)   │
└────────┬─────────────────────┘
         │
         ├─ Loop through skills
         │  ├─ Match trigger?
         │  ├─ Check cooldown?
         │  ├─ Check conditions?
         │
         ↓
┌──────────────────────────────┐
│  SkillAction execution       │
│  (executeSkillActions)       │
└────────┬─────────────────────┘
         │
         ├─ ACTION 1: GIVE_POTION
         │  └─→ Get effect name (STRENGTH)
         │  └─→ Get duration (20)
         │  └─→ Get amplifier (1)
         │  └─→ Registry.EFFECT.get()
         │  └─→ new PotionEffect()
         │  └─→ player.addPotionEffect() ✓
         │
         ├─ ACTION 2: ACTION_BAR
         │  └─→ Parse text
         │  └─→ player.sendActionBar() ✓
         │
         └─ ACTION 3: PLAY_SOUND
            └─→ Get sound name
            └─→ player.playSound() ✓
         
         ↓
┌──────────────────────────────┐
│  Mark cooldown               │
│  cooldownManager.markUsed()  │
└────────┬─────────────────────┘
         │
         ↓
┌──────────────────────────────┐
│  RESULT                      │
│  ✓ Potion applied to player  │
│  ✓ Messages sent             │
│  ✓ Sounds played             │
│  ✓ 30s cooldown set          │
└──────────────────────────────┘
```

---

## 📦 Class Dependencies

```
SkillEditorGui
    └─→ PotionPresetGui
        └─→ PotionEffectRegistry
            └─→ All potion effects
    
    └─→ PotionPresetComboGui
        └─→ PotionEffectHelper
            └─→ PotionEffectCombo list
                └─→ PotionEffectRegistry

Skill
    └─→ SkillAction
        ├─→ GIVE_POTION type
        │   └─→ Parameters: effect, duration, amplifier
        │
        └─→ REMOVE_POTION type
            └─→ Parameters: effect

SkillExecutor
    ├─→ executeSkillActions()
    │   └─→ case GIVE_POTION:
    │       └─→ Registry.EFFECT.get()
    │       └─→ new PotionEffect()
    │       └─→ player.addPotionEffect() ✓
    │
    └─→ case REMOVE_POTION:
        └─→ player.removePotionEffect() ✓

UItemSkillListener
    └─→ tickPlayerPeriodic()
        └─→ checkAndTickHolding()
        └─→ SkillExecutor.tickPeriodicSkills()
```

---

**Complete System Architecture Built!** ✅  
All components integrated and tested.
