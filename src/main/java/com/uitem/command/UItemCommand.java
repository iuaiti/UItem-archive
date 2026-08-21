package com.uitem.command;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.gui.ItemListGui;
import com.uitem.util.Messages;
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

public class UItemCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "give", "editor", "reload", "list", "check", "help"
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
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "editor", "edit", "gui" -> handleEditor(sender);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
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
        if (target == null) return;
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
        if (args.length == 2) {
            if (sender instanceof Player player) return new GiveTarget(player, 1);
            sender.sendMessage(TextUtil.error("Console must specify a player: /uitem give <id> <player> [amount]"));
            return null;
        }
        String token = args[2];
        Integer amountFromToken = tryParseAmount(token);
        if (args.length == 3 && amountFromToken != null) {
            if (sender instanceof Player player) return new GiveTarget(player, amountFromToken);
            sender.sendMessage(TextUtil.error("Console must specify a player"));
            return null;
        }
        Player target = Bukkit.getPlayerExact(token);
        if (target == null) {
            sender.sendMessage(TextUtil.error("Player '" + token + "' is not online!"));
            return null;
        }
        int amount = 1;
        if (args.length >= 4) {
            Integer parsed = tryParseAmount(args[3]);
            if (parsed == null) {
                sender.sendMessage(TextUtil.error("Invalid amount (1-64)."));
                return null;
            }
            amount = parsed;
        }
        return new GiveTarget(target, amount);
    }

    private Integer tryParseAmount(String token) {
        try {
            int amount = Integer.parseInt(token);
            if (amount < 1 || amount > 64) throw new NumberFormatException("out of range");
            return amount;
        } catch (NumberFormatException e) { return null; }
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
            sender.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }
        plugin.reloadConfig();
        com.uitem.util.TextUtil.loadPrefix(plugin);
        com.uitem.gui.GuiConfig.reload(plugin);
        Messages.reload();
        com.uitem.rarity.RarityConfig.reloadFromFile();
        com.uitem.tooltip.TooltipConfig.reload();
        // Do NOT saveAllNow() before loadAll(): that would flush the stale in-memory snapshot to
        // disk first, overwriting any manual edits made directly to the item config files.
        // Disk is the source of truth on reload - just re-read it.
        plugin.getStorageManager().loadAll(plugin.getItemManager());
        plugin.getProviderManager().detect();
        sender.sendMessage(TextUtil.success(Messages.reloaded(plugin.getItemManager().getAllItems().size())));
    }

    private void handleCheck(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
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
        } else {
            String detected = plugin.getProviderManager().detectSource(item);
            if (detected != null) {
                sender.sendMessage(TextUtil.prefixed("<yellow>Detected source: <white>" + detected));
                sender.sendMessage(TextUtil.prefixed("<gray>Use /uitem editor to create a UItem from this item."));
            } else {
                sender.sendMessage(TextUtil.error(Messages.notUItem()));
            }
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("uitem.editor")) {
            sender.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }
        var items = plugin.getItemManager().getAllItems();
        if (items.isEmpty()) {
            sender.sendMessage(TextUtil.info(Messages.noItemsYet()));
            return;
        }
        sender.sendMessage(TextUtil.prefixed("<gold><bold>Custom Items List (" + items.size() + "):"));
        sender.sendMessage(Component.text(""));
        for (var entry : items.entrySet()) {
            UItemData data = entry.getValue();
            String source = data.getSource() != null ? data.getSource() : "?";
            String displayName = data.getDisplayName() == null ? "<white>" + data.getId() : data.getDisplayName();
            Component line = miniMessage.deserialize(
                    "<gray>• <white>" + data.getId() + " <dark_gray>| <gray>" + source + " <dark_gray>| " + displayName)
                    .clickEvent(ClickEvent.runCommand("/ui give " + data.getId()))
                    .hoverEvent(HoverEvent.showText(
                            miniMessage.deserialize("<yellow>Click to receive this item")));
            sender.sendMessage(line);
        }
        sender.sendMessage(Component.text(""));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(miniMessage.deserialize(
                "  <gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <gray>— Custom Item Plugin"));
        sender.sendMessage(Component.text(""));
        sender.sendMessage(miniMessage.deserialize("  <gold>/uitem give <id> [player] [amount] <dark_gray>— <gray>Give item"));
        sender.sendMessage(miniMessage.deserialize("  <gold>/uitem editor <dark_gray>— <gray>Open the editor GUI"));
        sender.sendMessage(miniMessage.deserialize("  <gold>/uitem list <dark_gray>— <gray>List items"));
        sender.sendMessage(miniMessage.deserialize("  <gold>/uitem check <dark_gray>— <gray>Check/identify the held item"));
        sender.sendMessage(miniMessage.deserialize("  <gold>/uitem reload <dark_gray>— <gray>Reload plugin"));
        sender.sendMessage(miniMessage.deserialize("  <gold>/ui <dark_gray>— <gray>Short alias for /uitem"));
        sender.sendMessage(Component.text(""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            for (String sub : ROOT_SUBCOMMANDS) if (sub.startsWith(input)) completions.add(sub);
            return completions;
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && root.equals("give")) {
            String input = args[1].toLowerCase(Locale.ROOT);
            for (String id : plugin.getItemManager().getAllItems().keySet()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(input)) completions.add(id);
            }
            return completions;
        }
        if (root.equals("give") && args.length == 3) {
            String input = args[2].toLowerCase(Locale.ROOT);
            if (input.isBlank()) addOnlinePlayers(completions, "");
            else addOnlinePlayers(completions, input);
            return completions;
        }
        if (root.equals("give") && args.length == 4) {
            addAmountSuggestions(completions, args[3].toLowerCase(Locale.ROOT));
            return completions;
        }
        return completions;
    }

    private void addOnlinePlayers(List<String> out, String input) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (input.isBlank() || p.getName().toLowerCase(Locale.ROOT).startsWith(input)) out.add(p.getName());
        }
    }

    private void addAmountSuggestions(List<String> out, String input) {
        for (String a : List.of("1", "8", "16", "32", "64")) {
            if (input.isBlank() || a.startsWith(input)) out.add(a);
        }
    }

    private record GiveTarget(Player player, int amount) {}
}
