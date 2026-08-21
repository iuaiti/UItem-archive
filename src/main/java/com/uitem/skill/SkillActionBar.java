package com.uitem.skill;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SkillActionBar {

    private final UItemPlugin plugin;
    private final CooldownManager cooldowns;
    private final SkillExecutor executor;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // volatile: set on the main thread at disable, read from region threads.
    private volatile boolean shutdown = false;

    public SkillActionBar(UItemPlugin plugin, CooldownManager cooldowns, SkillExecutor executor) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.executor = executor;
    }

    /**
     * Start the once-per-second cooldown readout.
     *
     * <p>Folia-safe: the global timer only enumerates players, then hands each
     * one to their own entity scheduler so inventory reads happen on the owning
     * region thread. {@code Bukkit.getScheduler().runTaskTimer} throws
     * {@code UnsupportedOperationException} on Folia.
     */
    public void start() {
        SchedulerUtil.runGlobalTimer(plugin, 40L, 20L, () -> {
            if (shutdown) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                SchedulerUtil.runForEntity(plugin, player, () -> {
                    if (shutdown || !player.isOnline()) return;
                    tickPlayer(player);
                });
            }
        });
    }

    public void shutdown() {
        this.shutdown = true;
    }

    private void tickPlayer(Player player) {
        try {
            List<CooldownEntry> entries = new ArrayList<>();

            checkItem(player, player.getInventory().getItemInMainHand(), entries);
            checkItem(player, player.getInventory().getItemInOffHand(), entries);

            if (entries.isEmpty()) return;

            entries.sort(Comparator.comparingDouble(e -> e.remaining));

            int maxShow = Math.min(entries.size(), 3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maxShow; i++) {
                CooldownEntry e = entries.get(i);
                if (sb.length() > 0) sb.append("  ");
                String color = e.remaining > 5 ? "<green>" : e.remaining > 2 ? "<yellow>" : "<red>";
                sb.append(color).append(e.displayName).append(" <white>")
                        .append(String.format("%.1f", e.remaining)).append("s");
            }

            player.sendActionBar(mm.deserialize(sb.toString()));
        } catch (Exception ignored) {
        }
    }

    private void checkItem(Player player, ItemStack item, List<CooldownEntry> entries) {
        if (!UItemAPI.isAvailable()) return;
        UItemData data = UItemAPI.get().getUItemData(item).orElse(null);
        if (data == null || data.getSkills() == null) return;
        String itemId = data.getId();

        for (Skill skill : data.getSkills()) {
            if (skill.getCooldown() <= 0) continue;
            // Use the same CDR-adjusted value the executor gates on, so the HUD
            // never shows time remaining on a skill that is already castable.
            double cooldown = executor.getEffectiveCooldown(player, skill);
            if (cooldown <= 0) continue;

            double remaining = cooldowns.getRemainingSeconds(player, itemId, skill.getId(), cooldown);
            if (remaining <= 0) continue;

            String display = skill.getDisplayName() != null ? skill.getDisplayName() : skill.getId();

            entries.add(new CooldownEntry(display, remaining, cooldown));
        }
    }

    private record CooldownEntry(String displayName, double remaining, double cooldown) {
    }
}
