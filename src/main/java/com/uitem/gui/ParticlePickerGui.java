package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.skill.Skill;
import com.uitem.skill.SkillAction;
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
    private static final int ITEMS_PER_PAGE = 16;

    private static final List<Particle> ALL_PARTICLES = new ArrayList<>(List.of(Particle.values()));

    public ParticlePickerGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, "<gold><bold>Particle Picker</bold>");
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        int total = ALL_PARTICLES.size();
        int totalPages = (int) Math.ceil((double) total / ITEMS_PER_PAGE);

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

        int slot = 10;
        int slotsInRow = 0;
        for (int i = startIdx; i < endIdx; i++) {
            if (slotsInRow == 7) {
                slotsInRow = 0;
                slot += 2;
            }

            final Particle particle = ALL_PARTICLES.get(i);
            setItem(slot, createParticleIcon(particle), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                promptParticleConfig(particle);
            });

            slot++;
            slotsInRow++;
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
                "<yellow>Enter particle count (example: 20):",
                input -> {
                    try {
                        action.setParam("count", Integer.parseInt(input.trim()));
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
                        action.setParam("offset-x", Double.parseDouble(input.trim()));
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
                        action.setParam("offset-y", Double.parseDouble(input.trim()));
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
                        action.setParam("offset-z", Double.parseDouble(input.trim()));
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
                        action.setParam("speed", Double.parseDouble(input.trim()));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default speed: 0.01"));
                    }
                    promptInterval(action);
                });
    }

    private void promptInterval(SkillAction action) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Enter interval between spawns in ticks (0 = spawn all at once, 1 tick = 1/20 second, example: 5):",
                input -> {
                    try {
                        action.setParam("interval", Integer.parseInt(input.trim()));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.info("Using default interval: 0 (all at once)"));
                    }
                    finishParticle(action);
                });
    }

    private void finishParticle(SkillAction action) {
        skill.addAction(action);
        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveItemDebounced(plugin.getItemManager(), itemData);

        String name = action.getStringParam("particle", "FLAME").toLowerCase(Locale.ROOT).replace("_", " ");
        player.sendMessage(TextUtil.success("<green>✓ Added particle: " + name
                + " (count=" + action.getIntParam("count", 10)
                + ", interval=" + action.getIntParam("interval", 0) + "t)"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        goBack();
    }
}
