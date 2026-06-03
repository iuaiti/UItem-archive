package com.uitem;

import com.uitem.api.UItemAPI;
import com.uitem.chat.ChatInputManager;
import com.uitem.command.UItemCommand;
import com.uitem.gui.GuiListener;
import com.uitem.integration.IntegrationManager;
import com.uitem.ability.EquipAbilityFallbackListener;
import com.uitem.ability.ItemAbilityListener;
import com.uitem.ability.PaperEquipAbilityListener;
import com.uitem.util.ServerVersionUtil;
import com.uitem.listener.UItemActionListener;
import com.uitem.rarity.RarityConfig;
import com.uitem.recipe.RecipeManager;
import com.uitem.resourcepack.ResourcePackManager;
import com.uitem.resourcepack.ResourcePackServer;
import com.uitem.storage.StorageManager;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class UItemPlugin extends JavaPlugin {

    private static UItemPlugin instance;
    private UItemManager itemManager;
    private StorageManager storageManager;
    private IntegrationManager integrationManager;
    private ChatInputManager chatInputManager;
    private GuiListener guiListener;
    private ResourcePackManager resourcePackManager;
    private com.uitem.search.SearchManager searchManager;
    private ResourcePackServer packServer;
    private RecipeManager recipeManager;
    private com.uitem.potion.PotionManager potionManager;
    private Listener resourcePackJoinListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();

        if (!ServerVersionUtil.isSupportedRange()) {
            getLogger().warning("Server version " + ServerVersionUtil.displayVersion()
                    + " is outside the tested range "
                    + ServerVersionUtil.MIN_MAJOR + "." + ServerVersionUtil.MIN_MINOR + "."
                    + ServerVersionUtil.MIN_PATCH + " – "
                    + ServerVersionUtil.MAX_MAJOR + "." + ServerVersionUtil.MAX_MINOR + "."
                    + ServerVersionUtil.MAX_PATCH
                    + ". The plugin will still load; report issues on GitHub.");
        }

        this.storageManager = new StorageManager(this);
        this.itemManager = new UItemManager(this);
        this.integrationManager = new IntegrationManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.guiListener = new GuiListener();
        this.resourcePackManager = new ResourcePackManager(this, this.itemManager);
        this.searchManager = new com.uitem.search.SearchManager();

        storageManager.ensureDefaultExternalFiles();
        storageManager.loadAll(itemManager);
        integrationManager.detectIntegrations();
        resourcePackManager.syncCmdsToLiveItems();

        this.recipeManager = new RecipeManager(this, this.itemManager);
        this.potionManager = new com.uitem.potion.PotionManager(this);

        RarityConfig.load(this);

        // Khởi tạo Messages (language system)
        com.uitem.util.Messages.init(this);

        // Khởi tạo Public API — phải trước khi đăng ký listener
        UItemAPI.init(this);

        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(new UItemActionListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemAbilityListener(this), this);
        if (ServerVersionUtil.supportsEntityEquipmentChangedEvent()) {
            getServer().getPluginManager().registerEvents(new PaperEquipAbilityListener(this), this);
        } else {
            getServer().getPluginManager().registerEvents(new EquipAbilityFallbackListener(this), this);
        }

        UItemCommand command = new UItemCommand(this);
        Objects.requireNonNull(getCommand("uitem")).setExecutor(command);
        Objects.requireNonNull(getCommand("uitem")).setTabCompleter(command);

        com.uitem.command.CraftingCommand craftingCommand = new com.uitem.command.CraftingCommand(this);
        org.bukkit.command.PluginCommand craftCmd = getCommand("crafting");
        if (craftCmd != null) {
            craftCmd.setExecutor(craftingCommand);
        }

        // Khởi động resource pack server
        startPackServer();

        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║        ⚡ UItem v" + getDescription().getVersion() + " — Custom Item Engine");
        getLogger().info("║        ✦ Author: iuaititi");
        getLogger().info("╠══════════════════════════════════════════════╣");
        getLogger().info("║  ■ Loaded: " + itemManager.getAllItems().size() + " custom items");
        getLogger().info("║  ■ Potions: " + potionManager.getAll().size());
        getLogger().info("║  ■ Integrations: " + integrationManager.getStatus());
        getLogger().info("║  ■ Minecraft: " + ServerVersionUtil.displayVersion()
                + (ServerVersionUtil.isSupportedRange() ? " (supported)" : " (untested)"));
        getLogger().info("║  ■ Scheduler: " + (SchedulerUtil.isFolia() ? "🌐 Folia (Region)" : "☕ Paper/Spigot"));
        getLogger().info("╚══════════════════════════════════════════════╝");

        // Auto-export & auto-apply resource pack
        if (getConfig().getBoolean("resourcepack.auto-generate", true)) {
            SchedulerUtil.runAsync(this, () -> {
                try {
                    resourcePackManager.syncCmdsToLiveItems();
                    resourcePackManager.generatePack();
                    resourcePackManager.exportZip();
                    getLogger().info("[UItem] Resource pack exported successfully.");
                    if (getConfig().getBoolean("resourcepack.auto-apply", true) && packServer != null && packServer.isRunning()) {
                        String url = packServer.getPackUrl();
                        byte[] hash = resourcePackManager.getLastPackHash();
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            resourcePackManager.applyResourcePack(p, url, hash);
                        }
                    }
                } catch (IOException e) {
                    getLogger().warning("Could not generate/export resource pack: " + e.getMessage());
                }
            });
        }

        // Đăng ký join listener sau cùng
        registerResourcePackJoinListener();

        // Bắt plugin enable để re-detect integrations nếu plugin tới sau
        new com.uitem.integration.IntegrationHookListener(this).register();
    }

    @Override
    public void onDisable() {
        // Force-close all UItem GUIs to prevent item dupes during reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                var topInv = player.getOpenInventory().getTopInventory();
                if (topInv == null) continue;
                var holder = topInv.getHolder();
                if (holder instanceof com.uitem.gui.CraftingGui craftingGui) {
                    craftingGui.onClose();
                } else if (holder instanceof com.uitem.gui.RecipeEditorGui recipeEditorGui) {
                    recipeEditorGui.onClose(player);
                }
                if (holder instanceof com.uitem.gui.AbstractGui || holder instanceof com.uitem.gui.CraftingGui || holder instanceof com.uitem.gui.RecipeEditorGui) {
                    player.closeInventory();
                }
            } catch (Exception ignored) {
                // Player might be disconnecting, ignore to prevent crash on shutdown
            }
        }
        // Clear static crafting GUI tracking set
        com.uitem.gui.CraftingGui.removeCraftingPlayer(null);

        if (packServer != null) {
            packServer.stop();
        }
        if (storageManager != null && itemManager != null) {
            storageManager.saveAllNow(itemManager);
        }
        if (recipeManager != null) {
            recipeManager.saveRecipes();
        }
        UItemAPI.shutdown();
        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║   ✦ UItem disabled. All items saved.         ");
        getLogger().info("║   ✦ See you next time, iuaititi!              ");
        getLogger().info("╚══════════════════════════════════════════════╝");
        instance = null;
    }

    private void startPackServer() {
        if (!getConfig().getBoolean("resourcepack.auto-host", true)) {
            getLogger().info("[UItem] Resource pack server disabled in config.");
            return;
        }
        int port = getConfig().getInt("resourcepack.server-port", 9732);
        this.packServer = new ResourcePackServer(this);
        try {
            packServer.start(port);
            getLogger().info("[UItem] Resource pack server started on port " + port);
        } catch (IOException e) {
            getLogger().warning("[UItem] Could not start resource pack server: " + e.getMessage());
            getLogger().warning("[UItem] Auto-apply resource pack will not work. Set resourcepack.url manually.");
            this.packServer = null;
        }
    }

    private void registerResourcePackJoinListener() {
        if (resourcePackJoinListener != null) {
            HandlerList.unregisterAll(resourcePackJoinListener);
        }
        resourcePackJoinListener = new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (!getConfig().getBoolean("resourcepack.auto-apply", true)) {
                    return;
                }

                Player player = event.getPlayer();
                String url = null;
                byte[] hash = null;

                // Ưu tiên URL từ config (nếu người dùng tự host)
                String configUrl = getConfig().getString("resourcepack.url", "");
                if (configUrl != null && !configUrl.isBlank()) {
                    url = configUrl;
                    hash = resourcePackManager.getLastPackHash();
                } else if (packServer != null && packServer.isRunning()) {
                    // Tự động dùng server local
                    url = packServer.getPackUrl();
                    hash = resourcePackManager.getLastPackHash();
                }

                if (url == null || url.isBlank()) {
                    return;
                }

                player.sendTitle(
                        "Loading resource packs, please wait",
                        "Loading resource packs, please wait",
                        5, 60, 10
                );

                byte[] finalHash = hash;
                String finalUrl = url;
                resourcePackManager.applyResourcePack(player, finalUrl, finalHash);
            }

            @EventHandler
            public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
                if (!getConfig().getBoolean("resourcepack.auto-apply", true)) {
                    return;
                }
                if (!getConfig().getBoolean("resourcepack.kick-on-fail", true)) {
                    return;
                }

                PlayerResourcePackStatusEvent.Status status = event.getStatus();
                if (status == PlayerResourcePackStatusEvent.Status.DECLINED
                        || status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                        || status == PlayerResourcePackStatusEvent.Status.INVALID_URL
                        || status == PlayerResourcePackStatusEvent.Status.FAILED_RELOAD) {
                    String kickMessage = getConfig().getString(
                            "resourcepack.kick-message",
                            "Không tải được resource packs, vui lòng vào lại\nUnable to load resource packs, please rejoin"
                    );
                    event.getPlayer().kick(Component.text(kickMessage));
                }
            }
        };
        getServer().getPluginManager().registerEvents(resourcePackJoinListener, this);
    }

    public static UItemPlugin getInstance() {
        return instance;
    }

    public UItemManager getItemManager() {
        return itemManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public GuiListener getGuiListener() {
        return guiListener;
    }

    public ResourcePackManager getResourcePackManager() {
    return resourcePackManager;
}

    public com.uitem.search.SearchManager getSearchManager() {
        return searchManager;
    }

    public ResourcePackServer getPackServer() {
        return packServer;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public com.uitem.potion.PotionManager getPotionManager() {
        return potionManager;
    }
}
