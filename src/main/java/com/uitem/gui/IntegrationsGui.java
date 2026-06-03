package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.integration.IntegrationManager;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Trạng thái tích hợp Oraxen / Nexo / ItemsAdder / ExecutableItems.
 */
public class IntegrationsGui extends AbstractGui {

    private static final int SLOT_ORAXEN = 20;
    private static final int SLOT_NEXO = 21;
    private static final int SLOT_IA = 22;
    private static final int SLOT_EI = 23;
    private static final int SLOT_INFO = 31;

    public IntegrationsGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title(Messages.guiIntegrationsTitle()));
    }

    @Override
    protected void setup() {
        GuiTheme.fillBorder(this, 6);
        IntegrationManager mgr = plugin.getIntegrationManager();

        setItem(4, GuiTheme.header(Material.COMPARATOR, Messages.guiIntegrationsTitle(),
                List.of(
                        Messages.guiIntegrationsSubtitle(),
                        "",
                        "<gray>Dang ket noi: <yellow>" + mgr.hookedCount() + "/4"
                )));

        setItem(SLOT_ORAXEN, statusItem(Material.DIAMOND, "Oraxen",
                mgr.hasOraxen(), mgr.getOraxenVersion()));
        setItem(SLOT_NEXO, statusItem(Material.AMETHYST_SHARD, "Nexo",
                mgr.hasNexo(), mgr.getNexoVersion()));
        setItem(SLOT_IA, statusItem(Material.EMERALD, "ItemsAdder",
                mgr.hasItemsAdder(), mgr.getItemsAdderVersion()));
        setItem(SLOT_EI, statusItem(Material.BLAZE_POWDER, "ExecutableItems",
                mgr.hasExecutableItems(), mgr.getExecutableItemsVersion()));

        List<String> priorityLore = new ArrayList<>();
        priorityLore.add("<gray>Thu tu build item goc:");
        for (int i = 0; i < mgr.getBuildPriority().size(); i++) {
            priorityLore.add("<white>" + (i + 1) + ". <yellow>" + mgr.getBuildPriority().get(i));
        }
        priorityLore.add("");
        priorityLore.add("<dark_gray>config.yml → integration.priority-order");

        setItem(SLOT_INFO, GuiTheme.button(Material.BOOK,
                Messages.guiIntegrationsPriority(),
                priorityLore));

        setItem(45, createBackButton(), ev -> {
            GuiFeedback.click(player);
            new MainHubGui(plugin, player).open();
        });

        setItem(49, GuiTheme.actionSecondary(Material.ANVIL,
                Messages.guiIntegrationsRescan(),
                "<gray>Quet lai plugin tren server"), ev -> {
            GuiFeedback.click(player);
            plugin.getIntegrationManager().detectIntegrations();
            player.sendMessage(com.uitem.util.TextUtil.success(
                    "Integrations: " + plugin.getIntegrationManager().getStatus()));
            refresh();
        });
    }

    private org.bukkit.inventory.ItemStack statusItem(Material material, String name,
                                                      boolean hooked, String version) {
        Material icon = hooked ? material : Material.GRAY_STAINED_GLASS_PANE;
        List<String> lore = new ArrayList<>();
        if (hooked) {
            lore.add("<green>Đã kết nối");
            if (version != null) {
                lore.add("<gray>Phiên bản: <white>v" + version);
            }
        } else {
            lore.add("<red>Chưa cài hoặc API lỗi");
            lore.add("<dark_gray>Cài plugin " + name + " lên server");
        }
        lore.add("");
        lore.add("<gray>Trong Item Editor:");
        lore.add("<yellow>Shift+Click <gray>→ lấy ID từ item trên tay");
        if (hooked) {
            return GuiTheme.glowing(icon, name, lore);
        }
        return GuiTheme.button(icon, "<dark_gray>" + name, lore);
    }
}