package com.uitem.command;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.gui.ItemListGui;
import com.uitem.resourcepack.ResourcePackManager;
import com.uitem.util.Messages;
import com.uitem.util.SchedulerUtil;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Command handler cho /uitem và alias /ui.
 */
public class UItemCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "give", "editor", "reload", "list", "resourcepack", "rp", "craft", "potion", "help", "check"
    );

    private static final List<String> RP_ACTIONS = List.of(
            "build", "generate", "export", "sendall", "apply", "status", "gui", "merge"
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
            case "potion", "potions" -> handlePotion(sender);
            case "check" -> handleCheck(sender);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(TextUtil.error(Messages.invalidCommand(label)));
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("uitem.give")) {
            sender.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(TextUtil.error(Messages.giveUsage()));
            return;
        }

        String itemId = args[1].toLowerCase(Locale.ROOT);
        GiveTarget target = resolveGiveTarget(sender, args);
        if (target == null) {
            return;
        }

        if (plugin.getItemManager().hasItem(itemId)) {
            boolean success = com.uitem.api.UItemAPI.get().giveItem(
                    target.player(), itemId, target.amount(), sender);
            if (success) {
                sender.sendMessage(TextUtil.success(Messages.gaveItem(target.player().getName(), target.amount(), itemId)));
                if (!sender.equals(target.player())) {
                    target.player().sendMessage(TextUtil.success(Messages.receivedItem(target.amount(), itemId)));
                }
            } else {
                sender.sendMessage(TextUtil.error(Messages.cannotGive()));
            }
            return;
        }

        if (plugin.getPotionManager().has(itemId)) {
            org.bukkit.inventory.ItemStack potion = plugin.getPotionManager()
                    .buildItemStack(plugin.getPotionManager().get(itemId));
            potion.setAmount(target.amount());
            for (org.bukkit.inventory.ItemStack left : target.player().getInventory().addItem(potion).values()) {
                target.player().getWorld().dropItemNaturally(target.player().getLocation(), left);
            }
            sender.sendMessage(TextUtil.success(Messages.potionGiveSent(target.player().getName(), target.amount(), itemId)));
            if (!sender.equals(target.player())) {
                target.player().sendMessage(TextUtil.success(Messages.potionGiveReceived(target.amount(), itemId)));
            }
            return;
        }

        sender.sendMessage(TextUtil.error(Messages.itemNotFound(itemId)));
    }

    private void handlePotion(CommandSender sender) {
        if (!sender.hasPermission("uitem.potion") && !sender.hasPermission("uitem.admin")) {
            sender.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
            return;
        }
        new com.uitem.gui.PotionHubGui(plugin, player).open();
    }

    private GiveTarget resolveGiveTarget(CommandSender sender, String[] args) {
        // /uitem give <id>
        if (args.length == 2) {
            if (sender instanceof Player player) {
                return new GiveTarget(player, 1);
            }
            sender.sendMessage(TextUtil.error(Messages.consoleNeedPlayer()));
            return null;
        }

        String token = args[2];
        Integer amountFromToken = tryParseAmount(token);

        // /uitem give <id> <amount>  -> chỉ cho player hiện tại
        if (args.length == 3 && amountFromToken != null) {
            if (sender instanceof Player player) {
                return new GiveTarget(player, amountFromToken);
            }
            sender.sendMessage(TextUtil.error(Messages.consoleNeedPlayer()));
            return null;
        }

        // /uitem give <id> <player> [amount]
        Player target = Bukkit.getPlayerExact(token);
        if (target == null) {
            sender.sendMessage(TextUtil.error(Messages.playerNotFound(token)));
            return null;
        }

        int amount = 1;
        if (args.length >= 4) {
            Integer parsed = tryParseAmount(args[3]);
            if (parsed == null) {
                sender.sendMessage(TextUtil.error(Messages.invalidAmount()));
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
            sender.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
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
        com.uitem.util.Messages.reload();
        com.uitem.rarity.RarityConfig.reload(plugin);
        plugin.getStorageManager().saveAllNow(plugin.getItemManager());
        plugin.getStorageManager().loadAll(plugin.getItemManager());
        plugin.getPotionManager().reload();
        plugin.getIntegrationManager().detectIntegrations();

        // Bug E fix: reload ResourcePackManager config fields
        plugin.getResourcePackManager().reloadFromConfig(plugin.getConfig());
        plugin.getResourcePackManager().syncCmdsToLiveItems();

        boolean autoGenerate = plugin.getConfig().getBoolean("resourcepack.auto-generate", true);
        boolean autoApply = plugin.getConfig().getBoolean("resourcepack.auto-apply", false);
        String url = plugin.getConfig().getString("resourcepack.url", "");

        if (autoGenerate) {
            com.uitem.util.SchedulerUtil.runAsync(plugin, () -> {
                try {
                    var rpm = plugin.getResourcePackManager();
                    rpm.generatePack();
                    rpm.exportZip();
                    SchedulerUtil.runSync(plugin, sender,
                            () -> sender.sendMessage(TextUtil.success(Messages.rpRegenerated())));
                    String packUrl = url != null && !url.isBlank() ? url : rpm.resolvePackUrl();
                    if (autoApply && packUrl != null && !packUrl.isBlank()) {
                        byte[] hash = rpm.getLastPackHash();
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            rpm.applyResourcePack(player, packUrl, hash);
                        }
                    }
                } catch (Exception e) {
                    SchedulerUtil.runSync(plugin, sender,
                            () -> sender.sendMessage(TextUtil.error(Messages.rpGenerateFailed(e.getMessage()))));
                }
            });
        } else if (autoApply && url != null && !url.isBlank()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getResourcePackManager().applyResourcePack(player, url);
            }
        }

        sender.sendMessage(TextUtil.success(Messages.reloaded(plugin.getItemManager().getAllItems().size())));
    }

    private void handleCheck(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(TextUtil.error(Messages.notHoldingItem()));
            return;
        }

        var api = com.uitem.api.UItemAPI.get();
        if (api.isUItem(item)) {
            String id = api.getUItemId(item).orElse("unknown");
            sender.sendMessage(TextUtil.success(Messages.validUItem()));
            sender.sendMessage(TextUtil.prefixed("<green>ID: <white>" + id));
            sender.sendMessage(TextUtil.prefixed("<green>Type: <white>" + item.getType().name()));
        } else {
            sender.sendMessage(TextUtil.error(Messages.notUItem()));
            sender.sendMessage(TextUtil.prefixed("<red>" + Messages.missingPDC()));
            sender.sendMessage(TextUtil.prefixed("<red>" + Messages.useGiveToGet()));
        }
    }

    private void handleCraft(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
            return;
        }
        new com.uitem.gui.CraftingGui(plugin, player).open();
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
            sender.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }

        ResourcePackManager rpm = plugin.getResourcePackManager();

        if (args.length == 1) {
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpGenerate()));
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpExport()));
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpBuild()));
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpSendAll()));
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpGui()));
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpMerge()));
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpApply()));
            sender.sendMessage(TextUtil.prefixed(Messages.rpHelpStatus()));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextUtil.error(Messages.playersOnly()));
                    return;
                }
                new com.uitem.gui.ResourcePackGui(plugin, player).open();
            }
            case "build" -> runRpAsync(sender, rpm, true);
            case "merge" -> SchedulerUtil.runAsync(plugin, () -> {
                try {
                    int n = new com.uitem.resourcepack.PackImporter(plugin)
                            .importAll(rpm.getResourcePackDirectory().toPath(),
                                    rpm.getSettings().externalPacksDir());
                    SchedulerUtil.runSync(plugin, sender, () ->
                            sender.sendMessage(TextUtil.success(Messages.rpMerged(n))));
                } catch (Exception e) {
                    SchedulerUtil.runSync(plugin, sender, () ->
                            sender.sendMessage(TextUtil.error(Messages.rpGenerateFailed(e.getMessage()))));
                }
            });
            case "generate", "gen" -> runRpAsync(sender, rpm, false);
            case "export", "zip" -> SchedulerUtil.runAsync(plugin, () -> {
                try {
                    File zip = rpm.exportZip();
                    String hashHex = rpm.getLastPackHashHex();
                    SchedulerUtil.runSync(plugin, sender, () -> {
                        sender.sendMessage(TextUtil.success(Messages.rpExported(zip.getAbsolutePath())));
                        if (hashHex != null) {
                            sender.sendMessage(TextUtil.prefixed("<gold>SHA-1: <white>" + hashHex));
                        }
                    });
                } catch (Exception e) {
                    SchedulerUtil.runSync(plugin, sender, () ->
                            sender.sendMessage(TextUtil.error(Messages.rpExportFailed(e.getMessage()))));
                }
            });
            case "sendall" -> {
                rpm.sendPackToAllOnline();
                sender.sendMessage(TextUtil.success(Messages.rpSentAll()));
            }
            case "apply" -> {
                if (args.length < 3) {
                    sender.sendMessage(TextUtil.error(Messages.rpApplyUsage()));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(TextUtil.error(Messages.playerNotFound(args[2])));
                    return;
                }
                String url = args.length >= 4 ? args[3] : rpm.resolvePackUrl();
                if (url == null || url.isBlank()) {
                    sender.sendMessage(TextUtil.error(Messages.rpUrlNotSet()));
                    return;
                }
                String hashArg = args.length >= 5 ? args[4] : null;
                rpm.applyResourcePack(target, url, hashArg);
                sender.sendMessage(TextUtil.success(Messages.rpApplied(target.getName())));
            }
            case "status" -> {
                var st = rpm.getStatus();
                sender.sendMessage(TextUtil.prefixed("<gold>Resource Pack Status"));
                sender.sendMessage(TextUtil.prefixed("<gray>Pack: <white>" + st.packDirectory()));
                sender.sendMessage(TextUtil.prefixed("<gray>Import: <white>" + st.importDirectory()));
                sender.sendMessage(TextUtil.prefixed("<gray>Zip: <white>" + (st.lastZip() != null ? st.lastZip() : Messages.rpStatusNone())));
                sender.sendMessage(TextUtil.prefixed("<gray>SHA-1: <white>" + (st.sha1() != null ? st.sha1() : Messages.rpStatusNone())));
                sender.sendMessage(TextUtil.prefixed("<gray>URL: <white>" + (st.packUrl() != null ? st.packUrl() : Messages.rpStatusNone())));
                sender.sendMessage(TextUtil.prefixed("<gray>Host: <white>" + (st.hostRunning() ? "ON" : "OFF")));
                sender.sendMessage(TextUtil.prefixed("<gray>Items/CMD: <white>" + st.itemCount() + "/" + st.cmdAssignments()));
            }
            default -> sender.sendMessage(TextUtil.error(Messages.rpUsageFull()));
        }
    }

    private void runRpAsync(CommandSender sender, ResourcePackManager rpm, boolean fullBuild) {
        sender.sendMessage(TextUtil.info(fullBuild ? Messages.rpGuiBuilding() : Messages.rpGenerating()));
        SchedulerUtil.runAsync(plugin, () -> {
            try {
                if (fullBuild) {
                    File zip = rpm.rebuildAll();
                    SchedulerUtil.runSync(plugin, sender, () -> {
                        sender.sendMessage(TextUtil.success(Messages.rpRegenerated()));
                        sender.sendMessage(TextUtil.success(Messages.rpExported(zip.getAbsolutePath())));
                        if (plugin.getConfig().getBoolean("resourcepack.auto-apply", false)) {
                            rpm.sendPackToAllOnline();
                            sender.sendMessage(TextUtil.success(Messages.rpSentAll()));
                        }
                    });
                } else {
                    rpm.syncCmdsToLiveItems();
                    rpm.generatePack();
                    SchedulerUtil.runSync(plugin, sender,
                            () -> sender.sendMessage(TextUtil.success(Messages.rpRegenerated())));
                }
            } catch (Exception e) {
                SchedulerUtil.runSync(plugin, sender, () ->
                        sender.sendMessage(TextUtil.error(Messages.rpGenerateFailed(e.getMessage()))));
            }
        });
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(miniMessage.deserialize(Messages.helpTitle()));
        sender.sendMessage(Component.text(""));
        sender.sendMessage(miniMessage.deserialize(Messages.helpGive()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpEditor()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpList()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpRp()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpCheck()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpCraft()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpCraftingCmd()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpReload()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpHelp()));
        sender.sendMessage(miniMessage.deserialize(Messages.helpUi()));
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
                for (String id : plugin.getPotionManager().getAll().keySet()) {
                    if (id.toLowerCase(Locale.ROOT).startsWith(input) && !completions.contains(id)) {
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
