package com.max12311.maxnetworkhub.commands;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetupCommand implements CommandExecutor {
    private final MaxNetworkHub plugin;

    public SetupCommand(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command must be run in-game.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("maxnetwork.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("target")) {
            // /mnsetup target <serverName> — quick-set auto-target without going through GUI
            plugin.getMessenger().sendSetAutoTarget(player, args[1]);
            player.sendMessage(ChatColor.GREEN + "Auto-target set to: " + args[1]);
            return true;
        }

        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[MaxNetwork] " +
            ChatColor.GRAY + "Requesting server list from proxy...");
        plugin.getMessenger().sendRequestSetup(player);
        return true;
    }
}
