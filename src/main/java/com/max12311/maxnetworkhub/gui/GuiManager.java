package com.max12311.maxnetworkhub.gui;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GuiManager implements Listener {
    private final MaxNetworkHub plugin;
    private final Map<UUID, Consumer<String>> pinCallbacks = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingPins = new ConcurrentHashMap<>();
    private final Map<UUID, List<SelectorEntry>> cachedSelectors = new ConcurrentHashMap<>();

    public GuiManager(MaxNetworkHub plugin) {
        this.plugin = plugin;
    }

    // Config helpers
    private String cfg(String path, String def) { return plugin.getConfig().getString(path, def).replace('&', '§'); }
    private Material mat(String path, String defName) {
        try { return Material.valueOf(plugin.getConfig().getString(path, defName)); }
        catch (Exception e) { return Material.valueOf(defName); }
    }
    private String pinVerifyTitle()    { return cfg("gui.pin.verify_title",   "§0§lEnter Your PIN"); }
    private String pinRegisterTitle()  { return cfg("gui.pin.register_title", "§0§lCreate Your PIN"); }
    private int    pinSize()           { return plugin.getConfig().getInt("gui.pin.size", 54); }
    private Material keyMat()          { return mat("gui.pin.key_material",    "LIME_STAINED_GLASS_PANE"); }
    private Material filledMat()       { return mat("gui.pin.filled_material", "GREEN_STAINED_GLASS_PANE"); }
    private Material emptyMat()        { return mat("gui.pin.empty_material",  "WHITE_STAINED_GLASS_PANE"); }
    private Material clearMat()        { return mat("gui.pin.clear_material",  "RED_STAINED_GLASS_PANE"); }
    private Material bgMat()           { return mat("gui.pin.background_material", "BLACK_STAINED_GLASS_PANE"); }
    private String   selectorTitle()   { return cfg("gui.selector.title",     "§0§lSelect Your Server"); }
    private int      selectorSize()    { int s = plugin.getConfig().getInt("gui.selector.size", 27); int r = s % 9; return (r == 0) ? Math.max(9,s) : s - r + 9; }
    private boolean  selectorFill()    { return plugin.getConfig().getBoolean("gui.selector.fill_empty", true); }
    private Material selectorFillMat() { return mat("gui.selector.fill_material", "GRAY_STAINED_GLASS_PANE"); }

    // PIN GUI
    public void openPinRegister(Player player, Consumer<String> callback) {
        pinCallbacks.put(player.getUniqueId(), callback);
        pendingPins.remove(player.getUniqueId());
        player.openInventory(createPinInventory(pinRegisterTitle()));
    }

    public void openPinVerify(Player player, Consumer<String> callback) {
        pinCallbacks.put(player.getUniqueId(), callback);
        pendingPins.remove(player.getUniqueId());
        player.openInventory(createPinInventory(pinVerifyTitle()));
    }

    private Inventory createPinInventory(String title) {
        int size = pinSize();
        Inventory inv = Bukkit.createInventory(null, size, title);
        ItemStack bg = createButton(bgMat(), " ", null);
        for (int i = 0; i < size; i++) inv.setItem(i, bg);
        int[] keySlots = {11, 12, 13, 20, 21, 22, 29, 30, 31, 39};
        int[] digits   = {1,  2,  3,  4,  5,  6,  7,  8,  9,  0};
        for (int i = 0; i < keySlots.length; i++) {
            inv.setItem(keySlots[i], createButton(keyMat(), "§a§l" + digits[i], null));
        }
        inv.setItem(40, createButton(clearMat(), "§c§lClear", null));
        inv.setItem(4, createButton(bgMat(), "§8PIN:", null));
        for (int i = 0; i < 4; i++) inv.setItem(5 + i, createButton(emptyMat(), "§f_", null));
        return inv;
    }

    // Server Selector GUI — from proxy data
    public void openServerSelector(Player player, List<SelectorEntry> entries) {
        cachedSelectors.put(player.getUniqueId(), entries);
        int size = selectorSize();
        Inventory inv = Bukkit.createInventory(null, size, selectorTitle());
        if (selectorFill()) {
            ItemStack filler = createButton(selectorFillMat(), " ", null);
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }
        for (SelectorEntry e : entries) {
            if (e.slot < 0 || e.slot >= size) continue;
            Material m;
            try { m = Material.valueOf(e.material); } catch (Exception ex) { m = Material.GRASS_BLOCK; }
            inv.setItem(e.slot, createButton(m, e.display, e.lore));
        }
        player.openInventory(inv);
    }

    // Fallback: build selector from Hub config
    public void openServerSelector(Player player) {
        List<SelectorEntry> entries = buildFromHubConfig();
        openServerSelector(player, entries);
    }

    private List<SelectorEntry> buildFromHubConfig() {
        List<SelectorEntry> list = new ArrayList<>();
        var cfg = plugin.getConfig().getConfigurationSection("servers");
        if (cfg == null) return list;
        for (String key : cfg.getKeys(false)) {
            var sec = cfg.getConfigurationSection(key);
            if (sec == null) continue;
            list.add(new SelectorEntry(key,
                sec.getString("display", key).replace('&', '§'),
                sec.getString("material", "GRASS_BLOCK"),
                sec.getInt("slot", list.size()),
                sec.getStringList("lore").stream().map(s -> s.replace('&', '§')).toList()));
        }
        return list;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title.equals(pinVerifyTitle()) || title.equals(pinRegisterTitle())) {
            event.setCancelled(true);
            handlePinClick(player, event.getSlot());
        } else if (title.equals(selectorTitle())) {
            event.setCancelled(true);
            handleSelectorClick(player, event.getSlot());
        }
    }

    private void handlePinClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        Consumer<String> callback = pinCallbacks.get(uuid);
        int[] keySlots = {11, 12, 13, 20, 21, 22, 29, 30, 31, 39};
        int[] digits   = {1,  2,  3,  4,  5,  6,  7,  8,  9,  0};
        int clickedDigit = -1;
        for (int i = 0; i < keySlots.length; i++) {
            if (keySlots[i] == slot) { clickedDigit = digits[i]; break; }
        }
        if (clickedDigit >= 0) {
            String current = pendingPins.getOrDefault(uuid, "");
            if (current.length() < 4) {
                current += clickedDigit;
                pendingPins.put(uuid, current);
                updatePinDisplay(player, current);
                if (current.length() == 4) {
                    final String pin = current;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (callback != null) { callback.accept(pin); pinCallbacks.remove(uuid); pendingPins.remove(uuid); }
                    }, 10L);
                }
            }
        } else if (slot == 40) {
            pendingPins.put(uuid, "");
            updatePinDisplay(player, "");
        }
    }

    private void updatePinDisplay(Player player, String current) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < 4; i++) {
            inv.setItem(5 + i, i < current.length()
                ? createButton(filledMat(), "§a§l*", null)
                : createButton(emptyMat(),  "§f_",        null));
        }
    }

    private void handleSelectorClick(Player player, int slot) {
        List<SelectorEntry> entries = cachedSelectors.getOrDefault(player.getUniqueId(), buildFromHubConfig());
        for (SelectorEntry e : entries) {
            if (e.slot == slot) {
                player.closeInventory();
                String msg = plugin.getConfig().getString("messages.connected", "&aConnecting to &e%server%...");
                player.sendMessage(msg.replace("%server%", e.name).replace('&', '§'));
                plugin.getMessenger().sendSetDefaultServer(player, e.name);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!plugin.getAuthManager().isAuthenticating(uuid)) return;
        player.sendMessage("§c§l[PIN Required] §eType §f/pin <4-digit-code> §eor wait for the menu to reopen.");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !plugin.getAuthManager().isAuthenticating(uuid)) return;
            String t = plugin.getAuthManager().getAuthType(uuid);
            if ("LOGIN".equals(t)) openPinVerify(player, pin -> plugin.getAuthManager().submitPin(player, pin));
            else if ("REGISTER".equals(t)) openPinRegister(player, pin -> plugin.getAuthManager().submitPin(player, pin));
        }, 10L);
    }

    private ItemStack createButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore.stream().map(s -> s.replace('&', '§')).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class SelectorEntry {
        public final String name, display, material;
        public final int slot;
        public final List<String> lore;
        public SelectorEntry(String name, String display, String material, int slot, List<String> lore) {
            this.name = name; this.display = display; this.material = material;
            this.slot = slot; this.lore = lore != null ? lore : new ArrayList<>();
        }
    }
}
