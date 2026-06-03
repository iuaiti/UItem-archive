package com.uitem.resourcepack;

import com.uitem.UItemPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ResourcePackSettings {

    private final boolean autoGenerate;
    private final boolean autoApply;
    private final boolean autoHost;
    private final boolean importExternalPacks;
    private final boolean mergeVanillaOverrides;
    private final boolean pullOraxenTextures;
    private final boolean pullItemsAdderTextures;
    private final boolean generateOnItemSave;
    private final File sourceTextureDir;
    private final File externalPacksDir;
    private final List<File> textureSearchDirs;
    private final String namespace;
    private final String modelFolder;
    private final String textureFolder;
    private final String baseModelFolder;
    private final String outputZipName;
    private final String cmdStorageFile;
    private final int packFormat;
    private final String descriptionEn;
    private final String descriptionVi;
    private final int serverPort;
    private final String manualUrl;

    public ResourcePackSettings(UItemPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        File data = plugin.getDataFolder();

        this.autoGenerate = config.getBoolean("resourcepack.auto-generate", true);
        this.autoApply = config.getBoolean("resourcepack.auto-apply", false);
        this.autoHost = config.getBoolean("resourcepack.auto-host", true);
        this.importExternalPacks = config.getBoolean("resourcepack.import-external-packs", true);
        this.mergeVanillaOverrides = config.getBoolean("resourcepack.merge-vanilla-overrides", true);
        this.pullOraxenTextures = config.getBoolean("resourcepack.pull-oraxen-textures", true);
        this.pullItemsAdderTextures = config.getBoolean("resourcepack.pull-itemsadder-textures", true);
        this.generateOnItemSave = config.getBoolean("resourcepack.generate-on-item-save", false);
        this.serverPort = config.getInt("resourcepack.server-port", 9732);
        this.manualUrl = config.getString("resourcepack.url", "");

        this.sourceTextureDir = new File(data, config.getString("resourcepack.source-texture-folder", "textures"));
        this.externalPacksDir = new File(data, config.getString("resourcepack.external-packs-folder", "packs/import"));
        this.textureSearchDirs = new ArrayList<>();
        this.textureSearchDirs.add(sourceTextureDir);
        for (String extra : config.getStringList("resourcepack.texture-search-folders")) {
            if (extra != null && !extra.isBlank()) {
                textureSearchDirs.add(new File(data, extra));
            }
        }

        this.namespace = config.getString("resourcepack.namespace", "uitem");
        this.modelFolder = normalize(config.getString("resourcepack.model-folder", "assets/uitem/models/item"));
        this.textureFolder = normalize(config.getString("resourcepack.texture-folder", "assets/uitem/textures/item"));
        this.baseModelFolder = normalize(config.getString("resourcepack.base-model-folder", "assets/minecraft/models/item"));
        this.outputZipName = config.getString("resourcepack.output-zip", "UItem-ResourcePack.zip");
        this.cmdStorageFile = config.getString("resourcepack.cmd.storage-file", "custom_model_data.yml");
        this.packFormat = Math.max(1, config.getInt("resourcepack.pack-format", 34));
        this.descriptionEn = config.getString("resourcepack.description.en",
                "UItem Custom Resource Pack | Premium items");
        this.descriptionVi = config.getString("resourcepack.description.vi",
                "Goi tai nguyen UItem | Item tuy chinh");
    }

    private static String normalize(String folder) {
        if (folder == null || folder.isBlank()) return "";
        String n = folder.replace('\\', '/');
        while (n.startsWith("/")) n = n.substring(1);
        while (n.endsWith("/")) n = n.substring(0, n.length() - 1);
        return n;
    }

    public boolean autoGenerate() { return autoGenerate; }
    public boolean autoApply() { return autoApply; }
    public boolean autoHost() { return autoHost; }
    public boolean importExternalPacks() { return importExternalPacks; }
    public boolean mergeVanillaOverrides() { return mergeVanillaOverrides; }
    public boolean pullOraxenTextures() { return pullOraxenTextures; }
    public boolean pullItemsAdderTextures() { return pullItemsAdderTextures; }
    public boolean generateOnItemSave() { return generateOnItemSave; }
    public File sourceTextureDir() { return sourceTextureDir; }
    public File externalPacksDir() { return externalPacksDir; }
    public List<File> textureSearchDirs() { return textureSearchDirs; }
    public String namespace() { return namespace; }
    public String modelFolder() { return modelFolder; }
    public String textureFolder() { return textureFolder; }
    public String baseModelFolder() { return baseModelFolder; }
    public String outputZipName() { return outputZipName; }
    public String cmdStorageFile() { return cmdStorageFile; }
    public int packFormat() { return packFormat; }
    public String descriptionEn() { return descriptionEn; }
    public String descriptionVi() { return descriptionVi; }
    public int serverPort() { return serverPort; }
    public String manualUrl() { return manualUrl; }
}