package com.uitem;

import com.uitem.api.UItemAPI;
import com.uitem.chat.ChatInputManager;
import com.uitem.command.UItemCommand;
import com.uitem.gui.GuiListener;
import com.uitem.integration.IntegrationManager;
import com.uitem.listener.UItemActionListener;
import com.uitem.resourcepack.ResourcePackManager;
import com.uitem.storage.StorageManager;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
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

        // Khởi tạo Public API — phải trước khi đăng ký listener
        UItemAPI.init(this);

        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(new UItemActionListener(this), this);
        registerResourcePackJoinListener();

        UItemCommand command = new UItemCommand(this);
        Objects.requireNonNull(getCommand("uitem")).setExecutor(command);
        Objects.requireNonNull(getCommand("uitem")).setTabCompleter(command);

        getLogger().info("=================================");
        getLogger().info("  UItem v" + getDescription().getVersion() + " đã được bật!");
        getLogger().info("  Đã load " + itemManager.getAllItems().size() + " custom items.");
        getLogger().info("  Integrations: " + integrationManager.getStatus());
        getLogger().info("  Scheduler mode: " + (SchedulerUtil.isFolia() ? "Folia" : "Paper/Spigot"));

        if (getConfig().getBoolean("resourcepack.auto-generate", true)) {
            // Chạy async để không block main thread khi startup
            SchedulerUtil.runAsync(this, () -> {
                try {
                    resourcePackManager.exportZip();
                    getLogger().info("[UItem] Resource pack đã được export thành công.");
                    if (getConfig().getBoolean("resourcepack.auto-apply", false)) {
                        String url = getConfig().getString("resourcepack.url", "");
                        if (url != null && !url.isBlank()) {
                            SchedulerUtil.runSync(UItemPlugin.this, () -> {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    resourcePackManager.applyResourcePack(p, url);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    getLogger().warning("Không thể generate/export resource pack: " + e.getMessage());
                }
            });
        } else if (getConfig().getBoolean("resourcepack.auto-apply", false)) {
            String url = getConfig().getString("resourcepack.url", "");
            if (url != null && !url.isBlank()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    resourcePackManager.applyResourcePack(player, url);
                }
            }
        }

        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        if (storageManager != null && itemManager != null) {
            storageManager.saveAllNow(itemManager);
        }
        UItemAPI.shutdown();
        getLogger().info("UItem đã được tắt. Tất cả items đã được lưu.");
        instance = null;
    }

    private void registerResourcePackJoinListener() {
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (!getConfig().getBoolean("resourcepack.auto-apply", false)) {
                    return;
                }
                String url = getConfig().getString("resourcepack.url", "");
                if (url == null || url.isBlank()) {
                    return;
                }
                event.getPlayer().sendTitle(
                        "Đang tải resource packs, vui lòng đợi",
                        "Loading resource packs, please wait",
                        5, 60, 10
                );
                SchedulerUtil.runSync(UItemPlugin.this, () ->
                        resourcePackManager.applyResourcePack(event.getPlayer(), url));
            }

            @EventHandler
            public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
                if (!getConfig().getBoolean("resourcepack.auto-apply", false)) {
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
        }, this);
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
}
