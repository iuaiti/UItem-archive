package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.GuiFeedback;
import com.uitem.util.GuiTheme;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Main menu / start screen — premium entry point for the plugin.
 */
public class MainHubGui extends AbstractGui {

    private static final int SLOT_LOGO = 4;
    private static final int SLOT_START = 22;
    private static final int SLOT_ITEMS = 20;
    private static final int SLOT_RECIPES = 24;
    private static final int SLOT_RP = 13;
    private static final int SLOT_CRAFT = 31;
    private static final int SLOT_CREATE = 40;
    private static final int SLOT_PASSIVE = 38;

    public MainHubGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, GuiTheme.title("✦ UItem Studio"));
    }

    @Override
    protected void setup() {
        GuiTheme.fillBorder(this, 6);

        int items = plugin.getItemManager().getAllItems().size();
        int recipes = plugin.getRecipeManager().getAllRecipes().size();
        String version = plugin.getDescription().getVersion();

        setItem(SLOT_LOGO, GuiTheme.header(Material.END_CRYSTAL, "✦ UItem",
                List.of(
                        "<gray>Custom Item Engine",
                        "<dark_gray>v" + version,
                        "",
                        "<gray>Author: <white>iuaititi"
                )));

        setItem(SLOT_START, GuiTheme.startButton(items, recipes), ev -> {
            GuiFeedback.success(player);
            new ItemListGui(plugin, player).open();
        });

        if (player.hasPermission("uitem.reload")) {
            setItem(SLOT_RP, GuiTheme.glowing(Material.MAP,
                    Messages.rpGuiBuildTitle(),
                    List.of(
                            Messages.rpGuiBuildDesc1(),
                            Messages.rpGuiBuildDesc2(),
                            "<gray>/uitem rp gui"
                    )), ev -> {
                GuiFeedback.click(player);
                new ResourcePackGui(plugin, player).open();
            });
        }

        setItem(SLOT_ITEMS, GuiTheme.actionSecondary(Material.CHEST,
                "Item Library",
                "<gray>Xem va chinh sua tat ca item",
                "<yellow>" + items + " items dang co"), ev -> {
            GuiFeedback.click(player);
            new ItemListGui(plugin, player).open();
        });

        setItem(SLOT_RECIPES, GuiTheme.actionSecondary(Material.KNOWLEDGE_BOOK,
                "Recipes",
                "<gray>Danh sach cong thuc che tao",
                "<yellow>" + recipes + " recipes"), ev -> {
            GuiFeedback.click(player);
            new RecipeListGui(plugin, player).open();
        });

        if (player.hasPermission("craft") || player.hasPermission("uitem.craft")) {
            setItem(SLOT_CRAFT, GuiTheme.actionPrimary(Material.CRAFTING_TABLE,
                    "Crafting Table",
                    "<gray>Mo ban che tao UItem",
                    "<yellow>Can quyen craft"), ev -> {
                GuiFeedback.click(player);
                new CraftingGui(plugin, player).open();
            });
        } else {
            setItem(SLOT_CRAFT, GuiTheme.button(Material.GRAY_STAINED_GLASS_PANE,
                    "<dark_gray>Crafting Table",
                    List.of("<red>Can quyen <white>craft")));
        }

        if (player.hasPermission("uitem.passive") || player.hasPermission("uitem.admin")) {
            int passives = plugin.getPassiveBootstrap().manager().all().size();
            setItem(SLOT_PASSIVE, GuiTheme.glowing(Material.BEACON,
                    "Passive Studio",
                    List.of(
                            "<gray>Tao hanh dong tu dong — GUI",
                            "<yellow>" + passives + " passives"
                    )), ev -> {
                GuiFeedback.click(player);
                new com.uitem.passive.gui.PassiveHubGui(plugin, player).open();
            });
        }

        setItem(SLOT_CREATE, GuiTheme.actionPrimary(Material.EMERALD,
                "+ New Item",
                "<gray>Tao custom item moi",
                "<yellow>Nhap ID de bat dau"), ev -> {
            GuiFeedback.click(player);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhap ID item moi <gray>(a-z, 0-9, _):",
                    input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (id.isEmpty()) {
                            player.sendMessage(TextUtil.error("ID khong hop le!"));
                            open();
                            return;
                        }
                        if (plugin.getItemManager().hasItem(id)) {
                            player.sendMessage(TextUtil.error("Item '" + id + "' da ton tai!"));
                            open();
                            return;
                        }
                        var newData = new com.uitem.UItemData(id);
                        boolean created = com.uitem.api.UItemAPI.get().registerItem(newData, player);
                        if (!created) {
                            player.sendMessage(TextUtil.error("Khong the tao item (bi chan boi plugin khac)."));
                            open();
                            return;
                        }
                        plugin.getResourcePackManager().syncCmdsToLiveItems();
                        player.sendMessage(TextUtil.success("Da tao item '" + id + "'!"));
                        new ItemEditorGui(plugin, player, newData).open();
                    });
        });

        setItem(49, GuiTheme.button(Material.BOOK,
                "<gray><bold>Huong dan",
                List.of(
                        "<gray>/uitem editor <dark_gray>- Menu nay",
                        "<gray>/uitem give <id> <dark_gray>- Give item",
                        "<gray>/craft <dark_gray>- Ban che tao",
                        "",
                        "<dark_gray>Ho tro MiniMessage trong ten & lore"
                )));
    }
}