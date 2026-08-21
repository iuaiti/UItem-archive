package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI to give multiple items at once to one or more players.
 * A new feature similar to Oraxen/ItemsAdder but through a GUI.
 */
public class BulkGiveGui extends AbstractGui {

    private int page = 0;
    private final Set<String> selectedItems = new LinkedHashSet<>();
    private int giveAmount = 1;

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.length;

    public BulkGiveGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Bulk Give</bold></gradient>");
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.CHEST,
                "<gradient:#ff6b6b:#ffd93d><bold>Bulk Give</bold></gradient>",
                List.of(
                        "<gray>Click an item to select/deselect",
                        "<gray>Then choose a player to give to",
                        "",
                        "<yellow>Selected: <white>" + selectedItems.size() + " item(s)",
                        "<yellow>Amount per item: <white>x" + giveAmount
                )));

        // Items list
        List<UItemData> allItems = new ArrayList<>(plugin.getItemManager().getAllItems().values());
        allItems.sort(Comparator.comparing(UItemData::getId, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex >= allItems.size()) break;

            UItemData data = allItems.get(dataIndex);
            boolean selected = selectedItems.contains(data.getId());

            ItemStack item = plugin.getItemManager().buildItemStack(data);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>──────────────"));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>ID: <white>" + data.getId()));
                if (selected) {
                    lore.add(MiniMessage.miniMessage().deserialize("<green>✔ SELECTED"));
                    lore.add(MiniMessage.miniMessage().deserialize("<yellow>Click to deselect"));
                } else {
                    lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to select"));
                }
                meta.lore(lore);

                // Highlight selected items with enchant glow
                if (selected) {
                    meta.setEnchantmentGlintOverride(true);
                }
                item.setItemMeta(meta);
            }

            setItem(ITEM_SLOTS[i], item, ev -> {
                if (selectedItems.contains(data.getId())) {
                    selectedItems.remove(data.getId());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 0.8f);
                } else {
                    selectedItems.add(data.getId());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
                }
                refresh();
            });
        }

        // Amount selector
        setItem(37, createItem(Material.GOLD_INGOT,
                "<gold><bold>Amount: x" + giveAmount + "</bold>",
                List.of(
                        "<gray>Amount of each item to give",
                        "<yellow>Left: +1 | Right: -1",
                        "<green>Shift+Left: +8 | Shift+Right: -8"
                )), ev -> {
            if (ev.getClick() == ClickType.LEFT) giveAmount = Math.min(64, giveAmount + 1);
            else if (ev.getClick() == ClickType.RIGHT) giveAmount = Math.max(1, giveAmount - 1);
            else if (ev.getClick() == ClickType.SHIFT_LEFT) giveAmount = Math.min(64, giveAmount + 8);
            else if (ev.getClick() == ClickType.SHIFT_RIGHT) giveAmount = Math.max(1, giveAmount - 8);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            refresh();
        });

        // Select all / Deselect all
        setItem(38, createItem(Material.LIME_DYE,
                "<green><bold>Select All</bold>",
                List.of("<gray>Select all items on this page")),
                ev -> {
                    int start = page * ITEMS_PER_PAGE;
                    for (int i = start; i < Math.min(start + ITEMS_PER_PAGE, allItems.size()); i++) {
                        selectedItems.add(allItems.get(i).getId());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
                    refresh();
                });

        setItem(39, createItem(Material.RED_DYE,
                "<red><bold>Deselect All</bold>",
                List.of("<gray>Deselect all items")),
                ev -> {
                    selectedItems.clear();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 0.8f);
                    refresh();
                });

        // Give to self
        setItem(41, createItem(Material.PLAYER_HEAD,
                "<green><bold>Give To Self</bold>",
                List.of(
                        "<gray>Give " + selectedItems.size() + " item(s) to you",
                        "<gray>Each item: x" + giveAmount,
                        "",
                        selectedItems.isEmpty() ? "<red>No items selected!" : "<yellow>Click to give"
                )), ev -> {
            if (selectedItems.isEmpty()) {
                player.sendMessage(TextUtil.error("No items selected!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            giveItemsToPlayer(player, player);
        });

        // Give to another player
        setItem(42, createItem(Material.EMERALD,
                "<aqua><bold>Give To Another Player</bold>",
                List.of(
                        "<gray>Enter a player name to give to",
                        "<gray>Each item: x" + giveAmount,
                        "",
                        selectedItems.isEmpty() ? "<red>No items selected!" : "<yellow>Click to enter name"
                )), ev -> {
            if (selectedItems.isEmpty()) {
                player.sendMessage(TextUtil.error("No items selected!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Enter player name to give to (or 'cancel' to abort):",
                            input -> {
                                // No 'cancel' check needed: ChatInputManager handles the
                                // cancel words and reopens this GUI before we get here.
                                Player target = Bukkit.getPlayerExact(input.trim());
                                if (target == null) {
                                    player.sendMessage(TextUtil.error("Player '" + input + "' is not online!"));
                                    reopen();
                                    return;
                                }
                                giveItemsToPlayer(player, target);
                            });
        });

        // Give to all online players
        setItem(43, createItem(Material.BEACON,
                "<gold><bold>Give To All Online</bold>",
                List.of(
                        "<gray>Give to all currently online players",
                        "<gray>Each item: x" + giveAmount,
                        "",
                        selectedItems.isEmpty() ? "<red>No items selected!" : "<yellow>Click to give"
                )), ev -> {
            if (selectedItems.isEmpty()) {
                player.sendMessage(TextUtil.error("No items selected!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            int count = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                giveItemsToPlayer(player, online);
                count++;
            }
            player.sendMessage(TextUtil.success("Gave " + selectedItems.size() + " item(s) to " + count + " players!"));
        });

        // Pagination
        if (page > 0) {
            setItem(46, createPreviousButton(), ev -> {
                page--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }
        if (page < totalPages - 1) {
            setItem(52, createNextButton(), ev -> {
                page++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        // Back
        setItem(49, createBackButton(), ev -> goBack());
    }

    private void giveItemsToPlayer(Player sender, Player target) {
        if (!sender.hasPermission("uitem.give")) {
            sender.sendMessage(TextUtil.error("You do not have permission to give items!"));
            return;
        }

        int successCount = 0;
        for (String itemId : selectedItems) {
            boolean ok = com.uitem.api.UItemAPI.get().giveItem(target, itemId, giveAmount, sender);
            if (ok) successCount++;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        sender.sendMessage(TextUtil.success("Gave " + successCount + "/" + selectedItems.size() +
                " item(s) x" + giveAmount + " to " + target.getName() + "!"));
        if (!sender.equals(target)) {
            target.sendMessage(TextUtil.success("You received " + successCount + " item(s) from " + sender.getName() + "!"));
        }
    }
}
