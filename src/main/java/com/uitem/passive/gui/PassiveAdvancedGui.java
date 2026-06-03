package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.LogicMode;
import com.uitem.passive.PassiveAdvancedSettings;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PassiveAdvancedGui extends AbstractGui {

    public PassiveAdvancedGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 45, GuiTheme.title("Nang cao"));
    }

    @Override
    protected void setup() {
        fillBorder(5);
        var session = plugin.getPassiveBootstrap().manager().session(player);
        PassiveAdvancedSettings adv = session.advanced();

        setItem(4, GuiTheme.header(Material.NETHER_STAR,
                "Nang cao",
                List.of("<gray>Danh cho nguoi dung kinh nghiem")));

        setItem(20, GuiTheme.button(Material.COMPARATOR,
                "Logic dieu kien",
                List.of(
                        "<gray>Hien tai: <white>" + session.conditions().build().mode().displayName(),
                        "<gray>Click de doi"
                )), ev -> {
            LogicMode cur = session.conditions().build().mode();
            LogicMode next = switch (cur) {
                case ALL -> LogicMode.ANY;
                case ANY -> LogicMode.NOT;
                case NOT -> LogicMode.ALL;
            };
            session.conditions().mode(next);
            GuiFeedback.click(player);
            refresh();
        });

        setItem(22, GuiTheme.button(Material.CLOCK,
                "Cooldown (giay)",
                List.of("<gray>Hien tai: <white>" + adv.cooldownSeconds())), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Cooldown toan passive (giay, 0 = tat):",
                    input -> {
                        try {
                            adv.setCooldownSeconds(Integer.parseInt(input.trim()));
                            refresh();
                        } catch (NumberFormatException e) {
                            player.sendMessage(com.uitem.util.TextUtil.error("Nhap so hop le!"));
                            open();
                        }
                    });
        });

        setItem(24, GuiTheme.button(Material.GOLD_NUGGET,
                "Ti le toan passive (%)",
                List.of("<gray>Hien tai: <white>" + adv.globalChancePercent() + "%")), ev -> {
            double[] presets = {100, 75, 50, 25, 10, 5, 1};
            double cur = adv.globalChancePercent();
            double next = presets[0];
            for (int i = 0; i < presets.length; i++) {
                if (Math.abs(cur - presets[i]) < 0.01) {
                    next = presets[(i + 1) % presets.length];
                    break;
                }
            }
            adv.setGlobalChancePercent(next);
            refresh();
        });

        setItem(31, GuiTheme.button(Material.REPEATER,
                "Delay truoc actions (tick)",
                List.of("<gray>Hien tai: <white>" + adv.delayTicks())), ev -> {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Delay (tick, 0 = ngay lap tuc):",
                    input -> {
                        try {
                            adv.setDelayTicks(Integer.parseInt(input.trim()));
                            refresh();
                        } catch (NumberFormatException e) {
                            player.sendMessage(com.uitem.util.TextUtil.error("Nhap so hop le!"));
                            open();
                        }
                    });
        });

        setItem(40, createBackButton(), ev -> new StepConditionsGui(plugin, player).open());
    }
}