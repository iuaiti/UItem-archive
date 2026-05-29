package com.uitem.command;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.gui.ItemListGui;
import com.uitem.resourcepack.ResourcePackManager;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Command handler cho /uitem và alias /ui.
 */
public class UItemCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "give", "editor", "reload", "list", "resourcepack", "rp", "craft", "help", "check"
    );

    private static final List<String> RP_ACTIONS = List.of(
            "generate", "export", "apply", "status"
    );

    private final UItemPlugin plugin;
    private final MiniMessage miniMessage;

    public UItemCommand(UItemPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "give" -> handleGive(sender, args);
            case "editor", "edit", "gui" -> handleEditor(sender);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "resourcepack", "rp" -> handleResourcePack(sender, args);
            case "craft", "crafting" -> handleCraft(sender);
            case "check" -> handleCheck(sender);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(TextUtil.error("Invalid command. Use /" + label + " help"));
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("uitem.give")) {
            sender.sendMessage(TextUtil.error("You don't have permission to use this command!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(TextUtil.error("Usage: /uitem give <id> [player] [amount]"));
            return;
        }

        String itemId = args[1].toLowerCase(Locale.ROOT);
        if (!plugin.getItemManager().hasItem(itemId)) {
            sender.sendMessage(TextUtil.error("Item '" + itemId + "' does not exist!"));
            return;
        }

        GiveTarget target = resolveGiveTarget(sender, args);
        if (target == null) {
            return;
        }

        // Dùng UItemAPI.giveItem để fire UItemGiveEvent
        boolean success = com.uitem.api.UItemAPI.get().giveItem(
                target.player(), itemId, target.amount(), sender);

        if (success) {
            sender.sendMessage(TextUtil.success(
                    "Gave " + target.player().getName() + " x" + target.amount() + " " + itemId));
            if (!sender.equals(target.player())) {
                target.player().sendMessage(TextUtil.success(
                        "You received x" + target.amount() + " " + itemId));
            }
        } else {
            sender.sendMessage(TextUtil.error("Cannot give item (may have been blocked by another plugin)."));
        }
    }

    private GiveTarget resolveGiveTarget(CommandSender sender, String[] args) {
        // /uitem give <id>
        if (args.length == 2) {
            if (sender instanceof Player player) {
                return new GiveTarget(player, 1);
            }
            sender.sendMessage(TextUtil.error("Console must specify a player: /uitem give <id> <player> [amount]"));
            return null;
        }

        String token = args[2];
        Integer amountFromToken = tryParseAmount(token);

        // /uitem give <id> <amount>  -> chỉ cho player hiện tại
        if (args.length == 3 && amountFromToken != null) {
            if (sender instanceof Player player) {
                return new GiveTarget(player, amountFromToken);
            }
            sender.sendMessage(TextUtil.error("Console must specify a player: /uitem give <id> <player> [amount]"));
            return null;
        }

        // /uitem give <id> <player> [amount]
        Player target = Bukkit.getPlayerExact(token);
        if (target == null) {
            sender.sendMessage(TextUtil.error("Player '" + token + "' is not online!"));
            return null;
        }

        int amount = 1;
        if (args.length >= 4) {
            Integer parsed = tryParseAmount(args[3]);
            if (parsed == null) {
                sender.sendMessage(TextUtil.error("Invalid amount! Must be an integer from 1 to 64."));
                return null;
            }
            amount = parsed;
        }

        return new GiveTarget(target, amount);
    }

    private Integer tryParseAmount(String token) {
        try {
            int amount = Integer.parseInt(token);
            if (amount < 1 || amount > 64) {
                throw new NumberFormatException("out of range");
            }
            return amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleEditor(CommandSender sender) {
        if (!sender.hasPermission("uitem.editor")) {
            sender.sendMessage(TextUtil.error("You don't have permission to use this command!"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error("This command can only be used by players!"));
            return;
        }

        new ItemListGui(plugin, player).open();
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("uitem.reload")) {
            sender.sendMessage(TextUtil.error("You don't have permission to use this command!"));
            return;
        }

        plugin.reloadConfig();
        plugin.getStorageManager().saveAllNow(plugin.getItemManager());
        plugin.getStorageManager().loadAll(plugin.getItemManager());
        plugin.getIntegrationManager().detectIntegrations();

        // Bug E fix: reload ResourcePackManager config fields
        plugin.getResourcePackManager().reloadFromConfig(plugin.getConfig());
        plugin.getResourcePackManager().syncCmdsToLiveItems();

        boolean autoGenerate = plugin.getConfig().getBoolean("resourcepack.auto-generate", true);
        boolean autoApply = plugin.getConfig().getBoolean("resourcepack.auto-apply", false);
        String url = plugin.getConfig().getString("resourcepack.url", "");

        if (autoGenerate) {
            // Chạy async để không block
            com.uitem.util.SchedulerUtil.runAsync(plugin, () -> {
                try {
                    plugin.getResourcePackManager().exportZip();
                    com.uitem.util.SchedulerUtil.runSync(plugin,
                            () -> sender.sendMessage(TextUtil.success("Resource pack has been re-generated and exported.")));
                    if (autoApply && url != null && !url.isBlank()) {
                        com.uitem.util.SchedulerUtil.runSync(plugin, () -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                plugin.getResourcePackManager().applyResourcePack(player, url);
                            }
                        });
                    }
                } catch (Exception e) {
                    com.uitem.util.SchedulerUtil.runSync(plugin,
                            () -> sender.sendMessage(TextUtil.error("Cannot generate/export resource pack: " + e.getMessage())));
                }
            });
        } else if (autoApply && url != null && !url.isBlank()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getResourcePackManager().applyResourcePack(player, url);
            }
        }

        sender.sendMessage(TextUtil.success("Reloaded UItem! Loaded " + plugin.getItemManager().getAllItems().size() + " items."));
    }

    private void handleCheck(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error("This command can only be used by players!"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(TextUtil.error("You are not holding any item!"));
            return;
        }

        var api = com.uitem.api.UItemAPI.get();
        if (api.isUItem(item)) {
            String id = api.getUItemId(item).orElse("unknown");
            sender.sendMessage(TextUtil.success("✔ Item is a valid UItem!"));
            sender.sendMessage(TextUtil.prefixed("<green>ID: <white>" + id));
            sender.sendMessage(TextUtil.prefixed("<green>Type: <white>" + item.getType().name()));
        } else {
            sender.sendMessage(TextUtil.error("✗ Item is NOT a valid UItem!"));
            sender.sendMessage(TextUtil.prefixed("<red>Missing PDC tag 'uitem_id'. This item was not obtained through /uitem give."));
            sender.sendMessage(TextUtil.prefixed("<red>Use <white>/uitem give <id> <player><red> to get properly tagged items."));
        }
    }

    private void handleCraft(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error("This command can only be used by players!"));
            return;
        }
        new com.uitem.gui.CraftingGui(plugin, player).open();
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("uitem.editor")) {
            sender.sendMessage(TextUtil.error("You don't have permission to use this command!"));
            return;
        }

        var items = plugin.getItemManager().getAllItems();
        if (items.isEmpty()) {
            sender.sendMessage(TextUtil.info("No custom items yet. Use /uitem editor to create one."));
            return;
        }

        sender.sendMessage(TextUtil.prefixed("<gold><bold>Custom Items List (" + items.size() + "):"));
        sender.sendMessage(Component.text(""));

        for (var entry : items.entrySet()) {
            UItemData data = entry.getValue();
            String materialName = data.getMaterial() != null ? data.getMaterial().name() : "STONE";
            String displayName = data.getDisplayName() == null ? "<white>" + data.getId() : data.getDisplayName();
            Component line = miniMessage.deserialize(
                            "<gray>• <white>" + data.getId() +
                                    " <dark_gray>| <gray>" + materialName +
                                    " <dark_gray>| " + displayName)
                    .clickEvent(ClickEvent.runCommand("/ui give " + data.getId()))
                    .hoverEvent(HoverEvent.showText(
                            miniMessage.deserialize("<yellow>Click to receive this item")));
            sender.sendMessage(line);
        }

        sender.sendMessage(Component.text(""));
    }

    private void handleResourcePack(CommandSender sender, String[] args) {
        if (!sender.hasPermission("uitem.reload")) {
            sender.sendMessage(TextUtil.error("You don't have permission to use this command!"));
            return;
        }

        ResourcePackManager rpm = plugin.getResourcePackManager();

        if (args.length == 1) {
            sender.sendMessage(TextUtil.prefixed("<gold>Generate resource pack: <white>/uitem rp generate"));
            sender.sendMessage(TextUtil.prefixed("<gold>Export zip: <white>/uitem rp export"));
            sender.sendMessage(TextUtil.prefixed("<gold>Apply to player: <white>/uitem rp apply <player> <url> [hash]"));
            sender.sendMessage(TextUtil.prefixed("<gold>Status: <white>/uitem rp status"));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "generate", "gen", "reload" -> {
                plugin.getResourcePackManager().syncCmdsToLiveItems();
                rpm.generatePack();
                sender.sendMessage(TextUtil.success("Generated resource pack at data/resourcepack/"));
            }
            case "export", "zip" -> {
                try {
                    var zip = rpm.exportZip();
                    sender.sendMessage(TextUtil.success("Exported zip: " + zip.getAbsolutePath()));
                    String hashHex = rpm.getLastPackHashHex();
                    if (hashHex != null) {
                        sender.sendMessage(TextUtil.prefixed("<gold>SHA-1: <white>" + hashHex));
                    }
                } catch (Exception e) {
                    sender.sendMessage(TextUtil.error("Export failed: " + e.getMessage()));
                }
            }
            case "apply" -> {
                if (args.length < 4) {
                    sender.sendMessage(TextUtil.error("Usage: /uitem rp apply <player> <url> [hash]"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(TextUtil.error("Player '" + args[2] + "' is not online!"));
                    return;
                }
                String url = args[3];
                String hash = args.length >= 5 ? args[4] : null;
                rpm.applyResourcePack(target, url, hash);
                sender.sendMessage(TextUtil.success("Sent resource pack to " + target.getName()));
            }
            case "status" -> {
                sender.sendMessage(TextUtil.prefixed("<gold>Pack directory: <white>" + rpm.getResourcePackDirectory().getAbsolutePath()));
                sender.sendMessage(TextUtil.prefixed("<gold>CMD file: <white>" + rpm.getCmdConfigFile().getAbsolutePath()));
                sender.sendMessage(TextUtil.prefixed("<gold>Last zip: <white>" + (rpm.getLastZipFile() == null ? "Not exported" : rpm.getLastZipFile().getAbsolutePath())));
                sender.sendMessage(TextUtil.prefixed("<gold>Current hash: <white>" + (rpm.getLastPackHashHex() == null ? "None" : rpm.getLastPackHashHex())));
            }
            default -> sender.sendMessage(TextUtil.error("Usage: /uitem rp <generate|export|apply|status>"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(miniMessage.deserialize(
                "  <gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <gray>— Ultimate Item Plugin"));
        sender.sendMessage(Component.text(""));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem give <id> [player] [amount] <dark_gray>— <gray>Give item"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem editor <dark_gray>— <gray>Open editor GUI"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem list <dark_gray>— <gray>List items"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem rp <generate|export|apply|status> <dark_gray>— <gray>Manage resource pack"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem check <dark_gray>— <gray>Check if held item is a valid UItem"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem craft <dark_gray>— <gray>Open UItem crafting table"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/crafting <dark_gray>— <gray>List all crafting recipes"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/crafting craft <dark_gray>— <gray>Open UItem crafting table"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem reload <dark_gray>— <gray>Reload plugin"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/uitem help <dark_gray>— <gray>Show help"));
        sender.sendMessage(miniMessage.deserialize(
                "  <gold>/ui <dark_gray>— <gray>Short alias for /uitem"));
        sender.sendMessage(Component.text(""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            for (String sub : ROOT_SUBCOMMANDS) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            if (isGiveSubcommand(root)) {
                for (String id : plugin.getItemManager().getAllItems().keySet()) {
                    if (id.toLowerCase(Locale.ROOT).startsWith(input)) {
                        completions.add(id);
                    }
                }
                return completions;
            }
            if (isResourcePackSubcommand(root)) {
                for (String action : RP_ACTIONS) {
                    if (action.startsWith(input)) {
                        completions.add(action);
                    }
                }
                return completions;
            }
        }

        if (isGiveSubcommand(root) && args.length == 3) {
            String input = args[2].toLowerCase(Locale.ROOT);
            if (input.isBlank()) {
                addOnlinePlayers(completions, "");
                addAmountSuggestions(completions, "");
            } else if (isNumeric(input)) {
                addAmountSuggestions(completions, input);
            } else {
                addOnlinePlayers(completions, input);
            }
            return completions;
        }

        if (isGiveSubcommand(root) && args.length == 4) {
            String input = args[3].toLowerCase(Locale.ROOT);
            addAmountSuggestions(completions, input);
            return completions;
        }

        if (isResourcePackSubcommand(root) && args.length == 3 && args[1].equalsIgnoreCase("apply")) {
            String input = args[2].toLowerCase(Locale.ROOT);
            addOnlinePlayers(completions, input);
            return completions;
        }

        if (isResourcePackSubcommand(root) && args.length == 4 && args[1].equalsIgnoreCase("apply")) {
            // URL không thể đoán chính xác; không suggest gì để tránh rối.
            return completions;
        }

        return completions;
    }

    private void addOnlinePlayers(List<String> completions, String input) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (input.isBlank() || name.toLowerCase(Locale.ROOT).startsWith(input)) {
                completions.add(name);
            }
        }
    }

    private void addAmountSuggestions(List<String> completions, String input) {
        for (String amount : List.of("1", "8", "16", "32", "64")) {
            if (input.isBlank() || amount.startsWith(input)) {
                completions.add(amount);
            }
        }
    }

    private boolean isGiveSubcommand(String root) {
        return root.equals("give");
    }

    private boolean isResourcePackSubcommand(String root) {
        return root.equals("resourcepack") || root.equals("rp");
    }

    private boolean isNumeric(String input) {
        return tryParseAmount(input) != null;
    }

    private record GiveTarget(Player player, int amount) {
    }
}
