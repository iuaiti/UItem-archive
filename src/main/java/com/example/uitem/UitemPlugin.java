package com.example.uitem;

import com.example.uitem.gui.AdminGUI;
import com.example.uitem.gui.MainMenuGUI;
import com.example.uitem.listener.GUIListener;
import com.example.uitem.manager.ConfigManager;
import com.example.uitem.manager.RecipeManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class UitemPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private RecipeManager recipeManager;
    private MainMenuGUI mainMenuGUI;
    private AdminGUI adminGUI;

    @Override
    public void onEnable() {
        // Initialize managers
        configManager = new ConfigManager(this);
        recipeManager = new RecipeManager(this);

        // Load configurations
        configManager.loadItems();
        configManager.loadCategories();

        // Initialize GUIs
        mainMenuGUI = new MainMenuGUI(this);
        adminGUI = new AdminGUI(this);

        // Register events
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);

        // Register commands
        this.getCommand("crafting").setExecutor(new com.example.uitem.command.CraftingCommand(this));
        this.getCommand("admin").setExecutor(new com.example.uitem.command.AdminCommand(this));

        // Start recipe checking task (if needed)
        // recipeManager.startRealtimeCheck();

        getLogger().info("uitem has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save configurations
        configManager.saveItems();
        configManager.saveCategories();
        getLogger().info("uitem has been disabled!");
    }

    // Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public MainMenuGUI getMainMenuGUI() {
        return mainMenuGUI;
    }

    public AdminGUI getAdminGUI() {
        return adminGUI;
    }
}