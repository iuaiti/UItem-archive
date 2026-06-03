package com.uitem.passive.condition;

import com.uitem.passive.gui.ParamKind;
import com.uitem.passive.gui.ParamSpec;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum ConditionType {
    WORLD("world", "The gioi", Material.GRASS_BLOCK,
            List.of(new ParamSpec("world", "Ten world", ParamKind.WORLD, "world"))),
    BIOME("biome", "Quan xa (Biome)", Material.OAK_LEAVES,
            List.of(new ParamSpec("biome", "Biome", ParamKind.BIOME, "PLAINS"))),
    PERMISSION("permission", "Quyen", Material.NAME_TAG,
            List.of(new ParamSpec("permission", "Permission node", ParamKind.PERMISSION, "uitem.passive.use"))),
    ITEM_IN_HAND("item_in_hand", "Item tren tay", Material.DIAMOND,
            List.of(new ParamSpec("material", "Material", ParamKind.MATERIAL, "DIAMOND"))),
    ARMOR("armor", "Giap mac", Material.IRON_CHESTPLATE,
            List.of(
                    new ParamSpec("slot", "Slot (helmet/chest/legs/boots)", ParamKind.TEXT, "chest"),
                    new ParamSpec("material", "Material", ParamKind.MATERIAL, "IRON_CHESTPLATE"))),
    HEALTH_BELOW("health_below", "Mau duoi", Material.RED_DYE,
            List.of(new ParamSpec("value", "Nguong mau", ParamKind.DOUBLE, "10"))),
    HEALTH_ABOVE("health_above", "Mau tren", Material.APPLE,
            List.of(new ParamSpec("value", "Nguong mau", ParamKind.DOUBLE, "10"))),
    FOOD_LEVEL("food_level", "Do doi", Material.COOKED_BEEF,
            List.of(
                    new ParamSpec("min", "Toi thieu", ParamKind.INTEGER, "0"),
                    new ParamSpec("max", "Toi da", ParamKind.INTEGER, "20"))),
    BLOCK_TYPE("block_type", "Loai block", Material.STONE,
            List.of(new ParamSpec("block", "Block", ParamKind.BLOCK, "STONE"))),
    ENTITY_TYPE("entity_type", "Loai entity", Material.ZOMBIE_HEAD,
            List.of(new ParamSpec("entity", "Entity", ParamKind.ENTITY, "ZOMBIE"))),
    CHANCE("chance", "Ti le may man", Material.GOLD_NUGGET,
            List.of(new ParamSpec("percent", "Phan tram", ParamKind.PERCENT, "100"))),
    COOLDOWN("cooldown", "Thoi gian cho", Material.CLOCK,
            List.of(new ParamSpec("seconds", "Giay", ParamKind.INTEGER, "30"))),
    SNEAKING("sneaking", "Dang sneak", Material.LEATHER_BOOTS,
            List.of(new ParamSpec("required", "Phai sneak", ParamKind.BOOLEAN, "true"))),
    FLYING("flying", "Dang bay", Material.ELYTRA,
            List.of(new ParamSpec("required", "Phai bay", ParamKind.BOOLEAN, "true"))),
    WEATHER("weather", "Thoi tiet", Material.WATER_BUCKET,
            List.of(new ParamSpec("weather", "clear / rain / thunder", ParamKind.WEATHER, "clear"))),
    TIME("time", "Thoi gian trong ngay", Material.CLOCK,
            List.of(
                    new ParamSpec("from", "Tick bat dau", ParamKind.INTEGER, "0"),
                    new ParamSpec("to", "Tick ket thuc", ParamKind.INTEGER, "12000"))),
    GAME_MODE("game_mode", "Che do choi", Material.GOLDEN_APPLE,
            List.of(new ParamSpec("mode", "SURVIVAL / CREATIVE / ...", ParamKind.GAME_MODE, "SURVIVAL")));

    private final String key;
    private final String displayName;
    private final Material icon;
    private final List<ParamSpec> params;

    ConditionType(String key, String displayName, Material icon, List<ParamSpec> params) {
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
        StringBuilder sb = new StringBuilder(displayName);
        sb.append(": ");
        boolean first = true;
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    public static ConditionType fromKey(String key) {
        if (key == null) {
            return CHANCE;
        }
        String norm = key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (ConditionType t : values()) {
            if (t.key.equals(norm) || t.name().equalsIgnoreCase(norm)) {
                return t;
            }
        }
        return CHANCE;
    }

    public static List<ConditionType> all() {
        return List.of(values());
    }
}