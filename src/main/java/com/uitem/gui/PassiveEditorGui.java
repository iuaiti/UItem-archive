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

    private static final List<String> TRIGGERS = List.of("HIT", "DAMAGED", "HOLD", "EQUIP", "BREAK_BLOCK", "KILL");
    private static final List<String> ACTION_TYPES = List.of(
            "LIGHTNING", "FIRE", "FREEZE", "POTION", "HEAL", "FEED", "SATURATION",
            "SPEED", "JUMP", "GLOW", "INVISIBILITY", "KNOCKBACK",
            "DAMAGE_MULTIPLIER", "LIFE_STEAL", "CRIT", "AUTO_SMELT", "XP_BOOST", "REPAIR",
            "NIGHT_VISION", "WATER_BREATHING", "FIRE_RESISTANCE", "ABSORPTION",
            "PARTICLE", "SOUND", "MESSAGE", "TELEPORT", "EXPLODE",
            "IGNITE_NEARBY", "PULL_ENTITY", "PUSH_ENTITY"
    );

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
                        "<gray>Quản lý kỹ năng passive cho item",
                        "<gray>Hiện tại: <white>" + data.getPassives().size() + " passive(s)",
                        "",
                        "<yellow>Click trái: <white>Chỉnh sửa passive",
                        "<red>Click phải: <white>Xóa passive",
                        "<aqua>Click giữa: <white>Nhân đôi passive"
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
                "<green><bold>+ Thêm Passive</bold>",
                List.of("<gray>Click để thêm passive mới")), event -> {
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
        lore.add("<yellow>Click trái: Sửa");
        lore.add("<red>Click phải: Xóa");
        lore.add("<aqua>Click giữa: Nhân đôi");
        meta.lore(lore.stream().map(TextUtil::parse).toList());
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private void editPassive(int index) {
        new PassiveDetailGui(plugin, player, data, index).open();
    }

    private void promptNewPassiveType() {
        player.closeInventory();
        player.sendMessage(TextUtil.info("Chọn loại passive muốn thêm (gõ tên):"));
        player.sendMessage(TextUtil.parse("<dark_gray>╔══════════════════════════════════════╗"));
        StringBuilder line = new StringBuilder("<dark_gray>║ ");
        int count = 0;
        for (String type : ACTION_TYPES) {
            line.append("<white>").append(type).append(" <dark_gray>│ ");
            count++;
            if (count >= 5) {
                player.sendMessage(TextUtil.parse(line.toString()));
                line = new StringBuilder("<dark_gray>║ ");
                count = 0;
            }
        }
        if (count > 0) player.sendMessage(TextUtil.parse(line.toString()));
        player.sendMessage(TextUtil.parse("<dark_gray>╚══════════════════════════════════════╝"));

        plugin.getChatInputManager().requestInput(player,
                "<yellow>Nhập loại passive:",
                input -> {
                    String type = input.trim().toUpperCase();
                    if (!ACTION_TYPES.contains(type)) {
                        player.sendMessage(TextUtil.error("Loại passive '" + type + "' không hợp lệ!"));
                        open();
                        return;
                    }
                    promptTrigger(type);
                });
    }

    private void promptTrigger(String type) {
        player.closeInventory();
        player.sendMessage(TextUtil.info("Chọn trigger cho " + type + ":"));
        StringBuilder sb = new StringBuilder("<dark_gray>");
        for (String t : TRIGGERS) {
            sb.append("<aqua>").append(t).append(" <dark_gray>│ ");
        }
        player.sendMessage(TextUtil.parse(sb.toString()));

        plugin.getChatInputManager().requestInput(player,
                "<yellow>Nhập trigger:",
                input -> {
                    String trigger = input.trim().toUpperCase();
                    if (!TRIGGERS.contains(trigger)) {
                        player.sendMessage(TextUtil.error("Trigger không hợp lệ!"));
                        open();
                        return;
                    }
                    PassiveData pd = new PassiveData(type, trigger, new LinkedHashMap<>(), 0);
                    data.getPassives().add(pd);
                    save();
                    player.sendMessage(TextUtil.success("Đã thêm passive " + type + " cho trigger " + trigger));
                    editPassive(data.getPassives().size() - 1);
                });
    }

    private void save() {
        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveAll(plugin.getItemManager());
    }
}
