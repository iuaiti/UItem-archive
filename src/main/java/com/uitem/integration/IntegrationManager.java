package com.uitem.integration;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Quản lý tích hợp Oraxen, Nexo, ItemsAdder và ExecutableItems.
 */
public class IntegrationManager {

    public static final String ORAXEN = "oraxen";
    public static final String NEXO = "nexo";
    public static final String ITEMSADDER = "itemsadder";
    public static final String EXECUTABLEITEMS = "executableitems";

    private final UItemPlugin plugin;
    private OraxenIntegration oraxenIntegration;
    private NexoIntegration nexoIntegration;
    private ItemsAdderIntegration itemsAdderIntegration;
    private ExecutableItemsIntegration executableItemsIntegration;
    private boolean hasOraxen;
    private boolean hasNexo;
    private boolean hasItemsAdder;
    private boolean hasExecutableItems;

    private String oraxenVersion;
    private String nexoVersion;
    private String itemsAdderVersion;
    private String executableItemsVersion;

    private List<String> buildPriority = List.of(ORAXEN, NEXO, ITEMSADDER, EXECUTABLEITEMS);

    public IntegrationManager(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void detectIntegrations() {
        loadPriorityFromConfig();
        oraxenIntegration = null;
        nexoIntegration = null;
        itemsAdderIntegration = null;
        executableItemsIntegration = null;
        hasOraxen = false;
        hasNexo = false;
        hasItemsAdder = false;
        hasExecutableItems = false;

        detectOraxen();
        detectNexo();
        detectItemsAdder();
        detectExecutableItems();
    }

    private void loadPriorityFromConfig() {
        FileConfiguration config = plugin.getConfig();
        List<String> fromList = config.getStringList("integration.priority-order");
        if (fromList != null && !fromList.isEmpty()) {
            buildPriority = normalizePriorityList(fromList);
            return;
        }
        String legacy = config.getString("integration.priority", "oraxen");
        if (legacy != null && !legacy.isBlank()) {
            List<String> ordered = new ArrayList<>();
            ordered.add(legacy.toLowerCase(Locale.ROOT).trim());
            for (String def : List.of(ORAXEN, NEXO, ITEMSADDER, EXECUTABLEITEMS)) {
                if (!ordered.contains(def)) {
                    ordered.add(def);
                }
            }
            buildPriority = ordered;
        }
    }

    private static List<String> normalizePriorityList(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) continue;
            String key = entry.toLowerCase(Locale.ROOT).trim();
            if (!out.contains(key)) {
                out.add(key);
            }
        }
        for (String def : List.of(ORAXEN, NEXO, ITEMSADDER, EXECUTABLEITEMS)) {
            if (!out.contains(def)) {
                out.add(def);
            }
        }
        return out;
    }

    private void detectOraxen() {
        Plugin oraxenPlugin = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxenPlugin != null && oraxenPlugin.isEnabled()) {
            oraxenVersion = oraxenPlugin.getDescription().getVersion();
            try {
                if (tryFindClass("io.th0rgal.oraxen.api.OraxenItems") != null) {
                    oraxenIntegration = new OraxenIntegration();
                    hasOraxen = true;
                    plugin.getLogger().info("  ✔ Oraxen v" + oraxenVersion + " — hooked");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ Oraxen plugin found but API hook failed: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("  ○ Oraxen — not detected");
        }
    }

    private void detectNexo() {
        Plugin nexoPlugin = Bukkit.getPluginManager().getPlugin("Nexo");
        if (nexoPlugin != null && nexoPlugin.isEnabled()) {
            nexoVersion = nexoPlugin.getDescription().getVersion();
            try {
                if (tryFindClass("com.nexomc.nexo.api.NexoItems") != null) {
                    nexoIntegration = new NexoIntegration();
                    hasNexo = true;
                    plugin.getLogger().info("  ✔ Nexo v" + nexoVersion + " — hooked");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ Nexo plugin found but API hook failed: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("  ○ Nexo — not detected");
        }
    }

    private void detectItemsAdder() {
        Plugin itemsAdderPlugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdderPlugin != null && itemsAdderPlugin.isEnabled()) {
            itemsAdderVersion = itemsAdderPlugin.getDescription().getVersion();
            try {
                if (tryFindClass(
                        "dev.lone.itemsadder.api.CustomStack",
                        "dev.lone.itemsadder.api.ItemsAdder") != null) {
                    itemsAdderIntegration = new ItemsAdderIntegration();
                    hasItemsAdder = true;
                    plugin.getLogger().info("  ✔ ItemsAdder v" + itemsAdderVersion + " — hooked");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ ItemsAdder plugin found but API hook failed: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("  ○ ItemsAdder — not detected");
        }
    }

    private void detectExecutableItems() {
        Plugin executableItemsPlugin = Bukkit.getPluginManager().getPlugin("ExecutableItems");
        if (executableItemsPlugin == null) {
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
            }
        } else {
            plugin.getLogger().info("  ○ ExecutableItems — not detected");
        }
    }

    private Class<?> tryFindClass(String... classNames) {
        for (String name : classNames) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    public ItemStack tryBuildFromIntegration(UItemData data) {
        for (String key : buildPriority) {
            ItemStack item = tryBuildOne(key, data);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private ItemStack tryBuildOne(String key, UItemData data) {
        return switch (key) {
            case ORAXEN -> hasOraxen && data.getOraxenId() != null && !data.getOraxenId().isEmpty()
                    ? oraxenIntegration.getItem(data.getOraxenId()) : null;
            case NEXO -> hasNexo && data.getNexoId() != null && !data.getNexoId().isEmpty()
                    ? nexoIntegration.getItem(data.getNexoId()) : null;
            case ITEMSADDER -> hasItemsAdder && data.getItemsAdderId() != null && !data.getItemsAdderId().isEmpty()
                    ? itemsAdderIntegration.getItem(data.getItemsAdderId()) : null;
            case EXECUTABLEITEMS -> hasExecutableItems && data.getExecutableItemsId() != null
                    && !data.getExecutableItemsId().isEmpty()
                    ? executableItemsIntegration.getItem(data.getExecutableItemsId()) : null;
            default -> null;
        };
    }

    /**
     * Đọc ID plugin ngoài từ item đang cầm (main hand).
     */
    public String detectIdFromHand(Player player, String integrationKey) {
        if (player == null) return null;
        ItemStack hand = player.getInventory().getItemInMainHand();
        return detectIdFromItem(hand, integrationKey);
    }

    public String detectIdFromItem(ItemStack item, String integrationKey) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return switch (integrationKey) {
            case ORAXEN -> hasOraxen ? oraxenIntegration.getIdFromItem(item) : null;
            case NEXO -> hasNexo ? nexoIntegration.getIdFromItem(item) : null;
            case ITEMSADDER -> hasItemsAdder ? itemsAdderIntegration.getIdFromItem(item) : null;
            case EXECUTABLEITEMS -> hasExecutableItems
                    ? executableItemsIntegration.getExecutableItemId(item) : null;
            default -> null;
        };
    }

    public int hookedCount() {
        int n = 0;
        if (hasOraxen) n++;
        if (hasNexo) n++;
        if (hasItemsAdder) n++;
        if (hasExecutableItems) n++;
        return n;
    }

    public List<String> getBuildPriority() {
        return List.copyOf(buildPriority);
    }

    public boolean hasOraxen() {
        return hasOraxen;
    }

    public boolean hasNexo() {
        return hasNexo;
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

    public String getOraxenVersion() {
        return oraxenVersion;
    }

    public String getNexoVersion() {
        return nexoVersion;
    }

    public String getItemsAdderVersion() {
        return itemsAdderVersion;
    }

    public String getExecutableItemsVersion() {
        return executableItemsVersion;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(statusLine("Oraxen", hasOraxen, oraxenVersion)).append("  ");
        sb.append(statusLine("Nexo", hasNexo, nexoVersion)).append("  ");
        sb.append(statusLine("ItemsAdder", hasItemsAdder, itemsAdderVersion)).append("  ");
        sb.append(statusLine("ExecutableItems", hasExecutableItems, executableItemsVersion));
        return sb.toString();
    }

    private static String statusLine(String name, boolean hooked, String version) {
        if (hooked) {
            return name + " v" + (version != null ? version : "?") + " ✔";
        }
        return name + " ✘";
    }
}