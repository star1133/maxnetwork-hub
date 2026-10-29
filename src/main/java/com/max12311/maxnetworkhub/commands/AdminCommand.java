package com.max12311.maxnetworkhub.commands;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final MaxNetworkHub plugin;

    public AdminCommand(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command.");
            return true;
        }

        // Check basic permission
        if (!player.hasPermission("maxnetwork.admin")) {
            player.sendMessage("\u00a7cYou do not have admin permissions.");
            return true;
        }

        if (args.length == 0) {
            // First time opening - need strict verification
            // Send verify request to proxy with PIN
            player.sendMessage("\u00a7eEnter your 4-digit PIN for admin access...");
            // Open PIN GUI
            plugin.getAuthManager().startAdminAuth(player, (pin) -> {
                // After PIN entered, send to proxy for verification
                plugin.getMessenger().sendAdminVerifyRequest(player, pin);
            });
            return true;
        }

        // If already verified, process command directly
        if (plugin.getAdminGui().isAdmin(player)) {
            return handleAdminSubCommand(player, args);
        } else {
            player.sendMessage("\u00a7cAdmin verification required. Use /mnadmin without arguments first.");
            return true;
        }
    }

    private boolean handleAdminSubCommand(Player player, String[] args) {
        String sub = args[0].toLowerCase();
        int level = plugin.getAdminGui().getAdminLevel(player);

        switch (sub) {
            case "gui":
                if (level >= 1) plugin.getAdminGui().openAdminPanel(player);
                else player.sendMessage("\u00a7cLevel 1+ required.");
                break;
            case "move":
                if (level < 1 || args.length < 3) {
                    player.sendMessage("\u00a7cUsage: /mnadmin move <player> <server>");
                    return true;
                }
                plugin.getMessenger().sendAdminAction(player, "move", args[1], args[2]);
                player.sendMessage("\u00a7aRequesting move of " + args[1] + " to " + args[2] + "...");
                break;
            case "kick":
                if (level < 1 || args.length < 2) {
                    player.sendMessage("\u00a7cUsage: /mnadmin kick <player> [reason]");
                    return true;
                }
                String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Kicked by admin";
                plugin.getMessenger().sendAdminAction(player, "kick", args[1], reason);
                player.sendMessage("\u00a7aKicking " + args[1] + "...");
                break;
            case "ban":
                if (level < 2 || args.length < 2) {
                    player.sendMessage("\u00a7cUsage: /mnadmin ban <player> [reason]");
                    return true;
                }
                String banReason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Banned by admin";
                plugin.getMessenger().sendAdminAction(player, "ban", args[1], banReason);
                player.sendMessage("\u00a7aBanning " + args[1] + "...");
                break;
            case "broadcast":
                if (level < 1 || args.length < 2) {
                    player.sendMessage("\u00a7cUsage: /mnadmin broadcast <message>");
                    return true;
                }
                String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                plugin.getMessenger().sendAdminAction(player, "broadcast", "_all_", msg);
                player.sendMessage("\u00a7aBroadcast sent.");
                break;
            case "whitelist":
                if (level < 3 || args.length < 3) {
                    player.sendMessage("\u00a7cUsage: /mnadmin whitelist <add|remove> <player>");
                    return true;
                }
                boolean add = args[1].equalsIgnoreCase("add");
                String actionName = add ? "whitelist_add" : "whitelist_remove";
                plugin.getMessenger().sendAdminAction(player, actionName, args[2], "");
                player.sendMessage("\u00a7aWhitelist updated.");
                break;
            case "maintenance":
                if (level < 3 || args.length < 3) {
                    player.sendMessage("\u00a7cUsage: /mnadmin maintenance <server> <on|off>");
                    return true;
                }
                plugin.getMessenger().sendAdminAction(player, "maintenance", args[1], args[2]);
                player.sendMessage("\u00a7aMaintenance for " + args[1] + " set to: " + args[2]);
                break;
            case "unban":
                if (level < 2 || args.length < 2) {
                    player.sendMessage("\u00a7cUsage: /mnadmin unban <player>");
                    return true;
                }
                plugin.getMessenger().sendAdminAction(player, "unban", args[1], "");
                player.sendMessage("\u00a7aUnbanning " + args[1] + "...");
                break;
            default:
                player.sendMessage("\u00a7eMaxNetwork Admin Commands:");
                player.sendMessage("\u00a77/mnadmin\u00a7f - Open admin panel");
                player.sendMessage("\u00a77/mnadmin move <player> <server>");
                player.sendMessage("\u00a77/mnadmin kick <player> [reason]");
                player.sendMessage("\u00a77/mnadmin ban <player> [reason]");
                player.sendMessage("\u00a77/mnadmin broadcast <msg>");
                player.sendMessage("\u00a77/mnadmin whitelist <add|remove> <player>");
        }
        return true;
    }
}
