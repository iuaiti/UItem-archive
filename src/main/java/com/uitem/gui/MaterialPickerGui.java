package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Picker for Vanilla Material — list with search + pagination.
 * Callback receives source string in the form "vanilla:<material_name>".
 */
public class MaterialPickerGui extends AbstractGui {

    private final Consumer<String> callback;
    private int page;
    private String filter;
    private String category;
    private List<Material> materials;

    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] ITEM_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("swords", List.of("WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "DIAMOND_SWORD", "NETHERITE_SWORD"));
        CATEGORIES.put("tools", List.of(
                "WOODEN_PICKAXE", "STONE_PICKAXE", "IRON_PICKAXE", "GOLDEN_PICKAXE", "DIAMOND_PICKAXE", "NETHERITE_PICKAXE",
                "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLDEN_AXE", "DIAMOND_AXE", "NETHERITE_AXE",
                "WOODEN_SHOVEL", "STONE_SHOVEL", "IRON_SHOVEL", "GOLDEN_SHOVEL", "DIAMOND_SHOVEL", "NETHERITE_SHOVEL",
                "WOODEN_HOE", "STONE_HOE", "IRON_HOE", "GOLDEN_HOE", "DIAMOND_HOE", "NETHERITE_HOE"));
        CATEGORIES.put("armor", List.of(
                "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS",
                "CHAINMAIL_HELMET", "CHAINMAIL_CHESTPLATE", "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS",
                "IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS",
                "GOLDEN_HELMET", "GOLDEN_CHESTPLATE", "GOLDEN_LEGGINGS", "GOLDEN_BOOTS",
                "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS",
                "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS",
                "TURTLE_HELMET"));
        CATEGORIES.put("ranged", List.of("BOW", "CROSSBOW", "TRIDENT", "ARROW", "TIPPED_ARROW", "SPECTRAL_ARROW", "SNOWBALL", "EGG", "FIRE_CHARGE", "WIND_CHARGE"));
        CATEGORIES.put("magic", List.of("BLAZE_ROD", "ENDER_PEARL", "ENDER_EYE", "EXPERIENCE_BOTTLE", "TOMAHAWK", "TOTEM_OF_UNDYING", "GOAT_HORN"));
        CATEGORIES.put("consumable", List.of("APPLE", "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE", "MELON_SLICE", "COOKED_BEEF", "CHORUS_FRUIT", "POTION", "SPLASH_POTION", "LINGERING_POTION", "MILK_BUCKET"));
        CATEGORIES.put("material", List.of("DIAMOND", "DIAMOND_BLOCK", "EMERALD", "EMERALD_BLOCK", "GOLD_INGOT", "IRON_INGOT", "COPPER_INGOT", "NETHERITE_INGOT", "QUARTZ", "AMETHYST_SHARD", "LAPIS_LAZULI", "REDSTONE", "COAL", "CHARCOAL", "ENDER_PEARL"));
        CATEGORIES.put("all", List.of());
        CATEGORIES.put("blocks", List.of());
        CATEGORIES.put("items", List.of());
    }

    public MaterialPickerGui(UItemPlugin plugin, Player player, Consumer<String> callback) {
        this(plugin, player, callback, 0, null, "all");
    }

    public MaterialPickerGui(UItemPlugin plugin, Player player, Consumer<String> callback,
                             int page, String filter, String category) {
        super(plugin, player, 54, "<gold><bold>Choose Vanilla Material</bold>");
        this.callback = callback;
        this.page = page;
        this.filter = filter;
        this.category = category;
        this.materials = loadMaterials();
    }

    private List<Material> loadMaterials() {
        Set<Material> all = new HashSet<>();
        if ("all".equals(category) || "blocks".equals(category)) {
            for (Material m : Material.values()) if (m.isBlock()) all.add(m);
        }
        if ("all".equals(category) || "items".equals(category)) {
            for (Material m : Material.values()) if (!m.isBlock()) all.add(m);
        }
        if (CATEGORIES.containsKey(category) && !CATEGORIES.get(category).isEmpty()) {
            for (String n : CATEGORIES.get(category)) {
                try { all.add(Material.valueOf(n)); } catch (IllegalArgumentException ignored) {}
            }
        }
        List<Material> result = new ArrayList<>(all);
        result.removeIf(m -> !m.isItem());
        result.removeIf(m -> m.name().endsWith("_WALL_HEAD"));
        if (filter != null && !filter.isBlank()) {
            String lower = filter.toLowerCase();
            result.removeIf(m -> !m.name().toLowerCase().contains(lower));
        }
        result.sort(Comparator.comparing(Material::name));
        return result;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.COMPASS,
                "<gold><bold>Choose Material</bold>",
                List.of(
                        "<gray>Category: <white>" + category,
                        "<gray>Total: <white>" + materials.size(),
                        filter != null ? "<gray>Filter: <white>" + filter : ""
                )));

        String[] cats = {"all", "swords", "tools", "armor", "ranged", "magic", "consumable", "material", "blocks", "items"};
        String[] display = {"<white>All", "<red>Swords", "<gold>Tools", "<blue>Armor",
                "<aqua>Ranged", "<light_purple>Magic", "<green>Consumable", "<yellow>Material",
                "<green>Blocks", "<aqua>Items"};
        int[] catSlotsFixed = new int[]{1, 2, 3, 5, 6, 7, 10, 11, 15, 16};
        for (int i = 0; i < Math.min(cats.length, catSlotsFixed.length); i++) {
            String cat = cats[i];
            boolean active = category.equals(cat);
            String finalCat = cat;
            setItem(catSlotsFixed[i], createItem(
                    active ? Material.MAP : Material.PAPER,
                    (active ? "<bold>▶ </bold>" : "") + display[i]),
                    ev -> {
                        page = 0;
                        category = finalCat;
                        materials = loadMaterials();
                        refresh();
                    });
        }

        setItem(53, createItem(Material.OAK_SIGN,
                "<gold><bold>Search</bold>",
                List.of(filter != null ? "<gray>Filtering: <white>" + filter : "<gray>Click to search materials by name")),
                ev -> {
                    Runnable savedBack = getBackAction();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    player.closeInventory();
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Enter material name to search (Enter to clear filter):",
                            input -> {
                                String f = input.trim().isEmpty() ? null : input.trim();
                                MaterialPickerGui gui = new MaterialPickerGui(plugin, player, callback, 0, f, category);
                                if (savedBack != null) gui.setBackAction(savedBack);
                                gui.open();
                            });
                });

        int totalPages = Math.max(1, (int) Math.ceil((double) materials.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = startIndex + i;
            if (idx >= materials.size()) break;
            Material mat = materials.get(idx);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse("<white>" + mat.name()));
                meta.lore(List.of(
                        TextUtil.parse("<gray>Click to choose this material"),
                        TextUtil.parse("<yellow>vanilla:" + mat.name().toLowerCase())
                ));
                item.setItemMeta(meta);
            }
            setItem(ITEM_SLOTS[i], item, ev -> {
                String source = "vanilla:" + mat.name().toLowerCase(Locale.ROOT);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                player.sendMessage(TextUtil.success("Source = " + source));
                if (callback != null) callback.accept(source);
            });
        }

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { page--; refresh(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { page++; refresh(); } : null);

        setItem(45, createBackButton(), ev -> goBack());
    }
}
