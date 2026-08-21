package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.SkillAction;
import com.uitem.skill.SkillExecutor;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ParticlePickerGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private int currentPage = 0;
    private static final int[] PARTICLE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int ITEMS_PER_PAGE = PARTICLE_SLOTS.length;

    // Only particles that need no extra data object (DUST, BLOCK, ITEM... require
    // a data param that SPAWN_PARTICLE doesn't support and would throw at spawn time).
    private static final List<Particle> ALL_PARTICLES = Arrays.stream(Particle.values())
            .filter(p -> p.getDataType() == Void.class)
            .toList();

    public ParticlePickerGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, "<gold><bold>Particle Picker</bold>");
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        int total = ALL_PARTICLES.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        setItem(4, createItem(Material.FIREWORK_STAR,
                "<gold><bold>Particle Effects</bold>",
                List.of(
                        "<gray>Page: <white>" + (currentPage + 1) + "/" + totalPages,
                        "<gray>Total: <white>" + total + " particles",
                        "",
                        "<yellow>Click a particle to configure"
                )));

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, total);

        for (int i = startIdx; i < endIdx; i++) {
            final Particle particle = ALL_PARTICLES.get(i);
            int slot = PARTICLE_SLOTS[i - startIdx];
            setItem(slot, createParticleIcon(particle), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                promptParticleConfig(particle);
            });
        }

        if (currentPage > 0) {
            setItem(48, createItem(Material.ARROW, "<gray>< Previous Page"), ev -> {
                currentPage--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        if (endIdx < total) {
            setItem(50, createItem(Material.ARROW, "<gray>Next Page >"), ev -> {
                currentPage++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack createParticleIcon(Particle particle) {
        Material mat = switch (particle.name()) {
            case "FLAME" -> Material.BLAZE_POWDER;
            case "HEART" -> Material.REDSTONE;
            case "SOUL_FIRE_FLAME" -> Material.SOUL_SOIL;
            case "DRAGON_BREATH" -> Material.DRAGON_BREATH;
            case "ENCHANTMENT_TABLE" -> Material.ENCHANTING_TABLE;
            case "PORTAL" -> Material.OBSIDIAN;
            case "EXPLOSION" -> Material.TNT;
            case "END_ROD" -> Material.END_ROD;
            case "GLOW" -> Material.GLOWSTONE_DUST;
            case "SLIME" -> Material.SLIME_BALL;
            case "SNOWFLAKE" -> Material.SNOWBALL;
            case "WITCH" -> Material.BREWING_STAND;
            case "CAMPFIRE_COSY_SMOKE" -> Material.CAMPFIRE;
            case "ELECTRIC_SPARK" -> Material.LIGHTNING_ROD;
            case "SCRAPE" -> Material.TUFF;
            case "DUST" -> Material.GUNPOWDER;
            case "ITEM_SNOWBALL" -> Material.SNOWBALL;
            case "ITEM_SLIME" -> Material.SLIME_BALL;
            default -> Material.FIREWORK_STAR;
        };
        String name = particle.name().toLowerCase(Locale.ROOT).replace("_", " ");
        name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
        return createItem(mat,
                "<gold>" + name,
                List.of(
                        "<gray>Particle: <white>" + particle.name(),
                        "",
                        "<yellow>Click to configure"
                ));
    }

    private void promptParticleConfig(Particle particle) {
        SkillAction action = new SkillAction(SkillAction.ActionType.SPAWN_PARTICLE);
        action.setParam("particle", particle.name());
        action.setParam("count", 10);
        action.setParam("offset-x", 0.3);
        action.setParam("offset-y", 0.3);
        action.setParam("offset-z", 0.3);
        action.setParam("speed", 0.01);
        action.setParam("interval", 0);

        promptCount(action);
    }

    private void promptCount(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter particle count (1-" + SkillExecutor.PARTICLE_MAX_COUNT + ", example: 20):",
                input -> {
                    try {
                        int count = Integer.parseInt(input.trim());
                        int clamped = Math.max(1, Math.min(SkillExecutor.PARTICLE_MAX_COUNT, count));
                        if (clamped != count) {
                            player.sendMessage(TextUtil.info("Count clamped to " + clamped));
                        }
                        action.setParam("count", clamped);
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default count: 10"));
                    }
                    promptOffsetX(action);
                });
    }

    private void promptOffsetX(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter offset X (spread, example: 0.3):",
                input -> {
                    try {
                        action.setParam("offset-x", clampOffset(Double.parseDouble(input.trim())));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default offset-x: 0.3"));
                    }
                    promptOffsetY(action);
                });
    }

    private void promptOffsetY(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter offset Y (spread, example: 0.3):",
                input -> {
                    try {
                        action.setParam("offset-y", clampOffset(Double.parseDouble(input.trim())));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default offset-y: 0.3"));
                    }
                    promptOffsetZ(action);
                });
    }

    private void promptOffsetZ(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter offset Z (spread, example: 0.3):",
                input -> {
                    try {
                        action.setParam("offset-z", clampOffset(Double.parseDouble(input.trim())));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default offset-z: 0.3"));
                    }
                    promptSpeed(action);
                });
    }

    private void promptSpeed(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter speed/extra (example: 0.02):",
                input -> {
                    try {
                        double speed = Double.parseDouble(input.trim());
                        action.setParam("speed", Math.max(0, Math.min(SkillExecutor.PARTICLE_MAX_SPEED, speed)));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default speed: 0.01"));
                    }
                    promptInterval(action);
                });
    }

    private void promptInterval(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter interval between spawns in ticks (0 = spawn all at once, max "
                        + SkillExecutor.PARTICLE_MAX_INTERVAL + ", example: 5):",
                input -> {
                    try {
                        int interval = Integer.parseInt(input.trim());
                        action.setParam("interval", Math.max(0, Math.min(SkillExecutor.PARTICLE_MAX_INTERVAL, interval)));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default interval: 0 (all at once)"));
                    }
                    // 'count' means particles per spawn and 'repeat' means how many
                    // spawns — only meaningful when the effect is spread over time.
                    if (action.getIntParam("interval", 0) > 0) {
                        promptRepeat(action);
                    } else {
                        finishParticle(action);
                    }
                });
    }

    private void promptRepeat(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter how many times to spawn (repeat, 1-"
                        + SkillExecutor.PARTICLE_MAX_COUNT + ", example: 20):",
                input -> {
                    try {
                        int repeat = Integer.parseInt(input.trim());
                        action.setParam("repeat",
                                Math.max(1, Math.min(SkillExecutor.PARTICLE_MAX_COUNT, repeat)));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default repeat: 10"));
                        action.setParam("repeat", 10);
                    }
                    finishParticle(action);
                });
    }

    private static double clampOffset(double value) {
        return Math.max(0, Math.min(SkillExecutor.PARTICLE_MAX_OFFSET, value));
    }

    private void finishParticle(SkillAction action) {
        skill.addAction(action);
        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), itemData);

        String name = action.getStringParam("particle", "FLAME").toLowerCase(Locale.ROOT).replace("_", " ");
        int interval = action.getIntParam("interval", 0);
        String summary = "count=" + action.getIntParam("count", 10);
        if (interval > 0) {
            summary += ", repeat=" + action.getIntParam("repeat", 10) + " every " + interval + "t";
        }
        player.sendMessage(TextUtil.success("<green>✓ Added particle: " + name + " (" + summary + ")"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        goBack();
    }
}
