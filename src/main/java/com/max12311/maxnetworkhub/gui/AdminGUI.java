package com.max12311.maxnetworkhub.gui;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AdminGUI implements Listener {
    private final MaxNetworkHub plugin;

    private static final String MAIN_ADMIN_TITLE = "\u00a74\u00a7lAdmin Panel";
    private static final String PLAYER_SELECT_TITLE = "\u00a74\u00a7lSelect Player";
    private static final String CONFIRM_TITLE = "\u00a74\u00a7lConfirm Action";
    private static final String REASON_TITLE = "\u00a74\u00a7lSelect Reason";

    // Stores pending admin actions per player UUID
    private final Map<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> adminLevels = new ConcurrentHashMap<>();

    public AdminGUI(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    public void setAdminLevel(Player player, int level) {
        adminLevels.put(player.getUniqueId(), level);
    }

    public boolean isAdmin(Player player) {
        return adminLevels.containsKey(player.getUniqueId());
    }

    public int getAdminLevel(Player player) {
        return adminLevels.getOrDefault(player.getUniqueId(), 0);
    }

    public void openAdminPanel(Player player) {
        int level = getAdminLevel(player);
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_ADMIN_TITLE);

        // Row 1: Navigation / Info
        inv.setItem(4, createButton(Material.BEACON, "\u00a76\u00a7lAdmin Level: " + level, 
            List.of("\u00a77Your admin privilege level")));

        // Row 2: Action buttons
        int slot = 10;
        inv.setItem(slot++, createButton(Material.ENDER_PEARL, "\u00a7a\u00a7lMove Player",
            List.of("\u00a77Teleport player to another server", "\u00a7eRequires Level 1+")));
        
        inv.setItem(slot++, createButton(Material.IRON_BOOTS, "\u00a7e\u00a7lKick Player",
            List.of("\u00a77Kick a player from the network", "\u00a7eRequires Level 1+")));
        
        inv.setItem(slot++, createButton(Material.BARRIER, "\u00a7c\u00a7lBan Player",
            List.of("\u00a77Ban a player from the network", "\u00a7eRequires Level 2+")));
        
        inv.setItem(slot++, createButton(Material.PAPER, "\u00a7b\u00a7lBroadcast",
            List.of("\u00a77Send broadcast to all servers", "\u00a7eRequires Level 1+")));

        // Row 3: More actions
        slot = 19;
        inv.setItem(slot++, createButton(Material.COMMAND_BLOCK, "\u00a76\u00a7lMaintenance",
            List.of("\u00a77Toggle maintenance mode", "\u00a7eRequires Level 3")));

        inv.setItem(slot++, createButton(Material.BOOK, "\u00a73\u00a7lView Logs",
            List.of("\u00a77View audit log in-game")));

        inv.setItem(slot++, createButton(Material.KNOWLEDGE_BOOK, "\u00a7d\u00a7lReset PIN",
            List.of("\u00a77Reset a player's PIN", "\u00a77Player must re-register on next login", "\u00a7eRequires Level 2+")));

        // Close button
        inv.setItem(26, createButton(Material.RED_STAINED_GLASS_PANE, "\u00a7c\u00a7lClose", null));

        player.openInventory(inv);
    }

    public void openPlayerSelector(Player player, String action) {
        Inventory inv = Bukkit.createInventory(null, 54, PLAYER_SELECT_TITLE);
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        int slot = 0;
        for (Player target : online) {
            if (slot >= 54) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("\u00a7a" + target.getName());
                String server = plugin.getConfig().getString("server-name", "hub");
                meta.setLore(List.of(
                    "\u00a77UUID: " + target.getUniqueId(),
                    "\u00a77Current Server: " + server,
                    "\u00a7eClick to select"
                ));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // Back button
        inv.setItem(49, createButton(Material.ARROW, "\u00a7c\u00a7lBack", null));
        pendingActions.put(player.getUniqueId(), new PendingAction(action, null, null));
        player.openInventory(inv);
    }

    public void openReasonSelector(Player player, String targetName, String action) {
        Inventory inv = Bukkit.createInventory(null, 27, REASON_TITLE);
        List<String> reasons;
        
        if (action.equals("kick")) {
            reasons = List.of("AFK", "Toxic Behavior", "Cheating", "Spam", "Custom...");
        } else if (action.equals("ban")) {
            reasons = List.of("Hacking", "Griefing", "Toxic Behavior", "Exploits", "Custom...");
        } else {
            reasons = List.of();
        }

        int slot = 10;
        for (String reason : reasons) {
            inv.setItem(slot++, createButton(Material.PAPER, "\u00a7e" + reason, null));
        }
        
        inv.setItem(22, createButton(Material.ARROW, "\u00a7c\u00a7lBack", null));
        pendingActions.put(player.getUniqueId(), new PendingAction(action, targetName, null));
        player.openInventory(inv);
    }

    public void openConfirmDialog(Player player, String actionName, String target, String details) {
        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        
        inv.setItem(11, createButton(Material.GREEN_STAINED_GLASS_PANE, "\u00a7a\u00a7lCONFIRM", 
            List.of("\u00a77Action: " + actionName, "\u00a77Target: " + target)));
        
        inv.setItem(4, createButton(Material.BOOK, "\u00a7e\u00a7l" + actionName, 
            List.of("\u00a77Target: " + target, "\u00a77Details: " + details)));
        
        inv.setItem(15, createButton(Material.RED_STAINED_GLASS_PANE, "\u00a7c\u00a7lCANCEL", null));

        pendingActions.put(player.getUniqueId(), new PendingAction(actionName, target, details));
        player.openInventory(inv);
    }

    public void handleMainMenuClick(Player player, int slot) {
        int level = getAdminLevel(player);
        if (level == 0) {
            player.sendMessage("\u00a7cAdmin access revoked.");
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 10: // Move Player
                if (level >= 1) openPlayerSelector(player, "move");
                else player.sendMessage("\u00a7cLevel 1+ required.");
                break;
            case 11: // Kick Player
                if (level >= 1) openPlayerSelector(player, "kick");
                else player.sendMessage("\u00a7cLevel 1+ required.");
                break;
            case 12: // Ban Player
                if (level >= 2) openPlayerSelector(player, "ban");
                else player.sendMessage("\u00a7cLevel 2+ required.");
                break;
            case 13: // Broadcast
                if (level >= 1) {
                    player.closeInventory();
                    player.sendMessage("\u00a7eUse /mnadmin broadcast <message>");
                }
                break;
            case 19: // Maintenance
                if (level >= 3) {
                    player.closeInventory();
                    player.sendMessage("\u00a7eUse /mnadmin maintenance on/off");
                } else {
                    player.sendMessage("\u00a7cLevel 3 required for maintenance toggle.");
                }
                break;
            case 20: // View Logs
                player.sendMessage("\u00a77Use /mnadmin logs to view recent audit entries.");
                break;
            case 21: // Reset PIN
                if (level >= 2) openPlayerSelector(player, "reset_pin");
                else player.sendMessage("\u00a7cLevel 2+ required.");
                break;
            case 26: // Close
                player.closeInventory();
                break;
        }
    }

    public void handlePlayerSelectClick(Player player, ItemStack clicked, int slot) {
        if (slot == 49) {
            openAdminPanel(player);
            return;
        }
        
        if (clicked == null || !clicked.hasItemMeta()) return;
        String targetName = clicked.getItemMeta().getDisplayName().replace("\u00a7a", "").trim();
        PendingAction pending = pendingActions.get(player.getUniqueId());
        if (pending == null) return;

        if (pending.action.equals("move")) {
            openServerSelectorForMove(player, targetName);
        } else if (pending.action.equals("kick") || pending.action.equals("ban")) {
            openReasonSelector(player, targetName, pending.action);
        } else if (pending.action.equals("reset_pin")) {
            openConfirmDialog(player, "reset_pin", targetName, "PIN will be deleted \u2014 player must re-register");
        }
    }

    private void openServerSelectorForMove(Player player, String targetName) {
        Inventory inv = Bukkit.createInventory(null, 27, "\u00a74\u00a7lSelect Server for " + targetName);
        var cfg = plugin.getConfig().getConfigurationSection("servers");
        if (cfg != null) {
            int slot = 10;
            for (String key : cfg.getKeys(false)) {
                var section = cfg.getConfigurationSection(key);
                if (section == null) continue;
                String display = section.getString("display", key);
                String matStr = section.getString("material", "GRASS_BLOCK");
                Material mat;
                try { mat = Material.valueOf(matStr); } catch (Exception e) { mat = Material.GRASS_BLOCK; }
                inv.setItem(slot++, createButton(mat, display.replace('&', '\u00a7'), 
                    List.of("\u00a7eClick to move player here")));
            }
        }
        inv.setItem(22, createButton(Material.ARROW, "\u00a7c\u00a7lCancel", null));
        pendingActions.put(player.getUniqueId(), new PendingAction("move_confirm", targetName, null));
        player.openInventory(inv);
    }

    public void handleReasonClick(Player player, ItemStack clicked, int slot) {
        if (slot == 22) {
            openAdminPanel(player);
            return;
        }
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String reason = clicked.getItemMeta().getDisplayName().replace("\u00a7e", "").trim();
        PendingAction pending = pendingActions.get(player.getUniqueId());
        if (pending == null || pending.target == null) return;

        if (reason.equals("Custom...")) {
            player.closeInventory();
            player.sendMessage("\u00a7eType in chat: /mnadmin " + pending.action + " " + pending.target + " <reason>");
            return;
        }

        openConfirmDialog(player, pending.action, pending.target, reason);
    }

    public void handleConfirmClick(Player player, int slot) {
        PendingAction pending = pendingActions.get(player.getUniqueId());
        if (pending == null) return;

        if (slot == 11) { // CONFIRM
            plugin.getMessenger().sendAdminAction(player, pending.action, pending.target, pending.details);
            player.sendMessage("\u00a7aExecuting " + pending.action + " on " + pending.target + "...");
        } else if (slot == 15) { // CANCEL
            player.sendMessage("\u00a77Action cancelled.");
        }
        player.closeInventory();
        pendingActions.remove(player.getUniqueId());
    }

    public void handleServerSelectorForMove(Player player, ItemStack clicked, int slot) {
        if (slot == 22) {
            player.closeInventory();
            return;
        }
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String serverName = clicked.getItemMeta().getDisplayName().replace("\u00a7", "").replace("l", "").trim();
        // Extract raw server name from config
        String rawServer = findServerNameByDisplay(serverName);
        
        PendingAction pending = pendingActions.get(player.getUniqueId());
        if (pending != null \u0026\u0026 pending.target != null) {
            plugin.getMessenger().sendAdminAction(player, "move", pending.target, rawServer != null ? rawServer : serverName);
            player.sendMessage("\u00a7aMoving " + pending.target + " to " + serverName + "...");
        }
        player.closeInventory();
        pendingActions.remove(player.getUniqueId());
    }

    private String findServerNameByDisplay(String display) {
        var cfg = plugin.getConfig().getConfigurationSection("servers");
        if (cfg == null) return null;
        for (String key : cfg.getKeys(false)) {
            var section = cfg.getConfigurationSection(key);
            if (section == null) continue;
            String disp = section.getString("display", key).replace('&', '\u00a7');
            if (disp.equals(display)) return key;
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        int slot = event.getSlot();

        if (title.equals(MAIN_ADMIN_TITLE)) {
            event.setCancelled(true);
            handleMainMenuClick(player, slot);
        } else if (title.equals(PLAYER_SELECT_TITLE)) {
            event.setCancelled(true);
            handlePlayerSelectorClick(player, event.getCurrentItem(), slot);
        } else if (title.equals(CONFIRM_TITLE)) {
            event.setCancelled(true);
            handleConfirmClick(player, slot);
        } else if (title.equals(REASON_TITLE)) {
            event.setCancelled(true);
            handleReasonClick(player, event.getCurrentItem(), slot);
        } else if (title.startsWith("\u00a74\u00a7lSelect Server for ")) {
            event.setCancelled(true);
            handleServerSelectorForMove(player, event.getCurrentItem(), slot);
        }
    }

    private void handlePlayerSelectorClick(Player player, ItemStack currentItem, int slot) {
        handlePlayerSelectClick(player, currentItem, slot);
    }

    private ItemStack createButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Simple action record
    private static class PendingAction {
        final String action;
        final String target;
        final String details;

        PendingAction(String action, String target, String details) {
            this.action = action;
            this.target = target;
            this.details = details;
        }
    }
}
