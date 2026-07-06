package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.SkillAction;
import com.uitem.skill.SkillTrigger;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for a single skill/passive.
 * Allows:
 * - Changing trigger
 * - Setting cooldown + chance + cancel
 * - Quick add actions (preset buttons)
 * - Enter detailed actions via chat
 */
public class SkillEditorGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private int actionPage = 0;

    private static final List<SkillTrigger> ACTIVE_TRIGGERS = List.of(
            SkillTrigger.RIGHT_CLICK, SkillTrigger.LEFT_CLICK,
            SkillTrigger.SHIFT_RIGHT_CLICK, SkillTrigger.SHIFT_LEFT_CLICK,
            SkillTrigger.SNEAK, SkillTrigger.DOUBLE_SNEAK,
            SkillTrigger.DROP, SkillTrigger.SWAP_HAND,
            SkillTrigger.THROW, SkillTrigger.SHOOT   // THROW = drop with velocity; SHOOT = bow/crossbow/trident launch (new)
    );

    private static final List<SkillTrigger> PASSIVE_TRIGGERS = List.of(
            SkillTrigger.HIT, SkillTrigger.TAKE_DAMAGE, SkillTrigger.KILL,
            SkillTrigger.EQUIP, SkillTrigger.UNEQUIP,
            SkillTrigger.PERIODIC, SkillTrigger.WHILE_HOLDING, SkillTrigger.WHILE_WEARING
    );

    private static final List<List<SkillAction.ActionType>> ACTION_PAGES = List.of(
            List.of( // Page 1: Communication + Combat
                    SkillAction.ActionType.MESSAGE,
                    SkillAction.ActionType.ACTION_BAR,
                    SkillAction.ActionType.TITLE,
                    SkillAction.ActionType.PLAY_SOUND,
                    SkillAction.ActionType.SPAWN_PARTICLE,
                    SkillAction.ActionType.GIVE_POTION,
                    SkillAction.ActionType.HEAL,
                    SkillAction.ActionType.DAMAGE,
                    SkillAction.ActionType.SHOOT_PROJECTILE
            ),
            List.of( // Page 2: Commands + Movement
                    SkillAction.ActionType.CONSOLE_COMMAND,
                    SkillAction.ActionType.PLAYER_COMMAND,
                    SkillAction.ActionType.LAUNCH,
                    SkillAction.ActionType.TELEPORT,
                    SkillAction.ActionType.STRIKE_LIGHTNING,
                    SkillAction.ActionType.SET_FIRE,
                    SkillAction.ActionType.FEED,
                    SkillAction.ActionType.CONSUME_DURABILITY,
                    SkillAction.ActionType.CONSUME_ITEM
            ),
            List.of( // Page 3: Items + Effects
                    SkillAction.ActionType.GIVE_ITEM,
                    SkillAction.ActionType.REMOVE_POTION,
                    SkillAction.ActionType.BLOCK_BREAK,
                    SkillAction.ActionType.EXPLOSION,
                    SkillAction.ActionType.AREA_DAMAGE,
                    SkillAction.ActionType.POTION_AREA,
                    SkillAction.ActionType.BLIND,
                    SkillAction.ActionType.GLOW,
                    SkillAction.ActionType.CLEAR_EFFECTS
            ),
            List.of( // Page 4: Misc
                    SkillAction.ActionType.LEVITATE,
                    SkillAction.ActionType.SPAWN_MOB,
                    SkillAction.ActionType.FREEZE
            )
    );

    private static final java.util.Map<SkillAction.ActionType, Material> ACTION_MATERIALS = new java.util.LinkedHashMap<>();
    static {
        ACTION_MATERIALS.put(SkillAction.ActionType.MESSAGE, Material.PAPER);
        ACTION_MATERIALS.put(SkillAction.ActionType.ACTION_BAR, Material.NAME_TAG);
        ACTION_MATERIALS.put(SkillAction.ActionType.TITLE, Material.FILLED_MAP);
        ACTION_MATERIALS.put(SkillAction.ActionType.PLAY_SOUND, Material.NOTE_BLOCK);
        ACTION_MATERIALS.put(SkillAction.ActionType.SPAWN_PARTICLE, Material.BLAZE_POWDER);
        ACTION_MATERIALS.put(SkillAction.ActionType.GIVE_POTION, Material.POTION);
        ACTION_MATERIALS.put(SkillAction.ActionType.HEAL, Material.GOLDEN_APPLE);
        ACTION_MATERIALS.put(SkillAction.ActionType.DAMAGE, Material.IRON_SWORD);
        ACTION_MATERIALS.put(SkillAction.ActionType.SHOOT_PROJECTILE, Material.BOW);
        ACTION_MATERIALS.put(SkillAction.ActionType.CONSOLE_COMMAND, Material.COMMAND_BLOCK);
        ACTION_MATERIALS.put(SkillAction.ActionType.PLAYER_COMMAND, Material.REPEATER);
        ACTION_MATERIALS.put(SkillAction.ActionType.LAUNCH, Material.FIREWORK_ROCKET);
        ACTION_MATERIALS.put(SkillAction.ActionType.TELEPORT, Material.ENDER_PEARL);
        ACTION_MATERIALS.put(SkillAction.ActionType.STRIKE_LIGHTNING, Material.NETHER_STAR);
        ACTION_MATERIALS.put(SkillAction.ActionType.SET_FIRE, Material.FLINT_AND_STEEL);
        ACTION_MATERIALS.put(SkillAction.ActionType.FEED, Material.COOKED_BEEF);
        ACTION_MATERIALS.put(SkillAction.ActionType.CONSUME_DURABILITY, Material.ANVIL);
        ACTION_MATERIALS.put(SkillAction.ActionType.CONSUME_ITEM, Material.ITEM_FRAME);
        ACTION_MATERIALS.put(SkillAction.ActionType.GIVE_ITEM, Material.CHEST);
        ACTION_MATERIALS.put(SkillAction.ActionType.REMOVE_POTION, Material.GLASS_BOTTLE);
        ACTION_MATERIALS.put(SkillAction.ActionType.BLOCK_BREAK, Material.DIAMOND_PICKAXE);
        ACTION_MATERIALS.put(SkillAction.ActionType.EXPLOSION, Material.TNT);
        ACTION_MATERIALS.put(SkillAction.ActionType.AREA_DAMAGE, Material.DRAGON_BREATH);
        ACTION_MATERIALS.put(SkillAction.ActionType.POTION_AREA, Material.LINGERING_POTION);
        ACTION_MATERIALS.put(SkillAction.ActionType.BLIND, Material.INK_SAC);
        ACTION_MATERIALS.put(SkillAction.ActionType.GLOW, Material.GLOWSTONE_DUST);
        ACTION_MATERIALS.put(SkillAction.ActionType.CLEAR_EFFECTS, Material.MILK_BUCKET);
        ACTION_MATERIALS.put(SkillAction.ActionType.LEVITATE, Material.FEATHER);
        ACTION_MATERIALS.put(SkillAction.ActionType.SPAWN_MOB, Material.EGG);
        ACTION_MATERIALS.put(SkillAction.ActionType.FREEZE, Material.ICE);
    }

    public SkillEditorGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, Messages.skillEditorTitle(skill.getId()));
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.ENCHANTED_BOOK,
                Messages.skillEditorHeaderTitle(skill.getId(), skill.getTrigger().getDisplayName()),
                List.of(
                        Messages.skillEditorHeaderCooldown(skill.getCooldown()),
                        Messages.skillEditorHeaderChance((int) skill.getChance()),
                        Messages.skillEditorHeaderActions(skill.getActions().size())
                )));

        // Trigger: single compact field - left/right click cycles through the trigger list (no chat needed).
        List<SkillTrigger> triggerOptions = new ArrayList<>();
        triggerOptions.addAll(ACTIVE_TRIGGERS);
        triggerOptions.addAll(PASSIVE_TRIGGERS);
        SkillTrigger currentTrigger = skill.getTrigger();
        int triggerIdx = triggerOptions.indexOf(currentTrigger);
        if (triggerIdx < 0) triggerIdx = 0;
        final int currentTriggerIdx = triggerIdx;
        setItem(9, createItem(Material.NETHER_STAR, Messages.skillEditorTriggerField(),
                List.of(
                        "<gray>" + Messages.skillLabel("current", "Current") + ": <white>" + currentTrigger.getDisplayName() +
                                (currentTrigger.isActive() ? " <aqua>(" + Messages.skillLabel("active-state", "Active") + ")" : " <gold>(" + Messages.skillLabel("passive-state", "Passive") + ")"),
                        "<gray>" + currentTrigger.getDescription(),
                        "",
                        Messages.skillEditorTriggerClick()
                )), ev -> {
            int size = triggerOptions.size();
            int newIdx = ev.isRightClick()
                    ? (currentTriggerIdx - 1 + size) % size
                    : (currentTriggerIdx + 1) % size;
            skill.setTrigger(triggerOptions.get(newIdx));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
            saveAndRefresh();
        });
        // Filler for the space freed up by condensing the trigger grid into one field
        for (int i = 10; i <= 25; i++) {
            if (i == 17 || i == 18) continue; // already border-filled by fillBorder
            setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // === Cooldown (row 3) ===
        setItem(28, createItem(Material.CLOCK, String.format(Messages.skillEditorCooldown(), skill.getCooldown()),
                List.of(Messages.skillEditorCooldownHint())), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>" + Messages.skillPrompt("cooldown", "Enter cooldown (seconds, example 4.5):"), input -> {
                try {
                    double cd = Double.parseDouble(input.trim());
                    skill.setCooldown(cd);
                } catch (Exception ignored) {}
                saveAndReopen();
            }, this::reopen);
        });

        // Chance
        setItem(29, createItem(Material.GHAST_TEAR, String.format(Messages.skillEditorChance(), (int) skill.getChance()),
                List.of(Messages.skillEditorChanceHint())), ev -> {
            plugin.getChatInputManager().requestInput(player, "<yellow>" + Messages.skillPrompt("chance", "Enter chance (0-100):"), input -> {
                try { skill.setChance(Double.parseDouble(input.trim())); } catch (Exception ignored) {}
                saveAndReopen();
            }, this::reopen);
        });

        // Cancel event
        setItem(30, createItem(skill.isCancelEvent() ? Material.BARRIER : Material.PAPER,
                String.format(Messages.skillEditorCancelEvent(), skill.isCancelEvent() ? Messages.stateOn() : Messages.stateOff()),
                List.of(Messages.skillEditorCancelHint(), Messages.skillEditorCancelClick())), ev -> {
            skill.setCancelEvent(!skill.isCancelEvent());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            saveAndRefresh();
        });

        // Rename Skill (overrides the border pane at slot 26 - safe, purely decorative before this)
        setItem(26, createItem(Material.NAME_TAG, Messages.skillEditorRenameButton(),
                List.of(Messages.skillEditorRenameHint())), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>" + Messages.skillEditorRenamePrompt(), input -> {
                String newId = input.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "");
                if (newId.isEmpty()) {
                    player.sendMessage(TextUtil.error(Messages.skillEditorRenameInvalid()));
                    reopen();
                    return;
                }
                if (!newId.equals(skill.getId()) && itemData.getSkills().stream().anyMatch(s -> s.getId() != null && s.getId().equalsIgnoreCase(newId))) {
                    player.sendMessage(TextUtil.error(Messages.skillEditorRenameDuplicate(newId)));
                    reopen();
                    return;
                }
                skill.setId(newId);
                plugin.getItemManager().registerItem(itemData);
                plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), itemData);
                player.sendMessage(TextUtil.success(Messages.skillEditorRenamed(newId)));
                Runnable back = getBackAction();
                SkillEditorGui newGui = new SkillEditorGui(plugin, player, itemData, skill);
                if (back != null) newGui.setBackAction(back);
                newGui.open();
            }, this::reopen);
        });

        // === Actions section header (row 3) ===
        setItem(31, createItem(Material.WRITABLE_BOOK,
                Messages.skillEditorActionsHeader(skill.getActions().size()),
                List.of(Messages.skillEditorActionsHint())));

        // Action list items (row 3, slots 32-33 only to avoid overlap with potion presets at 34-35)
        List<SkillAction> acts = skill.getActions();
        int[] actSlots = {32, 33};
        for (int i = 0; i < Math.min(acts.size(), actSlots.length); i++) {
            final int idx = i;
            SkillAction a = acts.get(i);
            ItemStack icon = buildActionIcon(a);
            setItem(actSlots[i], icon, ev -> {
                if (idx >= skill.getActions().size()) return;
                if (ev.isRightClick()) {
                    skill.getActions().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    saveAndRefresh();
                } else {
                    editActionMainValue(a);
                }
            });
        }
        // Extra actions overflow (row 4, slots 36-43)
        int[] actSlotsExtra = {36, 37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < Math.min(acts.size() - actSlots.length, actSlotsExtra.length); i++) {
            final int idx = i + actSlots.length;
            if (idx >= acts.size()) break;
            SkillAction a = acts.get(idx);
            ItemStack icon = buildActionIcon(a);
            setItem(actSlotsExtra[i], icon, ev -> {
                if (idx >= skill.getActions().size()) return;
                if (ev.isRightClick()) {
                    skill.getActions().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    saveAndRefresh();
                } else {
                    editActionMainValue(a);
                }
            });
        }

        if (acts.isEmpty()) {
            setItem(31, createItem(Material.BARRIER, "<gray><italic>" + Messages.skillEditorNoActions() + "</italic>",
                    List.of(Messages.skillText("use-buttons-below", "<yellow>Use the action buttons below to add actions"))));
        }

        // Quick condition buttons (top row, outside the trigger grid)
        setItem(7, createItem(Material.EMERALD, Messages.skillEditorCondChance75(), List.of(Messages.skillEditorCondChance75Desc())), ev -> {
            skill.addCondition(com.uitem.skill.SkillCondition.chance(75));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            saveAndRefresh();
        });
        setItem(8, createItem(Material.REDSTONE, Messages.skillEditorCondChance50(), List.of(Messages.skillEditorCondChance50Desc())), ev -> {
            skill.addCondition(com.uitem.skill.SkillCondition.chance(50));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            saveAndRefresh();
        });

        // Potion presets (row 3, slots 34-35 - safe from action list)
        setItem(34, createItem(Material.REDSTONE, Messages.skillEditorPotionPresets(),
                List.of(Messages.skillEditorPotionPresetsDesc())), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            new PotionPresetGui(plugin, player, itemData, skill).openAsChildOf(this);
        });
        setItem(35, createItem(Material.BREWING_STAND, Messages.skillEditorPotionCombos(),
                List.of(Messages.skillEditorPotionCombosDesc())), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            new PotionPresetComboGui(plugin, player, itemData, skill).openAsChildOf(this);
        });

        // === Action type buttons (paged, slots 44-52, slot 53 = navigation) ===
        List<SkillAction.ActionType> pageActions = ACTION_PAGES.get(actionPage);
        int actionSlot = 44;
        for (SkillAction.ActionType at : pageActions) {
            if (actionSlot >= 53) break;
            Material mat = ACTION_MATERIALS.getOrDefault(at, Material.PAPER);
            addActionTypeButton(actionSlot++, mat, at, () -> addActionByType(at));
        }
        for (int i = actionSlot; i < 53; i++) {
            setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        // Page navigation at slot 53
        int totalPages = ACTION_PAGES.size();
        if (actionPage < totalPages - 1) {
            setItem(53, createItem(Material.ARROW, Messages.skillEditorPageNext(actionPage + 2),
                    List.of(Messages.skillEditorPageNextDesc())), ev -> {
                actionPage++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                refresh();
            });
        } else {
            setItem(53, createItem(Material.ARROW, Messages.skillEditorPageFirst(),
                    List.of(Messages.skillEditorPageFirstDesc())), ev -> {
                actionPage = 0;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                refresh();
            });
        }

        // Back (slot 27 is safe - passive triggers end at slot 26 for 8 triggers)
        setItem(27, createBackButton(), ev -> goBack());
    }

    private ItemStack buildActionIcon(SkillAction a) {
        Material mat = Material.PAPER;
        String name = "<white>" + a.getType().getDisplay();
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + Messages.skillLabel("type", "Type") + ": <white>" + a.getType().name());

        // Show the main parameter for readability
        switch (a.getType()) {
            case MESSAGE, ACTION_BAR -> {
                String txt = a.getStringParam("text", "");
                if (!txt.isEmpty()) lore.add("<gray>" + Messages.skillLabel("content", "Content") + ": <white>" + (txt.length() > 25 ? txt.substring(0, 25) + "..." : txt));
            }
            case PLAY_SOUND -> lore.add("<gray>" + Messages.skillLabel("sound", "Sound") + ": <white>" + a.getStringParam("sound", "?"));
            case GIVE_POTION -> lore.add("<gray>" + Messages.skillLabel("effect", "Effect") + ": <white>" + a.getStringParam("effect", "?") + " " + a.getIntParam("amplifier", 0));
            case HEAL, DAMAGE -> lore.add("<gray>" + Messages.skillLabel("value", "Value") + ": <white>" + a.getDoubleParam("amount", 0));
            case SHOOT_PROJECTILE -> lore.add("<gray>" + Messages.skillLabel("projectile", "Projectile") + ": <white>" + a.getStringParam("projectile", "FIREBALL"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> {
                String cmd = a.getStringParam("command", "");
                if (!cmd.isEmpty()) lore.add("<gray>" + Messages.skillLabel("command", "Command") + ": <white>" + (cmd.length() > 28 ? cmd.substring(0, 28) + "..." : cmd));
            }
            case LAUNCH -> lore.add("<gray>" + Messages.skillLabel("velocity", "Velocity") + ": <white>" + a.getDoubleParam("y", 0));
            case TELEPORT -> lore.add("<gray>" + Messages.skillLabel("y-offset", "Y offset") + ": <white>" + a.getDoubleParam("y", 0));
            case STRIKE_LIGHTNING -> lore.add("<gray>" + Messages.skillText("lightning-strike", "Lightning strike at target"));
            case SET_FIRE -> lore.add("<gray>" + Messages.skillLabel("fire", "Fire") + ": <white>" + a.getIntParam("seconds", 4) + "s");
            case FEED -> lore.add("<gray>" + Messages.skillLabel("food", "Food") + ": <white>" + a.getIntParam("amount", 4));
            case CONSUME_DURABILITY -> lore.add("<gray>" + Messages.skillLabel("durability", "Durability") + ": <white>" + a.getIntParam("amount", 1));
            case CONSUME_ITEM -> lore.add("<gray>" + Messages.skillText("consume-item", "Consume 1 item"));
            case GIVE_ITEM -> lore.add("<gray>" + Messages.skillLabel("item", "Item") + ": <white>" + a.getStringParam("material", "DIAMOND") + " x" + a.getIntParam("amount", 1));
            case REMOVE_POTION -> lore.add("<gray>" + Messages.skillLabel("remove-effect", "Remove effect") + ": <white>" + a.getStringParam("effect", "all"));
            case BLOCK_BREAK -> {
                int r = a.getIntParam("range", 5);
                int rad = a.getIntParam("radius", 0);
                lore.add("<gray>" + Messages.skillLabel("range", "Range") + ": <white>" + r + (rad > 0 ? "  <yellow>" + Messages.skillLabel("radius", "Radius") + ": " + rad + " (3x3+)" : ""));
            }
            case EXPLOSION -> lore.add("<gray>" + Messages.skillLabel("power", "Power") + ": <white>" + a.getDoubleParam("power", 3) + " " + Messages.skillLabel("fire", "Fire") + ": " + a.getBooleanParam("fire", false));
            case AREA_DAMAGE -> lore.add("<gray>" + Messages.skillLabel("dmg", "DMG") + ": <white>" + a.getDoubleParam("amount", 5) + " " + Messages.skillLabel("radius", "Radius") + ": " + a.getDoubleParam("radius", 5));
            case POTION_AREA -> lore.add("<gray>" + Messages.skillLabel("effect", "Effect") + ": <white>" + a.getStringParam("effect", "?") + " " + Messages.skillLabel("range", "Range") + ": " + a.getDoubleParam("radius", 5));
            case BLIND, GLOW, LEVITATE -> lore.add("<gray>" + Messages.skillLabel("duration", "Duration") + ": <white>" + a.getIntParam("duration", 5) + "s");
            case CLEAR_EFFECTS -> lore.add("<gray>" + Messages.skillText("clear-effects", "Clear all effects"));
            case SPAWN_MOB -> lore.add("<gray>" + Messages.skillLabel("entity", "Entity") + ": <white>" + a.getStringParam("entity", "ZOMBIE") + " x" + a.getIntParam("count", 1));
            case FREEZE -> lore.add("<gray>" + Messages.skillLabel("freeze", "Freeze") + ": <white>" + a.getIntParam("duration", 5) + "s");
        }

        lore.add("");
        lore.add(Messages.skillText("edit-hint", "<yellow>Left: Edit value"));
        lore.add(Messages.skillText("delete-hint", "<red>Right: Delete"));
        return createItem(mat, name, lore);
    }

    /** Add quick action type button (bilingual) */
    private void addActionTypeButton(int slot, Material mat, SkillAction.ActionType type, Runnable onClick) {
        String display = Messages.skillActionDisplay(type);
        setItem(slot, createItem(mat, "<green>+ " + display, List.of("<gray>" + type.getDisplay())), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            onClick.run();
        });
    }

    private void addActionByType(SkillAction.ActionType type) {
        switch (type) {
            case MESSAGE, ACTION_BAR, TITLE, PLAY_SOUND, GIVE_POTION, SHOOT_PROJECTILE,
                 CONSOLE_COMMAND, PLAYER_COMMAND, SPAWN_PARTICLE ->
                addActionWithPrompt(type);
            case HEAL, DAMAGE, FEED, SET_FIRE, CONSUME_DURABILITY ->
                addNumericAction(type);
            case LAUNCH, TELEPORT ->
                addActionWithPrompt(type);
            case BLOCK_BREAK, EXPLOSION, AREA_DAMAGE, POTION_AREA, BLIND,
                 GLOW, LEVITATE, SPAWN_MOB, FREEZE ->
                addActionWithPrompt(type);
            case STRIKE_LIGHTNING, CLEAR_EFFECTS, CONSUME_ITEM, GIVE_ITEM,
                 REMOVE_POTION, NOTHING ->
                addDirectAction(type);
        }
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), itemData);
        refresh();
    }

    private void saveAndReopen() {
        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), itemData);
        reopen();
    }

    // ==================== Helpers for adding actions entirely via GUI ====================

    /**
     * Add an action and immediately ask for the main value via chat (most convenient for text/command).
     * The action is only added to the skill after the user confirms the input.
     */
    private void addActionWithPrompt(SkillAction.ActionType type) {
        SkillAction action = new SkillAction(type);

        // Set sensible default values for some types
        switch (type) {
            case PLAY_SOUND -> {
                action.setParam("sound", "UI_BUTTON_CLICK");
                action.setParam("volume", 1.0);
                action.setParam("pitch", 1.0);
            }
            case GIVE_POTION -> {
                action.setParam("effect", "SPEED");
                action.setParam("duration", 10);
                action.setParam("amplifier", 0);
            }
            case SHOOT_PROJECTILE -> {
                action.setParam("projectile", "SMALL_FIREBALL");
                action.setParam("speed", 1.5);
                action.setParam("damage", 5.0);
            }
            case LAUNCH -> {
                action.setParam("x", 0);
                action.setParam("y", 1.2);
                action.setParam("z", 0);
                action.setParam("relative", true);
            }
            case TELEPORT -> {
                action.setParam("x", 0);
                action.setParam("y", 5);
                action.setParam("z", 0);
                action.setParam("relative", true);
            }
            case BLOCK_BREAK -> {
                action.setParam("range", 5);
                action.setParam("radius", 0);
            }
            case EXPLOSION -> {
                action.setParam("power", 3.0);
                action.setParam("fire", false);
            }
            case AREA_DAMAGE -> {
                action.setParam("amount", 5.0);
                action.setParam("radius", 5.0);
            }
            case POTION_AREA -> {
                action.setParam("effect", "POISON");
                action.setParam("duration", 5);
                action.setParam("amplifier", 0);
                action.setParam("radius", 5.0);
            }
            case SPAWN_PARTICLE -> {
                action.setParam("particle", "FLAME");
                action.setParam("count", 10);
                action.setParam("offset", 0.3);
            }
            case BLIND, GLOW, LEVITATE, FREEZE -> {
                action.setParam("duration", 5);
                if (type == SkillAction.ActionType.LEVITATE) action.setParam("amplifier", 0);
            }
            case SPAWN_MOB -> {
                action.setParam("entity", "ZOMBIE");
                action.setParam("count", 1);
            }
        }

        // Ask for the most important value first, before adding to skill
        // Only addAction when the user provides valid input
        switch (type) {
            case MESSAGE -> promptForActionParamThenAdd(action, "text", Messages.skillPrompt("message", "Enter message content (supports %player% %target%):"));
            case ACTION_BAR -> promptForActionParamThenAdd(action, "text", Messages.skillPrompt("action-bar", "Enter action bar text:"));
            case TITLE -> promptForActionParamThenAdd(action, "title", Messages.skillPrompt("title", "Enter Title:"));
            case PLAY_SOUND -> promptForActionParamThenAdd(action, "sound", Messages.skillPrompt("sound", "Enter sound name (e.g. ENTITY_BLAZE_SHOOT):"));
            case SPAWN_PARTICLE -> promptForActionParamThenAdd(action, "particle", Messages.skillPrompt("particle", "Enter particle name (e.g. FLAME, HEART):"));
            case GIVE_POTION -> promptForActionParamThenAdd(action, "effect", Messages.skillPrompt("potion-effect", "Enter potion effect (e.g. SPEED, STRENGTH):"));
            case SHOOT_PROJECTILE -> promptForActionParamThenAdd(action, "projectile", Messages.skillPrompt("projectile", "Projectile (SMALL_FIREBALL / ARROW / WITHER_SKULL...):"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> promptForActionParamThenAdd(action, "command", Messages.skillPrompt("command", "Enter command (supports %player% %target%):"));
            case LAUNCH -> promptForActionParamThenAdd(action, "y", Messages.skillPrompt("launch-y", "Enter launch Y power (e.g. 1.5):"));
            case TELEPORT -> promptForActionParamThenAdd(action, "y", Messages.skillPrompt("teleport-y", "Enter teleport Y offset:"));
            case BLOCK_BREAK -> promptForActionParamThenAdd(action, "range", Messages.skillPrompt("block-break-range", "Enter block break range (1-10). Radius=0 for single block; set later via edit or YAML:"));
            case EXPLOSION -> promptForActionParamThenAdd(action, "power", Messages.skillPrompt("explosion-power", "Enter explosion power (0.1-10):"));
            case AREA_DAMAGE -> promptForActionParamThenAdd(action, "amount", Messages.skillPrompt("area-damage", "Enter area damage amount:"));
            case POTION_AREA -> promptForActionParamThenAdd(action, "effect", Messages.skillPrompt("area-potion", "Enter area potion effect (e.g. POISON, WITHER):"));
            case BLIND -> promptForActionParamThenAdd(action, "duration", Messages.skillPrompt("blind-duration", "Enter blind duration (seconds):"));
            case GLOW -> promptForActionParamThenAdd(action, "duration", Messages.skillPrompt("glow-duration", "Enter glow duration (seconds):"));
            case LEVITATE -> promptForActionParamThenAdd(action, "duration", Messages.skillPrompt("levitate-duration", "Enter levitate duration (seconds):"));
            case SPAWN_MOB -> promptForActionParamThenAdd(action, "entity", Messages.skillPrompt("spawn-mob-entity", "Enter entity type (e.g. ZOMBIE, SKELETON, WOLF):"));
            case FREEZE -> promptForActionParamThenAdd(action, "duration", Messages.skillPrompt("freeze-duration", "Enter freeze duration (seconds):"));
            default -> {
                action.setParam("value", 1);
                skill.addAction(action);
                player.sendMessage(TextUtil.success(Messages.skillAdded(type.name())));
                saveAndRefresh();
            }
        }
    }

    /**
     * Add a numeric action, allowing quick selection via button before asking for a custom value.
     * The action is only added to the skill after the user confirms the input.
     */
    private void addNumericAction(SkillAction.ActionType type) {
        String prompt;
        double defaultValue = 1.0;
        String paramKey = "amount";

        switch (type) {
            case HEAL -> { prompt = Messages.skillPrompt("heal-amount", "Enter heal amount (e.g. 3.5):"); defaultValue = 3.0; }
            case DAMAGE -> { prompt = Messages.skillPrompt("damage-amount", "Enter damage amount (e.g. 5):"); defaultValue = 4.0; }
            case FEED -> { prompt = Messages.skillPrompt("feed-amount", "Enter food amount (1-20):"); defaultValue = 4; }
            case SET_FIRE -> { prompt = Messages.skillPrompt("fire-duration", "Enter fire duration (seconds):"); defaultValue = 4; paramKey = "seconds"; }
            case CONSUME_DURABILITY -> { prompt = Messages.skillPrompt("durability-cost", "Enter durability cost:"); defaultValue = 1; }
            default -> prompt = Messages.skillPrompt("generic-value", "Enter value:");
        }

        SkillAction action = new SkillAction(type);
        if (paramKey.equals("seconds")) {
            action.setParam(paramKey, (int) defaultValue);
        } else {
            action.setParam(paramKey, defaultValue);
        }

        promptForActionParamThenAdd(action, paramKey, prompt);
    }

    /** Add an action that needs no input (has good default values) */
    private void addDirectAction(SkillAction.ActionType type) {
        SkillAction action = new SkillAction(type);
        skill.addAction(action);
        player.sendMessage(TextUtil.success(Messages.skillAdded(type.getDisplay())));
        saveAndRefresh();
    }

    /** Ask for a param value, then save + refresh (the action already exists in the skill) */
    private void promptForActionParam(SkillAction action, String paramKey, String promptMessage, String successMsg) {
        plugin.getChatInputManager().requestInput(player, "<yellow>" + promptMessage, input -> {
            try {
                String val = input.trim();
                if (paramKey.equals("amount") || paramKey.equals("y") || paramKey.equals("seconds") || paramKey.equals("speed") || paramKey.equals("damage")) {
                    double num = Double.parseDouble(val);
                    action.setParam(paramKey, num);
                } else {
                    action.setParam(paramKey, val);
                }
                player.sendMessage(TextUtil.success(successMsg));
            } catch (Exception e) {
                player.sendMessage(TextUtil.info(Messages.skillText("added-default", "Added with default value (click the action again to edit).")));
            }
            saveAndReopen();
        }, this::reopen);
    }

    /**
     * Ask for a param value; only add the action to the skill on valid input.
     */
    private void promptForActionParamThenAdd(SkillAction action, String paramKey, String promptMessage) {
        plugin.getChatInputManager().requestInput(player, "<yellow>" + promptMessage, input -> {
            try {
                String val = input.trim();
                if (paramKey.equals("amount") || paramKey.equals("y") || paramKey.equals("seconds") || paramKey.equals("speed") || paramKey.equals("damage")) {
                    double num = Double.parseDouble(val);
                    action.setParam(paramKey, num);
                } else {
                    action.setParam(paramKey, val);
                }
            } catch (Exception e) {
                player.sendMessage(TextUtil.info(Messages.skillText("using-default", "Using default value.")));
            }
            skill.addAction(action);
            player.sendMessage(TextUtil.success(Messages.skillAdded(action.getType().getDisplay())));
            saveAndReopen();
        }, this::reopen);
    }

    /**
     * When clicking an existing action in the list → allows editing the main value.
     */
    private void editActionMainValue(SkillAction action) {
        switch (action.getType()) {
            case MESSAGE, ACTION_BAR -> promptForActionParam(action, "text", Messages.skillPrompt("edit-content", "Enter new content:"), Messages.skillUpdated("content"));
            case PLAY_SOUND -> promptForActionParam(action, "sound", Messages.skillPrompt("edit-sound", "Enter new sound:"), Messages.skillUpdated("sound"));
            case GIVE_POTION -> promptForActionParam(action, "effect", Messages.skillPrompt("edit-potion-effect", "Enter new effect (e.g. STRENGTH):"), Messages.skillUpdated("potion"));
            case HEAL, DAMAGE, AREA_DAMAGE -> promptForActionParam(action, "amount", Messages.skillPrompt("edit-value", "Enter new value:"), Messages.skillUpdated("value"));
            case SHOOT_PROJECTILE -> promptForActionParam(action, "projectile", Messages.skillPrompt("edit-projectile", "Enter new projectile type:"), Messages.skillUpdated("projectile"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> promptForActionParam(action, "command", Messages.skillPrompt("edit-command", "Enter new command:"), Messages.skillUpdated("command"));
            case LAUNCH -> promptForActionParam(action, "y", Messages.skillPrompt("edit-launch-y", "Enter new Y power:"), Messages.skillUpdated("launch"));
            case TELEPORT -> promptForActionParam(action, "y", Messages.skillPrompt("edit-teleport-y", "Enter new Y offset:"), Messages.skillUpdated("teleport"));
            case CONSUME_DURABILITY -> promptForActionParam(action, "amount", Messages.skillPrompt("edit-durability", "Enter durability cost:"), Messages.skillUpdated("durability"));
            case BLOCK_BREAK -> {
                // Chain: edit range, then immediately edit radius for convenience (3x3 support)
                plugin.getChatInputManager().requestInput(player, "<yellow>" + Messages.skillPrompt("edit-range", "Enter new range (1-10):"), input -> {
                    try {
                        action.setParam("range", Integer.parseInt(input.trim()));
                    } catch (Exception ignored) {}
                    // Now prompt radius right after
                    plugin.getChatInputManager().requestInput(player, "<yellow>" + Messages.skillPrompt("edit-radius", "Enter radius for area (0=single, 1=3x3, 2=5x5...):"), rinput -> {
                        try {
                            action.setParam("radius", Integer.parseInt(rinput.trim()));
                        } catch (Exception ignored) {}
                        player.sendMessage(TextUtil.success(Messages.skillUpdated("range+radius")));
                        saveAndReopen();
                    }, this::reopen);
                }, this::reopen);
            }
            case EXPLOSION -> promptForActionParam(action, "power", Messages.skillPrompt("edit-explosion-power", "Enter new explosion power:"), Messages.skillUpdated("power"));
            case POTION_AREA -> promptForActionParam(action, "effect", Messages.skillPrompt("edit-area-effect", "Enter new effect:"), Messages.skillUpdated("effect"));
            case BLIND, GLOW, LEVITATE, FREEZE -> promptForActionParam(action, "duration", Messages.skillPrompt("edit-duration", "Enter new duration (seconds):"), Messages.skillUpdated("duration"));
            case SPAWN_MOB -> promptForActionParam(action, "entity", Messages.skillPrompt("edit-entity", "Enter new entity type:"), Messages.skillUpdated("entity"));
            default -> {
                player.sendMessage(TextUtil.info(Messages.skillText("edit-defaults-only", "This action mainly uses default values. You can delete and re-add it.")));
            }
        }
    }
}
