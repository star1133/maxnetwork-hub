package com.max12311.maxnetworkhub.gui;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-game network statistics GUI — replaces the removed web panel's stats view.
 * 3 pages: Overview, Players, Server TPS/Memory.
 */
public class StatsGUI implements Listener {

    private static final String TITLE_OVERVIEW = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Network Stats — Overview";
    private static final String TITLE_PLAYERS  = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Network Stats — Players";
    private static final String TITLE_SERVERS  = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Network Stats — Servers";

    private final MaxNetworkHub plugin;
    // Cached data keyed by player UUID (updated each time stats arrive)
    private final Map<UUID, NetworkSnapshot> snapshots = new ConcurrentHashMap<>();
    // Current page per player
    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public StatsGUI(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------
    // Called by ProxyMessageHandler when NetworkStats arrives
    // -------------------------------------------------------
    public void onStatsReceived(Player player, NetworkSnapshot snap) {
        snapshots.put(player.getUniqueId(), snap);
        int page = pages.getOrDefault(player.getUniqueId(), 1);
        openPage(player, page);
    }

    public void openOverview(Player player) {
        NetworkSnapshot snap = snapshots.get(player.getUniqueId());
        if (snap == null) {
            player.sendMessage(ChatColor.GRAY + "Loading stats...");
            plugin.getMessenger().sendRequestNetworkStats(player);
            pages.put(player.getUniqueId(), 1);
            return;
        }
        pages.put(player.getUniqueId(), 1);
        openPage(player, 1);
    }

    private void openPage(Player player, int page) {
        NetworkSnapshot snap = snapshots.get(player.getUniqueId());
        if (snap == null) return;
        if (page == 1) buildOverview(player, snap);
        else if (page == 2) buildPlayers(player, snap);
        else if (page == 3) buildServers(player, snap);
    }

    // -------------------------------------------------------
    // Page 1: Overview
    // -------------------------------------------------------
    private void buildOverview(Player player, NetworkSnapshot snap) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_OVERVIEW);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        // Total online
        inv.setItem(11, makeItem(Material.EMERALD,
            ChatColor.GREEN + "Online Players: " + ChatColor.WHITE + snap.totalOnline,
            list("§7All servers combined", "§7Peak 24h: §f" + snap.peakOnline24h)));

        // Server count
        inv.setItem(13, makeItem(Material.COMPASS,
            ChatColor.AQUA + "Active Servers: " + ChatColor.WHITE + snap.serverCounts.size(),
            list("§7Servers reporting TPS")));

        // Uptime / analytics
        long playtimeHours = snap.totalPlaytimeMinutes / 60;
        inv.setItem(15, makeItem(Material.CLOCK,
            ChatColor.YELLOW + "Total Playtime Today: " + ChatColor.WHITE + playtimeHours + "h",
            list("§7Combined player time across network")));

        // Server breakdown summary
        int slot = 19;
        for (Map.Entry<String, Integer> e : snap.serverCounts.entrySet()) {
            if (slot > 25) break;
            Material mat = tpsToMaterial(snap.tpsData.getOrDefault(e.getKey() + "_tps", 20.0));
            inv.setItem(slot++, makeItem(mat,
                ChatColor.WHITE + e.getKey(),
                list("§7Players: §f" + e.getValue(),
                     "§7TPS: §f" + String.format("%.1f", snap.tpsData.getOrDefault(e.getKey() + "_tps", -1.0)))));
        }

        inv.setItem(26, makeItem(Material.ARROW, ChatColor.GREEN + "Players →",
            list("§7See who is online")));

        player.openInventory(inv);
    }

    // -------------------------------------------------------
    // Page 2: Player list
    // -------------------------------------------------------
    private void buildPlayers(Player player, NetworkSnapshot snap) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PLAYERS);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        int slot = 0;
        for (Map.Entry<String, String> e : snap.playerServers.entrySet()) {
            if (slot >= 45) break;
            String pName = e.getKey();
            String server = e.getValue();

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(pName));
                meta.setDisplayName(ChatColor.YELLOW + pName);
                List<String> lore = list("§7Server: §f" + server);
                // Mark Bedrock players
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(pName);
                if (op.getUniqueId().getMostSignificantBits() == 0L) {
                    lore.add("§b[Bedrock]");
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            inv.setItem(slot++, skull);
        }

        if (snap.playerServers.isEmpty()) {
            inv.setItem(22, makeItem(Material.BARRIER, ChatColor.RED + "No players online", null));
        }

        inv.setItem(45, makeItem(Material.ARROW, ChatColor.YELLOW + "← Overview", list("§7Back")));
        inv.setItem(53, makeItem(Material.ARROW, ChatColor.GREEN + "Servers →", list("§7TPS & Memory")));
        player.openInventory(inv);
    }

    // -------------------------------------------------------
    // Page 3: TPS & Memory per server
    // -------------------------------------------------------
    private void buildServers(Player player, NetworkSnapshot snap) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SERVERS);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (Map.Entry<String, Integer> e : snap.serverCounts.entrySet()) {
            if (slot > 16) break;
            String srvName = e.getKey();
            double tps = snap.tpsData.getOrDefault(srvName + "_tps", -1.0);
            long mem  = snap.tpsData.containsKey(srvName + "_mem")
                ? snap.tpsData.get(srvName + "_mem").longValue() : -1L;

            String tpsStr = tps < 0 ? "§7N/A" : tpsColor(tps) + String.format("%.1f", tps);
            String memStr = mem < 0 ? "§7N/A" : "§f" + (mem / 1024 / 1024) + " MB";

            inv.setItem(slot++, makeItem(tpsToMaterial(tps),
                ChatColor.WHITE + "" + ChatColor.BOLD + srvName,
                list("§7Players: §f" + e.getValue(),
                     "§7TPS: " + tpsStr,
                     "§7Memory: " + memStr)));
        }

        inv.setItem(18, makeItem(Material.ARROW, ChatColor.YELLOW + "← Players", list("§7Back")));
        inv.setItem(26, makeItem(Material.LIME_DYE, ChatColor.GREEN + "Refresh",
            list("§7Re-fetch stats from proxy")));
        player.openInventory(inv);
    }

    // -------------------------------------------------------
    // Click handler
    // -------------------------------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals(TITLE_OVERVIEW) && !title.equals(TITLE_PLAYERS) && !title.equals(TITLE_SERVERS)) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();
        NetworkSnapshot snap = snapshots.get(player.getUniqueId());

        if (title.equals(TITLE_OVERVIEW) && slot == 26) {
            pages.put(player.getUniqueId(), 2); openPage(player, 2);
        } else if (title.equals(TITLE_PLAYERS)) {
            if (slot == 45) { pages.put(player.getUniqueId(), 1); openPage(player, 1); }
            if (slot == 53) { pages.put(player.getUniqueId(), 3); openPage(player, 3); }
        } else if (title.equals(TITLE_SERVERS)) {
            if (slot == 18) { pages.put(player.getUniqueId(), 2); openPage(player, 2); }
            if (slot == 26) {
                // Refresh
                snapshots.remove(player.getUniqueId());
                plugin.getMessenger().sendRequestNetworkStats(player);
                player.sendMessage(ChatColor.GRAY + "Refreshing stats...");
            }
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private Material tpsToMaterial(double tps) {
        if (tps < 0)  return Material.GRAY_STAINED_GLASS_PANE;
        if (tps < 10) return Material.RED_STAINED_GLASS_PANE;
        if (tps < 18) return Material.YELLOW_STAINED_GLASS_PANE;
        return Material.LIME_STAINED_GLASS_PANE;
    }

    private String tpsColor(double tps) {
        if (tps < 10) return "§c";
        if (tps < 18) return "§e";
        return "§a";
    }

    private void fill(Inventory inv, Material mat) {
        ItemStack f = new ItemStack(mat);
        ItemMeta m = f.getItemMeta();
        if (m != null) { m.setDisplayName(" "); f.setItemMeta(m); }
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, f);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> list(String... lines) {
        List<String> l = new ArrayList<>();
        for (String s : lines) if (s != null) l.add(s);
        return l;
    }

    // -------------------------------------------------------
    public static class NetworkSnapshot {
        public int totalOnline;
        public int peakOnline24h;
        public long totalPlaytimeMinutes;
        public Map<String, Integer> serverCounts   = new java.util.LinkedHashMap<>();
        public Map<String, String>  playerServers  = new java.util.LinkedHashMap<>();
        public Map<String, Double>  tpsData        = new java.util.HashMap<>();
    }
}
