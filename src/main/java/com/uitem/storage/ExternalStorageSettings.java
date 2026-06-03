package com.uitem.storage;

import com.uitem.UItemPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * Reads storage layout from config.yml (per-file folders vs legacy single YAML).
 */
public final class ExternalStorageSettings {

    public enum StorageType {
        FOLDER,
        FILE
    }

    private final StorageType itemsType;
    private final File itemsDirectory;
    private final File itemsLegacyFile;
    private final StorageType recipesType;
    private final File recipesDirectory;
    private final File recipesLegacyFile;
    private final boolean importLegacyOnLoad;

    public ExternalStorageSettings(UItemPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        File dataFolder = plugin.getDataFolder();

        this.itemsType = parseType(config.getString("storage.items.type", "folder"));
        this.itemsDirectory = new File(dataFolder, config.getString("storage.items.directory", "items"));
        this.itemsLegacyFile = new File(dataFolder, config.getString("storage.items.legacy-file", "items.yml"));

        this.recipesType = parseType(config.getString("storage.recipes.type", "folder"));
        this.recipesDirectory = new File(dataFolder, config.getString("storage.recipes.directory", "recipes"));
        this.recipesLegacyFile = new File(dataFolder, config.getString("storage.recipes.legacy-file", "recipes.yml"));

        this.importLegacyOnLoad = config.getBoolean("storage.import-legacy-on-load", true);
    }

    private static StorageType parseType(String raw) {
        if (raw == null) return StorageType.FOLDER;
        return switch (raw.trim().toLowerCase()) {
            case "file", "single", "legacy" -> StorageType.FILE;
            default -> StorageType.FOLDER;
        };
    }

    public StorageType itemsType() {
        return itemsType;
    }

    public File itemsDirectory() {
        return itemsDirectory;
    }

    public File itemsLegacyFile() {
        return itemsLegacyFile;
    }

    public StorageType recipesType() {
        return recipesType;
    }

    public File recipesDirectory() {
        return recipesDirectory;
    }

    public File recipesLegacyFile() {
        return recipesLegacyFile;
    }

    public boolean importLegacyOnLoad() {
        return importLegacyOnLoad;
    }

    public boolean useItemFolder() {
        return itemsType == StorageType.FOLDER;
    }

    public boolean useRecipeFolder() {
        return recipesType == StorageType.FOLDER;
    }
}