package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.resourcepack.ResourcePackManager;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

/**
 * Resource pack control panel (Oraxen / ItemsAdder style) — bilingual labels via Messages.
 */
public class ResourcePackGui extends AbstractGui {

    public ResourcePackGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("Resource Pack"));
    }

    @Override
    protected void setup() {
        fillBorder(6);
        ResourcePackManager rpm = plugin.getResourcePackManager();
        var status = rpm.getStatus();

        setItem(4, GuiTheme.header(Material.MAP,
                "Resource Pack",
                List.of(
                        Messages.rpGuiItemsLoaded(status.itemCount()),
                        Messages.rpGuiCmdCount(status.cmdAssignments()),
                        Messages.rpGuiHost(status.hostRunning()),
                        Messages.rpGuiUrlLine(status.packUrl() == null ? Messages.rpStatusNone() : status.packUrl())
                )));

        setItem(20, GuiTheme.actionPrimary(Material.ANVIL,
                Messages.rpGuiBuildTitle(),
                Messages.rpGuiBuildDesc1(),
                Messages.rpGuiBuildDesc2()), ev -> runAsyncBuild(player));

        setItem(22, GuiTheme.glowing(Material.NETHER_STAR,
                Messages.rpGuiSendAllTitle(),
                List.of(
                        Messages.rpGuiSendAllDesc1(),
                        Messages.rpGuiSendAllDesc2()
                )), ev -> {
            GuiFeedback.click(player);
            rpm.sendPackToAllOnline();
            player.sendMessage(TextUtil.success(Messages.rpSentAll()));
        });

        setItem(24, GuiTheme.actionSecondary(Material.CHEST,
                Messages.rpGuiExportTitle(),
                Messages.rpGuiExportDesc1(),
                Messages.rpGuiExportDesc2()), ev -> {
            GuiFeedback.click(player);
            SchedulerUtil.runAsync(plugin, () -> {
                try {
                    File zip = rpm.exportZip();
                    SchedulerUtil.runSync(plugin, () -> {
                        player.sendMessage(TextUtil.success(Messages.rpExported(zip.getAbsolutePath())));
                        if (rpm.getLastPackHashHex() != null) {
                            player.sendMessage(TextUtil.prefixed("<gold>SHA-1: <white>" + rpm.getLastPackHashHex()));
                        }
                        open();
                    });
                } catch (Exception ex) {
                    SchedulerUtil.runSync(plugin, () ->
                            player.sendMessage(TextUtil.error(Messages.rpExportFailed(ex.getMessage()))));
                }
            });
        });

        setItem(30, GuiTheme.button(Material.FILLED_MAP,
                Messages.rpGuiFolderTitle(),
                List.of(
                        "<gray>" + status.packDirectory(),
                        "<yellow>" + Messages.rpGuiFolderHint()
                )));

        setItem(32, GuiTheme.button(Material.BUNDLE,
                Messages.rpGuiImportTitle(),
                List.of(
                        "<gray>" + status.importDirectory(),
                        "<yellow>" + Messages.rpGuiImportHint()
                )));

        setItem(49, GuiTheme.button(Material.BOOK,
                Messages.rpGuiHelpTitle(),
                List.of(
                        Messages.rpHelpGenerate(),
                        Messages.rpHelpExport(),
                        Messages.rpHelpBuild(),
                        Messages.rpHelpSendAll()
                )));

        setItem(45, createHomeButton(), ev -> {
            GuiFeedback.click(player);
            new MainHubGui(plugin, player).open();
        });
    }

    private void runAsyncBuild(Player target) {
        GuiFeedback.click(target);
        target.sendMessage(TextUtil.info(Messages.rpGuiBuilding()));
        SchedulerUtil.runAsync(plugin, () -> {
            try {
                File zip = plugin.getResourcePackManager().rebuildAll();
                SchedulerUtil.runSync(plugin, () -> {
                    target.sendMessage(TextUtil.success(Messages.rpRegenerated()));
                    target.sendMessage(TextUtil.success(Messages.rpExported(zip.getAbsolutePath())));
                    String url = plugin.getResourcePackManager().resolvePackUrl();
                    if (url != null && plugin.getConfig().getBoolean("resourcepack.auto-apply", false)) {
                        plugin.getResourcePackManager().sendPackToAllOnline();
                        target.sendMessage(TextUtil.success(Messages.rpSentAll()));
                    }
                    new ResourcePackGui(plugin, target).open();
                });
            } catch (Exception e) {
                SchedulerUtil.runSync(plugin, () ->
                        target.sendMessage(TextUtil.error(Messages.rpGenerateFailed(e.getMessage()))));
            }
        });
    }
}