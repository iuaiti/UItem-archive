package com.uitem.command;

import com.uitem.UItemPlugin;
import com.uitem.gui.CraftingGui;
import com.uitem.gui.CraftingHomeGui;
import com.uitem.gui.RecipeDetailGui;
import com.uitem.gui.RecipeListGui;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CraftingCommand implements CommandExecutor, TabCompleter {

    private final UItemPlugin plugin;

    public CraftingCommand(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
            return true;
        }

        if (!player.hasPermission("uitem.craft")) {
            player.sendMessage(TextUtil.error(Messages.noPermission()));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("home")) {
            new CraftingHomeGui(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "craft", "table" -> new CraftingGui(plugin, player).open();
            case "list", "recipes" -> new RecipeListGui(plugin, player).open();
            case "view", "recipe" -> {
                if (args.length < 2) {
                    player.sendMessage(TextUtil.error("Usage: /crafting view <itemId>"));
                    return true;
                }
                if (plugin.getRecipeManager().getRecipe(args[1]) == null) {
                    player.sendMessage(TextUtil.error("Recipe '" + args[1] + "' does not exist."));
                    return true;
                }
                new RecipeDetailGui(plugin, player, args[1]).open();
            }
            default -> {
                player.sendMessage(TextUtil.prefixed("<gold>/crafting <gray>- open crafting home"));
                player.sendMessage(TextUtil.prefixed("<gold>/crafting list <gray>- view recipes"));
                player.sendMessage(TextUtil.prefixed("<gold>/crafting view <id> <gray>- view one recipe"));
                player.sendMessage(TextUtil.prefixed("<gold>/crafting craft <gray>- open custom crafting table"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            for (String sub : List.of("home", "list", "view", "craft")) {
                if (sub.startsWith(input)) out.add(sub);
            }
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("recipe"))) {
            String input = args[1].toLowerCase(Locale.ROOT);
            for (String id : plugin.getRecipeManager().getAllRecipes().keySet()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(input)) out.add(id);
            }
        }
        return out;
    }
}
