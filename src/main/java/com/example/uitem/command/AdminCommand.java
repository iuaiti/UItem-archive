package com.example.uitem.command;

import com.example.uitem.UitemPlugin;
import com.example.uitem.gui.AdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the admin GUI.
 */
public class AdminCommand implements CommandExecutor {

    private final UitemPlugin plugin;

    public AdminCommand(UitemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        // Check permission
        if (!player.hasPermission("uitem.admin")) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }
        new AdminGUI(plugin).open(player);
        return true;
    }
}