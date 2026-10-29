package com.max12311.maxnetworkhub;

import com.max12311.maxnetworkhub.auth.AuthManager;
import com.max12311.maxnetworkhub.util.GeyserSupport;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoFlowListener implements Listener {
    private final MaxNetworkHub plugin;

    public AutoFlowListener(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Hide join message to prevent spam
        event.setJoinMessage(null);
        
        // Small delay to ensure player is fully loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                startAuthFlow(player);
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null);
        AuthManager authManager = plugin.getAuthManager();
        authManager.cleanup(player.getUniqueId());
    }

    private void startAuthFlow(Player player) {
        boolean bedrock = GeyserSupport.isBedrockPlayer(player);
        plugin.getAuthManager().startAuthFlow(player, bedrock);
        // Ask proxy — if session is still valid (within 24h) it will bypass auth
        plugin.getMessenger().requestSessionCheck(player);
    }
}
