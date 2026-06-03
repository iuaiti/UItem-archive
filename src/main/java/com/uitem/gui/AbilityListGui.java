package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.ability.AbilityHolder;
import com.uitem.ability.ItemAbilityRule;
import com.uitem.ability.ItemTrigger;
import com.uitem.potion.UPotionData;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AbilityListGui extends AbstractGui {

    private final AbilityHolder holder;
    private final String kindLabel;
    private final Runnable onBack;
    private final Runnable persistFull;
    private final Runnable persistMemory;

    private AbilityListGui(UItemPlugin plugin, Player player, AbilityHolder holder, String kindLabel,
                           Runnable onBack, Runnable persistFull, Runnable persistMemory) {
        super(plugin, player, 54, GuiTheme.title("Kỹ năng — " + holder.getAbilityId()));
        this.holder = holder;
        this.kindLabel = kindLabel;
        this.onBack = onBack;
        this.persistFull = persistFull;
        this.persistMemory = persistMemory;
    }

    public static AbilityListGui forItem(UItemPlugin plugin, Player player, UItemData data) {
        return new AbilityListGui(plugin, player, data, "item",
                () -> new ItemEditorGui(plugin, player, data).open(),
                () -> {
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveAllNow(plugin.getItemManager());
                },
                () -> plugin.getItemManager().registerItem(data));
    }

    public static AbilityListGui forPotion(UItemPlugin plugin, Player player, UPotionData data) {
        return new AbilityListGui(plugin, player, data, "potion",
                () -> new PotionEditorGui(plugin, player, data).open(),
                () -> plugin.getPotionManager().save(data),
                () -> plugin.getPotionManager().register(data));
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(
                holder.isAbilitiesEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                "<gold><bold>Kỹ năng " + kindLabel + "</bold>",
                List.of(
                        "<gray>Trạng thái: " + (holder.isAbilitiesEnabled() ? "<green>BẬT" : "<red>TẮT"),
                        "<gray>Số rule: <white>" + holder.getAbilities().size(),
                        "",
                        "<yellow>Click <gray>bật/tắt toàn bộ kỹ năng",
                        "<gray>Đánh, giết, bị đánh, uống, click..."
                )), ev -> {
            holder.setAbilitiesEnabled(!holder.isAbilitiesEnabled());
            GuiFeedback.click(player);
            saveAndRefresh();
        });

        List<ItemAbilityRule> rules = holder.getAbilities();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < slots.length && i < rules.size(); i++) {
            ItemAbilityRule rule = rules.get(i);
            int slotIndex = i;
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Kích hoạt: <white>" + rule.getTrigger().label());
            lore.add("<gray>Vị trí: <white>" + rule.getSlot().label());
            lore.add("<gray>CD: <white>" + rule.getCooldownSeconds() + "s");
            lore.add("<gray>Tỉ lệ: <white>" + rule.getChancePercent() + "%");
            lore.add("<gray>Hành động: <white>" + rule.getActions().size());
            lore.add("");
            lore.add("<yellow>Click <gray>sửa rule");
            lore.add("<red>Shift+Click <gray>xóa rule");
            Material icon = "potion".equals(kindLabel) ? Material.SPLASH_POTION : Material.ENCHANTED_BOOK;
            setItem(slots[i], createItem(icon, "<aqua>Rule #" + (i + 1), lore), ev -> {
                if (ev.isShiftClick()) {
                    rules.remove(slotIndex);
                    player.sendMessage(TextUtil.success("Rule deleted."));
                    saveAndRefresh();
                } else {
                    GuiFeedback.click(player);
                    AbilityEditorGui.open(plugin, player, holder, kindLabel, slotIndex, onBack, persistFull, persistMemory);
                }
            });
        }

        setItem(48, createItem(Material.EMERALD,
                "<green><bold>+ Thêm rule</bold>",
                List.of("<yellow>Click để thêm")), ev -> {
            ItemAbilityRule rule = new ItemAbilityRule();
            rule.setRuleId("rule_" + holder.getAbilities().size());
            rule.setTrigger("potion".equals(kindLabel) ? ItemTrigger.ON_DRINK : ItemTrigger.ON_HIT);
            holder.getAbilities().add(rule);
            GuiFeedback.click(player);
            saveAndRefresh();
            AbilityEditorGui.open(plugin, player, holder, kindLabel, holder.getAbilities().size() - 1,
                    onBack, persistFull, persistMemory);
        });

        setItem(46, createBackButton(), ev -> {
            GuiFeedback.click(player);
            onBack.run();
        });

        setItem(49, GuiTheme.actionPrimary(Material.LIME_CONCRETE, "SAVE",
                "<gray>Lưu vào file " + kindLabel), ev -> {
            persistFull.run();
            player.sendMessage(TextUtil.success("Saved ability for '" + holder.getAbilityId() + "'"));
            GuiFeedback.success(player);
            refresh();
        });
    }

    private void saveAndRefresh() {
        persistMemory.run();
        refresh();
    }
}