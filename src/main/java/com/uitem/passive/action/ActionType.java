package com.uitem.passive.action;

import com.uitem.passive.gui.ParamKind;
import com.uitem.passive.gui.ParamSpec;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum ActionType {
    SEND_MESSAGE("send_message", "Gui tin nhan", Material.PAPER,
            List.of(new ParamSpec("message", "Noi dung (MiniMessage)", ParamKind.TEXT, "<green>Hello!"))),
    EXECUTE_COMMAND("execute_command", "Chay lenh", Material.COMMAND_BLOCK,
            List.of(
                    new ParamSpec("command", "Lenh (co %player%)", ParamKind.TEXT, "say hello"),
                    new ParamSpec("as", "Chay nhu", ParamKind.COMMAND_AS, "player"))),
    GIVE_ITEM("give_item", "Cho vat pham", Material.CHEST,
            List.of(
                    new ParamSpec("material", "Material", ParamKind.MATERIAL, "DIAMOND"),
                    new ParamSpec("amount", "So luong", ParamKind.INTEGER, "1"))),
    REMOVE_ITEM("remove_item", "Xoa vat pham", Material.LAVA_BUCKET,
            List.of(
                    new ParamSpec("material", "Material", ParamKind.MATERIAL, "DIRT"),
                    new ParamSpec("amount", "So luong", ParamKind.INTEGER, "1"))),
    HEAL("heal", "Hoi mau", Material.GOLDEN_APPLE,
            List.of(new ParamSpec("amount", "Luong hoi", ParamKind.DOUBLE, "5"))),
    DAMAGE("damage", "Gay sat thuong", Material.IRON_SWORD,
            List.of(new ParamSpec("amount", "Sat thuong", ParamKind.DOUBLE, "2"))),
    ADD_EXPERIENCE("add_experience", "Cong kinh nghiem", Material.EXPERIENCE_BOTTLE,
            List.of(
                    new ParamSpec("amount", "Diem XP", ParamKind.INTEGER, "10"),
                    new ParamSpec("levels", "Cong level (true/false)", ParamKind.BOOLEAN, "false"))),
    POTION_EFFECT("potion_effect", "Hieu ung thuoc", Material.POTION,
            List.of(
                    new ParamSpec("effect", "PotionEffectType", ParamKind.TEXT, "SPEED"),
                    new ParamSpec("duration", "Giay", ParamKind.DURATION_SECONDS, "30"),
                    new ParamSpec("amplifier", "Cap do (0+)", ParamKind.INTEGER, "0"))),
    SOUND("sound", "Am thanh", Material.NOTE_BLOCK,
            List.of(
                    new ParamSpec("sound", "Sound key", ParamKind.TEXT, "ENTITY_PLAYER_LEVELUP"),
                    new ParamSpec("volume", "Am luong", ParamKind.DOUBLE, "1"),
                    new ParamSpec("pitch", "Pitch", ParamKind.DOUBLE, "1"))),
    PARTICLE("particle", "Hat particle", Material.BLAZE_POWDER,
            List.of(new ParamSpec("particle", "Particle", ParamKind.TEXT, "HAPPY_VILLAGER"))),
    TELEPORT("teleport", "Dich chuyen", Material.ENDER_PEARL,
            List.of(
                    new ParamSpec("world", "World", ParamKind.WORLD, "world"),
                    new ParamSpec("x", "X", ParamKind.DOUBLE, "0"),
                    new ParamSpec("y", "Y", ParamKind.DOUBLE, "64"),
                    new ParamSpec("z", "Z", ParamKind.DOUBLE, "0"))),
    LIGHTNING("lightning", "Set", Material.LIGHTNING_ROD,
            List.of(new ParamSpec("at_target", "Danh vi tri target", ParamKind.BOOLEAN, "true"))),
    FIREWORK("firework", "Phao hoa", Material.FIREWORK_ROCKET,
            List.of()),
    BROADCAST("broadcast", "Thong bao server", Material.BELL,
            List.of(new ParamSpec("message", "Noi dung", ParamKind.TEXT, "<gold>Thong bao!"))),
    ACTION_BAR("action_bar", "Action bar", Material.OAK_SIGN,
            List.of(new ParamSpec("message", "Noi dung", ParamKind.TEXT, "<yellow>Action bar"))),
    TITLE("title", "Title / Subtitle", Material.NAME_TAG,
            List.of(
                    new ParamSpec("title", "Title", ParamKind.TEXT, "<gold>Tieu de"),
                    new ParamSpec("subtitle", "Subtitle", ParamKind.TEXT, "<gray>Phu de"),
                    new ParamSpec("fade_in", "Fade in (tick)", ParamKind.INTEGER, "10"),
                    new ParamSpec("stay", "Stay (tick)", ParamKind.INTEGER, "40"),
                    new ParamSpec("fade_out", "Fade out (tick)", ParamKind.INTEGER, "10")));

    private final String key;
    private final String displayName;
    private final Material icon;
    private final List<ParamSpec> params;

    ActionType(String key, String displayName, Material icon, List<ParamSpec> params) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.params = params;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public List<ParamSpec> params() {
        return params;
    }

    public Map<String, Object> defaultParams() {
        Map<String, Object> map = new HashMap<>();
        for (ParamSpec p : params) {
            map.put(p.key(), p.defaultValue());
        }
        return map;
    }

    public String formatSummary(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return displayName;
        }
        return displayName + " — " + params;
    }

    public static ActionType fromKey(String key) {
        if (key == null) {
            return SEND_MESSAGE;
        }
        String norm = key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (ActionType t : values()) {
            if (t.key.equals(norm) || t.name().equalsIgnoreCase(norm)) {
                return t;
            }
        }
        return SEND_MESSAGE;
    }

    public static List<ActionType> all() {
        return List.of(values());
    }
}