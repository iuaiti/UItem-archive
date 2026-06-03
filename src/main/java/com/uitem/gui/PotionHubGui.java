package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class PotionHubGui extends AbstractGui {

    public PotionHubGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title(Messages.potionHubTitle()));
    }

    @Override
    protected void setup() {
        GuiTheme.fillBorder(this, 6);
        int count = plugin.getPotionManager().getAll().size();

        setItem(4, GuiTheme.header(Material.BREWING_STAND, Messages.potionHubTitle(),
                List.of(
                        Messages.potionHubSubtitle(),
                        "<yellow>" + count + " <gray>potion dang co"
                )));

        setItem(20, GuiTheme.actionPrimary(Material.LINGERING_POTION,
                Messages.potionListButton(),
                Messages.potionListButtonDesc()), ev -> {
            GuiFeedback.click(player);
            new PotionListGui(plugin, player).open();
        });

        setItem(24, GuiTheme.actionPrimary(Material.EMERALD,
                Messages.potionCreateButton(),
                Messages.potionCreateButtonDesc()), ev -> {
            GuiFeedback.success(player);
            plugin.getChatInputManager().requestInput(player,
                    Messages.potionCreatePrompt(),
                    this::promptCreate);
        });

        setItem(49, createHomeButton(), ev -> new MainHubGui(plugin, player).open());
    }

    private void promptCreate(String input) {
        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (id.isEmpty()) {
            player.sendMessage(com.uitem.util.TextUtil.error(Messages.potionInvalidId()));
            new PotionHubGui(plugin, player).open();
            return;
        }
        if (plugin.getPotionManager().has(id)) {
            player.sendMessage(com.uitem.util.TextUtil.error(Messages.potionExists(id)));
            new PotionHubGui(plugin, player).open();
            return;
        }
        var data = new com.uitem.potion.UPotionData(id);
        plugin.getPotionManager().save(data);
        player.sendMessage(com.uitem.util.TextUtil.success(Messages.potionCreated(id)));
        new PotionEditorGui(plugin, player, data).open();
    }
}