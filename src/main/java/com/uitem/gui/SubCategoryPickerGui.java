package com.uitem.gui;

import com.uitem.UItemPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Picker for Sub-Category — depends on the parent Category.
 */
public class SubCategoryPickerGui extends AbstractGui {

    private final String category;
    private final String current;
    private final Consumer<String> callback;

    private static final Map<String, List<String>> SUBS = new LinkedHashMap<>();
    static {
        SUBS.put("WEAPON", List.of("SWORD", "AXE", "DAGGER", "SPEAR", "HAMMER", "STAFF"));
        SUBS.put("ARMOR", List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"));
        SUBS.put("TOOL", List.of("PICKAXE", "AXE", "HOE", "SHOVEL", "FISHING_ROD"));
        SUBS.put("RANGED", List.of("BOW", "CROSSBOW", "TRIDENT"));
        SUBS.put("MAGIC", List.of("WAND", "SCROLL", "TALISMAN"));
        SUBS.put("CONSUMABLE", List.of("FOOD", "POTION", "SCROLL"));
        SUBS.put("MATERIAL", List.of("GEM", "ORE", "CRAFTING"));
        SUBS.put("MISC", List.of());

        // Default icon map for sub cats
    }

    private static final Map<String, Material> ICONS = new LinkedHashMap<>();
    static {
        ICONS.put("SWORD", Material.DIAMOND_SWORD); ICONS.put("AXE", Material.DIAMOND_AXE);
        ICONS.put("DAGGER", Material.IRON_SWORD); ICONS.put("SPEAR", Material.TRIDENT);
        ICONS.put("HAMMER", Material.MACE); ICONS.put("STAFF", Material.BLAZE_ROD);
        ICONS.put("HELMET", Material.DIAMOND_HELMET); ICONS.put("CHESTPLATE", Material.DIAMOND_CHESTPLATE);
        ICONS.put("LEGGINGS", Material.DIAMOND_LEGGINGS); ICONS.put("BOOTS", Material.DIAMOND_BOOTS);
        ICONS.put("PICKAXE", Material.DIAMOND_PICKAXE); ICONS.put("HOE", Material.DIAMOND_HOE);
        ICONS.put("SHOVEL", Material.DIAMOND_SHOVEL); ICONS.put("FISHING_ROD", Material.FISHING_ROD);
        ICONS.put("BOW", Material.BOW); ICONS.put("CROSSBOW", Material.CROSSBOW); ICONS.put("TRIDENT", Material.TRIDENT);
        ICONS.put("WAND", Material.BLAZE_ROD); ICONS.put("SCROLL", Material.PAPER); ICONS.put("TALISMAN", Material.TOTEM_OF_UNDYING);
        ICONS.put("FOOD", Material.APPLE); ICONS.put("POTION", Material.POTION);
        ICONS.put("GEM", Material.EMERALD); ICONS.put("ORE", Material.COAL_ORE); ICONS.put("CRAFTING", Material.CRAFTING_TABLE);
    }

    public SubCategoryPickerGui(UItemPlugin plugin, Player player, String category, String current, Consumer<String> callback) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Choose Sub-Category</bold></gradient>");
        this.category = category;
        this.current = current;
        this.callback = callback;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.NETHER_STAR,
                "<gradient:#ff6b6b:#ffd93d><bold>Sub-Category</bold></gradient>",
                List.of(
                        "Category: " + (category != null ? category : "—"),
                        current != null ? "<green>Current: <white>" + current : ""
                )));

        List<String> subs = category != null ? SUBS.getOrDefault(category, List.of()) : List.of();
        if (subs.isEmpty()) {
            setItem(22, createItem(Material.PAPER,
                    "<gray>This category has no fixed sub-category",
                    List.of("<yellow>You can skip this step")));
        }

        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28};
        for (int i = 0; i < Math.min(subs.size(), slots.length); i++) {
            String sub = subs.get(i);
            boolean active = sub.equalsIgnoreCase(current);
            Material icon = ICONS.getOrDefault(sub, Material.PAPER);
            setItem(slots[i], createItem(icon,
                    (active ? "<green><bold>✔ " : "<aqua>") + prettify(sub),
                    List.of("<yellow>Click to choose")), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                if (callback != null) callback.accept(sub);
            });
        }

        setItem(49, createItem(Material.BARRIER, "<red>Skip <gray>(no sub)"), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            if (callback != null) callback.accept(null);
        });

        setItem(45, createBackButton(), ev -> goBack());
    }

    private String prettify(String s) {
        String[] parts = s.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
