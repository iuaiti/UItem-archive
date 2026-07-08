package com.uitem.hook;

import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Detects and holds the single active {@link SkillsHook}.
 * <p>
 * Rule: AT MOST ONE hook is ever active. If both AuraSkills and EcoSkills are
 * installed, the one selected by {@code hooks.skills.plugin} in config.yml wins
 * ({@code auto} prefers AuraSkills and logs that EcoSkills was skipped).
 */
public class SkillsHookManager {

    private final UItemPlugin plugin;
    private SkillsHook active;

    public SkillsHookManager(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Re-detect. Safe to call again (e.g. when a softdepend enables late). */
    public void detect() {
        this.active = null;
        String pref = plugin.getConfig().getString("hooks.skills.plugin", "auto")
                .toLowerCase(Locale.ROOT).trim();

        if (pref.equals("none")) {
            plugin.getLogger().info("[UItem] Skills hook disabled by config (hooks.skills.plugin: none)");
            return;
        }

        boolean auraPresent = isEnabled("AuraSkills");
        boolean ecoPresent = isEnabled("EcoSkills");

        switch (pref) {
            case "auraskills" -> tryActivate("AuraSkills", auraPresent);
            case "ecoskills" -> tryActivate("EcoSkills", ecoPresent);
            default -> { // auto: AuraSkills first, EcoSkills as fallback
                if (auraPresent) tryActivate("AuraSkills", true);
                if (active == null && ecoPresent) tryActivate("EcoSkills", true);
                if (active != null && auraPresent && ecoPresent) {
                    plugin.getLogger().info("[UItem] Both AuraSkills and EcoSkills found — using "
                            + active.getName() + " only (set hooks.skills.plugin to override)");
                }
            }
        }

        if (active == null && (auraPresent || ecoPresent || !pref.equals("auto"))) {
            plugin.getLogger().info("[UItem] Skills hook: none active");
        }
    }

    private boolean isEnabled(String pluginName) {
        Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
        return p != null && p.isEnabled();
    }

    private void tryActivate(String name, boolean present) {
        if (!present) {
            plugin.getLogger().info("[UItem] Skills hook '" + name + "' requested but plugin not found");
            return;
        }
        try {
            this.active = switch (name) {
                case "AuraSkills" -> new AuraSkillsHook(plugin.getLogger());
                case "EcoSkills" -> new EcoSkillsHook(plugin.getLogger());
                default -> null;
            };
            if (active != null) {
                plugin.getLogger().info("  ✔ " + name + " — skills hook active");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("  ○ " + name + " found but hook failed: " + t.getMessage());
            this.active = null;
        }
    }

    /** The single active hook, or null when no skills plugin is hooked. */
    public SkillsHook getActive() {
        return active;
    }

    public boolean isActive() {
        return active != null;
    }

    /** Convenience: level via the active hook, or -1 when no hook is active. */
    public int getSkillLevel(Player player, String skillId) {
        return active != null ? active.getSkillLevel(player, skillId) : -1;
    }
}
