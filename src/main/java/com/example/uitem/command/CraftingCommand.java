package com.example.uitem.command;

import com.example.uitem.UitemPlugin;
import com.example.uitem.gui.CraftingGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the crafting GUI.
 */
public class CraftingCommand implements CommandExecutor {

    private final UitemPlugin plugin;

    public CraftingCommand(UitemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        new CraftingGUI(plugin).open(player);
        return true;
    }
}