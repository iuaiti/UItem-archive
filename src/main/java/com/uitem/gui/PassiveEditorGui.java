package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.passive.PassiveData;
import com.uitem.passive.PassiveManager;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PassiveEditorGui extends AbstractGui {

    private final UItemData data;
    private int page;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public PassiveEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        this(plugin, player, data, 0);
    }

    public PassiveEditorGui(UItemPlugin plugin, Player player, UItemData data, int page) {
        super(plugin, player, 54, "<gold><bold>Passive Editor</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.BLAZE_POWDER,
                "<gold><bold>Passive Editor</bold>",
                List.of(
                        "<gray>Manage passive abilities for this item",
                        "<gray>Current: <white>" + data.getPassives().size() + " passive(s)",
                        "",
                        "<yellow>Left click: <white>Edit passive",
                        "<red>Right click: <white>Delete passive",
                        "<aqua>Middle click: <white>Duplicate passive"
                )));

        List<PassiveData> passives = data.getPassives();
        int totalPages = Math.max(1, (int) Math.ceil((double) passives.size() / ITEMS_PER_PAGE));
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = startIndex + i;
            if (idx >= passives.size()) break;
            PassiveData pd = passives.get(idx);
            int finalIdx = idx;

            ItemStack item = buildPassiveItem(pd, finalIdx);
            setItem(ITEM_SLOTS[i], item, event -> {
                if (event.getClick() == ClickType.LEFT) {
                    editPassive(finalIdx);
                } else if (event.getClick() == ClickType.RIGHT) {
                    passives.remove(finalIdx);
                    save();
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    refresh();
                } else if (event.getClick() == ClickType.MIDDLE) {
                    PassiveData clone = pd.copy();
                    passives.add(finalIdx + 1, clone);
                    save();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.3f);
                    refresh();
                }
            });
        }

        setItem(49, createItem(Material.EMERALD,
                "<green><bold>+ Add Passive</bold>",
                List.of("<gray>Click to add a new passive")), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            promptNewPassiveType();
        });

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { page--; refresh(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { page++; refresh(); } : null);

        setItem(45, createBackButton(), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new ItemEditorGui(plugin, player, data).open();
        });
    }

    private ItemStack buildPassiveItem(PassiveData pd, int index) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PassiveManager pm = new PassiveManager(plugin, plugin.getItemManager());
        meta.displayName(TextUtil.parse("<gold><bold>#" + (index + 1) + " " + pd.getType()));

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Trigger: <white>" + pd.getTrigger());
        lore.add("<gray>Cooldown: <white>" + pd.getCooldown() + "s");
        lore.add("");
        lore.add(pm.getPassiveDescription(pd));
        if (pd.getConditions() != null && !pd.getConditions().isEmpty()) {
            lore.add("");
            lore.add("<gold>Conditions:");
            for (Map.Entry<String, Object> e : pd.getConditions().entrySet()) {
                lore.add("<gray>  " + e.getKey() + ": <white>" + e.getValue());
            }
        }
        lore.add("");
        lore.add("<yellow>Left click: Edit");
        lore.add("<red>Right click: Delete");
        lore.add("<aqua>Middle click: Duplicate");
        meta.lore(lore.stream().map(TextUtil::parse).toList());
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private void editPassive(int index) {
        new PassiveDetailGui(plugin, player, data, index).open();
    }

    private void promptNewPassiveType() {
        new PassiveTypeSelectGui(plugin, player, data).open();
    }

    private void save() {
        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveAll(plugin.getItemManager());
    }
}
