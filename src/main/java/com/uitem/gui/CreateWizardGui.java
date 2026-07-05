package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.provider.BrowseableProvider;
import com.uitem.provider.ItemProvider;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Wizard for creating a new UItem in 3 steps + editor:
 *  1) Choose a Category
 *  2) Choose a Provider (only shows available ones + "Item in hand")
 *  3) Choose the target item: vanilla material list / browseable list / item in hand
 *     then the item is auto-created and the full {@link ItemEditorGui} opens for configuration.
 *
 * The "unusual" constructor that accepts an existing {@code UItemData} is used when changing
 * the Source of an existing item. The wizard runs in a shortened form: provider + target.
 */
public class CreateWizardGui extends AbstractGui {

    /** If != null, used to change the source of an existing item. Wizard runs shortened: provider + target. */
    private final UItemData existing;
    /** State between steps. */
    private final WizardState state = new WizardState();

    public CreateWizardGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Create New UItem</bold></gradient>");
        this.existing = null;
    }

    /** Wizard for changing the source of an existing item (opened from ItemEditorGui). */
    public CreateWizardGui(UItemPlugin plugin, Player player, UItemData existing) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Change Source for " + existing.getId() + "</bold></gradient>");
        this.existing = existing;
        // Pre-fill the category info so the user can see it, but the user can re-pick it at step 1 (optional).
        state.category = existing.getCategory();
    }

    @Override
    protected void setup() {
        // Entry screen - press "Start".
        fillBorder(6);
        setItem(22, createItem(Material.NETHER_STAR,
                "<gradient:#ff6b6b:#ffd93d><bold>Click to start the wizard</bold></gradient>",
                List.of(
                        "<gray>3 steps to create a UItem:",
                        "<yellow>1. <gray>Choose a Category",
                        "<yellow>2. <gray>Choose a Provider (source)",
                        "<yellow>3. <gray>Choose target item / item in hand",
                        "<yellow>→ <green>Then edit UItem in the editor"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            // Start: if changing source for an existing item -> skip category.
            if (existing != null) startStep2();
            else startStep1();
        });
        setItem(45, createBackButton(), ev -> goBack());
        if (existing != null) startStep2(); // auto-start for an existing item
    }

    // -- Step 1: Category -----------------------------------------------
    private void startStep1() {
        new CategoryPickerGui(plugin, player, state.category, picked -> {
            state.category = picked;
            startStep2();
        }).openAsChildOf(this);
    }

    // -- Step 2: Provider ------------------------------------------------
    private void startStep2() {
        new ProviderPickerGui(plugin, player, this::onProviderPicked, this::onPickFromHand).openAsChildOf(this);
    }

    private void onProviderPicked(ItemProvider p) {
        state.provider = p;
        if ("vanilla".equals(p.getNamespace())) {
            // Vanilla uses the dedicated Material list picker.
            new MaterialPickerGui(plugin, player, this::onTargetPicked).openAsChildOf(this);
        } else if (p instanceof BrowseableProvider bp) {
            new BrowseableItemPickerGui(plugin, player, bp, this::onTargetPicked).openAsChildOf(this);
        } else {
            player.sendMessage(TextUtil.error("This provider does not support browsing - please use 'Item in hand'."));
            new ProviderPickerGui(plugin, player, this::onProviderPicked, this::onPickFromHand).openAsChildOf(this);
        }
    }

    private void onPickFromHand() {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(TextUtil.error("You are not holding any item!"));
            new ProviderPickerGui(plugin, player, this::onProviderPicked, this::onPickFromHand).openAsChildOf(this);
            return;
        }
        String source = plugin.getProviderManager().detectSource(hand);
        if (source == null) {
            player.sendMessage(TextUtil.error("Could not detect which plugin created this item."));
            new ProviderPickerGui(plugin, player, this::onProviderPicked, this::onPickFromHand).openAsChildOf(this);
            return;
        }
        player.sendMessage(TextUtil.success("Detected: <white>" + source));
        state.source = source;
        onTargetPicked(source);
    }

    private void onTargetPicked(String source) {
        state.source = source;
        if (existing != null) {
            // Shortened - just re-handle the source + save.
            existing.setSource(source);
            plugin.getItemManager().registerItem(existing);
            plugin.getStorageManager().saveItem(plugin.getItemManager(), existing);
            player.sendMessage(TextUtil.success("Changed source of '" + existing.getId() + "' to " + source));
            new ItemEditorGui(plugin, player, existing).open();
            return;
        }
        // Generate a default ID from source, create the item, and open the full editor directly.
        String defaultId = generateDefaultId(source);
        if (plugin.getItemManager().hasItem(defaultId)) {
            int n = 1;
            while (plugin.getItemManager().hasItem(defaultId + "_" + n)) n++;
            defaultId = defaultId + "_" + n;
        }
        UItemData data = new UItemData(defaultId);
        data.setSource(source);
        data.setCategory(state.category);
        data.setDisplayName("<white>" + prettify(defaultId));
        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
        player.sendMessage(TextUtil.success("Created UItem '" + data.getId() + "'! Now configure it in the editor."));
        new ItemEditorGui(plugin, player, data).open();
    }

    /** State shared between the steps of the wizard. */
    public static class WizardState {
        public String category;
        public String subCategory;
        public ItemProvider provider;
        public String source; // "namespace:id"
    }

    private String generateDefaultId(String source) {
        // Extract the item name from "namespace:id"
        int idx = source.indexOf(':');
        String part = idx < 0 ? source : source.substring(idx + 1);
        // Clean: only lowercase letters, digits, underscores
        String clean = part.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (clean.isEmpty()) return "custom_item";
        if (clean.charAt(0) == '_') clean = "item" + clean;
        return clean;
    }

    private String prettify(String id) {
        String[] parts = id.replace("-", "_").split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
