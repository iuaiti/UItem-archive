package com.uitem;

import com.uitem.api.UItemAPI;
import com.uitem.chat.ChatInputManager;
import com.uitem.command.UItemCommand;
import com.uitem.gui.GuiListener;
import com.uitem.provider.ItemProvider;
import com.uitem.provider.ProviderManager;
import com.uitem.provider.VanillaProvider;
import com.uitem.listener.UItemActionListener;
import com.uitem.rarity.RarityConfig;
import com.uitem.recipe.RecipeManager;
import com.uitem.storage.StorageManager;
import com.uitem.util.Messages;
import com.uitem.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Objects;

public final class UItemPlugin extends JavaPlugin {

    private static UItemPlugin instance;
    private UItemManager itemManager;
    private StorageManager storageManager;
    private ProviderManager providerManager;
    private ChatInputManager chatInputManager;
    private GuiListener guiListener;
    private RecipeManager recipeManager;
    private com.uitem.search.SearchManager searchManager;
    private com.uitem.hook.SkillsHookManager skillsHookManager;
    private com.uitem.stat.StatManager statManager;

    // Native Skill system
    private com.uitem.skill.CooldownManager skillCooldownManager;
    private com.uitem.skill.SkillExecutor skillExecutor;
    private com.uitem.listener.UItemSkillListener skillListener;
    private com.uitem.skill.SkillActionBar skillActionBar;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File itemsDir = new File(getDataFolder(), "items");
        File recipesDir = new File(getDataFolder(), "recipes");

        // Keep installations from older UItem versions working: do not create
        // folders (or example files) before managers have a chance to read
        // legacy items.yml / recipes.yml files.
        File legacyItemsFile = new File(getDataFolder(), "items.yml");
        File legacyRecipesFile = new File(getDataFolder(), "recipes.yml");
        boolean useLegacyItems = legacyItemsFile.isFile() && !containsYamlFile(itemsDir);
        boolean useLegacyRecipes = legacyRecipesFile.isFile() && !containsYamlFile(recipesDir);
        if (!useLegacyItems) {
            itemsDir.mkdirs();
        } else {
            getLogger().info("[UItem] Using legacy items.yml; it will be migrated to items/ on the next save.");
        }
        if (!useLegacyRecipes) {
            recipesDir.mkdirs();
        } else {
            getLogger().info("[UItem] Using legacy recipes.yml; it will be migrated to recipes/ on the next save.");
        }
        seedExampleConfigsIfNeeded(itemsDir, !useLegacyItems, recipesDir, !useLegacyRecipes);

        boolean useConsolidated = getConfig().getBoolean("storage.use-consolidated-files", false);
        if (useConsolidated) {
            if (!useLegacyItems) createMainExternalConfigIfNeeded(itemsDir, "item.yml", "items");
            if (!useLegacyRecipes) createMainExternalConfigIfNeeded(recipesDir, "recipe.yml", "recipes");
        }

        saveDefaultConfig();
        com.uitem.util.TextUtil.loadPrefix(this);
        com.uitem.gui.GuiConfig.init(this);

        this.storageManager = new StorageManager(this);
        this.itemManager = new UItemManager(this);
        this.providerManager = new ProviderManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.guiListener = new GuiListener();
        this.searchManager = new com.uitem.search.SearchManager();

        providerManager.detect();
        this.skillsHookManager = new com.uitem.hook.SkillsHookManager(this);
        skillsHookManager.detect();
        this.statManager = new com.uitem.stat.StatManager(this);
        storageManager.loadAll(itemManager);

        this.recipeManager = new RecipeManager(this, this.itemManager);

        this.skillCooldownManager = new com.uitem.skill.CooldownManager(this);
        this.skillExecutor = new com.uitem.skill.SkillExecutor(this, this.skillCooldownManager);
        this.skillListener = new com.uitem.listener.UItemSkillListener(this, this.skillExecutor);
        this.skillActionBar = new com.uitem.skill.SkillActionBar(this, this.skillCooldownManager);
        this.skillActionBar.start();

        RarityConfig.load(this);
        com.uitem.tooltip.TooltipConfig.load(this);
        Messages.init(this);

        UItemAPI.init(this);

        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(new UItemActionListener(this), this);
        getServer().getPluginManager().registerEvents(skillListener, this);
        com.uitem.listener.UItemStatListener statListener = new com.uitem.listener.UItemStatListener(this, this.statManager);
        getServer().getPluginManager().registerEvents(statListener, this);
        statListener.startRegenTask();

        UItemCommand command = new UItemCommand(this);
        Objects.requireNonNull(getCommand("uitem")).setExecutor(command);
        Objects.requireNonNull(getCommand("uitem")).setTabCompleter(command);

        com.uitem.command.CraftingCommand craftingCommand = new com.uitem.command.CraftingCommand(this);
        org.bukkit.command.PluginCommand craftCmd = getCommand("crafting");
        if (craftCmd != null) {
            craftCmd.setExecutor(craftingCommand);
            craftCmd.setTabCompleter(craftingCommand);
        }

        registerIntegrationEnableListener();

        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║        ⚡ UItem v" + getPluginMeta().getVersion() + " — Custom Item Engine");
        getLogger().info("║        ✦ Author: iuaititi");
        getLogger().info("╠══════════════════════════════════════════════╣");
        getLogger().info("║  ■ Loaded: " + itemManager.getAllItems().size() + " custom items");
        getLogger().info("║  ■ Providers: " + providerStatus());
        getLogger().info("║  ■ Scheduler: " + (SchedulerUtil.isFolia() ? "🌐 Folia (Region)" : "☕ Paper/Spigot"));
        getLogger().info("╚══════════════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (providerManager != null) providerManager.shutdown();
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                var topInv = player.getOpenInventory().getTopInventory();
                if (topInv == null) continue;
                var holder = topInv.getHolder();
                if (holder instanceof com.uitem.gui.CraftingGui craftingGui) {
                    craftingGui.onClose();
                } else if (holder instanceof com.uitem.gui.RecipeEditorGui recipeEditorGui) {
                    recipeEditorGui.onClose(player);
                }
                if (holder instanceof com.uitem.gui.AbstractGui || holder instanceof com.uitem.gui.CraftingGui || holder instanceof com.uitem.gui.RecipeEditorGui) {
                    player.closeInventory();
                }
            } catch (Exception ignored) {}
        }
        com.uitem.gui.CraftingGui.removeCraftingPlayer(null);

        if (storageManager != null && itemManager != null) {
            storageManager.saveAllNow(itemManager);
        }
        if (recipeManager != null) {
            recipeManager.saveRecipes();
        }
        if (skillListener != null) {
            skillListener.shutdown();
        }
        if (skillActionBar != null) {
            skillActionBar.shutdown();
        }
        UItemAPI.shutdown();
        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║   ✦ UItem disabled. All items saved.         ");
        getLogger().info("║   ✦ See you next time, iuaititi!              ");
        getLogger().info("╚══════════════════════════════════════════════╝");
        instance = null;
    }

    private String providerStatus() {
        StringBuilder sb = new StringBuilder();
        for (ItemProvider p : providerManager.all()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p.getDisplayName())
                    .append(" [").append(p.getNamespace()).append("]")
                    .append(p.isAvailable() ? "✔" : "✘");
        }
        return sb.toString();
    }

    private void registerIntegrationEnableListener() {
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                Plugin p = event.getPlugin();
                String name = p.getName();
                if (name.equals("Oraxen") || name.equals("ItemsAdder")) {
                    getLogger().info("[UItem] Detected " + name + " enabling — re-detecting providers...");
                    providerManager.detect();
                    getLogger().info("[UItem] Providers: " + providerStatus());
                }
                if (name.equals("AuraSkills") || name.equals("EcoSkills")) {
                    getLogger().info("[UItem] Detected " + name + " enabling — re-detecting skills hook...");
                    skillsHookManager.detect();
                }
            }
        }, this);
    }

    public static UItemPlugin getInstance() {
        return instance;
    }

    public UItemManager getItemManager() {
        return itemManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public ProviderManager getProviderManager() {
        return providerManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public GuiListener getGuiListener() {
        return guiListener;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public com.uitem.skill.CooldownManager getSkillCooldownManager() {
        return skillCooldownManager;
    }

    public com.uitem.skill.SkillExecutor getSkillExecutor() {
        return skillExecutor;
    }

    public com.uitem.search.SearchManager getSearchManager() {
        return searchManager;
    }

    public com.uitem.hook.SkillsHookManager getSkillsHookManager() {
        return skillsHookManager;
    }

    public com.uitem.stat.StatManager getStatManager() {
        return statManager;
    }

    /** Returns whether this directory contains at least one item YAML file. */
    private static boolean containsYamlFile(File directory) {
        if (directory == null || !directory.isDirectory()) return false;
        File[] children = directory.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (child.isDirectory() && containsYamlFile(child)) return true;
            if (child.isFile() && child.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".yml")) return true;
        }
        return false;
    }

    private void seedExampleConfigsIfNeeded(File itemsDir, boolean seedItems, File recipesDir, boolean seedRecipes) {
        try {
            boolean hasItemYml = !seedItems || containsYamlFile(itemsDir);

            if (!hasItemYml) {
                File exampleItems = new File(itemsDir, "examples.yml");
                if (!exampleItems.exists()) {
                    YamlConfiguration ex = new YamlConfiguration();
                    ex.set("items.kiem_vi_du.source", "vanilla:diamond_sword");
                    ex.set("items.kiem_vi_du.category", "WEAPON");
                    ex.set("items.kiem_vi_du.sub-category", "SWORD");
                    ex.set("items.kiem_vi_du.display-name", "<gold>Kiếm Ví Dụ");
                    ex.set("items.kiem_vi_du.lore", List.of("<gray>A sample custom item", "<yellow>Put your config here!"));
                    ex.set("items.kiem_vi_du.rarity", "RARE");
                    ex.set("items.kiem_vi_du.cooldown", 5.0);
                    ex.set("items.kiem_vi_du.permission", "uitem.item.kiem_vi_du");
                    ex.set("items.kiem_vi_du.enchantments.sharpness", 3);
                    ex.set("items.kiem_vi_du.effects.e0.trigger", "HIT");
                    ex.set("items.kiem_vi_du.effects.e0.type", "EXTRA_DAMAGE");
                    ex.set("items.kiem_vi_du.effects.e0.value", 3.0);
                    ex.set("items.kiem_vi_du.effects.e0.chance", 50);
                    ex.set("items.kiem_vi_du.effects.e1.trigger", "DAMAGE");
                    ex.set("items.kiem_vi_du.effects.e1.type", "REFLECT");
                    ex.set("items.kiem_vi_du.effects.e1.value", 25.0);
                    ex.set("items.kiem_vi_du.effects.e1.chance", 100);

                    ex.set("items.kiem_vi_du.skills.fireball.trigger", "RIGHT_CLICK");
                    ex.set("items.kiem_vi_du.skills.fireball.cooldown", 5.0);
                    ex.set("items.kiem_vi_du.skills.fireball.cancel-event", true);
                    ex.set("items.kiem_vi_du.skills.fireball.display-name", "<red>Fireball");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.type", "PLAY_SOUND");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.sound", "ENTITY_BLAZE_SHOOT");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.volume", 1.0);
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.pitch", 1.0);
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.type", "SHOOT_PROJECTILE");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.projectile", "SMALL_FIREBALL");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.speed", 1.6);
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.damage", 7.0);

                    ex.save(exampleItems);
                    getLogger().info("[UItem] Created example item config in items/examples.yml");
                }
            }

            boolean hasRecipeYml = !seedRecipes || containsYamlFile(recipesDir);

            if (!hasRecipeYml) {
                File exampleRecipes = new File(recipesDir, "examples.yml");
                if (!exampleRecipes.exists()) {
                    YamlConfiguration ex = new YamlConfiguration();
                    ex.set("cong_thuc_kiem_vi_du.result-item", "kiem_vi_du");
                    ex.set("cong_thuc_kiem_vi_du.result-amount", 1);
                    ex.set("cong_thuc_kiem_vi_du.shaped", true);
                    ex.set("cong_thuc_kiem_vi_du.shape", List.of(" I ", " S ", " S "));
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.I.uitem-id", "kiem_vi_du");
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.I.amount", 1);
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.S.material", "STICK");
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.S.amount", 1);

                    ex.save(exampleRecipes);
                    getLogger().info("[UItem] Created example recipe config in recipes/examples.yml");
                }
            }
        } catch (Exception e) {
            getLogger().warning("[UItem] Could not seed example configs: " + e.getMessage());
        }
    }

    private void createMainExternalConfigIfNeeded(File dir, String fileName, String section) {
        File mainFile = new File(dir, fileName);
        if (mainFile.exists()) return;
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.options().setHeader(java.util.Arrays.asList(
                    "Main external config file for " + section,
                    "The plugin loads all .yml files in this directory."
            ));
            if ("items".equals(section)) {
                cfg.set("items.kiem_co_ban.source", "vanilla:iron_sword");
                cfg.set("items.kiem_co_ban.category", "WEAPON");
                cfg.set("items.kiem_co_ban.sub-category", "SWORD");
                cfg.set("items.kiem_co_ban.display-name", "<gray>Basic Sword");
                cfg.set("items.kiem_co_ban.lore", java.util.List.of("<gray>A simple iron sword", "<yellow>Used as material for advanced items"));
                cfg.set("items.kiem_co_ban.enchantments.sharpness", 1);

                cfg.set("items.kiem_nang_cao.source", "vanilla:diamond_sword");
                cfg.set("items.kiem_nang_cao.category", "WEAPON");
                cfg.set("items.kiem_nang_cao.sub-category", "SWORD");
                cfg.set("items.kiem_nang_cao.display-name", "<aqua>Advanced Sword");
                cfg.set("items.kiem_nang_cao.lore", java.util.List.of("<gray>Forged from a Basic Sword", "<yellow>Extremely sharp!"));
                cfg.set("items.kiem_nang_cao.enchantments.sharpness", 5);
            } else {
                cfg.set("recipes.cong_thuc_kiem_co_ban.result-item", "kiem_co_ban");
                cfg.set("recipes.cong_thuc_kiem_co_ban.result-amount", 1);
                cfg.set("recipes.cong_thuc_kiem_co_ban.shaped", true);
                cfg.set("recipes.cong_thuc_kiem_co_ban.shape", java.util.List.of(" I ", " S ", " S "));
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.I.material", "IRON_INGOT");
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.I.amount", 1);
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.S.material", "STICK");
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.S.amount", 1);

                cfg.set("recipes.cong_thuc_kiem_nang_cao.result-item", "kiem_nang_cao");
                cfg.set("recipes.cong_thuc_kiem_nang_cao.result-amount", 1);
                cfg.set("recipes.cong_thuc_kiem_nang_cao.shaped", true);
                cfg.set("recipes.cong_thuc_kiem_nang_cao.shape", java.util.List.of(" B ", " S ", " S "));
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.B.uitem-id", "kiem_co_ban");
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.B.amount", 1);
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.S.material", "STICK");
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.S.amount", 1);
            }
            cfg.save(mainFile);
            getLogger().info("[UItem] Created config file " + fileName + " in " + dir.getName() + "/");
        } catch (Exception e) {
            getLogger().warning("[UItem] Could not create " + fileName + ": " + e.getMessage());
        }
    }

}
