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
    private static final int SLOT_INTEGRATIONS = 38;
    private static final int SLOT_POTION = 41;

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
                "<gray>Danh sách công thức chế tạo",
                "<yellow>" + recipes + " recipes"), ev -> {
            GuiFeedback.click(player);
            new RecipeListGui(plugin, player).open();
        });

        if (player.hasPermission("craft") || player.hasPermission("uitem.craft")) {
            setItem(SLOT_CRAFT, GuiTheme.actionPrimary(Material.CRAFTING_TABLE,
                    Messages.craftGuiTitle(),
                    "<gray>Mở bàn chế tạo",
                    "<yellow>Can quyen craft"), ev -> {
                GuiFeedback.click(player);
                new CraftingGui(plugin, player).open();
            });
        } else {
            setItem(SLOT_CRAFT, GuiTheme.button(Material.GRAY_STAINED_GLASS_PANE,
                    "<dark_gray>Crafting Table",
                    List.of("<red>Can quyen <white>craft")));
        }

        var integrations = plugin.getIntegrationManager();
        setItem(SLOT_INTEGRATIONS, GuiTheme.glowing(Material.COMPARATOR,
                Messages.guiIntegrationsHub(),
                List.of(
                        Messages.guiIntegrationsHubDesc(),
                        "<yellow>" + integrations.hookedCount() + "/4 <gray>plugin dang ket noi",
                        "<dark_gray>Oraxen · Nexo · ItemsAdder · EI"
                )), ev -> {
            GuiFeedback.click(player);
            new IntegrationsGui(plugin, player).open();
        });

        if (player.hasPermission("uitem.potion") || player.hasPermission("uitem.admin")) {
            int potions = plugin.getPotionManager().getAll().size();
            setItem(SLOT_POTION, GuiTheme.glowing(Material.BREWING_STAND,
                    Messages.potionHubTitle(),
                    List.of(
                            Messages.potionHubSubtitle(),
                            "<yellow>" + potions + " potions"
                    )), ev -> {
                GuiFeedback.click(player);
                new PotionHubGui(plugin, player).open();
            });
        }

        setItem(SLOT_CREATE, GuiTheme.actionPrimary(Material.EMERALD,
                "+ New Item",
                Messages.guiCreateHint(),
                Messages.guiCreateHint2()), ev -> {
            GuiFeedback.click(player);
            plugin.getChatInputManager().requestInput(player,
                    Messages.guiCreatePrompt(),
                    input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (id.isEmpty()) {
                            player.sendMessage(TextUtil.error(Messages.invalidId()));
                            open();
                            return;
                        }
                        if (plugin.getItemManager().hasItem(id)) {
                            player.sendMessage(TextUtil.error(Messages.itemAlreadyExists(id)));
                            open();
                            return;
                        }
                        var newData = new com.uitem.UItemData(id);
                        boolean created = com.uitem.api.UItemAPI.get().registerItem(newData, player);
                        if (!created) {
                            player.sendMessage(TextUtil.error(Messages.cannotCreateBlocked(id)));
                            open();
                            return;
                        }
                        plugin.getResourcePackManager().syncCmdsToLiveItems();
                        player.sendMessage(TextUtil.success(Messages.createdItem(id)));
                        new ItemEditorGui(plugin, player, newData).open();
                    });
        });

        setItem(49, GuiTheme.button(Material.BOOK,
                "<gray><bold>Help",
                List.of(
                        "<gray>/uitem editor <dark_gray>- This menu",
                        "<gray>/uitem give <id> <dark_gray>- Give item",
                        "<gray>/craft <dark_gray>- Crafting table",
                        "",
                        "<dark_gray>MiniMessage supported in name & lore"
                )));
    }
}