package com.uitem;

import com.uitem.api.UItemAPI;
import com.uitem.chat.ChatInputManager;
import com.uitem.command.UItemCommand;
import com.uitem.gui.GuiListener;
import com.uitem.integration.IntegrationManager;
import com.uitem.listener.PassiveListener;
import com.uitem.listener.UItemActionListener;
import com.uitem.passive.PassiveManager;
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
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

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
    private PassiveManager passiveManager;
    private PassiveListener passiveListener;
    private ResourcePackServer packServer;
    private RecipeManager recipeManager;
    private Listener resourcePackJoinListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();

        this.storageManager = new StorageManager(this);
        this.itemManager = new UItemManager(this);
        this.integrationManager = new IntegrationManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.guiListener = new GuiListener();
        this.resourcePackManager = new ResourcePackManager(this, this.itemManager);
        this.searchManager = new com.uitem.search.SearchManager();

        storageManager.loadAll(itemManager);
        integrationManager.detectIntegrations();
        resourcePackManager.syncCmdsToLiveItems();

        this.recipeManager = new RecipeManager(this, this.itemManager);

        RarityConfig.load(this);

        this.passiveManager = new PassiveManager(this, this.itemManager);
        this.passiveListener = new PassiveListener(this);

        // Khởi tạo Public API — phải trước khi đăng ký listener
        UItemAPI.init(this);

        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(new UItemActionListener(this), this);
        getServer().getPluginManager().registerEvents(passiveListener, this);
        passiveListener.startHoldTickTask();

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

        getLogger().info("=================================");
        getLogger().info("  UItem v" + getDescription().getVersion() + " enabled!");
        getLogger().info("  Loaded " + itemManager.getAllItems().size() + " custom items.");
        getLogger().info("  Integrations: " + integrationManager.getStatus());
        getLogger().info("  Scheduler mode: " + (SchedulerUtil.isFolia() ? "Folia" : "Paper/Spigot"));

        // Auto-export & auto-apply resource pack như Oraxen/ItemsAdder
        if (getConfig().getBoolean("resourcepack.auto-generate", true)) {
            SchedulerUtil.runAsync(this, () -> {
                try {
                    resourcePackManager.exportZip();
                    getLogger().info("[UItem] Resource pack exported successfully.");
                    if (getConfig().getBoolean("resourcepack.auto-apply", true) && packServer != null && packServer.isRunning()) {
                        String url = packServer.getPackUrl();
                        byte[] hash = resourcePackManager.getLastPackHash();
                        SchedulerUtil.runSync(UItemPlugin.this, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                resourcePackManager.applyResourcePack(p, url, hash);
                            }
                        });
                    }
                } catch (IOException e) {
                    getLogger().warning("Could not generate/export resource pack: " + e.getMessage());
                }
            });
        }

        // Đăng ký join listener sau cùng
        registerResourcePackJoinListener();

        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
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
        if (passiveManager != null) {
            com.uitem.passive.PassiveManager.clearInstance();
        }
        getLogger().info("UItem disabled. All items have been saved.");
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
                SchedulerUtil.runSync(UItemPlugin.this, () ->
                        resourcePackManager.applyResourcePack(player, finalUrl, finalHash));
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

    public PassiveManager getPassiveManager() {
        return passiveManager;
    }

    public PassiveListener getPassiveListener() {
        return passiveListener;
    }

    public ResourcePackServer getPackServer() {
        return packServer;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
