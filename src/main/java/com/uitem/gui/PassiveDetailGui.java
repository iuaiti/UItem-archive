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

import java.util.*;
import java.util.function.Consumer;

public class PassiveDetailGui extends AbstractGui {

    private final UItemData data;
    private final int passiveIndex;
    private PassiveData passive;
    private String editingField;

    public PassiveDetailGui(UItemPlugin plugin, Player player, UItemData data, int passiveIndex) {
        super(plugin, player, 36, "<gold>Passive: " + data.getPassives().get(passiveIndex).getType());
        this.data = data;
        this.passiveIndex = passiveIndex;
        this.passive = data.getPassives().get(passiveIndex);
    }

    @Override
    protected void setup() {
        for (int i = 0; i < 36; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 3 || col == 0 || col == 8) {
                setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        setItem(4, createItem(Material.BLAZE_POWDER,
                "<gold><bold>Edit Passive</bold>",
                List.of("<gray>Type: <white>" + passive.getType(), "<gray>Trigger: <white>" + passive.getTrigger())));

        setItem(10, createItem(Material.REPEATER,
                "<gold><bold>Type</bold>",
                List.of("<gray>Current: <white>" + passive.getType(), "", "<yellow>Click to change")),
                ev -> editField("type"));

        setItem(11, createItem(Material.COMPARATOR,
                "<aqua><bold>Trigger</bold>",
                List.of("<gray>Current: <white>" + passive.getTrigger(), "", "<yellow>Click to change")),
                ev -> editField("trigger"));

        setItem(12, createItem(Material.CLOCK,
                "<yellow><bold>Cooldown</bold>",
                List.of("<gray>Current: <white>" + passive.getCooldown() + "s", "", "<yellow>Click to change")),
                ev -> editField("cooldown"));

        setItem(14, createItem(Material.TRIPWIRE_HOOK,
                "<green><bold>Conditions</bold>",
                List.of("<gray>" + (passive.getConditions() != null ? passive.getConditions().size() : 0) + " condition(s)", "", "<yellow>Click to manage")),
                ev -> manageConditions());

        setItem(15, createItem(Material.ENDER_PEARL,
                "<light_purple><bold>Test Passive</bold>",
                List.of("<gray>Activate passive immediately")),
                ev -> {
                    player.closeInventory();
                    Map<String, Object> ctx = new HashMap<>();
                    ctx.put("target", player);
                    ctx.put("damage", 10.0);
                    ctx.put("block", player.getLocation().getBlock());
                    plugin.getPassiveManager().executePassives(player, data.getId(), passive.getTrigger(), ctx);
                    player.sendMessage(TextUtil.success("Tested passive " + passive.getType() + "!"));
                });

        setItem(31, createBackButton(), ev -> new PassiveEditorGui(plugin, player, data).open());

        updatePreview();
    }

    private void updatePreview() {
        PassiveManager pm = plugin.getPassiveManager();
        ItemStack preview = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse("<gold>" + passive.getType()));
            meta.lore(List.of(
                    TextUtil.parse("<gray>" + pm.getPassiveDescription(passive)),
                    TextUtil.parse(""),
                    TextUtil.parse("<dark_gray>Trigger: <white>" + passive.getTrigger()),
                    TextUtil.parse("<dark_gray>Cooldown: <white>" + passive.getCooldown() + "s"),
                    TextUtil.parse("<dark_gray>Conditions: <white>" + (passive.getConditions() != null ? passive.getConditions().size() : 0))
            ));
            preview.setItemMeta(meta);
        }
        setItem(22, preview);
    }

    private void editField(String field) {
        editingField = field;
        player.closeInventory();

        String prompt = switch (field) {
            case "type" -> "<yellow>Enter passive type:";
            case "trigger" -> "<yellow>Enter trigger (HIT/DAMAGED/HOLD/EQUIP/BREAK_BLOCK/KILL):";
            case "cooldown" -> "<yellow>Enter cooldown (seconds, 0 = no cooldown):";
            default -> "<yellow>Enter value:";
        };

        plugin.getChatInputManager().requestInput(player, prompt, input -> {
            String val = input.trim();
            switch (field) {
                case "type" -> passive.setType(val.toUpperCase());
                case "trigger" -> passive.setTrigger(val.toUpperCase());
                case "cooldown" -> {
                    try { passive.setCooldown(Integer.parseInt(val)); }
                    catch (NumberFormatException e) { player.sendMessage(TextUtil.error("Invalid number!")); }
                }
            }
            save();
            open();
        });
    }

    private void manageConditions() {
        new ConditionEditorGui(plugin, player, data, passiveIndex).open();
    }

    private void save() {
        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveAll(plugin.getItemManager());
    }

    private static class ConditionEditorGui extends AbstractGui {
        private final UItemData data;
        private final int passiveIndex;
        private PassiveData passive;

        public ConditionEditorGui(UItemPlugin plugin, Player player, UItemData data, int passiveIndex) {
            super(plugin, player, 54, "<green>Conditions Editor");
            this.data = data;
            this.passiveIndex = passiveIndex;
            this.passive = data.getPassives().get(passiveIndex);
        }

        @Override
        protected void setup() {
            fillBorder(6);

            setItem(4, createItem(Material.TRIPWIRE_HOOK,
                    "<green><bold>Conditions</bold>",
                    List.of("<gray>For passive: <white>" + passive.getType())));

            String[] condTypes = {"chance", "health_below", "sneaking", "sprinting", "world", "biome", "time", "weather"};
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19};

            for (int i = 0; i < condTypes.length; i++) {
                String key = condTypes[i];
                boolean has = passive.getConditions() != null && passive.getConditions().containsKey(key);
                Object val = has ? passive.getConditions().get(key) : null;

                List<String> lore = new ArrayList<>();
                if (has) lore.add("<gray>Value: <white>" + val);
                else lore.add("<red>✘ Not set");
                lore.add("");
                lore.add("<yellow>Left click: Set/Edit");
                lore.add("<red>Right click: Delete");

                setItem(slots[i], createItem(
                        has ? Material.LIME_DYE : Material.RED_DYE,
                        (has ? "<green>✔ " : "<red>✘ ") + key, lore),
                        event -> {
                            if (event.getClick() == ClickType.RIGHT) {
                                if (passive.getConditions() != null) {
                                    passive.getConditions().remove(key);
                                }
                                save();
                                refresh();
                                return;
                            }
                            player.closeInventory();
                            String prompt = getPrompt(key);
                            plugin.getChatInputManager().requestInput(player, prompt, input -> {
                                String v = input.trim();
                                Object parsed = parseCondition(key, v);
                                if (passive.getConditions() == null) {
                                    passive.setConditions(new LinkedHashMap<>());
                                }
                                passive.getConditions().put(key, parsed);
                                save();
                                player.sendMessage(TextUtil.success("Set " + key + " = " + parsed));
                                open();
                            });
                        });
            }

            setItem(49, createItem(Material.EMERALD,
                    "<green><bold>Add custom condition</bold>",
                    List.of("<gray>Add any key-value pair")),
                    ev -> {
                        player.closeInventory();
                        plugin.getChatInputManager().requestInput(player,
                                "<yellow>Enter key=value (e.g. my_custom_cond=hello):",
                                input -> {
                                    String[] parts = input.split("=", 2);
                                    if (parts.length == 2) {
                                        if (passive.getConditions() == null) {
                                            passive.setConditions(new LinkedHashMap<>());
                                        }
                                        passive.getConditions().put(parts[0].trim(), parts[1].trim());
                                        save();
                                        player.sendMessage(TextUtil.success("Added condition!"));
                                    }
                                    open();
                                });
                    });

            setItem(45, createBackButton(), ev -> new PassiveDetailGui(plugin, player, data, passiveIndex).open());
        }

        private String getPrompt(String key) {
            return switch (key) {
                case "chance" -> "<yellow>Enter chance (% from 0-100):";
                case "health_below" -> "<yellow>Enter max health threshold to activate:";
                case "sneaking" -> "<yellow>Enter true/false (require sneaking/not sneaking):";
                case "sprinting" -> "<yellow>Enter true/false (require sprinting/not sprinting):";
                case "world" -> "<yellow>Enter world name:";
                case "biome" -> "<yellow>Enter biome name (e.g. plains, desert):";
                case "time" -> "<yellow>Enter time (day/night/dawn/dusk):";
                case "weather" -> "<yellow>Enter weather (clear/rain/thunder):";
                default -> "<yellow>Enter value for " + key + ":";
            };
        }

        private Object parseCondition(String key, String val) {
            return switch (key) {
                case "chance", "health_below" -> {
                    try { yield Double.parseDouble(val); }
                    catch (NumberFormatException e) { yield val; }
                }
                case "sneaking", "sprinting", "self", "break-blocks" -> val.equalsIgnoreCase("true");
                case "ticks", "duration", "amplifier", "food", "count", "max_logs", "radius" -> {
                    try { yield Integer.parseInt(val); }
                    catch (NumberFormatException e) { yield val; }
                }
                case "percent", "multiplier", "force", "power", "amount" -> {
                    try { yield Double.parseDouble(val); }
                    catch (NumberFormatException e) { yield val; }
                }
                default -> val;
            };
        }

        private void save() {
            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveAll(plugin.getItemManager());
        }
    }
}
