package com.max12311.maxnetworkhub.messaging;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import com.max12311.maxnetworkhub.gui.GuiManager;
import org.bukkit.entity.Player;

import com.max12311.maxnetworkhub.gui.SetupWizard;
import com.max12311.maxnetworkhub.gui.StatsGUI;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProxyMessageHandler implements org.bukkit.plugin.messaging.PluginMessageListener {
    private final MaxNetworkHub plugin;

    public ProxyMessageHandler(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("maxnetwork:main")) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String sub = in.readUTF();
            switch (sub) {
                case "SessionValid":    handleSessionValid(player, in);       break;
                case "SessionInvalid":  handleSessionInvalid(player, in);     break;
                case "OpenSelector":    handleOpenSelector(player, in);       break;
                case "OpenPinGUI":      handleOpenPinGUI(player, in);         break;
                case "AdminVerifyResult": handleAdminVerifyResult(player, in); break;
                case "HubReload":         handleHubReload();                      break;
                case "ResetPlayerPin":  handleResetPlayerPin(player, in);     break;
                case "SetupData":       handleSetupData(player, in);          break;
                case "NetworkStats":    handleNetworkStats(player, in);       break;
                default:
                    plugin.getLogger().warning("[ProxyMsg] Unknown subChannel: " + sub);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[ProxyMsg] Failed to read: " + e.getMessage());
        }
    }

    private void handleSessionValid(Player player, DataInputStream in) throws IOException {
        String uuidStr = in.readUTF();
        if (!player.getUniqueId().equals(UUID.fromString(uuidStr))) return;

        plugin.getAuthManager().cleanup(player.getUniqueId());
        player.closeInventory();

        boolean hasDefault = "true".equals(in.readUTF());
        if (hasDefault) {
            player.sendMessage("§aAuto-connecting to your saved server...");
        } else {
            plugin.getGuiManager().openServerSelector(player);
        }
    }

    private void handleSessionInvalid(Player player, DataInputStream in) throws IOException {
        String uuidStr = in.readUTF();
        if (!player.getUniqueId().equals(UUID.fromString(uuidStr))) return;
        // Auth flow is already open from AutoFlowListener — nothing to do
    }

    private void handleOpenSelector(Player player, DataInputStream in) throws IOException {
        // Proxy sends UUID + server entry list
        String uuidStr = in.readUTF();
        if (!player.getUniqueId().equals(UUID.fromString(uuidStr))) return;

        int count = in.readInt();
        List<GuiManager.SelectorEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name     = in.readUTF();
            String display  = in.readUTF().replace('&', '§');
            String material = in.readUTF();
            int slot        = in.readInt();
            int loreCount   = in.readInt();
            List<String> lore = new ArrayList<>();
            for (int l = 0; l < loreCount; l++) lore.add(in.readUTF().replace('&', '§'));
            entries.add(new GuiManager.SelectorEntry(name, display, material, slot, lore));
        }

        plugin.getGuiManager().openServerSelector(player, entries);
    }

    private void handleOpenPinGUI(Player player, DataInputStream in) throws IOException {
        // Optional UUID field — ignore it, player is already known
        try { in.readUTF(); } catch (IOException ignored) {}
        plugin.getGuiManager().openPinVerify(player, pin ->
            plugin.getMessenger().sendVerifyPinGUI(player, pin));
    }

    private void handleAdminVerifyResult(Player player, DataInputStream in) throws IOException {
        String uuidStr = in.readUTF();
        if (!player.getUniqueId().equals(UUID.fromString(uuidStr))) return;

        boolean success = "true".equals(in.readUTF());
        String detail = in.readUTF();

        if (success) {
            int level;
            try { level = Integer.parseInt(detail); } catch (NumberFormatException e) { level = 1; }
            plugin.getAdminGui().setAdminLevel(player, level);
            player.sendMessage("§a✓ Admin verified! Level " + level);
            plugin.getAdminGui().openAdminPanel(player);
        } else {
            player.sendMessage("§c✗ Admin verification failed: " + detail);
        }
    }

    private void handleHubReload() {
        plugin.reloadConfig();
        plugin.getLogger().info("[MaxNetwork-Hub] Config reloaded via proxy signal.");
    }

    private void handleResetPlayerPin(Player admin, DataInputStream in) throws IOException {
        UUID targetUuid = UUID.fromString(in.readUTF());
        boolean deleted = plugin.getAuthManager().resetPin(targetUuid);
        plugin.getLogger().info("[AdminReset] PIN .dat deleted for " + targetUuid + " by " + admin.getName() + " — " + (deleted ? "OK" : "file not found"));
        admin.sendMessage("§a✓ PIN reset complete for " + targetUuid + ". Player must re-register on next login.");
        // If target is online on hub, kick them so auth flow re-triggers on reconnect
        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(targetUuid);
        if (target != null) {
            target.kickPlayer("§eYour PIN has been reset by an admin.\n§fPlease reconnect to set a new PIN.");
        }
    }

    private void handleNetworkStats(Player player, DataInputStream in) throws IOException {
        String uuidStr = in.readUTF();
        if (!player.getUniqueId().equals(UUID.fromString(uuidStr))) return;

        StatsGUI.NetworkSnapshot snap = new StatsGUI.NetworkSnapshot();
        snap.totalOnline       = in.readInt();
        snap.peakOnline24h     = in.readInt();
        snap.totalPlaytimeMinutes = in.readLong();

        int serverCount = in.readInt();
        for (int i = 0; i < serverCount; i++) {
            snap.serverCounts.put(in.readUTF(), in.readInt());
        }
        int playerCount = in.readInt();
        for (int i = 0; i < playerCount; i++) {
            snap.playerServers.put(in.readUTF(), in.readUTF());
        }
        int tpsCount = in.readInt();
        for (int i = 0; i < tpsCount; i++) {
            snap.tpsData.put(in.readUTF(), in.readDouble());
        }

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
            plugin.getStatsGui().onStatsReceived(player, snap));
    }

    private void handleSetupData(Player player, DataInputStream in) throws IOException {
        String uuidStr = in.readUTF();
        if (!player.getUniqueId().equals(UUID.fromString(uuidStr))) return;

        String routingMode = in.readUTF();
        String autoTarget  = in.readUTF();

        int serverCount = in.readInt();
        List<String> allServers = new ArrayList<>();
        for (int i = 0; i < serverCount; i++) allServers.add(in.readUTF());

        int selectorCount = in.readInt();
        List<String> selectorServers = new ArrayList<>();
        for (int i = 0; i < selectorCount; i++) selectorServers.add(in.readUTF());

        // Must open inventory on main thread
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
            plugin.getSetupWizard().startWizard(player, routingMode, autoTarget, allServers, selectorServers));
    }
}
