package com.uitem.hook;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Hook for AuraSkills (https://wiki.aurelium.dev/auraskills).
 * <p>
 * Uses reflection against the public API (dev.aurelium.auraskills.api) so UItem
 * compiles and runs without the plugin present. The constructor resolves every
 * class/method up front and throws if anything is missing — the manager treats
 * that as "hook unavailable".
 */
final class AuraSkillsHook implements SkillsHook {

    private final Logger logger;

    private final Object api;                 // AuraSkillsApi instance
    private final Method getUser;             // AuraSkillsApi#getUser(UUID) -> SkillsUser
    private final Method getSkill;            // GlobalRegistry#getSkill(NamespacedId) -> Skill
    private final Method fromDefault;         // NamespacedId.fromDefault(String)
    private final Object registry;            // GlobalRegistry instance
    private final Method getSkillLevel;       // SkillsUser#getSkillLevel(Skill) -> int
    private final Method addSkillXp;          // SkillsUser#addSkillXp(Skill, double), may be null
    private final Method getMana;             // SkillsUser#getMana() -> double, may be null
    private final Method setMana;             // SkillsUser#setMana(double), may be null
    private final Method getMaxMana;          // SkillsUser#getMaxMana() -> double, may be null

    /** Cache of resolved Skill objects; null value = known-missing skill id. */
    private final Map<String, Object> skillCache = new ConcurrentHashMap<>();
    private static final Object MISSING = new Object();

    AuraSkillsHook(Logger logger) throws Exception {
        this.logger = logger;

        Class<?> apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
        this.api = apiClass.getMethod("get").invoke(null);
        this.getUser = apiClass.getMethod("getUser", UUID.class);
        this.registry = apiClass.getMethod("getGlobalRegistry").invoke(api);

        Class<?> nidClass = Class.forName("dev.aurelium.auraskills.api.registry.NamespacedId");
        this.fromDefault = nidClass.getMethod("fromDefault", String.class);
        // Look the method up on the API type, not the runtime impl class —
        // the impl may be package-private and would fail on invoke.
        Class<?> registryClass = Class.forName("dev.aurelium.auraskills.api.registry.GlobalRegistry");
        this.getSkill = registryClass.getMethod("getSkill", nidClass);

        Class<?> skillClass = Class.forName("dev.aurelium.auraskills.api.skill.Skill");
        Class<?> userClass = Class.forName("dev.aurelium.auraskills.api.user.SkillsUser");
        this.getSkillLevel = userClass.getMethod("getSkillLevel", skillClass);

        Method xp = null;
        try {
            xp = userClass.getMethod("addSkillXp", skillClass, double.class);
        } catch (NoSuchMethodException ignored) {
            // Older API — XP giving simply unsupported
        }
        this.addSkillXp = xp;

        // Mana — optional capability, probe quietly
        this.getMana = findMethod(userClass, "getMana");
        this.setMana = findMethod(userClass, "setMana", double.class);
        this.getMaxMana = findMethod(userClass, "getMaxMana");
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            return cls.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "AuraSkills";
    }

    @Override
    public int getSkillLevel(Player player, String skillId) {
        try {
            Object skill = resolveSkill(skillId);
            if (skill == null) return -1;
            Object user = getUser.invoke(api, player.getUniqueId());
            if (user == null) return -1;
            return (int) getSkillLevel.invoke(user, skill);
        } catch (Throwable t) {
            logger.warning("[UItem] AuraSkills level lookup failed for '" + skillId + "': " + t.getMessage());
            return -1;
        }
    }

    @Override
    public boolean hasSkill(String skillId) {
        return resolveSkill(skillId) != null;
    }

    @Override
    public boolean giveSkillXp(Player player, String skillId, double amount) {
        if (addSkillXp == null) return false;
        try {
            Object skill = resolveSkill(skillId);
            if (skill == null) return false;
            Object user = getUser.invoke(api, player.getUniqueId());
            if (user == null) return false;
            addSkillXp.invoke(user, skill, amount);
            return true;
        } catch (Throwable t) {
            logger.warning("[UItem] AuraSkills giveSkillXp failed for '" + skillId + "': " + t.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasManaSupport() {
        return getMana != null && setMana != null;
    }

    @Override
    public double getMana(Player player) {
        if (getMana == null) return -1;
        try {
            Object user = getUser.invoke(api, player.getUniqueId());
            if (user == null) return -1;
            Object result = getMana.invoke(user);
            return result instanceof Number n ? n.doubleValue() : -1;
        } catch (Throwable t) {
            logger.warning("[UItem] AuraSkills getMana failed: " + t.getMessage());
            return -1;
        }
    }

    @Override
    public double getMaxMana(Player player) {
        if (getMaxMana == null) return -1;
        try {
            Object user = getUser.invoke(api, player.getUniqueId());
            if (user == null) return -1;
            Object result = getMaxMana.invoke(user);
            return result instanceof Number n ? n.doubleValue() : -1;
        } catch (Throwable t) {
            logger.warning("[UItem] AuraSkills getMaxMana failed: " + t.getMessage());
            return -1;
        }
    }

    @Override
    public boolean consumeMana(Player player, double amount) {
        if (!hasManaSupport() || amount <= 0) return false;
        try {
            Object user = getUser.invoke(api, player.getUniqueId());
            if (user == null) return false;
            Object current = getMana.invoke(user);
            double mana = current instanceof Number n ? n.doubleValue() : -1;
            if (mana < 0) return false;
            setMana.invoke(user, Math.max(0, mana - amount));
            return true;
        } catch (Throwable t) {
            logger.warning("[UItem] AuraSkills consumeMana failed: " + t.getMessage());
            return false;
        }
    }

    private Object resolveSkill(String skillId) {
        if (skillId == null || skillId.isBlank()) return null;
        Object cached = skillCache.computeIfAbsent(skillId.toLowerCase(java.util.Locale.ROOT), id -> {
            try {
                Object nid = fromDefault.invoke(null, id);
                Object skill = getSkill.invoke(registry, nid);
                if (skill == null) {
                    logger.warning("[UItem] AuraSkills: unknown skill id '" + id + "'");
                    return MISSING;
                }
                return skill;
            } catch (Throwable t) {
                logger.warning("[UItem] AuraSkills: cannot resolve skill '" + id + "': " + t.getMessage());
                return MISSING;
            }
        });
        return cached == MISSING ? null : cached;
    }
}
