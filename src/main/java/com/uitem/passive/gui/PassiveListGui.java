package com.uitem.passive.gui;

import com.uitem.UItemPlugin;
import com.uitem.gui.AbstractGui;
import com.uitem.passive.PassiveDefinition;
import com.uitem.passive.PassiveEditorSession;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PassiveListGui extends AbstractGui {

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private int page;

    public PassiveListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public PassiveListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, GuiTheme.title("Passive List"));
        this.page = page;
    }

    @Override
    protected void setup() {
        fillBorder(6);
        List<PassiveDefinition> all = new ArrayList<>(plugin.getPassiveBootstrap().manager().all());
        all.sort(Comparator.comparing(PassiveDefinition::id));
        int perPage = SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / perPage));
        if (page >= totalPages) page = totalPages - 1;

        setItem(4, GuiTheme.header(Material.BOOK,
                "Passive List",
                List.of(
                        "<gray>Trai: <white>Sua  |  Phai: <white>Xoa",
                        "<gray>Shift+Trai: <white>Nhan ban",
                        "<yellow>Trang " + (page + 1) + "/" + totalPages
                )));

        int start = page * perPage;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            if (idx < all.size()) {
                PassiveDefinition def = all.get(idx);
                Material icon = def.trigger().icon();
                List<String> lore = new ArrayList<>(com.uitem.passive.util.PassivePreviewUtil.loreFromDefinition(def));
                lore.add("");
                lore.add(def.enabled() ? "<green>ON" : "<red>OFF");
                lore.add("<gray>Trai: sua | Phai: xoa | Shift: copy");
                setItem(SLOTS[i], GuiTheme.button(icon,
                        (def.enabled() ? "<green>" : "<red>") + def.id(),
                        lore), ev -> handleClick(ev, def));
            }
        }

        if (page > 0) {
            setItem(45, createPreviousButton(), ev -> new PassiveListGui(plugin, player, page - 1).open());
        }
        if (page < totalPages - 1) {
            setItem(53, createNextButton(), ev -> new PassiveListGui(plugin, player, page + 1).open());
        }
        setItem(49, createBackButton(), ev -> new PassiveHubGui(plugin, player).open());
    }

    private void handleClick(InventoryClickEvent ev, PassiveDefinition def) {
        if (ev.getClick() == ClickType.SHIFT_LEFT) {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>ID passive moi (copy tu " + def.id() + "):",
                    input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (plugin.getPassiveBootstrap().manager().has(id)) {
                            player.sendMessage(com.uitem.util.TextUtil.error("ID da ton tai!"));
                            new PassiveListGui(plugin, player, page).open();
                            return;
                        }
                        plugin.getPassiveBootstrap().manager().duplicate(def.id(), id);
                        player.sendMessage(com.uitem.util.TextUtil.success("Da nhan ban thanh " + id));
                        new PassiveListGui(plugin, player, page).open();
                    });
            return;
        }
        if (ev.isRightClick()) {
            new PassiveConfirmDeleteGui(plugin, player, def, page).open();
            return;
        }
        var session = plugin.getPassiveBootstrap().manager().session(player);
        session.loadFrom(def);
        PassiveWizardGui.open(plugin, player, PassiveEditorSession.WizardStep.TRIGGER);
    }
}