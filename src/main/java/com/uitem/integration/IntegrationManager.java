package com.uitem.integration;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Quản lý tích hợp với Oraxen, ItemsAdder và ExecutableItems.
 * Detect runtime và delegate build item nếu có integration ID.
 */
public class IntegrationManager {

    private final UItemPlugin plugin;
    private OraxenIntegration oraxenIntegration;
    private ItemsAdderIntegration itemsAdderIntegration;
    private ExecutableItemsIntegration executableItemsIntegration;
    private boolean hasOraxen;
    private boolean hasItemsAdder;
    private boolean hasExecutableItems;

    private String oraxenVersion;
    private String itemsAdderVersion;
    private String executableItemsVersion;

    public IntegrationManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.hasOraxen = false;
        this.hasItemsAdder = false;
        this.hasExecutableItems = false;
    }

    /**
     * Detect xem Oraxen, ItemsAdder và ExecutableItems có đang cài không.
     * Dùng Class.forName() thay vì plugin name để tránh sai tên plugin.
     */
    public void detectIntegrations() {
        oraxenIntegration = null;
        itemsAdderIntegration = null;
        executableItemsIntegration = null;
        hasOraxen = false;
        hasItemsAdder = false;
        hasExecutableItems = false;

        // ── Check Oraxen ──
        oraxenVersion = getPluginVersion("io.th0rgal.oraxen.api.OraxenItems", "Oraxen");
        try {
            Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            oraxenIntegration = new OraxenIntegration();
            hasOraxen = true;
            plugin.getLogger().info("  ✔ Oraxen" + (oraxenVersion != null ? " v" + oraxenVersion : "") + " — hooked");
        } catch (Exception e) {
            plugin.getLogger().info("  ○ Oraxen — not detected");
        }

        // ── Check ItemsAdder ──
        itemsAdderVersion = getPluginVersion("dev.lone.itemsadder.api.CustomStack", "ItemsAdder");
        try {
            Class.forName("dev.lone.itemsadder.api.CustomStack");
            itemsAdderIntegration = new ItemsAdderIntegration();
            hasItemsAdder = true;
            plugin.getLogger().info("  ✔ ItemsAdder" + (itemsAdderVersion != null ? " v" + itemsAdderVersion : "") + " — hooked");
        } catch (Exception e) {
            plugin.getLogger().info("  ○ ItemsAdder — not detected");
        }

        // ── Check ExecutableItems ──
        executableItemsVersion = getPluginVersion(null, "ExecutableItems");
        try {
            ExecutableItemsIntegration testEi = new ExecutableItemsIntegration();
            executableItemsIntegration = testEi;
            hasExecutableItems = true;
            plugin.getLogger().info("  ✔ ExecutableItems" + (executableItemsVersion != null ? " v" + executableItemsVersion : "") + " — hooked");
        } catch (Exception e) {
            plugin.getLogger().info("  ○ ExecutableItems — not detected");
        }
    }

    /**
     * Tìm version của plugin từ classpath bằng cách scan Bukkit plugins.
     */
    private String getPluginVersion(String className, String fallbackName) {
        if (className != null) {
            try {
                Class<?> cls = Class.forName(className);
                var packageInfo = cls.getPackage();
                if (packageInfo != null) {
                    String implVersion = packageInfo.getImplementationVersion();
                    if (implVersion != null) return implVersion;
                }
            } catch (Exception ignored) {}
        }

        // Fallback: scan plugin name
        try {
            var plugin = Bukkit.getPluginManager().getPlugin(fallbackName);
            if (plugin != null) return plugin.getDescription().getVersion();
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Thử build ItemStack từ integration (Oraxen/ItemsAdder/ExecutableItems).
     * Trả về null nếu không có integration hoặc không có ID.
     */
    public ItemStack tryBuildFromIntegration(UItemData data) {
        // Ưu tiên Oraxen
        if (hasOraxen && data.getOraxenId() != null && !data.getOraxenId().isEmpty()) {
            ItemStack item = oraxenIntegration.getItem(data.getOraxenId());
            if (item != null) return item;
        }

        // Rồi ItemsAdder
        if (hasItemsAdder && data.getItemsAdderId() != null && !data.getItemsAdderId().isEmpty()) {
            ItemStack item = itemsAdderIntegration.getItem(data.getItemsAdderId());
            if (item != null) return item;
        }

        // Sau đó ExecutableItems
        if (hasExecutableItems && data.getExecutableItemsId() != null && !data.getExecutableItemsId().isEmpty()) {
            ItemStack item = executableItemsIntegration.getItem(data.getExecutableItemsId());
            if (item != null) return item;
        }

        return null;
    }

    public boolean hasOraxen() {
        return hasOraxen;
    }

    public boolean hasItemsAdder() {
        return hasItemsAdder;
    }

    public boolean hasExecutableItems() {
        return hasExecutableItems;
    }

    public ExecutableItemsIntegration getExecutableItemsIntegration() {
        return executableItemsIntegration;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        if (hasOraxen) sb.append("Oraxen v").append(oraxenVersion).append(" ✔  ");
        else sb.append("Oraxen ✘  ");
        if (hasItemsAdder) sb.append("ItemsAdder v").append(itemsAdderVersion).append(" ✔  ");
        else sb.append("ItemsAdder ✘  ");
        if (hasExecutableItems) sb.append("ExecutableItems v").append(executableItemsVersion).append(" ✔");
        else sb.append("ExecutableItems ✘");
        return sb.toString();
    }
}
