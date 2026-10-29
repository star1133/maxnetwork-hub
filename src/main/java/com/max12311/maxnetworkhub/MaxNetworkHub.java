package com.max12311.maxnetworkhub;

import com.max12311.maxnetworkhub.auth.AuthManager;
import com.max12311.maxnetworkhub.commands.SetupCommand;
import com.max12311.maxnetworkhub.commands.StatsCommand;
import com.max12311.maxnetworkhub.gui.AdminGUI;
import com.max12311.maxnetworkhub.gui.GuiManager;
import com.max12311.maxnetworkhub.gui.SetupWizard;
import com.max12311.maxnetworkhub.gui.StatsGUI;
import com.max12311.maxnetworkhub.messaging.HubMessenger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MaxNetworkHub extends JavaPlugin {
    private static MaxNetworkHub instance;
    private AuthManager authManager;
    private GuiManager guiManager;
    private HubMessenger messenger;
    private AdminGUI adminGui;
    private SetupWizard setupWizard;
    private StatsGUI statsGui;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        authManager = new AuthManager(this);
        guiManager = new GuiManager(this);
        adminGui = new AdminGUI(this);
        setupWizard = new SetupWizard(this);
        statsGui = new StatsGUI(this);
        messenger = new HubMessenger(this);
        messenger.init();

        getServer().getPluginManager().registerEvents(new AutoFlowListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(adminGui, this);
        getServer().getPluginManager().registerEvents(setupWizard, this);
        getServer().getPluginManager().registerEvents(statsGui, this);
        getServer().getPluginManager().registerEvents(new AuthEventListener(this), this);

        // Register commands
        getCommand("mnadmin").setExecutor(new com.max12311.maxnetworkhub.commands.AdminCommand(this));
        getCommand("mnsetup").setExecutor(new SetupCommand(this));
        getCommand("mnstats").setExecutor(new StatsCommand(this));

        getLogger().info("MaxNetwork-Hub v2.0.0 AutoFlow enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MaxNetwork-Hub disabled.");
    }

    public static MaxNetworkHub getInstance() { return instance; }
    public AuthManager getAuthManager() { return authManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public AdminGUI getAdminGui() { return adminGui; }
    public SetupWizard getSetupWizard() { return setupWizard; }
    public StatsGUI getStatsGui() { return statsGui; }
    public HubMessenger getMessenger() { return messenger; }
}
