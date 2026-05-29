package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.passive.PassiveData;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PassiveTriggerSelectGui extends AbstractGui {

    private final UItemData data;
    private final String passiveType;

    private static final Map<String, Material> TRIGGER_MATERIALS = new LinkedHashMap<>();

    static {
        TRIGGER_MATERIALS.put("HIT", Material.IRON_SWORD);
        TRIGGER_MATERIALS.put("DAMAGED", Material.SHIELD);
        TRIGGER_MATERIALS.put("HOLD", Material.PLAYER_HEAD);
        TRIGGER_MATERIALS.put("EQUIP", Material.CHAINMAIL_CHESTPLATE);
        TRIGGER_MATERIALS.put("BREAK_BLOCK", Material.DIAMOND_PICKAXE);
        TRIGGER_MATERIALS.put("KILL", Material.SKULL_BANNER_PATTERN);
        TRIGGER_MATERIALS.put("LEFT_CLICK", Material.STONE_SWORD);
        TRIGGER_MATERIALS.put("RIGHT_CLICK", Material.BLAZE_ROD);
    }

    private static final Map<String, String> TRIGGER_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        TRIGGER_DESCRIPTIONS.put("HIT", "When you hit an entity");
        TRIGGER_DESCRIPTIONS.put("DAMAGED", "When you take damage");
        TRIGGER_DESCRIPTIONS.put("HOLD", "While holding the item");
        TRIGGER_DESCRIPTIONS.put("EQUIP", "When equipped in any slot");
        TRIGGER_DESCRIPTIONS.put("BREAK_BLOCK", "When breaking a block");
        TRIGGER_DESCRIPTIONS.put("KILL", "When you kill an entity");
        TRIGGER_DESCRIPTIONS.put("LEFT_CLICK", "When you left-click with the item");
        TRIGGER_DESCRIPTIONS.put("RIGHT_CLICK", "When you right-click with the item");
    }

    public PassiveTriggerSelectGui(UItemPlugin plugin, Player player, UItemData data, String passiveType) {
        super(plugin, player, 36, "<gold>Select Trigger: " + passiveType);
        this.data = data;
        this.passiveType = passiveType;
    }

    @Override
    protected void setup() {
        for (int i = 0; i < 36; i++) {
            int row = i / 9;
            if (row == 0 || row == 2 || row == 3) {
                setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        setItem(4, createItem(Material.BLAZE_POWDER,
                "<gold><bold>" + passiveType + "</bold>",
                List.of("<gray>Choose a trigger for this passive")));

        String[] triggers = {"HIT", "DAMAGED", "HOLD", "EQUIP", "BREAK_BLOCK", "KILL", "LEFT_CLICK", "RIGHT_CLICK"};
        int[] triggerSlots = {10, 11, 12, 13, 14, 15, 16, 17};

        for (int i = 0; i < triggers.length; i++) {
            String trigger = triggers[i];
            Material mat = TRIGGER_MATERIALS.getOrDefault(trigger, Material.LEVER);
            String desc = TRIGGER_DESCRIPTIONS.getOrDefault(trigger, "");

            List<String> lore = List.of(
                    "<gray>" + desc,
                    "",
                    "<yellow>Click to select</yellow>"
            );

            ItemStack item = createItem(mat, "<aqua><bold>" + trigger, lore);
            setItem(triggerSlots[i], item, event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

                PassiveData pd = new PassiveData(passiveType, trigger, new LinkedHashMap<>(), 0);
                data.getPassives().add(pd);

                plugin.getItemManager().registerItem(data);
                plugin.getStorageManager().saveAll(plugin.getItemManager());

                player.sendMessage(TextUtil.success("Added passive " + passiveType + " [" + trigger + "]"));
                new PassiveDetailGui(plugin, player, data, data.getPassives().size() - 1).open();
            });
        }

        setItem(31, createItem(Material.REDSTONE, "<red>Cancel", List.of("<gray>Go back")),
                ev -> new PassiveTypeSelectGui(plugin, player, data).open());
    }
}
