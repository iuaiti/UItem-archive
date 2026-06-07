# Potion System API Reference

## 📦 Classes & Methods

### 1. PotionEffectRegistry (Utility)
**Package**: `com.uitem.util`

```java
// Get all potion effects
Map<String, PotionEffectRegistry.PotionInfo> effects = 
    PotionEffectRegistry.getAllEffects();

// Get effect by key
PotionEffectRegistry.PotionInfo info = 
    PotionEffectRegistry.getInfo("STRENGTH");

// Get effect keys list
List<String> keys = PotionEffectRegistry.getAllEffectKeys();

// Check if effect valid
boolean isValid = PotionEffectRegistry.isValidEffect("STRENGTH");

// Get total effects
int total = PotionEffectRegistry.getTotalEffects();

// Get by index (for pagination)
PotionEffectRegistry.PotionInfo info = 
    PotionEffectRegistry.getByIndex(0);
```

**PotionInfo Structure:**
```java
public static class PotionInfo {
    public final String key;              // "STRENGTH"
    public final String displayName;      // "Sức Mạnh"
    public final String description;      // "Tăng sát thương"
}
```

---

### 2. PotionEffectHelper (Skill Helper)
**Package**: `com.uitem.skill`

```java
// Get all presets
Map<String, PotionEffectHelper.PotionPreset> presets = 
    PotionEffectHelper.getAllPresets();

// Get preset by name
PotionEffectHelper.PotionPreset preset = 
    PotionEffectHelper.getPreset("Warrior Buff");

// Get preset by index
PotionEffectHelper.PotionPreset preset = 
    PotionEffectHelper.getPresetByIndex(0);

// Create potion action from combo
SkillAction action = PotionEffectHelper.createPotionAction(
    new PotionEffectHelper.PotionEffectCombo("STRENGTH", 20, 1)
);

// Add entire preset to skill
PotionEffectHelper.addPresetToSkill(skill, preset);

// Validate effect
boolean valid = PotionEffectHelper.isValidEffect("STRENGTH");

// Get total presets
int total = PotionEffectHelper.getTotalPresets();
```

**PotionEffectCombo:**
```java
public static class PotionEffectCombo {
    public final String effectType;     // "STRENGTH"
    public final int durationSeconds;   // 20
    public final int amplifier;         // 1 (Level II)
}
```

**PotionPreset:**
```java
public static class PotionPreset {
    public final String name;                          // "Warrior Buff"
    public final List<PotionEffectCombo> effects;      // 2-3 effects
}
```

---

### 3. PotionAuraManager (Aura Helper)
**Package**: `com.uitem.skill`

```java
// Create single aura potion
Skill aura = PotionAuraManager.createAuraPotionSkill(
    "speed_aura",           // skill id
    "<aqua>Speed Aura",     // display name
    "SPEED",                // effect type
    0,                      // amplifier (Level I)
    5                       // interval (seconds)
);

// Create multi-effect aura
List<PotionEffectHelper.PotionEffectCombo> effects = List.of(
    new PotionEffectHelper.PotionEffectCombo("STRENGTH", 5, 1),
    new PotionEffectHelper.PotionEffectCombo("RESISTANCE", 5, 0)
);
Skill multiAura = PotionAuraManager.createMultiAuraSkill(
    "warrior_aura",
    "<red>Warrior Aura",
    effects,
    5  // interval
);

// Use preset auras
Skill speedAura = PotionAuraManager.AuraPresets.speedAura("item_id");
Skill strengthAura = PotionAuraManager.AuraPresets.strengthAura("item_id");
Skill resistanceAura = PotionAuraManager.AuraPresets.resistanceAura("item_id");
Skill nightVisionAura = PotionAuraManager.AuraPresets.nightVisionAura("item_id");
Skill regenAura = PotionAuraManager.AuraPresets.regenAura("item_id");
Skill lifeStealerAura = PotionAuraManager.AuraPresets.lifeStealerAura("item_id");
```

---

### 4. SkillAction (Existing)
**Package**: `com.uitem.skill`

```java
// Create GIVE_POTION action
SkillAction action = SkillAction.givePotion(
    "STRENGTH",      // effect name
    20,              // duration (seconds)
    1                // amplifier (Level II)
);

// Parameters set automatically:
// - effect: String
// - duration: int
// - amplifier: int
```

---

### 5. GUI Classes

#### PotionPresetGui
**Package**: `com.uitem.gui`

Browse all 30+ potion effects with pagination:
```java
new PotionPresetGui(plugin, player, itemData, skill).open();
```

Features:
- Browse all potion effects
- Pagination support
- Enter duration & amplifier
- Add to skill

#### PotionPresetComboGui
**Package**: `com.uitem.gui`

Browse preset potion combos:
```java
new PotionPresetComboGui(plugin, player, itemData, skill).open();
```

Features:
- 10 preset combos
- Pagination support
- Add entire combo at once

---

## 📖 Usage Examples

### Example 1: Simple Potion Skill
```java
// Create a RIGHT_CLICK skill that gives Strength
Skill skill = new Skill("strength_boost");
skill.setDisplayName("<red>Strength Boost");
skill.setTrigger(SkillTrigger.RIGHT_CLICK);
skill.setCooldown(30.0);
skill.setCancelEvent(true);

// Add potion action
SkillAction potionAction = SkillAction.givePotion("STRENGTH", 20, 1);
skill.addAction(potionAction);

// Add message action
SkillAction messageAction = SkillAction.actionBar("<red>+Strength II!");
skill.addAction(messageAction);

// Add to item
itemData.getSkills().add(skill);
```

### Example 2: Aura Skill
```java
// Speed aura when holding item
Skill aura = PotionAuraManager.createAuraPotionSkill(
    "speed_aura",
    "<aqua>Speed Aura",
    "SPEED",
    1,  // Level II
    4   // apply every 4 seconds
);
itemData.getSkills().add(aura);
```

### Example 3: Multi-Effect Aura
```java
// Buff aura with multiple effects
List<PotionEffectHelper.PotionEffectCombo> effects = List.of(
    new PotionEffectHelper.PotionEffectCombo("STRENGTH", 5, 2),
    new PotionEffectHelper.PotionEffectCombo("RESISTANCE", 5, 0),
    new PotionEffectHelper.PotionEffectCombo("JUMP_BOOST", 5, 1)
);

Skill battleAura = PotionAuraManager.createMultiAuraSkill(
    "battle_aura",
    "<gold>Battle Aura",
    effects,
    5
);
itemData.getSkills().add(battleAura);
```

### Example 4: Using Presets
```java
// Get Warrior Buff preset
PotionEffectHelper.PotionPreset preset = 
    PotionEffectHelper.getPreset("Warrior Buff");

// Create skill
Skill skill = new Skill("warrior_buff");
skill.setTrigger(SkillTrigger.RIGHT_CLICK);
skill.setCooldown(30);

// Add entire preset
PotionEffectHelper.addPresetToSkill(skill, preset);

itemData.getSkills().add(skill);
```

---

## 🎯 Available Preset Combos

```java
PotionEffectHelper.getPreset("Warrior Buff");        // Strength + Resistance
PotionEffectHelper.getPreset("Mage Buff");           // Speed + Haste
PotionEffectHelper.getPreset("Healer Buff");         // Regen + Absorption
PotionEffectHelper.getPreset("Speed Boost");         // Speed III
PotionEffectHelper.getPreset("Night Vision");        // Night Vision
PotionEffectHelper.getPreset("Defensive Aura");      // Resistance + Fire Resist
PotionEffectHelper.getPreset("Damage Boost");        // Strength III
PotionEffectHelper.getPreset("Healing Potion");      // Instant Health III
PotionEffectHelper.getPreset("Debuff: Weakness");    // Weakness
PotionEffectHelper.getPreset("Debuff: Poison");      // Poison II
```

---

## 🛠️ Integration Points

### With SkillExecutor
The existing SkillExecutor already supports:
- `GIVE_POTION` action type
- `REMOVE_POTION` action type
- Full potion effect application via Bukkit API

### With Storage
Potion effects are stored in YAML:
```yaml
skills:
  my_skill:
    actions:
      a0:
        type: GIVE_POTION
        effect: STRENGTH
        duration: 20
        amplifier: 1
```

---

## 📊 Potion Duration & Ticks

Duration in seconds → Ticks conversion:
```
seconds × 20 = ticks
(1 second = 20 ticks in Minecraft)

5 seconds = 100 ticks
10 seconds = 200 ticks
20 seconds = 400 ticks
30 seconds = 600 ticks
```

For aura skills:
```java
periodTicks = intervalSeconds * 20;

// Example: 5 second interval
skill.setPeriodTicks(5 * 20);  // 100 ticks
```

---

## ⚠️ Important Notes

1. **Effect Names**: Must be UPPERCASE and match Bukkit registry
   - Valid: `STRENGTH`, `SPEED`, `HASTE`
   - Invalid: `strength`, `Speed`, `Str`

2. **Amplifier Range**: 0-5 (representing Level I-VI)
   - 0 = Level I (base effect)
   - 1 = Level II (stronger)
   - etc.

3. **Duration**: Must be positive integer (seconds)
   - Min: 1 second
   - Max: 50+ seconds (practical limit)

4. **Aura Period**: Minimum 3-5 seconds recommended
   - Too short: server lag
   - Too long: effect seems to disappear

5. **Performance**: 
   - Max ~5 aura skills per item
   - Max ~3-4 effects per combo
   - Avoid instant health with high amplifier

---

## 🔗 Related Classes

- `Skill` - Base skill class
- `SkillTrigger` - Trigger types (RIGHT_CLICK, WHILE_HOLDING, etc.)
- `SkillAction` - Base action class
- `SkillExecutor` - Executes skills and actions
- `SkillListener` - Listens for skill triggers

---

**Version**: 1.0.0+  
**Last Updated**: 2026-06-06
