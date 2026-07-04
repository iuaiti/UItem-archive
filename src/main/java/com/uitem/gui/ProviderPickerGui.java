package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.provider.ItemProvider;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * Wizard step 2: choose the item source.
 * Only shows providers currently available + "Item in hand".
 */
public class ProviderPickerGui extends AbstractGui {

    private final Consumer<ItemProvider> onProviderPicked;
    private final Runnable onPickFromHand;

    public ProviderPickerGui(UItemPlugin plugin, Player player,
                             Consumer<ItemProvider> onProviderPicked,
                             Runnable onPickFromHand) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Choose Item Source</bold></gradient>");
        this.onProviderPicked = onProviderPicked;
        this.onPickFromHand = onPickFromHand;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.NETHER_STAR,
                "<gradient:#ff6b6b:#ffd93d><bold>Choose Source</bold></gradient>",
                List.of(
                        "<gray>Only shows sources currently available",
                        "<gray>on this server"
                )));

        List<ItemProvider> browseable = plugin.getProviderManager().browseable();
        int[] slots = {19, 20, 21, 22, 23, 24};
        for (int i = 0; i < Math.min(browseable.size(), slots.length); i++) {
            ItemProvider p = browseable.get(i);
            Material icon = "vanilla".equals(p.getNamespace()) ? Material.GRASS_BLOCK
                    : "ia".equals(p.getNamespace()) ? Material.EMERALD
                    : "oraxen".equals(p.getNamespace()) ? Material.DIAMOND
                    : Material.CHEST;
            setItem(slots[i], createItem(icon,
                            "<aqua><bold>" + p.getDisplayName() + "</bold>",
                            List.of(
                                    "<gray>Namespace: <white>" + p.getNamespace(),
                                    p instanceof com.uitem.provider.BrowseableProvider
                                            ? "<green>✔ Browse & Search"
                                            : "<gray>Manually identified only",
                                    "",
                                    "<yellow>Click to choose"
                            )),
                    ev -> {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                        if (onProviderPicked != null) onProviderPicked.accept(p);
                    });
        }

        // "Item in hand" — always available
        setItem(25, createItem(Material.PLAYER_HEAD,
                        "<gold><bold>Item in hand</bold>",
                        List.of(
                                "<gray>Auto-detect plugin + namespace + id",
                                "<gray>from the item you are holding",
                                "",
                                "<yellow>Click to use the item in hand"
                        )),
                ev -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    if (onPickFromHand != null) onPickFromHand.run();
                });

        setItem(45, createBackButton(), ev -> goBack());
    }
}
