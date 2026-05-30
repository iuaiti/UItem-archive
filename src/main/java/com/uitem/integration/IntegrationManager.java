package com.uitem.integration;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

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
     * Dùng plugin name trước (ổn định hơn), sau đó fallback về Class.forName().
     */
    public void detectIntegrations() {
        oraxenIntegration = null;
        itemsAdderIntegration = null;
        executableItemsIntegration = null;
        hasOraxen = false;
        hasItemsAdder = false;
        hasExecutableItems = false;

        // ── Check Oraxen ──
        Plugin oraxenPlugin = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxenPlugin != null && oraxenPlugin.isEnabled()) {
            oraxenVersion = oraxenPlugin.getDescription().getVersion();
            try {
                // Try multiple known class paths for Oraxen API
                Class<?> oraxenItemsClass = tryFindClass(
                        "io.th0rgal.oraxen.api.OraxenItems"
                );
                if (oraxenItemsClass != null) {
                    oraxenIntegration = new OraxenIntegration();
                    hasOraxen = true;
                    plugin.getLogger().info("  ✔ Oraxen v" + oraxenVersion + " — hooked");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ Oraxen plugin found but API hook failed: " + e.getMessage());
                plugin.getLogger().warning("    Please report this to the UItem developer with your Oraxen version.");
            }
        } else {
            plugin.getLogger().info("  ○ Oraxen — not detected");
        }

        // ── Check ItemsAdder ──
        Plugin itemsAdderPlugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdderPlugin != null && itemsAdderPlugin.isEnabled()) {
            itemsAdderVersion = itemsAdderPlugin.getDescription().getVersion();
            try {
                // Try multiple known class paths for ItemsAdder API
                Class<?> customStackClass = tryFindClass(
                        "dev.lone.itemsadder.api.CustomStack",
                        "dev.lone.itemsadder.api.ItemsAdder"
                );
                if (customStackClass != null) {
                    itemsAdderIntegration = new ItemsAdderIntegration();
                    hasItemsAdder = true;
                    plugin.getLogger().info("  ✔ ItemsAdder v" + itemsAdderVersion + " — hooked");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ ItemsAdder plugin found but API hook failed: " + e.getMessage());
                plugin.getLogger().warning("    Please report this to the UItem developer with your ItemsAdder version.");
            }
        } else {
            plugin.getLogger().info("  ○ ItemsAdder — not detected");
        }

        // ── Check ExecutableItems ──
        Plugin executableItemsPlugin = Bukkit.getPluginManager().getPlugin("ExecutableItems");
        if (executableItemsPlugin == null) {
            // ExecutableItems might be listed as "ExecutableItems" or part of SCore
            executableItemsPlugin = Bukkit.getPluginManager().getPlugin("SCore");
        }
        if (executableItemsPlugin != null && executableItemsPlugin.isEnabled()) {
            executableItemsVersion = executableItemsPlugin.getDescription().getVersion();
            try {
                ExecutableItemsIntegration testEi = new ExecutableItemsIntegration();
                executableItemsIntegration = testEi;
                hasExecutableItems = true;
                plugin.getLogger().info("  ✔ ExecutableItems v" + executableItemsVersion + " — hooked");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ ExecutableItems plugin found but API hook failed: " + e.getMessage());
                plugin.getLogger().warning("    Please report this to the UItem developer with your ExecutableItems version.");
            }
        } else {
            plugin.getLogger().info("  ○ ExecutableItems — not detected");
        }
    }

    /**
     * Try to find a class from multiple possible paths.
     * Returns the first matching Class, or null if none found.
     */
    private Class<?> tryFindClass(String... classNames) {
        for (String name : classNames) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
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
