package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.provider.BrowseableProvider;
import com.uitem.provider.ItemProvider;
import com.uitem.provider.VanillaProvider;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Wizard for creating a new UItem in 4 steps:
 *  1) Choose a Category
 *  2) Choose a Provider (only shows available ones + "Item in hand")
 *  3) Choose the target item: vanilla material list / browseable list / item in hand
 *  4) Configure the UItem (ID, display, rarity, cooldown, permission, sub-category, stack size)
 *     then register + save + open the ItemEditorGui.
 *
 * The "unusual" constructor that accepts an existing {@code UItemData} is used when changing
 * the Source of an existing item. The wizard runs in a shortened form: provider + target.
 * The callbacks use a chain of {@link WizardState} to share data between steps.
 */
public class CreateWizardGui extends AbstractGui {

    /** Wizard mapping to an ID that does not yet exist (entered by the user at step 4). */
    private final String newItemId;
    /** If != null, used to change the source of an existing item. Wizard runs shortened: provider + target. */
    private final UItemData existing;
    /** State between steps. */
    private final WizardState state = new WizardState();

    public CreateWizardGui(UItemPlugin plugin, Player player, String newItemId) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Create New UItem</bold></gradient>");
        this.newItemId = newItemId;
        this.existing = null;
    }

    /** Wizard for changing the source of an existing item (opened from ItemEditorGui). */
    public CreateWizardGui(UItemPlugin plugin, Player player, UItemData existing) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Change Source for " + existing.getId() + "</bold></gradient>");
        this.newItemId = null;
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
                        "<gray>4 steps to create a UItem:",
                        "<yellow>1. <gray>Choose a Category",
                        "<yellow>2. <gray>Choose a Provider (source)",
                        "<yellow>3. <gray>Choose target item / item in hand",
                        "<yellow>4. <gray>Configure UItem (ID, display, ...)"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            // Start: if changing source for an existing item -> skip category.
            if (existing != null) startStep2();
            else startStep1();
        });
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
        if (p instanceof BrowseableProvider bp) {
            if ("vanilla".equals(p.getNamespace())) {
                // Vanilla uses the Material list directly.
                new MaterialPickerGui(plugin, player, this::onTargetPicked).openAsChildOf(this);
            } else {
                new BrowseableItemPickerGui(plugin, player, bp, this::onTargetPicked).openAsChildOf(this);
            }
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
        startStep4();
    }

    // -- Step 4: Final UItem configuration -------------------------------
    private void startStep4() {
        // Set a default display based on the id (the user can change it later).
        SetupItemStepGui step = new SetupItemStepGui(plugin, player, state, newItemId, data -> {
            // data has been fully populated in SetupItemStepGui.
            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
            player.sendMessage(TextUtil.success("Created UItem '" + data.getId() + "'!"));
            new ItemEditorGui(plugin, player, data).open();
        });
        step.openAsChildOf(this);
    }

    /** State shared between the steps of the wizard. */
    public static class WizardState {
        public String category;
        public String subCategory;
        public ItemProvider provider;
        public String source; // "namespace:id"
    }
}
