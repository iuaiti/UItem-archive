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
                "<gold><bold>Chỉnh Sửa Passive</bold>",
                List.of("<gray>Loại: <white>" + passive.getType(), "<gray>Trigger: <white>" + passive.getTrigger())));

        setItem(10, createItem(Material.REPEATER,
                "<gold><bold>Loại</bold>",
                List.of("<gray>Hiện tại: <white>" + passive.getType(), "", "<yellow>Click để đổi")),
                ev -> editField("type"));

        setItem(11, createItem(Material.COMPARATOR,
                "<aqua><bold>Trigger</bold>",
                List.of("<gray>Hiện tại: <white>" + passive.getTrigger(), "", "<yellow>Click để đổi")),
                ev -> editField("trigger"));

        setItem(12, createItem(Material.CLOCK,
                "<yellow><bold>Cooldown</bold>",
                List.of("<gray>Hiện tại: <white>" + passive.getCooldown() + "s", "", "<yellow>Click để đổi")),
                ev -> editField("cooldown"));

        setItem(14, createItem(Material.TRIPWIRE_HOOK,
                "<green><bold>Conditions</bold>",
                List.of("<gray>" + (passive.getConditions() != null ? passive.getConditions().size() : 0) + " condition(s)", "", "<yellow>Click để quản lý")),
                ev -> manageConditions());

        setItem(15, createItem(Material.ENDER_PEARL,
                "<light_purple><bold>Test Passive</bold>",
                List.of("<gray>Kích hoạt passive ngay lập tức")),
                ev -> {
                    player.closeInventory();
                    Map<String, Object> ctx = new HashMap<>();
                    ctx.put("target", player);
                    PassiveManager pm = new PassiveManager(plugin, plugin.getItemManager());
                    pm.executePassives(player, data.getId(), passive.getTrigger(), ctx);
                    player.sendMessage(TextUtil.success("Đã test passive " + passive.getType() + "!"));
                });

        setItem(31, createBackButton(), ev -> new PassiveEditorGui(plugin, player, data).open());

        updatePreview();
    }

    private void updatePreview() {
        PassiveManager pm = new PassiveManager(plugin, plugin.getItemManager());
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
            case "type" -> "<yellow>Nhập loại passive:";
            case "trigger" -> "<yellow>Nhập trigger (HIT/DAMAGED/HOLD/EQUIP/BREAK_BLOCK/KILL):";
            case "cooldown" -> "<yellow>Nhập cooldown (giây, 0 = không cooldown):";
            default -> "<yellow>Nhập giá trị:";
        };

        plugin.getChatInputManager().requestInput(player, prompt, input -> {
            String val = input.trim();
            switch (field) {
                case "type" -> passive.setType(val.toUpperCase());
                case "trigger" -> passive.setTrigger(val.toUpperCase());
                case "cooldown" -> {
                    try { passive.setCooldown(Integer.parseInt(val)); }
                    catch (NumberFormatException e) { player.sendMessage(TextUtil.error("Số không hợp lệ!")); }
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
                    List.of("<gray>Cho passive: <white>" + passive.getType())));

            String[] condTypes = {"chance", "health_below", "sneaking", "sprinting", "world", "biome", "time", "weather"};
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19};

            for (int i = 0; i < condTypes.length; i++) {
                String key = condTypes[i];
                boolean has = passive.getConditions() != null && passive.getConditions().containsKey(key);
                Object val = has ? passive.getConditions().get(key) : null;

                List<String> lore = new ArrayList<>();
                if (has) lore.add("<gray>Giá trị: <white>" + val);
                else lore.add("<red>✘ Chưa đặt");
                lore.add("");
                lore.add("<yellow>Click trái: Đặt/Sửa");
                lore.add("<red>Click phải: Xóa");

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
                                player.sendMessage(TextUtil.success("Đã đặt " + key + " = " + parsed));
                                open();
                            });
                        });
            }

            setItem(49, createItem(Material.EMERALD,
                    "<green><bold>Thêm condition tùy chỉnh</bold>",
                    List.of("<gray>Thêm key-value bất kỳ")),
                    ev -> {
                        player.closeInventory();
                        plugin.getChatInputManager().requestInput(player,
                                "<yellow>Nhập key=value (VD: my_custom_cond=hello):",
                                input -> {
                                    String[] parts = input.split("=", 2);
                                    if (parts.length == 2) {
                                        if (passive.getConditions() == null) {
                                            passive.setConditions(new LinkedHashMap<>());
                                        }
                                        passive.getConditions().put(parts[0].trim(), parts[1].trim());
                                        save();
                                        player.sendMessage(TextUtil.success("Đã thêm condition!"));
                                    }
                                    open();
                                });
                    });

            setItem(45, createBackButton(), ev -> new PassiveDetailGui(plugin, player, data, passiveIndex).open());
        }

        private String getPrompt(String key) {
            return switch (key) {
                case "chance" -> "<yellow>Nhập chance (% từ 0-100):";
                case "health_below" -> "<yellow>Nhập ngưỡng máu tối đa để kích hoạt:";
                case "sneaking" -> "<yellow>Nhập true/false (yêu cầu đang ngồi/không ngồi):";
                case "sprinting" -> "<yellow>Nhập true/false (yêu cầu đang chạy/không chạy):";
                case "world" -> "<yellow>Nhập tên world:";
                case "biome" -> "<yellow>Nhập tên biome (VD: plains, desert):";
                case "time" -> "<yellow>Nhập thời gian (day/night/dawn/dusk):";
                case "weather" -> "<yellow>Nhập thời tiết (clear/rain/thunder):";
                default -> "<yellow>Nhập giá trị cho " + key + ":";
            };
        }

        private Object parseCondition(String key, String val) {
            return switch (key) {
                case "chance", "health_below" -> {
                    try { yield Double.parseDouble(val); }
                    catch (NumberFormatException e) { yield val; }
                }
                case "sneaking", "sprinting" -> val.equalsIgnoreCase("true");
                case "ticks" -> {
                    try { yield Integer.parseInt(val); }
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
