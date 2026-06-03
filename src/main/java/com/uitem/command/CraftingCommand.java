package com.uitem.command;

import com.uitem.UItemPlugin;
import com.uitem.gui.CraftingGui;
import com.uitem.gui.RecipeListGui;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CraftingCommand implements CommandExecutor, TabCompleter {

    private final UItemPlugin plugin;

    public CraftingCommand(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
            return true;
        }

        if (!hasCraftPermission(player)) {
            player.sendMessage(TextUtil.error(Messages.noPermission()));
            return true;
        }

        if (args.length == 0) {
            new RecipeListGui(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("craft".equals(sub) || "table".equals(sub) || "bench".equals(sub)) {
            new CraftingGui(plugin, player).open();
            return true;
        }

        if ("list".equals(sub) || "recipes".equals(sub)) {
            new RecipeListGui(plugin, player).open();
            return true;
        }

        player.sendMessage(TextUtil.error(Messages.craftingUsage(label)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("craft", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return Collections.emptyList();
    }

    private boolean hasCraftPermission(Player player) {
        return player.hasPermission("craft") || player.hasPermission("uitem.craft");
    }
}