package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.ActionEntry;
import com.uitem.passive.ConditionEntry;

import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PassiveValueEditorGui extends AbstractGui {

    private final boolean conditionMode;
    private final int index;

    public PassiveValueEditorGui(UItemPlugin plugin, Player player, boolean conditionMode, int index) {
        super(plugin, player, 45, GuiTheme.title("Cau hinh"));
        this.conditionMode = conditionMode;
        this.index = index;
    }

    @Override
    protected void setup() {
        fillBorder(5);
        var session = plugin.getPassiveBootstrap().manager().session(player);
        List<ParamSpec> specs;
        Map<String, Object> params;
        if (conditionMode) {
            var entries = session.conditions().entries();
            if (index < 0 || index >= entries.size()) {
                new StepConditionsGui(plugin, player).open();
                return;
            }
            ConditionEntry entry = entries.get(index);
            specs = entry.type().params();
            params = new HashMap<>(entry.params());
            setItem(4, GuiTheme.header(entry.type().icon(), entry.type().displayName(),
                    List.of("<gray>Chon truong ben duoi")));
            int slot = 19;
            for (ParamSpec spec : specs) {
                if (slot > 25) break;
                final int s = slot++;
                renderField(spec, params, s, () -> commitCondition(session, entry, params));
            }
            setItem(40, createBackButton(), ev -> new StepConditionsGui(plugin, player).open());
        } else {
            var actions = session.actions();
            if (index < 0 || index >= actions.size()) {
                new StepActionsGui(plugin, player).open();
                return;
            }
            ActionEntry entry = actions.get(index);
            specs = entry.type().params();
            params = new HashMap<>(entry.params());
            setItem(4, GuiTheme.header(entry.type().icon(), entry.type().displayName(),
                    List.of("<gray>Chon truong ben duoi")));
            int slot = 19;
            for (ParamSpec spec : specs) {
                if (slot > 25) break;
                final int s = slot++;
                renderField(spec, params, s, () -> commitAction(session, entry, params));
            }
            setItem(40, createBackButton(), ev -> new StepActionsGui(plugin, player).open());
        }
    }

    private void renderField(ParamSpec spec, Map<String, Object> params, int slot, Runnable onUpdate) {
        String val = String.valueOf(params.getOrDefault(spec.key(), spec.defaultValue()));
        setItem(slot, GuiTheme.button(Material.PAPER,
                "<yellow>" + spec.label(),
                List.of("<gray>Hien tai: <white>" + val, "<gray>Click de doi")), ev -> {
            GuiFeedback.click(player);
            handleKind(spec, params, onUpdate);
        });
    }

    private void handleKind(ParamSpec spec, Map<String, Object> params, Runnable onUpdate) {
        switch (spec.kind()) {
            case BOOLEAN -> {
                boolean cur = Boolean.parseBoolean(String.valueOf(params.get(spec.key())));
                params.put(spec.key(), String.valueOf(!cur));
                onUpdate.run();
                refresh();
            }
            case PERCENT -> cyclePercent(spec, params, onUpdate);
            case GAME_MODE -> cycleGameMode(spec, params, onUpdate);
            case WEATHER -> cycleWeather(spec, params, onUpdate);
            case WORLD -> {
                var worlds = Bukkit.getWorlds();
                if (!worlds.isEmpty()) {
                    String cur = String.valueOf(params.get(spec.key()));
                    int idx = 0;
                    for (int i = 0; i < worlds.size(); i++) {
                        if (worlds.get(i).getName().equalsIgnoreCase(cur)) {
                            idx = (i + 1) % worlds.size();
                            break;
                        }
                    }
                    params.put(spec.key(), worlds.get(idx).getName());
                    onUpdate.run();
                    refresh();
                }
            }
            case COMMAND_AS -> {
                String cur = String.valueOf(params.getOrDefault(spec.key(), "player"));
                String next = switch (cur.toLowerCase(Locale.ROOT)) {
                    case "player" -> "console";
                    case "console" -> "op";
                    default -> "player";
                };
                params.put(spec.key(), next);
                onUpdate.run();
                refresh();
            }
            default -> plugin.getChatInputManager().requestInput(player,
                    "<yellow>" + spec.label() + " <gray>(hien tai: " + params.get(spec.key()) + "):",
                    input -> {
                        if (spec.kind() == ParamKind.INTEGER || spec.kind() == ParamKind.DURATION_SECONDS) {
                            try {
                                params.put(spec.key(), Integer.parseInt(input.trim()));
                            } catch (NumberFormatException e) {
                                player.sendMessage(TextUtil.error("Nhap so nguyen hop le!"));
                                open();
                                return;
                            }
                        } else if (spec.kind() == ParamKind.DOUBLE || spec.kind() == ParamKind.PERCENT) {
                            try {
                                params.put(spec.key(), Double.parseDouble(input.trim().replace("%", "")));
                            } catch (NumberFormatException e) {
                                player.sendMessage(TextUtil.error("Nhap so hop le!"));
                                open();
                                return;
                            }
                        } else {
                            params.put(spec.key(), input);
                        }
                        onUpdate.run();
                        open();
                    });
        }
    }

    private void cyclePercent(ParamSpec spec, Map<String, Object> params, Runnable onUpdate) {
        double[] presets = {1, 5, 10, 25, 50, 75, 100};
        double cur = Double.parseDouble(String.valueOf(params.getOrDefault(spec.key(), "100")));
        double next = presets[0];
        for (int i = 0; i < presets.length; i++) {
            if (Math.abs(cur - presets[i]) < 0.01) {
                next = presets[(i + 1) % presets.length];
                break;
            }
        }
        params.put(spec.key(), next);
        onUpdate.run();
        refresh();
    }

    private void cycleGameMode(ParamSpec spec, Map<String, Object> params, Runnable onUpdate) {
        GameMode[] modes = GameMode.values();
        GameMode cur;
        try {
            cur = GameMode.valueOf(String.valueOf(params.get(spec.key())).toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            cur = GameMode.SURVIVAL;
        }
        int next = (cur.ordinal() + 1) % modes.length;
        params.put(spec.key(), modes[next].name());
        onUpdate.run();
        refresh();
    }

    private void cycleWeather(ParamSpec spec, Map<String, Object> params, Runnable onUpdate) {
        String[] w = {"clear", "rain", "thunder"};
        String cur = String.valueOf(params.getOrDefault(spec.key(), "clear")).toLowerCase(Locale.ROOT);
        int idx = 0;
        for (int i = 0; i < w.length; i++) {
            if (w[i].equals(cur)) {
                idx = (i + 1) % w.length;
                break;
            }
        }
        params.put(spec.key(), w[idx]);
        onUpdate.run();
        refresh();
    }

    private void commitCondition(com.uitem.passive.PassiveEditorSession session, ConditionEntry old, Map<String, Object> params) {
        session.conditions().set(index, new ConditionEntry(old.type(), params));
    }

    private void commitAction(com.uitem.passive.PassiveEditorSession session, ActionEntry old, Map<String, Object> params) {
        session.actions().set(index, new ActionEntry(old.type(), params, old.delayTicks()));
    }
}