package com.max12311.maxnetworkhub.commands;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {
    private final MaxNetworkHub plugin;

    public StatsCommand(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command must be run in-game.");
            return true;
        }
        Player player = (Player) sender;
        player.sendMessage(ChatColor.GRAY + "Fetching network stats...");
        plugin.getMessenger().sendRequestNetworkStats(player);
        return true;
    }
}
