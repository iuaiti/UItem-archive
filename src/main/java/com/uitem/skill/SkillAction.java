package com.uitem.skill;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Đại diện cho một hành động (action) trong skill.
 * Dễ mở rộng: thêm type mới vào enum + xử lý trong SkillExecutor.
 */
public class SkillAction {

    public enum ActionType {
        // Communication
        MESSAGE("Gửi tin nhắn"),
        ACTION_BAR("Gửi action bar"),
        TITLE("Gửi title + subtitle"),

        // Audio/Visual
        PLAY_SOUND("Phát âm thanh"),
        SPAWN_PARTICLE("Tạo particle"),

        // Combat / Stat
        DAMAGE("Gây sát thương"),
        HEAL("Hồi máu"),
        FEED("Hồi đói"),
        SET_FIRE("Đốt cháy"),

        // Movement
        LAUNCH("Đẩy / Launch (velocity)"),
        TELEPORT("Dịch chuyển"),

        // World effect
        STRIKE_LIGHTNING("Gọi sét"),
        SHOOT_PROJECTILE("Bắn projectile (fireball, arrow...)"),

        // Command
        PLAYER_COMMAND("Chạy lệnh như player"),
        CONSOLE_COMMAND("Chạy lệnh console"),

        // Item / Durability
        CONSUME_DURABILITY("Tiêu hao độ bền"),
        CONSUME_ITEM("Tiêu hao 1 item (tay đang cầm)"),

        // Potion
        GIVE_POTION("Cho potion effect"),
        REMOVE_POTION("Xóa potion effect"),

        // Other
        GIVE_ITEM("Give item (vanilla hoặc uitem)"),
        NOTHING("Không làm gì (placeholder)");
        
        private final String display;

        ActionType(String display) {
            this.display = display;
        }

        public String getDisplay() { return display; }
    }

    private ActionType type;
    private Map<String, Object> params;

    public SkillAction() {
        this.type = ActionType.MESSAGE;
        this.params = new LinkedHashMap<>();
    }

    public SkillAction(ActionType type) {
        this.type = type;
        this.params = new LinkedHashMap<>();
    }

    public SkillAction(ActionType type, Map<String, Object> params) {
        this.type = type;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public void setParam(String key, Object value) {
        if (params == null) params = new LinkedHashMap<>();
        params.put(key, value);
    }

    public Object getParam(String key) {
        return params != null ? params.get(key) : null;
    }

    public String getStringParam(String key, String def) {
        Object v = getParam(key);
        return v != null ? String.valueOf(v) : def;
    }

    public double getDoubleParam(String key, double def) {
        Object v = getParam(key);
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    public int getIntParam(String key, int def) {
        Object v = getParam(key);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    public boolean getBooleanParam(String key, boolean def) {
        Object v = getParam(key);
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    // ==================== Factory helpers (dễ dùng khi code) ====================

    public static SkillAction message(String text) {
        SkillAction a = new SkillAction(ActionType.MESSAGE);
        a.setParam("text", text);
        return a;
    }

    public static SkillAction actionBar(String text) {
        SkillAction a = new SkillAction(ActionType.ACTION_BAR);
        a.setParam("text", text);
        return a;
    }

    public static SkillAction playSound(String sound, float volume, float pitch) {
        SkillAction a = new SkillAction(ActionType.PLAY_SOUND);
        a.setParam("sound", sound);
        a.setParam("volume", volume);
        a.setParam("pitch", pitch);
        return a;
    }

    public static SkillAction givePotion(String effect, int durationSeconds, int amplifier) {
        SkillAction a = new SkillAction(ActionType.GIVE_POTION);
        a.setParam("effect", effect);
        a.setParam("duration", durationSeconds);
        a.setParam("amplifier", amplifier);
        return a;
    }

    public static SkillAction damage(double amount) {
        SkillAction a = new SkillAction(ActionType.DAMAGE);
        a.setParam("amount", amount);
        return a;
    }

    public static SkillAction heal(double amount) {
        SkillAction a = new SkillAction(ActionType.HEAL);
        a.setParam("amount", amount);
        return a;
    }

    public static SkillAction launch(double x, double y, double z) {
        SkillAction a = new SkillAction(ActionType.LAUNCH);
        a.setParam("x", x);
        a.setParam("y", y);
        a.setParam("z", z);
        return a;
    }

    public static SkillAction shootProjectile(String projectile, double speed, double damage) {
        SkillAction a = new SkillAction(ActionType.SHOOT_PROJECTILE);
        a.setParam("projectile", projectile); // FIREBALL, ARROW, SMALL_FIREBALL, WITHER_SKULL, etc.
        a.setParam("speed", speed);
        a.setParam("damage", damage);
        return a;
    }

    public static SkillAction consoleCommand(String cmd) {
        SkillAction a = new SkillAction(ActionType.CONSOLE_COMMAND);
        a.setParam("command", cmd);
        return a;
    }

    public static SkillAction playerCommand(String cmd) {
        SkillAction a = new SkillAction(ActionType.PLAYER_COMMAND);
        a.setParam("command", cmd);
        return a;
    }

    public static SkillAction strikeLightning() {
        return new SkillAction(ActionType.STRIKE_LIGHTNING);
    }

    public static SkillAction consumeDurability(int amount) {
        SkillAction a = new SkillAction(ActionType.CONSUME_DURABILITY);
        a.setParam("amount", amount);
        return a;
    }

    // Deep copy
    public SkillAction copy() {
        return new SkillAction(this.type, new LinkedHashMap<>(this.params));
    }

    @Override
    public String toString() {
        return type + " " + params;
    }
}
