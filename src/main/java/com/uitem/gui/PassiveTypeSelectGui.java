package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PassiveTypeSelectGui extends AbstractGui {

    private final UItemData data;
    private int page;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final List<String> ACTION_TYPES = List.of(
            "LIGHTNING", "FIRE", "FREEZE", "POTION", "HEAL", "FEED", "SATURATION",
            "SPEED", "JUMP", "GLOW", "INVISIBILITY", "KNOCKBACK",
            "DAMAGE_MULTIPLIER", "LIFE_STEAL", "CRIT", "AUTO_SMELT", "XP_BOOST", "REPAIR",
            "NIGHT_VISION", "WATER_BREATHING", "FIRE_RESISTANCE", "ABSORPTION",
            "PARTICLE", "SOUND", "MESSAGE", "TELEPORT", "EXPLODE",
            "IGNITE_NEARBY", "PULL_ENTITY", "PUSH_ENTITY",
            "TNT_MINER", "TREE_FELLER", "MACE_LIGHTNING", "MACE_TNT"
    );

    private static final Map<String, Material> TYPE_MATERIALS = new LinkedHashMap<>();

    static {
        TYPE_MATERIALS.put("LIGHTNING", Material.NETHERITE_SWORD);
        TYPE_MATERIALS.put("FIRE", Material.FLINT_AND_STEEL);
        TYPE_MATERIALS.put("FREEZE", Material.BLUE_ICE);
        TYPE_MATERIALS.put("POTION", Material.POTION);
        TYPE_MATERIALS.put("HEAL", Material.GOLDEN_APPLE);
        TYPE_MATERIALS.put("FEED", Material.COOKED_BEEF);
        TYPE_MATERIALS.put("SATURATION", Material.MUSHROOM_STEW);
        TYPE_MATERIALS.put("SPEED", Material.SUGAR);
        TYPE_MATERIALS.put("JUMP", Material.RABBIT_FOOT);
        TYPE_MATERIALS.put("GLOW", Material.GLOWSTONE_DUST);
        TYPE_MATERIALS.put("INVISIBILITY", Material.GLASS);
        TYPE_MATERIALS.put("KNOCKBACK", Material.PISTON);
        TYPE_MATERIALS.put("DAMAGE_MULTIPLIER", Material.ENCHANTED_BOOK);
        TYPE_MATERIALS.put("LIFE_STEAL", Material.WITHER_ROSE);
        TYPE_MATERIALS.put("CRIT", Material.DIAMOND);
        TYPE_MATERIALS.put("AUTO_SMELT", Material.FURNACE);
        TYPE_MATERIALS.put("XP_BOOST", Material.EXPERIENCE_BOTTLE);
        TYPE_MATERIALS.put("REPAIR", Material.ANVIL);
        TYPE_MATERIALS.put("NIGHT_VISION", Material.ENDER_EYE);
        TYPE_MATERIALS.put("WATER_BREATHING", Material.PRISMARINE_CRYSTALS);
        TYPE_MATERIALS.put("FIRE_RESISTANCE", Material.MAGMA_CREAM);
        TYPE_MATERIALS.put("ABSORPTION", Material.GOLDEN_APPLE);
        TYPE_MATERIALS.put("PARTICLE", Material.FIREWORK_STAR);
        TYPE_MATERIALS.put("SOUND", Material.NOTE_BLOCK);
        TYPE_MATERIALS.put("MESSAGE", Material.OAK_SIGN);
        TYPE_MATERIALS.put("TELEPORT", Material.ENDER_PEARL);
        TYPE_MATERIALS.put("EXPLODE", Material.TNT);
        TYPE_MATERIALS.put("IGNITE_NEARBY", Material.FIRE_CHARGE);
        TYPE_MATERIALS.put("PULL_ENTITY", Material.FISHING_ROD);
        TYPE_MATERIALS.put("PUSH_ENTITY", Material.STICKY_PISTON);
        TYPE_MATERIALS.put("TNT_MINER", Material.DIAMOND_PICKAXE);
        TYPE_MATERIALS.put("TREE_FELLER", Material.GOLDEN_AXE);
        TYPE_MATERIALS.put("MACE_LIGHTNING", Material.MACE);
        TYPE_MATERIALS.put("MACE_TNT", Material.MACE);
    }

    private static final Map<String, String> TYPE_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        TYPE_DESCRIPTIONS.put("LIGHTNING", "Strike lightning at target");
        TYPE_DESCRIPTIONS.put("FIRE", "Set target on fire");
        TYPE_DESCRIPTIONS.put("FREEZE", "Freeze target");
        TYPE_DESCRIPTIONS.put("POTION", "Apply potion effect");
        TYPE_DESCRIPTIONS.put("HEAL", "Heal the player");
        TYPE_DESCRIPTIONS.put("FEED", "Feed the player");
        TYPE_DESCRIPTIONS.put("SATURATION", "Restore saturation");
        TYPE_DESCRIPTIONS.put("SPEED", "Speed boost");
        TYPE_DESCRIPTIONS.put("JUMP", "Jump boost");
        TYPE_DESCRIPTIONS.put("GLOW", "Make target glow");
        TYPE_DESCRIPTIONS.put("INVISIBILITY", "Become invisible");
        TYPE_DESCRIPTIONS.put("KNOCKBACK", "Knockback target");
        TYPE_DESCRIPTIONS.put("DAMAGE_MULTIPLIER", "Multiply outgoing damage");
        TYPE_DESCRIPTIONS.put("LIFE_STEAL", "Heal % of dealt damage");
        TYPE_DESCRIPTIONS.put("CRIT", "Force critical hit");
        TYPE_DESCRIPTIONS.put("AUTO_SMELT", "Auto-smelt mined blocks");
        TYPE_DESCRIPTIONS.put("XP_BOOST", "Multiply XP drops");
        TYPE_DESCRIPTIONS.put("REPAIR", "Repair held item durability");
        TYPE_DESCRIPTIONS.put("NIGHT_VISION", "Night vision effect");
        TYPE_DESCRIPTIONS.put("WATER_BREATHING", "Water breathing effect");
        TYPE_DESCRIPTIONS.put("FIRE_RESISTANCE", "Fire resistance effect");
        TYPE_DESCRIPTIONS.put("ABSORPTION", "Absorption effect");
        TYPE_DESCRIPTIONS.put("PARTICLE", "Spawn particles");
        TYPE_DESCRIPTIONS.put("SOUND", "Play a sound");
        TYPE_DESCRIPTIONS.put("MESSAGE", "Send a message");
        TYPE_DESCRIPTIONS.put("TELEPORT", "Teleport to coordinates");
        TYPE_DESCRIPTIONS.put("EXPLODE", "Create explosion");
        TYPE_DESCRIPTIONS.put("IGNITE_NEARBY", "Ignite nearby entities");
        TYPE_DESCRIPTIONS.put("PULL_ENTITY", "Pull target toward you");
        TYPE_DESCRIPTIONS.put("PUSH_ENTITY", "Push target away");
        TYPE_DESCRIPTIONS.put("TNT_MINER", "Explode mined block");
        TYPE_DESCRIPTIONS.put("TREE_FELLER", "Fell entire tree");
        TYPE_DESCRIPTIONS.put("MACE_LIGHTNING", "Mace + lightning strike");
        TYPE_DESCRIPTIONS.put("MACE_TNT", "Mace + explosion");
    }

    public PassiveTypeSelectGui(UItemPlugin plugin, Player player, UItemData data) {
        this(plugin, player, data, 0);
    }

    public PassiveTypeSelectGui(UItemPlugin plugin, Player player, UItemData data, int page) {
        super(plugin, player, 54, "<gold><bold>Select Passive Type</bold>");
        this.data = data;
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.BLAZE_POWDER,
                "<gold><bold>Select Passive Type</bold>",
                List.of(
                        "<gray>Choose a passive ability to add",
                        "<gray>Item: <white>" + data.getId()
                )));

        int totalPages = Math.max(1, (int) Math.ceil((double) ACTION_TYPES.size() / ITEMS_PER_PAGE));
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = startIndex + i;
            if (idx >= ACTION_TYPES.size()) break;
            String type = ACTION_TYPES.get(idx);

            Material mat = TYPE_MATERIALS.getOrDefault(type, Material.ENCHANTED_BOOK);
            String desc = TYPE_DESCRIPTIONS.getOrDefault(type, "");

            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + desc);
            lore.add("");
            lore.add("<yellow>Click to select</yellow>");

            ItemStack item = createItem(mat, "<gold><bold>" + type, lore);
            setItem(ITEM_SLOTS[i], item, event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new PassiveTriggerSelectGui(plugin, player, data, type).open();
            });
        }

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { page--; refresh(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { page++; refresh(); } : null);

        setItem(49, createItem(Material.REDSTONE, "<red>Cancel", List.of("<gray>Go back")),
                ev -> new PassiveEditorGui(plugin, player, data).open());
    }
}
