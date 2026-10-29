package com.max12311.maxnetworkhub.messaging;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class HubMessenger {
    private final MaxNetworkHub plugin;
    private static final String CHANNEL = "maxnetwork:main";

    public HubMessenger(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    public void init() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, new ProxyMessageHandler(plugin));
    }

    public void send(Player player, String subChannel, String... data) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF(subChannel);
            for (String s : data) {
                out.writeUTF(s);
            }
            player.sendPluginMessage(plugin, CHANNEL, b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("[Messenger] Failed to send to proxy: " + e.getMessage());
        }
    }

    public void requestSessionCheck(Player player) {
        send(player, "CheckSession", player.getUniqueId().toString());
    }

    public void sendAuthSuccess(Player player) {
        send(player, "AuthSuccess", player.getUniqueId().toString());
    }

    public void sendSetDefaultServer(Player player, String serverName) {
        send(player, "SetDefaultServer", player.getUniqueId().toString(), serverName);
    }

    public void sendRequestSelector(Player player) {
        send(player, "RequestSelector", player.getUniqueId().toString());
    }

    public void sendAdminVerifyRequest(Player player, String pin) {
        // Send SHA-256 hash — proxy compares stored hash against received hash
        send(player, "AdminVerifyRequest", player.getUniqueId().toString(), hashPin(pin));
    }

    public void sendAdminAction(Player player, String action, String target, String details) {
        send(player, "AdminAction", player.getUniqueId().toString(), action, target, details);
    }

    public void sendVerifyPinGUI(Player player, String pin) {
        // Send SHA-256 hash — proxy compares stored hash against received hash
        send(player, "VerifyPinGUI", player.getUniqueId().toString(), hashPin(pin));
    }

    public void sendSyncPin(Player player, String pin) {
        // Send SHA-256 hash — never send plaintext PIN over the channel
        send(player, "SyncPin", player.getUniqueId().toString(), hashPin(pin));
    }

    public void sendRequestNetworkStats(Player player) {
        send(player, "RequestNetworkStats", player.getUniqueId().toString());
    }

    public void sendRequestSetup(Player player) {
        send(player, "RequestSetup", player.getUniqueId().toString());
    }

    public void sendSetAutoTarget(Player player, String target) {
        send(player, "SetAutoTarget", player.getUniqueId().toString(), target);
    }

    public void sendSaveSetupConfig(Player player, String routingMode, String autoTarget, List<String> selectorServers) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("SaveSetupConfig");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(routingMode);
            out.writeUTF(autoTarget != null ? autoTarget : "");
            out.writeInt(selectorServers.size());
            for (String s : selectorServers) out.writeUTF(s);
            player.sendPluginMessage(plugin, CHANNEL, b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("[Messenger] Failed to send SaveSetupConfig: " + e.getMessage());
        }
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(("maxnetwork:" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
