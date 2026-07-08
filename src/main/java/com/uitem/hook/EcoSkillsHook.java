package com.uitem.hook;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Hook for EcoSkills (Auxilor).
 * <p>
 * EcoSkills is closed-source and its API surface has shifted between major
 * versions, so this hook resolves everything by reflection and probes a couple
 * of known shapes:
 * <ul>
 *   <li>Registry: {@code com.willfp.ecoskills.skills.Skills.INSTANCE.get(String)}</li>
 *   <li>Level:    a static {@code getSkillLevel(Player/OfflinePlayer, Skill)} on
 *       {@code com.willfp.ecoskills.api.EcoSkillsAPI}</li>
 * </ul>
 * If the installed EcoSkills version doesn't match any probe, the constructor
 * throws and the manager reports the hook as unavailable (with a log line), so
 * the server keeps running normally.
 */
final class EcoSkillsHook implements SkillsHook {

    private final Logger logger;

    private final Object skillsRegistry;      // Skills.INSTANCE
    private final Method registryGet;         // Skills#get(String) -> Skill
    private final Method getSkillLevel;       // static EcoSkillsAPI.getSkillLevel(player, skill) -> int
    private final Method giveSkillXp;         // static xp method, may be null
    private final Method getMagic;            // static getMagic/getMana(player), may be null
    private final Method setMagic;            // static setMagic/setMana(player, double), may be null
    private final Method getMaxMagic;         // static max variant, may be null

    private final Map<String, Object> skillCache = new ConcurrentHashMap<>();
    private static final Object MISSING = new Object();

    EcoSkillsHook(Logger logger) throws Exception {
        this.logger = logger;

        Class<?> skillsClass = Class.forName("com.willfp.ecoskills.skills.Skills");
        this.skillsRegistry = skillsClass.getField("INSTANCE").get(null);
        this.registryGet = findMethod(skillsClass, "get", String.class);
        if (registryGet == null) {
            throw new NoSuchMethodException("Skills.get(String) not found");
        }

        Class<?> apiClass = Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI");
        Method level = null;
        Method xp = null;
        for (Method m : apiClass.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 2) continue;
            Class<?> p0 = m.getParameterTypes()[0];
            boolean playerParam = p0.isAssignableFrom(Player.class) || p0 == OfflinePlayer.class;
            if (!playerParam) continue;
            String name = m.getName();
            if (level == null && name.equals("getSkillLevel")) level = m;
        }
        for (Method m : apiClass.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 3) continue;
            String name = m.getName().toLowerCase(Locale.ROOT);
            if (xp == null && (name.contains("gainskillxp") || name.contains("giveskillxp")
                    || name.contains("giveskillexperience"))) {
                xp = m;
            }
        }
        if (level == null) {
            throw new NoSuchMethodException("EcoSkillsAPI.getSkillLevel(player, skill) not found "
                    + "(installed EcoSkills version may use a different API — adjust EcoSkillsHook)");
        }
        this.getSkillLevel = level;
        this.giveSkillXp = xp;

        // Mana ("magic" in EcoSkills) — optional capability, probe known names quietly
        Method magicGet = null, magicSet = null, magicMax = null;
        for (Method m : apiClass.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            String name = m.getName().toLowerCase(Locale.ROOT);
            boolean playerFirst = m.getParameterCount() >= 1
                    && (m.getParameterTypes()[0].isAssignableFrom(Player.class)
                        || m.getParameterTypes()[0] == OfflinePlayer.class);
            if (!playerFirst) continue;
            if (m.getParameterCount() == 1) {
                if (magicMax == null && (name.equals("getmaxmagic") || name.equals("getmaxmana"))) magicMax = m;
                else if (magicGet == null && (name.equals("getmagic") || name.equals("getmana"))) magicGet = m;
            } else if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == double.class) {
                if (magicSet == null && (name.equals("setmagic") || name.equals("setmana"))) magicSet = m;
            }
        }
        this.getMagic = magicGet;
        this.setMagic = magicSet;
        this.getMaxMagic = magicMax;
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
        return "EcoSkills";
    }

    @Override
    public int getSkillLevel(Player player, String skillId) {
        try {
            Object skill = resolveSkill(skillId);
            if (skill == null) return -1;
            Object result = getSkillLevel.invoke(null, player, skill);
            return result instanceof Number n ? n.intValue() : -1;
        } catch (Throwable t) {
            logger.warning("[UItem] EcoSkills level lookup failed for '" + skillId + "': " + t.getMessage());
            return -1;
        }
    }

    @Override
    public boolean hasSkill(String skillId) {
        return resolveSkill(skillId) != null;
    }

    @Override
    public boolean giveSkillXp(Player player, String skillId, double amount) {
        if (giveSkillXp == null) return false;
        try {
            Object skill = resolveSkill(skillId);
            if (skill == null) return false;
            giveSkillXp.invoke(null, player, skill, amount);
            return true;
        } catch (Throwable t) {
            logger.warning("[UItem] EcoSkills giveSkillXp failed for '" + skillId + "': " + t.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasManaSupport() {
        return getMagic != null && setMagic != null;
    }

    @Override
    public double getMana(Player player) {
        if (getMagic == null) return -1;
        try {
            Object result = getMagic.invoke(null, player);
            return result instanceof Number n ? n.doubleValue() : -1;
        } catch (Throwable t) {
            logger.warning("[UItem] EcoSkills getMana failed: " + t.getMessage());
            return -1;
        }
    }

    @Override
    public double getMaxMana(Player player) {
        if (getMaxMagic == null) return -1;
        try {
            Object result = getMaxMagic.invoke(null, player);
            return result instanceof Number n ? n.doubleValue() : -1;
        } catch (Throwable t) {
            logger.warning("[UItem] EcoSkills getMaxMana failed: " + t.getMessage());
            return -1;
        }
    }

    @Override
    public boolean consumeMana(Player player, double amount) {
        if (!hasManaSupport() || amount <= 0) return false;
        try {
            double mana = getMana(player);
            if (mana < 0) return false;
            setMagic.invoke(null, player, Math.max(0, mana - amount));
            return true;
        } catch (Throwable t) {
            logger.warning("[UItem] EcoSkills consumeMana failed: " + t.getMessage());
            return false;
        }
    }

    private Object resolveSkill(String skillId) {
        if (skillId == null || skillId.isBlank()) return null;
        Object cached = skillCache.computeIfAbsent(skillId.toLowerCase(Locale.ROOT), id -> {
            try {
                Object skill = registryGet.invoke(skillsRegistry, id);
                if (skill == null) {
                    logger.warning("[UItem] EcoSkills: unknown skill id '" + id + "'");
                    return MISSING;
                }
                return skill;
            } catch (Throwable t) {
                logger.warning("[UItem] EcoSkills: cannot resolve skill '" + id + "': " + t.getMessage());
                return MISSING;
            }
        });
        return cached == MISSING ? null : cached;
    }
}
