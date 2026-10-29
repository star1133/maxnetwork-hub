package com.max12311.maxnetworkhub;

import com.max12311.maxnetworkhub.auth.AuthManager;
import com.max12311.maxnetworkhub.util.GeyserSupport;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class AuthEventListener implements Listener {
    private final MaxNetworkHub plugin;

    public AuthEventListener(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isAuthenticating(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        AuthManager auth = plugin.getAuthManager();

        if (!auth.isAuthenticating(uuid)) return;

        String msg = event.getMessage();

        // Allow /pin command during auth
        if (msg.toLowerCase().startsWith("/pin ")) {
            event.setCancelled(true);
            String[] parts = msg.split(" ");
            if (parts.length == 2 && parts[1].matches("\\d{4}")) {
                auth.submitPin(player, parts[1]);
            } else {
                player.sendMessage("\u00a7c\u00a7l[PIN] \u00a7eUsage: \u00a7f/pin <4-digit-code>");
            }
            return;
        }

        if (msg.equalsIgnoreCase("/pin")) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7l[PIN] \u00a7eUsage: \u00a7f/pin <4-digit-code>");
            return;
        }

        // Block all other commands while authenticating
        event.setCancelled(true);
        player.sendMessage("\u00a7c\u00a7l[PIN Required] \u00a7ePlease enter your PIN first. \u00a7f/pin <code>");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        AuthManager auth = plugin.getAuthManager();
        if (!auth.isAuthenticating(player.getUniqueId())) return;

        event.setCancelled(true);

        // Bedrock players (GeyserMC/Floodgate) can type their PIN in chat
        // since inventory GUIs may not render correctly on some Bedrock clients.
        if (GeyserSupport.isBedrockPlayer(player)) {
            String msg = event.getMessage().trim();
            if (msg.matches("\\d{4}")) {
                // Schedule on main thread \u2014 auth state manipulation is not thread-safe
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                    auth.submitPin(player, msg));
            } else {
                player.sendMessage("\u00a7e[Bedrock PIN] \u00a7fType your 4-digit PIN in chat (e.g. \u00a7a1234\u00a7f)");
            }
            return;
        }

        player.sendMessage("\u00a7c\u00a7l[PIN Required] \u00a7ePlease enter your PIN first. \u00a7f/pin <code>");
    }
}
