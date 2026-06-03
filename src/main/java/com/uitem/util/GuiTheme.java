package com.uitem.util;

import com.uitem.gui.AbstractGui;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared visual theme for UItem GUIs — borders, headers, and action buttons.
 */
public final class GuiTheme {

    public static final String GRADIENT = "<gradient:#ff6b6b:#ffd93d>";
    public static final String GRADIENT_BOLD = GRADIENT + "<bold>";
    public static final String GRADIENT_END = "</bold></gradient>";
    public static final String DIVIDER = "<dark_gray><st>────────────────────</st>";

    private GuiTheme() {
    }

    public static String title(String name) {
        return GRADIENT_BOLD + name + GRADIENT_END;
    }

    public static String titleEdit(String id) {
        return GRADIENT + "Edit: </gradient><white>" + id;
    }

    // --- Item stacks ---

    public static ItemStack border() {
        return pane(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack accent() {
        return pane(Material.ORANGE_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack accentAlt() {
        return pane(Material.RED_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack filler() {
        return pane(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack pane(Material material, String nameMiniMessage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(nameMiniMessage));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack glowing(Material material, String name, List<String> lore) {
        ItemStack item = button(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack button(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            if (loreLines != null && !loreLines.isEmpty()) {
                meta.lore(TextUtil.buildLore(loreLines));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack button(Material material, String name, String... loreLines) {
        return button(material, name, List.of(loreLines));
    }

    public static ItemStack header(Material icon, String title, List<String> body) {
        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.addAll(body);
        lore.add(DIVIDER);
        return glowing(icon, GRADIENT_BOLD + title + GRADIENT_END, lore);
    }

    public static ItemStack startButton(int itemCount, int recipeCount) {
        return glowing(Material.NETHER_STAR,
                GRADIENT_BOLD + "▶ START" + GRADIENT_END,
                List.of(
                        "",
                        "<white><bold>Ultimate Item Studio</bold>",
                        "<gray>Quản lý custom item chuyên nghiệp",
                        "",
                        DIVIDER,
                        "<gray>Items: <white>" + itemCount,
                        "<gray>Recipes: <white>" + recipeCount,
                        DIVIDER,
                        "",
                        "<gradient:#7bed9f:#2ed573><bold>» Click để bắt đầu</bold></gradient>"
                ));
    }

    public static ItemStack navBack() {
        return button(Material.SPECTRAL_ARROW,
                "<yellow><bold>← Back</bold>",
                List.of("<gray>Quay lại menu trước"));
    }

    public static ItemStack navHome() {
        return glowing(Material.END_CRYSTAL,
                GRADIENT_BOLD + "⌂ Home" + GRADIENT_END,
                List.of("<gray>Ve man hinh chinh UItem"));
    }

    public static ItemStack navPrevious() {
        return button(Material.ARROW,
                "<yellow><bold>◄</bold> <gray>Trang truoc",
                List.of("<dark_gray>Previous page"));
    }

    public static ItemStack navNext() {
        return button(Material.ARROW,
                "<gray>Trang sau <bold>►</bold>",
                List.of("<dark_gray>Next page"));
    }

    public static ItemStack actionPrimary(Material material, String label, String... lore) {
        return button(material, "<gradient:#7bed9f:#2ed573><bold>" + label + "</bold></gradient>", List.of(lore));
    }

    public static ItemStack actionSecondary(Material material, String label, String... lore) {
        return button(material, "<aqua><bold>" + label + "</bold>", List.of(lore));
    }

    public static ItemStack actionDanger(Material material, String label, String... lore) {
        return button(material, "<red><bold>" + label + "</bold>", List.of(lore));
    }

    public static ItemStack disabledNav() {
        return filler();
    }

    // --- Layout helpers ---

    public static void fillBorder(AbstractGui gui, int rows) {
        int size = rows * 9;
        ItemStack border = border();
        ItemStack accent = accent();
        ItemStack filler = filler();

        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                boolean corner = (row == 0 || row == rows - 1) && (col == 0 || col == 8);
                gui.setItemPublic(i, corner ? accent : border);
            } else if (row == 1 && col >= 1 && col <= 7) {
                gui.setItemPublic(i, filler);
            }
        }
        // Accent stripes under header row
        if (rows >= 6) {
            gui.setItemPublic(2, accentAlt());
            gui.setItemPublic(6, accentAlt());
            gui.setItemPublic(size - 11, accentAlt());
            gui.setItemPublic(size - 7, accentAlt());
        }
    }

    /**
     * Frame for CraftingGui — vanilla-style (khong branding UItem).
     */
    public static void frameCraftingInventory(Inventory inventory, int size, int[] emptySlots, int... extraEmpty) {
        ItemStack pane = pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        ItemStack tableIcon = button(Material.CRAFTING_TABLE,
                Messages.craftGuiTitle(),
                List.of(
                        Messages.craftGuiHintPlace(),
                        Messages.craftGuiHintTake()
                ));

        for (int i = 0; i < size; i++) {
            inventory.setItem(i, pane.clone());
        }
        for (int slot : emptySlots) {
            inventory.setItem(slot, null);
        }
        for (int slot : extraEmpty) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(4, tableIcon);
        inventory.setItem(13, pane.clone());
        inventory.setItem(22, pane.clone());
        inventory.setItem(31, pane.clone());
    }
}