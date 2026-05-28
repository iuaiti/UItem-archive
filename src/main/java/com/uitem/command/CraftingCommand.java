package com.uitem.command;

import com.uitem.UItemPlugin;
import com.uitem.gui.CraftingGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CraftingCommand implements CommandExecutor {

    private final UItemPlugin plugin;

    public CraftingCommand(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        new CraftingGui(plugin, player).open();
        return true;
    }
}
