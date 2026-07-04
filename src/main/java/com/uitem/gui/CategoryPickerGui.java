package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Picker for the main Category:
 * Weapon / Armor / Tool / Ranged / Magic / Consumable / Material / Misc.
 */
public class CategoryPickerGui extends AbstractGui {

    private final String current;
    private final Consumer<String> callback;

    public static final List<String> CATEGORIES = List.of(
            "WEAPON", "ARMOR", "TOOL", "RANGED", "MAGIC", "CONSUMABLE", "MATERIAL", "MISC"
    );

    public static final Map<String, Material> ICONS = new LinkedHashMap<>();
    static {
        ICONS.put("WEAPON", Material.DIAMOND_SWORD);
        ICONS.put("ARMOR", Material.DIAMOND_CHESTPLATE);
        ICONS.put("TOOL", Material.DIAMOND_PICKAXE);
        ICONS.put("RANGED", Material.BOW);
        ICONS.put("MAGIC", Material.BLAZE_ROD);
        ICONS.put("CONSUMABLE", Material.GOLDEN_APPLE);
        ICONS.put("MATERIAL", Material.DIAMOND);
        ICONS.put("MISC", Material.CHEST);
    }

    private static final Map<String, String> DESCS = new LinkedHashMap<>();
    static {
        DESCS.put("WEAPON", "Sword, Axe, Dagger, Spear, Hammer, Staff");
        DESCS.put("ARMOR", "Helmet, Chestplate, Leggings, Boots");
        DESCS.put("TOOL", "Pickaxe, Axe, Hoe, Shovel, Fishing Rod");
        DESCS.put("RANGED", "Bow, Crossbow, Trident");
        DESCS.put("MAGIC", "Wand, charm, scroll");
        DESCS.put("CONSUMABLE", "Food, Potion, Scroll");
        DESCS.put("MATERIAL", "Gem, Ore, Crafting material");
        DESCS.put("MISC", "Everything else");
    }

    public CategoryPickerGui(UItemPlugin plugin, Player player, String current, Consumer<String> callback) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Choose Item Category</bold></gradient>");
        this.current = current;
        this.callback = callback;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.NETHER_STAR,
                "<gradient:#ff6b6b:#ffd93d><bold>Choose Category</bold></gradient>",
                List.of(
                        "<gray>Category is only used to manage items",
                        "<gray>Does not affect the item source",
                        current != null ? "<green>Current: <white>" + current : ""
                )));

        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28};
        for (int i = 0; i < CATEGORIES.size(); i++) {
            String cat = CATEGORIES.get(i);
            boolean active = cat.equalsIgnoreCase(current);
            setItem(slots[i], createItem(ICONS.get(cat),
                    (active ? "<green><bold>✔ " : "<aqua>") + cat,
                    List.of(
                            "<gray>" + DESCS.get(cat),
                            "",
                            "<yellow>Click to select"
                    )), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                if (callback != null) callback.accept(cat);
            });
        }

        setItem(49, createBackButton(), ev -> goBack());
    }
}
