package com.max12311.maxnetworkhub.gui;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SetupWizard implements Listener {

    public static final String PAGE1_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "MaxNetwork Setup (1/3)";
    public static final String PAGE2_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "MaxNetwork Setup (2/3)";
    public static final String PAGE3_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "MaxNetwork Setup (3/3)";

    private final MaxNetworkHub plugin;
    private final Map<UUID, WizardState> states = new ConcurrentHashMap<>();

    public SetupWizard(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------
    // Entry point — called after proxy sends SetupData
    // -------------------------------------------------------
    public void startWizard(Player player, String routingMode, String autoTarget,
                             List<String> allServers, List<String> selectorServers) {
        WizardState state = new WizardState();
        state.routingMode = routingMode;
        state.autoTarget  = autoTarget;
        state.allServers  = allServers;
        state.selectorServers = new ArrayList<>(selectorServers);
        states.put(player.getUniqueId(), state);
        openPage1(player, state);
    }

    // -------------------------------------------------------
    // Page 1: Routing mode
    // -------------------------------------------------------
    private void openPage1(Player player, WizardState state) {
        Inventory inv = Bukkit.createInventory(null, 27, PAGE1_TITLE);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        String[] modes     = {"auto", "selector", "last_server", "default_server"};
        Material[] mats    = {Material.LIME_WOOL,    Material.CYAN_WOOL,    Material.YELLOW_WOOL,  Material.ORANGE_WOOL};
        String[] labels    = {
            ChatColor.GREEN + "AUTO MODE",
            ChatColor.AQUA + "SELECTOR MODE",
            ChatColor.YELLOW + "LAST SERVER MODE",
            ChatColor.GOLD + "DEFAULT SERVER MODE"
        };
        String[] descs = {
            "Always send players directly to one server.",
            "Always show the server selection menu.",
            "Reconnect player to their last visited server.",
            "Send player to their saved default server."
        };
        int[] slots = {10, 11, 12, 13};

        for (int i = 0; i < modes.length; i++) {
            boolean selected = modes[i].equalsIgnoreCase(state.routingMode);
            ItemStack item = makeItem(mats[i], labels[i],
                Arrays.asList(
                    ChatColor.GRAY + descs[i],
                    "",
                    selected ? ChatColor.GREEN + "✔ Currently selected" : ChatColor.DARK_GRAY + "Click to select"
                ),
                selected ? 1 : 0);
            inv.setItem(slots[i], item);
        }

        // Auto target field info
        if ("auto".equalsIgnoreCase(state.routingMode)) {
            inv.setItem(22, makeItem(Material.COMMAND_BLOCK,
                ChatColor.LIGHT_PURPLE + "Auto Target: " + state.autoTarget,
                Arrays.asList(ChatColor.GRAY + "Players will be sent to: " + ChatColor.WHITE + state.autoTarget,
                              ChatColor.YELLOW + "(Change via /mnsetup target <name>)")));
        }

        inv.setItem(26, makeItem(Material.ARROW, ChatColor.GREEN + "Next →",
            Arrays.asList(ChatColor.GRAY + "Server Selector Configuration")));

        player.openInventory(inv);
    }

    // -------------------------------------------------------
    // Page 2: Server selector configuration
    // -------------------------------------------------------
    private void openPage2(Player player, WizardState state) {
        Inventory inv = Bukkit.createInventory(null, 54, PAGE2_TITLE);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        int slot = 0;
        for (int i = 0; i < state.allServers.size() && slot < 45; i++) {
            String name = state.allServers.get(i);
            boolean included = state.selectorServers.contains(name);
            Material mat = included ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String status = included ? ChatColor.GREEN + "✔ In selector" : ChatColor.RED + "✘ Not in selector";
            inv.setItem(slot++, makeItem(mat,
                (included ? ChatColor.GREEN : ChatColor.RED) + name,
                Arrays.asList(ChatColor.GRAY + "Server: " + name, "", status,
                              ChatColor.YELLOW + "Click to toggle")));
        }

        if (state.allServers.isEmpty()) {
            inv.setItem(22, makeItem(Material.BARRIER, ChatColor.RED + "No servers detected",
                Arrays.asList(ChatColor.GRAY + "Make sure servers are registered",
                              ChatColor.GRAY + "in BungeeCord config.yml")));
        }

        inv.setItem(45, makeItem(Material.ARROW, ChatColor.YELLOW + "← Back",
            Arrays.asList(ChatColor.GRAY + "Routing Mode")));
        inv.setItem(53, makeItem(Material.ARROW, ChatColor.GREEN + "Next →",
            Arrays.asList(ChatColor.GRAY + "Review & Save")));

        player.openInventory(inv);
    }

    // -------------------------------------------------------
    // Page 3: Confirm & save
    // -------------------------------------------------------
    private void openPage3(Player player, WizardState state) {
        Inventory inv = Bukkit.createInventory(null, 27, PAGE3_TITLE);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(11, makeItem(Material.PAPER,
            ChatColor.WHITE + "" + ChatColor.BOLD + "Summary",
            Arrays.asList(
                ChatColor.GRAY + "Routing mode: " + ChatColor.WHITE + state.routingMode,
                state.autoTarget != null && !state.autoTarget.isEmpty()
                    ? ChatColor.GRAY + "Auto target: " + ChatColor.WHITE + state.autoTarget : null,
                ChatColor.GRAY + "Selector servers: " + ChatColor.WHITE + state.selectorServers.size(),
                "",
                ChatColor.YELLOW + "Click Save to apply these settings."
            )));

        inv.setItem(13, makeItem(Material.LIME_STAINED_GLASS_PANE,
            ChatColor.GREEN + "" + ChatColor.BOLD + "✔ SAVE",
            Arrays.asList(ChatColor.GRAY + "Save configuration to proxy")));

        inv.setItem(9, makeItem(Material.ARROW, ChatColor.YELLOW + "← Back",
            Arrays.asList(ChatColor.GRAY + "Server Selector")));

        player.openInventory(inv);
    }

    // -------------------------------------------------------
    // Inventory click handler
    // -------------------------------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        WizardState state = states.get(player.getUniqueId());
        if (state == null) return;

        String title = event.getView().getTitle();
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        if (title.equals(PAGE1_TITLE)) {
            handlePage1Click(player, state, slot, clicked);
        } else if (title.equals(PAGE2_TITLE)) {
            handlePage2Click(player, state, slot, clicked);
        } else if (title.equals(PAGE3_TITLE)) {
            handlePage3Click(player, state, slot, clicked);
        }
    }

    private void handlePage1Click(Player player, WizardState state, int slot, ItemStack item) {
        String[] modes = {"auto", "selector", "last_server", "default_server"};
        int[] modeSlots = {10, 11, 12, 13};
        for (int i = 0; i < modeSlots.length; i++) {
            if (slot == modeSlots[i]) {
                state.routingMode = modes[i];
                player.sendMessage(ChatColor.GREEN + "Routing mode set to: " + modes[i]);
                openPage1(player, state);
                return;
            }
        }
        if (slot == 26) {
            openPage2(player, state);
        }
    }

    private void handlePage2Click(Player player, WizardState state, int slot, ItemStack item) {
        if (slot == 45) { openPage1(player, state); return; }
        if (slot == 53) { openPage3(player, state); return; }
        if (slot < 45 && item.getItemMeta() != null) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (state.selectorServers.contains(name)) {
                state.selectorServers.remove(name);
            } else {
                state.selectorServers.add(name);
            }
            openPage2(player, state);
        }
    }

    private void handlePage3Click(Player player, WizardState state, int slot, ItemStack item) {
        if (slot == 9) { openPage2(player, state); return; }
        if (slot == 13) {
            // Save: send config to proxy via plugin message
            plugin.getMessenger().sendSaveSetupConfig(player,
                state.routingMode, state.autoTarget, state.selectorServers);
            player.closeInventory();
            states.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ Setup saved!");
            player.sendMessage(ChatColor.GRAY + "Configuration has been applied to the proxy.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        String title = event.getView().getTitle();
        // Only remove state if not navigating between pages (pages re-open immediately)
        if (!title.equals(PAGE1_TITLE) && !title.equals(PAGE2_TITLE) && !title.equals(PAGE3_TITLE)) {
            states.remove(event.getPlayer().getUniqueId());
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private void fill(Inventory inv, Material mat) {
        ItemStack filler = makeItem(mat, " ", null);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        return makeItem(mat, name, lore, 0);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore, int enchantGlow) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore != null) {
            List<String> filtered = new ArrayList<>();
            for (String l : lore) { if (l != null) filtered.add(l); }
            meta.setLore(filtered);
        }
        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------
    public static class WizardState {
        public String routingMode = "selector";
        public String autoTarget  = "main";
        public List<String> allServers      = new ArrayList<>();
        public List<String> selectorServers = new ArrayList<>();
    }
}
