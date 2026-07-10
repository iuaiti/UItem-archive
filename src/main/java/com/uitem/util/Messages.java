package com.uitem.util;

import com.uitem.UItemPlugin;
import com.uitem.skill.SkillAction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

/**
 * Centralized message system.
 * All strings live in {@code lang.yml} (English). Users can edit values there;
 * keys are looked up at runtime, falling back to the default value if missing.
 */
public final class Messages {

    private static UItemPlugin plugin;
    private static FileConfiguration lang;
    private static File langFile;

    private Messages() {}

    public static void init(UItemPlugin pluginInstance) {
        plugin = pluginInstance;
        reload();
    }

    public static void reload() {
        if (plugin == null) { lang = null; return; }
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        langFile = file;
        lang = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration getLang() {
        if (lang == null) reload();
        return lang;
    }

    /** Direct lookup by key with default fallback. */
    public static String t(String key, String def) {
        FileConfiguration cfg = getLang();
        if (cfg != null && cfg.contains(key)) return cfg.getString(key, def);
        return def;
    }

    /** Direct lookup by key with default fallback + placeholder replacement. */
    public static String t(String key, String def, String placeholder, String value) {
        return t(key, def).replace(placeholder, value);
    }

    // ==================== Generic ====================
    public static String noPermission() { return t("no-permission", "You don't have permission to use this command!"); }
    public static String playersOnly() { return t("players-only", "This command can only be used by players!"); }
    public static String playerNotFound(String name) { return t("player-not-found", "Player '" + name + "' is not online!", "{player}", name); }
    public static String invalidCommand(String label) { return t("invalid-command", "Invalid command. Use /" + label + " help", "{label}", label); }

    // ==================== Give ====================
    public static String giveUsage() { return t("give-usage", "Usage: /uitem give <id> [player] [amount]"); }
    public static String itemNotFound(String id) { return t("item-not-found", "Item '" + id + "' does not exist!", "{id}", id); }
    public static String gaveItem(String player, int amount, String itemId) {
        return t("gave-item", "Gave {player} x{amount} {item}")
                .replace("{player}", player)
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", itemId);
    }
    public static String receivedItem(int amount, String itemId) {
        return t("received-item", "You received x{amount} {item}")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", itemId);
    }
    public static String cannotGive() { return t("cannot-give", "Cannot give item (may have been blocked by another plugin)."); }
    public static String consoleNeedPlayer() { return t("console-need-player", "Console must specify a player: /uitem give <id> <player> [amount]"); }
    public static String invalidAmount() { return t("invalid-amount", "Invalid amount! Must be an integer from 1 to 64."); }

    // ==================== Reload ====================
    public static String reloaded(int count) { return t("reloaded", "Reloaded UItem! Loaded {count} items.", "{count}", String.valueOf(count)); }

    // ==================== Check ====================
    public static String notHoldingItem() { return t("not-holding-item", "You are not holding any item!"); }
    public static String validUItem() { return t("valid-uitem", "\u2714 Item is a valid UItem!"); }
    public static String notUItem() { return t("not-uitem", "\u2717 Item is NOT a valid UItem!"); }
    public static String missingPDC() { return t("missing-pdc", "Missing PDC tag. This item was not obtained through /uitem give."); }
    public static String useGiveToGet() { return t("use-give-to-get", "Use /uitem give <id> <player> to get properly tagged items."); }

    // ==================== Editor ====================
    public static String noItemsYet() { return t("no-items-yet", "No custom items yet. Use /uitem editor to create one."); }

    // ==================== GUI: Item Editor ====================
    public static String guiEditTitle(String id) { return t("gui-edit-title", "<gradient:#ff6b6b:#ffd93d>Edit: </gradient><white>" + id, "{id}", id); }
    public static String guiEditorTitle() { return t("gui-editor-title", "<gradient:#ff6b6b:#ffd93d><bold>UItem Editor</bold></gradient>"); }
    public static String guiManageItems() { return t("gui-manage-items", "<gray>Manage all custom items"); }
    public static String guiCreateNew() { return t("gui-create-new", "<green><bold>+ Create New Item</bold>"); }
    public static String guiCreateHint() { return t("gui-create-hint", "<gray>Click to create a new custom item"); }
    public static String guiCreatePrompt() { return t("gui-create-prompt", "<yellow>Enter ID for new item <gray>(letters, numbers, underscores only):"); }
    public static String guiCreateHint2() { return t("gui-create-hint2", "<yellow>You will be prompted to enter an ID"); }
    public static String guiSearchButton() { return t("gui-search-btn", "<aqua><bold>Search Item</bold>"); }
    public static String guiSearchHint1() { return t("gui-search-hint1", "<gray>Enter keyword to filter list"); }
    public static String guiSearchHint2() { return t("gui-search-hint2", "<gray>Type 'clear' to clear filter"); }
    public static String guiSearchPrompt() { return t("gui-search-prompt", "<yellow>Enter search keyword (<gray>or 'clear' to clear):"); }

    // ==================== GUI: Material Picker ====================
    public static String guiMaterialTitle() { return t("gui-mat-title", "<gold><bold>Choose Material</bold>"); }
    public static String guiMaterialSearch() { return t("gui-mat-search", "<gold><bold>Search</bold>"); }
    public static String guiMaterialSearchHint() { return t("gui-mat-search-hint", "<gray>Click to search material by name"); }
    public static String guiMaterialSelectHint() { return t("gui-mat-select", "<gray>Click to select this material"); }
    public static String guiMaterialPrompt() { return t("gui-mat-prompt", "<yellow>Enter material name to search (Enter to clear filter):"); }

    // ==================== Item Editor fields ====================
    public static String guiMaterialField() { return t("gui-field-material", "<gold><bold>Material</bold>"); }
    public static String guiMaterialClick() { return t("gui-field-material-click", "<yellow>Click to open material list"); }
    public static String guiDisplayNameField() { return t("gui-field-name", "<gold><bold>Display Name</bold>"); }
    public static String guiDisplayNameClick() { return t("gui-field-name-click", "<yellow>Click to change name"); }
    public static String guiDisplayNameSupport() { return t("gui-field-name-support", "<gray>Supports MiniMessage format"); }
    public static String guiDisplayNameExample() { return t("gui-field-name-example", "<gray>Example: <red>Ruby <gold>Sword"); }
    public static String guiDisplayNamePrompt() { return t("gui-field-name-prompt", "<yellow>Enter display name <gray>(supports MiniMessage):"); }
    public static String renamedTo(String name) { return t("renamed-to", "Renamed to: {name}", "{name}", name); }
    public static String guiLoreField() { return t("gui-field-lore", "<gold><bold>Lore</bold>"); }
    public static String guiLoreClick() { return t("gui-field-lore-click", "<yellow>Click to edit lore"); }
    public static String guiLoreOpen() { return t("gui-field-lore-open", "<gray>Open Lore Editor GUI"); }
    public static String guiEnchantsField() { return t("gui-field-ench", "<gold><bold>Enchantments</bold>"); }
    public static String guiEnchantsClick() { return t("gui-field-ench-click", "<yellow>Click to edit enchantments"); }
    public static String guiFlagsField() { return t("gui-field-flags", "<gold><bold>Item Flags</bold>"); }
    public static String guiFlagsClick() { return t("gui-field-flags-click", "<yellow>Click to edit flags"); }
    public static String guiCmdField() { return t("gui-field-cmd", "<gold><bold>Custom Model Data</bold>"); }
    public static String guiCmdClick() { return t("gui-field-cmd-click", "<yellow>Click to change"); }
    public static String guiCmdHint() { return t("gui-field-cmd-hint", "<gray>Enter number (-1 to disable)"); }
    public static String guiCmdPrompt() { return t("gui-field-cmd-prompt", "<yellow>Enter Custom Model Data number <gray>(-1 to disable):"); }
    public static String guiUnbreakableField() { return t("gui-field-unbreak", "<gold><bold>Unbreakable</bold>"); }
    public static String guiUnbreakableOn() { return t("gui-field-unbreak-on", "<green>ON"); }
    public static String guiUnbreakableOff() { return t("gui-field-unbreak-off", "<red>OFF"); }
    public static String guiUnbreakableToggle() { return t("gui-field-unbreak-toggle", "<yellow>Click to toggle"); }
    public static String guiDurabilityField() { return t("gui-field-dur", "<gold><bold>Durability</bold>"); }
    public static String guiDurabilityClick() { return t("gui-field-dur-click", "<yellow>Click to set max durability"); }
    public static String guiDurabilityHint1() { return t("gui-field-dur-hint1", "<gray>Enter -1 for default material durability"); }
    public static String guiDurabilityHint2() { return t("gui-field-dur-hint2", "<gray>Enter 0 for unbreakable-like (no damage bar)"); }
    public static String guiDurabilityHint3() { return t("gui-field-dur-hint3", "<gray>Or enter any number > 0"); }
    public static String guiDurabilityPrompt() { return t("gui-field-dur-prompt", "<yellow>Enter max durability <gray>(-1 = default, 0 = no damage bar, >0 = custom):"); }
    public static String durabilitySet(String status) { return t("durability-set", "Max durability = {status}", "{status}", status); }
    public static String durabilityDefault() { return t("durability-default", "Default"); }
    public static String durabilityNoBar() { return t("durability-no-bar", "No bar"); }
    public static String guiGlowingField() { return t("gui-field-glow", "<gold><bold>Glowing</bold>"); }
    public static String guiGlowingToggle() { return t("gui-field-glow-toggle", "<yellow>Click to toggle"); }
    public static String guiGlowingHint() { return t("gui-field-glow-hint", "<gray>Add glowing effect to item"); }
    public static String guiAttributesField() { return t("gui-field-attr", "<gold><bold>Attributes</bold>"); }

    // ==================== GUI: Attributes ====================
    public static String guiAttributesClick() { return t("gui-attributes-click", "<yellow>Click to add attribute"); }
    public static String guiAttributesFormat() { return t("gui-attributes-format", "<gray>Format: attribute amount operation slot"); }
    public static String guiAttributesExample() { return t("gui-attributes-example", "<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND"); }
    public static String guiAttributesPrompt() { return t("gui-attributes-prompt", "<yellow>Enter attribute <gray>(format: attribute amount operation slot)\\n<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND\\n<gray>Type 'clear' to remove all attributes:"); }
    public static String guiAttributesCleared() { return t("gui-attributes-cleared", "Removed all attributes!"); }
    public static String guiAttributesError() { return t("gui-attributes-error", "Format: attribute amount operation slot"); }
    public static String guiAttributesAdded(String attr) { return t("gui-attributes-added", "Added attribute {attr}!", "{attr}", attr); }

    // ==================== GUI: Rarity ====================
    public static String guiRarityField() { return t("gui-rarity-field", "<gold><bold>Rarity</bold>"); }
    public static String guiRarityNone() { return t("gui-rarity-none", "None"); }
    public static String guiRarityClick() { return t("gui-rarity-click", "<yellow>Click to choose rarity"); }
    public static String guiRarityHints() { return t("gui-rarity-hints", "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA"); }
    public static String guiRarityListHeader() { return t("gui-rarity-list-header", "<gold>Available rarities:<dark_gray> "); }
    public static String guiRarityPrompt() { return t("gui-rarity-prompt", "<yellow>Enter rarity name (or 'none' to remove):"); }
    public static String guiRarityRemoved() { return t("gui-rarity-removed", "Removed rarity!"); }
    public static String guiRaritySet(String rarity) { return t("gui-rarity-set", "Set rarity = {rarity}", "{rarity}", rarity); }
    public static String guiRarityNotFound(String rarity) { return t("gui-rarity-not-found", "Rarity '{rarity}' does not exist!", "{rarity}", rarity); }

    // ==================== GUI: ExecutableItems ====================
    public static String guiEIIdField() { return t("gui-eiid-field", "<gold><bold>\u26a1 ExecutableItems</bold>"); }
    public static String guiEIIdCurrent() { return t("gui-eiid-current", "None"); }
    public static String guiEIIdClick() { return t("gui-eiid-click", "<yellow>Click to set ExecutableItems ID"); }
    public static String guiEIIdHint() { return t("gui-eiid-hint", "<gray>Skills/Passives will be handled by EI"); }
    public static String guiEIIdPrompt() { return t("gui-eiid-prompt", "<yellow>Enter ExecutableItems item ID <gray>(or 'none' to remove):"); }

    // ==================== GUI: Oraxen ====================
    public static String guiOraxenField() { return t("gui-oraxen-field", "<aqua><bold>Oraxen ID</bold>"); }
    public static String guiOraxenCurrent() { return t("gui-oraxen-current", "None"); }
    public static String guiOraxenClick() { return t("gui-oraxen-click", "<yellow>Click to set Oraxen item ID"); }
    public static String guiOraxenHint() { return t("gui-oraxen-hint", "<gray>Use item from Oraxen as base"); }
    public static String guiOraxenPrompt() { return t("gui-oraxen-prompt", "<yellow>Enter Oraxen item ID <gray>(or 'none' to remove):"); }

    // ==================== GUI: ItemsAdder ====================
    public static String guiIaField() { return t("gui-ia-field", "<green><bold>ItemsAdder ID</bold>"); }
    public static String guiIaCurrent() { return t("gui-ia-current", "None"); }
    public static String guiIaClick() { return t("gui-ia-click", "<yellow>Click to set ItemsAdder item ID"); }
    public static String guiIaHint() { return t("gui-ia-hint", "<gray>Use item from ItemsAdder as base"); }
    public static String guiIaPrompt() { return t("gui-ia-prompt", "<yellow>Enter ItemsAdder namespaced ID <gray>(or 'none' to remove):"); }

    // ==================== GUI: Recipe ====================
    public static String guiRecipeField() { return t("gui-recipe-field", "<gold><bold>Crafting Recipe</bold>"); }
    public static String guiRecipeHas() { return t("gui-recipe-has", "<green>Has recipe"); }
    public static String guiRecipeNone() { return t("gui-recipe-none", "<red>No recipe"); }
    public static String guiRecipeClick() { return t("gui-recipe-click", "<yellow>Click to edit recipe"); }
    public static String guiRecipeHint() { return t("gui-recipe-hint", "<gray>Set shaped/shapeless crafting recipe"); }

    // ==================== GUI: Save / Delete Buttons ====================
    public static String guiSaveButton() { return t("gui-save-button", "<green><bold>💾 SAVE</bold>"); }
    public static String guiSaveDesc() { return t("gui-save-desc", "<gray>Save all changes to file"); }
    public static String guiSaveClick() { return t("gui-save-click", "<yellow>Click to save"); }
    public static String guiDeleteButton() { return t("gui-delete-button", "<red><bold>🗑 DELETE</bold>"); }
    public static String guiDeleteDesc() { return t("gui-delete-desc", "<gray>Delete this item"); }
    public static String guiDeleteWarn() { return t("gui-delete-warn", "<red>\u26a0 Click to delete"); }

    // ==================== Item CRUD Messages ====================
    public static String savedItem(String id) { return t("saved-item", "Saved item '{id}'!", "{id}", id); }
    public static String invalidId() { return t("invalid-id", "Invalid ID!"); }
    public static String itemAlreadyExists(String id) { return t("item-already-exists", "Item '{id}' already exists!", "{id}", id); }
    public static String cannotCreateBlocked(String id) { return t("cannot-create-blocked", "Cannot create item '{id}' (blocked by another plugin).", "{id}", id); }
    public static String createdItem(String id) { return t("created-item", "Created item '{id}'!", "{id}", id); }
    public static String cannotDeleteBlocked(String id) { return t("cannot-delete-blocked", "Cannot delete item '{id}' (blocked by another plugin).", "{id}", id); }
    public static String deletedItem(String id) { return t("deleted-item", "Deleted item '{id}'!", "{id}", id); }

    // ==================== Confirm Delete GUI ====================
    public static String confirmDeleteTitle() { return t("confirm-delete-title", "<red><bold>Confirm Delete Item"); }
    public static String confirmDeleteDesc() { return t("confirm-delete-desc", "Delete item: "); }
    public static String confirmDeleteWarn() { return t("confirm-delete-warn", "<red>\u26a0 This action cannot be undone!"); }
    public static String confirmDeleteYes() { return t("confirm-delete-yes", "<green><bold>CONFIRM DELETE"); }
    public static String confirmDeleteNo() { return t("confirm-delete-no", "<red><bold>CANCEL"); }
    public static String confirmDeleteBack() { return t("confirm-delete-back", "<gray>Back to item list"); }

    // ==================== Navigation ====================
    public static String guiBack() { return t("gui-back", "<yellow>\u2190 Back"); }
    public static String guiBackDesc() { return t("gui-back-desc", "<gray>Click to go back"); }
    public static String guiPrevious() { return t("gui-previous", "<yellow>\u25c4 Previous page"); }
    public static String guiNext() { return t("gui-next", "<yellow>Next page \u25ba"); }

    // ==================== Lore Editor ====================
    public static String loreEditorTitle(String id) { return t("lore-editor-title", "<gold><bold>Lore Editor</bold> <dark_gray>- <white>{id}", "{id}", id); }
    public static String loreEditorHeader() { return t("lore-editor-header", "<gold><bold>Lore Editor</bold>"); }
    public static String loreEditorDesc() { return t("lore-editor-desc", "<gray>Edit item description"); }
    public static String loreEditorLeftClick() { return t("lore-editor-left", "<yellow>Left click: <white>Edit line"); }
    public static String loreEditorRightClick() { return t("lore-editor-right", "<yellow>Right click: <white>Delete line"); }
    public static String loreEditorShiftClick() { return t("lore-editor-shift", "<yellow>Shift+Click: <white>Insert blank line above"); }
    public static String loreEditorLine(int line) { return t("lore-editor-line", "Line {line}", "{line}", String.valueOf(line)); }
    public static String loreEditorContent() { return t("lore-editor-content", "<gray>Content:"); }
    public static String loreEditorEmpty() { return t("lore-editor-empty", "<dark_gray>(empty)"); }
    public static String loreEditorEdit() { return t("lore-editor-edit", "<yellow>Left click: <white>Edit"); }
    public static String loreEditorDelete() { return t("lore-editor-delete", "<red>Right click: <white>Delete"); }
    public static String loreEditorInsert() { return t("lore-editor-insert", "<aqua>Shift+Click: <white>Insert blank line"); }
    public static String loreEditorPrompt(int line) { return t("lore-editor-prompt", "<yellow>Enter new content for line {line} <gray>(supports MiniMessage):", "{line}", String.valueOf(line)); }
    public static String loreEditorAddButton() { return t("lore-editor-add-btn", "<green><bold>+ Add new line</bold>"); }
    public static String loreEditorAddDesc() { return t("lore-editor-add-desc", "<gray>Add a new line at the end of lore"); }
    public static String loreEditorAddPrompt() { return t("lore-editor-add-prompt", "<yellow>Enter new line content <gray>(supports MiniMessage):"); }
    public static String loreEditorClearButton() { return t("lore-editor-clear-btn", "<red><bold>Clear all lore</bold>"); }
    public static String loreEditorClearDesc() { return t("lore-editor-clear-desc", "<gray>Delete all lore lines"); }

    // ==================== Enchantment Editor ====================
    public static String enchEditorTitle(String id) { return t("ench-editor-title", "<light_purple><bold>Enchantments</bold> <dark_gray>- <white>{id}", "{id}", id); }
    public static String enchEditorHeader() { return t("ench-editor-header", "<light_purple><bold>Enchantment Editor</bold>"); }
    public static String enchEditorDesc() { return t("ench-editor-desc", "<gray>Edit enchantments for this item"); }
    public static String enchEditorLeft() { return t("ench-editor-left", "<yellow>Left click: <white>Increase level"); }
    public static String enchEditorRight() { return t("ench-editor-right", "<yellow>Right click: <white>Decrease level"); }
    public static String enchEditorShift() { return t("ench-editor-shift", "<yellow>Shift+Click: <white>Remove entirely"); }
    public static String enchEditorKey(String key) { return t("ench-editor-key", "Key: {key}", "{key}", key); }
    public static String enchEditorAdded(int level) { return t("ench-editor-added", "\u2714 Added - Level {level}", "{level}", String.valueOf(level)); }
    public static String enchEditorNotAdded() { return t("ench-editor-not-added", "\u2718 Not added"); }

    // ==================== Flag Editor ====================
    public static String flagEditorTitle(String id) { return t("flag-editor-title", "<blue><bold>Item Flags</bold> <dark_gray>- <white>{id}", "{id}", id); }
    public static String flagEditorOn() { return t("flag-editor-on", "<green>\u2714 ON"); }
    public static String flagEditorOff() { return t("flag-editor-off", "<red>\u2718 OFF"); }
    public static String flagEditorToggle() { return t("flag-editor-toggle", "<yellow>Click to toggle"); }

    // ==================== Recipe Editor ====================
    public static String recipeEditorTitle(String id) { return t("recipe-editor-title", "<gold>Recipe: <white>{id}", "{id}", id); }
    public static String recipeEditorGuide() { return t("recipe-editor-guide", "<gold><bold>Guide</bold>"); }
    public static String recipeEditorGuide1() { return t("recipe-editor-guide1", "<gray>Place ingredients in the left slots"); }
    public static String recipeEditorGuide2() { return t("recipe-editor-guide2", "<gray>Result is this item"); }
    public static String recipeEditorGuide3() { return t("recipe-editor-guide3", "<green>Shaped: <gray>save with shape pattern"); }
    public static String recipeEditorGuide4() { return t("recipe-editor-guide4", "<aqua>Shapeless: <gray>save without shape pattern"); }
    public static String recipeEditorSaveShaped() { return t("recipe-editor-save-shaped", "<green><bold>Save Shaped</bold>"); }
    public static String recipeEditorSaveShapedDesc() { return t("recipe-editor-save-shaped-desc", "<gray>Save shaped recipe (3x3)"); }
    public static String recipeEditorSaveShapeless() { return t("recipe-editor-save-shapeless", "<aqua><bold>Save Shapeless</bold>"); }
    public static String recipeEditorSaveShapelessDesc() { return t("recipe-editor-save-shapeless-desc", "<gray>Save shapeless recipe"); }
    public static String recipeEditorDelete() { return t("recipe-editor-delete", "<red><bold>Delete Recipe</bold>"); }
    public static String recipeEditorDeleteDesc() { return t("recipe-editor-delete-desc", "<gray>Delete current recipe"); }
    public static String recipeEditorHasRecipe() { return t("recipe-editor-has-recipe", "<green><bold>Has recipe</bold>"); }
    public static String recipeEditorTypeShaped() { return t("recipe-editor-type-shaped", "Shaped"); }
    public static String recipeEditorTypeShapeless() { return t("recipe-editor-type-shapeless", "Shapeless"); }
    public static String recipeEditorAmount(int amount) { return t("recipe-editor-amount", "Amount: {amount}", "{amount}", String.valueOf(amount)); }
    public static String recipeEditorEmptySlots() { return t("recipe-editor-empty-slots", "Please place ingredients in the crafting slots!"); }
    public static String recipeEditorSaved(boolean shaped, String id) {
        String type = shaped ? "shaped" : "shapeless";
        return t("recipe-editor-saved", "Saved recipe {type} for '{id}'!")
                .replace("{type}", type)
                .replace("{id}", id);
    }
    public static String recipeEditorDeleted(String id) { return t("recipe-editor-deleted", "Deleted recipe for '{id}'!", "{id}", id); }

    // ==================== Recipe List ====================
    public static String recipeListTitle() { return t("recipe-list-title", "<gradient:#ff6b6b:#ffd93d>Recipe List</gradient>"); }
    public static String recipeListHeader() { return t("recipe-list-header", "<gradient:#ff6b6b:#ffd93d><bold>Recipe List</bold></gradient>"); }
    public static String recipeListClick() { return t("recipe-list-click", "<gray>Click a recipe to view details"); }
    public static String recipeListCraft() { return t("recipe-list-craft", "<green><bold>Craft</bold>"); }
    public static String recipeListCraftDesc() { return t("recipe-list-craft-desc", "<gray>Open crafting table"); }
    public static String recipeListDeletedItem() { return t("recipe-list-deleted-item", "<gray>Item deleted"); }

    // ==================== Recipe Detail ====================
    public static String recipeDetailTitle(String id) { return t("recipe-detail-title", "<gold>Recipe: <white>{id}", "{id}", id); }
    public static String recipeDetailNotFound() { return t("recipe-detail-not-found", "<red>Not found"); }

    // ==================== Crafting GUI ====================
    public static String craftAmount1() { return t("craft-amount-1", "<gray>Craft 1 item"); }
    public static String craftAmount8() { return t("craft-amount-8", "<gray>Craft 8 items"); }
    public static String craftAmount64() { return t("craft-amount-64", "<gray>Craft 64 items"); }
    public static String craftAmountMax() { return t("craft-amount-max", "<gray>Craft as many as possible"); }
    public static String craftSelected() { return t("craft-selected", "<green>\u2714 Selected"); }
    public static String craftClickToSelect() { return t("craft-click-to-select", "<yellow>Click to select"); }

    // ==================== Chat Input ====================
    public static String chatCancel() { return t("chat-cancel", "cancel"); }
    public static String chatCancelHint() { return t("chat-cancel-hint", "Type 'cancel' to cancel"); }
    public static String invalidNumber() { return t("invalid-number", "Invalid number!"); }
    public static String materialSet(String material) { return t("material-set", "Material set to: {material}", "{material}", material); }

    // ==================== Search ====================
    public static String searchNoPermission() { return t("search-no-permission", "You don't have permission to use search (uitem.search)."); }

    // ==================== Skill System ====================
    public static String skillListTitle(String id) { return t("skill-list-title", "<light_purple><bold>Skills & Passives</bold> <dark_gray>- {id}", "{id}", id); }
    public static String skillListHeader(String id) { return t("skill-list-header", "<light_purple><bold>Skills & Passives for {id}</bold>", "{id}", id); }
    public static String skillListDesc() { return t("skill-list-desc", "<gray>Native UItem skill system (no ExecutableItems needed)"); }
    public static String skillListAdd() { return t("skill-list-add", "<green><bold>+ ADD NEW SKILL</bold>"); }
    public static String skillListAddDesc() { return t("skill-list-add-desc", "<gray>Create active or passive skill"); }
    public static String skillListClickEdit() { return t("skill-list-click-edit", "<yellow>Left click: Edit"); }
    public static String skillListClickDelete() { return t("skill-list-click-delete", "<red>Right click: Delete"); }
    public static String skillListFolia() { return t("skill-list-folia", "<yellow>Easy & Stable • Folia-safe"); }
    public static String skillListEmpty() { return t("skill-list-empty", "<gray><italic>No skills yet</italic>"); }
    public static String skillListEmptyHint() { return t("skill-list-empty-hint", "<yellow>Click button below to add new skill"); }
    public static String skillListAddClick() { return t("skill-list-add-click", "<yellow>Click to create"); }

    public static String skillEditorTitle(String skillId) { return t("skill-editor-title", "<light_purple><bold>Edit Skill:</bold> <white>{id}", "{id}", skillId); }
    public static String skillEditorActiveTriggers() { return t("skill-editor-active-triggers", "<gold><bold>Active Triggers</bold>"); }
    public static String skillEditorPassiveTriggers() { return t("skill-editor-passive-triggers", "<gold><bold>Passive / Combat Triggers</bold>"); }
    public static String skillEditorCooldown() { return t("skill-editor-cooldown", "<gold><bold>Cooldown: <white>{seconds}s</bold>", "{seconds}", "%s"); }
    public static String skillEditorCooldownHint() { return t("skill-editor-cooldown-hint", "<yellow>Click to enter cooldown in seconds"); }
    public static String skillEditorChance() { return t("skill-editor-chance", "<gold><bold>Chance: <white>{percent}%</bold>", "{percent}", "%s"); }
    public static String skillEditorCancelEvent() { return t("skill-editor-cancel", "<gold><bold>Cancel Event: <white>{state}</bold>", "{state}", "%s"); }
    public static String skillEditorActionsHeader(int count) { return t("skill-editor-actions", "<gold><bold>Actions ({count})</bold>", "{count}", String.valueOf(count)); }
    public static String skillEditorActionsHint() { return t("skill-editor-actions-hint", "<gray>Left click: Edit value | Right click: Delete"); }
    public static String skillEditorAddAction() { return t("skill-editor-add-action", "<green><bold>+ ADD ACTION</bold>"); }
    public static String skillEditorChooseAction() { return t("skill-editor-choose-action", "<green><bold>CHOOSE ACTION TYPE (Click to add)</bold>"); }
    public static String skillEditorNoActions() { return t("skill-editor-no-actions", "<gray><italic>No actions yet</italic>"); }

    public static String skillEditorBrowseActions() { return t("skill-editor-browse-actions", "Browse All Actions"); }

    public static String skillEditorBrowseActionsDesc() { return t("skill-editor-browse-actions-desc", "<gray>Open the full action picker with all action types"); }

    public static String skillEditorTriggerField() { return t("skill-editor-trigger-field", "<gold><bold>⚡ Trigger</bold>"); }

    public static String skillEditorHeaderTitle(String id, String triggerDisplay) {
        return t("skill-editor-header-title", "<light_purple><bold>{id}</bold> <dark_gray>- {trigger}")
                .replace("{id}", id).replace("{trigger}", triggerDisplay);
    }
    public static String skillEditorHeaderCooldown(double seconds) { return t("skill-editor-header-cooldown", "<gray>Cooldown: <white>{seconds}s", "{seconds}", String.valueOf(seconds)); }
    public static String skillEditorHeaderChance(int percent) { return t("skill-editor-header-chance", "<gray>Chance: <white>{percent}%", "{percent}", String.valueOf(percent)); }
    public static String skillEditorHeaderActions(int count) { return t("skill-editor-header-actions", "<gray>Actions: <white>{count}", "{count}", String.valueOf(count)); }
    public static String skillEditorChanceHint() { return t("skill-editor-chance-hint", "<gray>% trigger chance (useful for passive)"); }
    public static String skillEditorCancelHint() { return t("skill-editor-cancel-hint", "<gray>Only affects active triggers (click/drop)"); }
    public static String skillEditorCancelClick() { return t("skill-editor-cancel-click", "<yellow>Click to toggle"); }
    public static String stateOn() { return t("state-on", "ON"); }
    public static String stateOff() { return t("state-off", "OFF"); }
    public static String skillEditorCondChance75() { return t("skill-editor-cond-chance-75", "<green>Cond: Chance 75%"); }
    public static String skillEditorCondChance75Desc() { return t("skill-editor-cond-chance-75-desc", "<gray>Click to add 75% chance"); }
    public static String skillEditorCondChance50() { return t("skill-editor-cond-chance-50", "<red>Cond: Chance 50%"); }
    public static String skillEditorCondChance50Desc() { return t("skill-editor-cond-chance-50-desc", "<gray>Click to add 50% chance"); }
    public static String skillEditorParticlePicker() { return t("skill-editor-particle-picker", "<gold>✦ Particle Picker"); }
    public static String skillEditorParticlePickerDesc() { return t("skill-editor-particle-picker-desc", "<gray>Click to browse all particles and configure them"); }
    public static String skillEditorPotionPresets() { return t("skill-editor-potion-presets", "<light_purple>⚡ Potion Presets"); }
    public static String skillEditorPotionPresetsDesc() { return t("skill-editor-potion-presets-desc", "<gray>Click to view the potion list"); }
    public static String skillEditorPotionCombos() { return t("skill-editor-potion-combos", "<light_purple>🍶 Potion Combos"); }
    public static String skillEditorPotionCombosDesc() { return t("skill-editor-potion-combos-desc", "<gray>Click to view preset combos"); }
    public static String skillEditorPageNext(int page) { return t("skill-editor-page-next", "<yellow>Page {page} ►", "{page}", String.valueOf(page)); }
    public static String skillEditorPageNextDesc() { return t("skill-editor-page-next-desc", "<gray>Click to view more actions"); }
    public static String skillEditorPageFirst() { return t("skill-editor-page-first", "<yellow>◄ Page 1"); }
    public static String skillEditorPageFirstDesc() { return t("skill-editor-page-first-desc", "<gray>Back to first page"); }
    public static String skillEditorRenameButton() { return t("skill-editor-rename-button", "<gold><bold>✎ Rename Skill</bold>"); }
    public static String skillEditorRenameHint() { return t("skill-editor-rename-hint", "<yellow>Click to rename this skill's ID"); }
    public static String skillEditorRenamePrompt() { return t("skill-editor-rename-prompt", "Enter new skill ID (letters/numbers/underscore):"); }
    public static String skillEditorRenameInvalid() { return t("skill-editor-rename-invalid", "Invalid skill ID."); }
    public static String skillEditorRenameDuplicate(String id) { return t("skill-editor-rename-duplicate", "A skill with ID '{id}' already exists on this item.", "{id}", id); }
    public static String skillEditorRenamed(String id) { return t("skill-editor-renamed", "Skill renamed to '{id}'.", "{id}", id); }
    public static String skillListShiftDelete() { return t("skill-list-shift-delete", "<red>Shift + Right-click to delete"); }
    public static String skillLabel(String key, String def) { return t("skill-label-" + key, def); }
    public static String skillPrompt(String key, String def) { return t("skill-prompt-" + key, def); }
    public static String skillText(String key, String def) { return t("skill-text-" + key, def); }
    public static String skillEditorTriggerClick() { return t("skill-editor-trigger-click", "<yellow>Left-click: Next  <gray>|  <yellow>Right-click: Previous"); }

    // Skill trigger display names (used in GUI + lore)
    public static String skillTriggerDisplayName(String key) {
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "RIGHT_CLICK" -> t("trigger-right", "Right Click");
            case "LEFT_CLICK" -> t("trigger-left", "Left Click");
            case "SHIFT_RIGHT_CLICK" -> t("trigger-sright", "Shift + Right Click");
            case "SHIFT_LEFT_CLICK" -> t("trigger-sleft", "Shift + Left Click");
            case "SNEAK" -> t("trigger-sneak", "Sneak");
            case "DOUBLE_SNEAK" -> t("trigger-double-sneak", "Double Sneak");
            case "DROP" -> t("trigger-drop", "Drop (Q)");
            case "SWAP_HAND" -> t("trigger-swap", "Swap Hand (F)");
            case "THROW" -> t("trigger-throw", "Throw");
            case "SHOOT" -> t("trigger-shoot", "Shoot / Throw (Bow, Crossbow, Trident)");
            case "HIT" -> t("trigger-hit", "On Hit");
            case "TAKE_DAMAGE" -> t("trigger-damage", "On Damaged");
            case "KILL" -> t("trigger-kill", "On Kill");
            case "EQUIP" -> t("trigger-equip", "On Equip");
            case "UNEQUIP" -> t("trigger-unequip", "On Unequip");
            case "PERIODIC" -> t("trigger-periodic", "Periodic");
            case "WHILE_HOLDING" -> t("trigger-holding", "While Holding");
            case "WHILE_WEARING" -> t("trigger-wearing", "While Wearing");
            default -> key;
        };
    }

    public static String skillTriggerDescription(String key) {
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "RIGHT_CLICK" -> t("trigger-desc-right", "Activate on right click");
            case "LEFT_CLICK" -> t("trigger-desc-left", "Activate on left click");
            case "SHIFT_RIGHT_CLICK" -> t("trigger-desc-sright", "Activate on shift + right click");
            case "SHIFT_LEFT_CLICK" -> t("trigger-desc-sleft", "Activate on shift + left click");
            case "SNEAK" -> t("trigger-desc-sneak", "Activate when the player starts sneaking");
            case "DOUBLE_SNEAK" -> t("trigger-desc-double-sneak", "Activate when the player sneaks twice quickly");
            case "DROP" -> t("trigger-desc-drop", "Activate when dropping the item (Q)");
            case "SWAP_HAND" -> t("trigger-desc-swap", "Activate when swapping hands (F)");
            case "THROW" -> t("trigger-desc-throw", "Activate when throwing the item");
            case "SHOOT" -> t("trigger-desc-shoot", "Activate when shooting with bow/crossbow or throwing trident");
            case "HIT" -> t("trigger-desc-hit", "Activate when hitting a target");
            case "TAKE_DAMAGE" -> t("trigger-desc-damage", "Activate when taking damage");
            case "KILL" -> t("trigger-desc-kill", "Activate when killing with this item");
            case "EQUIP" -> t("trigger-desc-equip", "Activate when equipping or holding");
            case "UNEQUIP" -> t("trigger-desc-unequip", "Activate when unequipping");
            case "PERIODIC" -> t("trigger-desc-periodic", "Repeats every period (for regen, aura...)");
            case "WHILE_HOLDING" -> t("trigger-desc-holding", "Repeats while holding in hand");
            case "WHILE_WEARING" -> t("trigger-desc-wearing", "Repeats while wearing as armor");
            default -> "";
        };
    }

    // Action type display (for buttons)
    public static String skillActionDisplay(SkillAction.ActionType type) {
        String key = "action-" + type.name().toLowerCase(Locale.ROOT);
        String def = type.getDisplay();
        return t(key, def);
    }

    // Feedback messages
    public static String skillAdded(String type) { return t("skill-added", "Added {type}", "{type}", type); }
    public static String skillUpdated(String type) { return t("skill-updated", "Updated {type}", "{type}", type); }
    // ==================== Help ====================
    public static String helpTitle() { return t("help-title", "  <gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <gray>\u2014 Ultimate Item Plugin"); }
    public static String helpGive() { return t("help-give", "  <gold>/uitem give <id> [player] [amount] <dark_gray>\u2014 <gray>Give item"); }
    public static String helpEditor() { return t("help-editor", "  <gold>/uitem editor <dark_gray>\u2014 <gray>Open editor GUI"); }
    public static String helpList() { return t("help-list", "  <gold>/uitem list <dark_gray>\u2014 <gray>List items"); }
    public static String helpCheck() { return t("help-check", "  <gold>/uitem check <dark_gray>\u2014 <gray>Check if held item is a valid UItem"); }
    public static String helpCraft() { return t("help-craft", "  <gold>/uitem craft <dark_gray>\u2014 <gray>Open UItem crafting table"); }
    public static String helpCraftingCmd() { return t("help-crafting-cmd", "  <gold>/crafting <dark_gray>\u2014 <gray>List all crafting recipes"); }
    public static String helpCraftingTable() { return t("help-crafting-table", "  <gold>/crafting craft <dark_gray>\u2014 <gray>Open UItem crafting table"); }
    public static String helpReload() { return t("help-reload", "  <gold>/uitem reload <dark_gray>\u2014 <gray>Reload plugin"); }
    public static String helpHelp() { return t("help-help", "  <gold>/uitem help <dark_gray>\u2014 <gray>Show help"); }
    public static String helpUi() { return t("help-ui", "  <gold>/ui <dark_gray>\u2014 <gray>Short alias for /uitem"); }
}
