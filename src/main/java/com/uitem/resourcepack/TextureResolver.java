package com.uitem.resourcepack;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Resolves item textures from multiple folders (Oraxen / ItemsAdder style).
 */
public final class TextureResolver {

    private final UItemPlugin plugin;
    private final ResourcePackSettings settings;
    public TextureResolver(UItemPlugin plugin, ResourcePackSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public List<Path> resolveSources(UItemData data) {
        List<Path> found = new ArrayList<>();
        if (data == null) return found;

        String safeId = sanitize(data.getId());
        String custom = data.getTexturePath();

        if (custom != null && !custom.isBlank()) {
            addIfExists(found, resolveCustomPath(custom));
            addIfExists(found, resolveCustomPath(custom + ".png"));
        }

        for (File dir : settings.textureSearchDirs()) {
            addIfExists(found, dir.toPath().resolve(safeId + ".png"));
            addIfExists(found, dir.toPath().resolve(safeId + ".mcmeta"));
            addIfExists(found, dir.toPath().resolve("items").resolve(safeId + ".png"));
            addIfExists(found, dir.toPath().resolve("item").resolve(safeId + ".png"));
        }

        if (settings.pullOraxenTextures() && data.getOraxenId() != null) {
            found.addAll(findOraxenTexture(data.getOraxenId()));
        }
        if (settings.pullItemsAdderTextures() && data.getItemsAdderId() != null) {
            found.addAll(findItemsAdderTexture(data.getItemsAdderId()));
        }

        return found;
    }

    public boolean copyToPack(UItemData data, Path targetPng) {
        List<Path> sources = resolveSources(data);
        Path png = sources.stream()
                .filter(p -> p != null && Files.isRegularFile(p) && p.toString().endsWith(".png"))
                .findFirst()
                .orElse(null);

        try {
            Files.createDirectories(targetPng.getParent());
            if (png != null) {
                Files.copy(png, targetPng, StandardCopyOption.REPLACE_EXISTING);
                Path mcmeta = png.resolveSibling(png.getFileName().toString().replace(".png", "") + ".png.mcmeta");
                if (!mcmeta.equals(png) && Files.exists(mcmeta)) {
                    Files.copy(mcmeta, targetPng.resolveSibling(targetPng.getFileName() + ".mcmeta"),
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Path altMcmeta = png.getParent().resolve(sanitize(data.getId()) + ".png.mcmeta");
                    if (Files.exists(altMcmeta)) {
                        Files.copy(altMcmeta, targetPng.resolveSibling(targetPng.getFileName() + ".mcmeta"),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                return true;
            }
            return false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Texture copy failed for " + data.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private Path resolveCustomPath(String path) {
        String clean = path.replace('\\', '/');
        if (clean.endsWith(".png")) {
            return settings.sourceTextureDir().toPath().getParent().resolve(clean).normalize();
        }
        return settings.sourceTextureDir().toPath().resolve(clean + ".png");
    }

    private List<Path> findOraxenTexture(String oraxenId) {
        List<Path> list = new ArrayList<>();
        File plugins = Bukkit.getPluginsFolder();
        String[] bases = {
                "Oraxen/pack/textures",
                "Oraxen/pack/assets/oraxen/textures",
                "oraxen/pack/textures"
        };
        for (String base : bases) {
            Path p = plugins.toPath().resolve(base);
            tryFindTexture(list, p, oraxenId);
            tryFindTexture(list, p.resolve("item"), oraxenId);
        }
        return list;
    }

    private List<Path> findItemsAdderTexture(String namespacedId) {
        List<Path> list = new ArrayList<>();
        String id = namespacedId;
        String ns = "minecraft";
        if (namespacedId.contains(":")) {
            String[] parts = namespacedId.split(":", 2);
            ns = parts[0];
            id = parts[1];
        }
        File plugins = Bukkit.getPluginsFolder();
        Path contents = plugins.toPath().resolve("ItemsAdder/contents");
        if (!Files.isDirectory(contents)) {
            contents = plugins.toPath().resolve("ItemsAdder/data/resourcepack");
        }
        tryFindTexture(list, contents.resolve(ns).resolve("textures"), id);
        tryFindTexture(list, contents.resolve(ns).resolve("textures/item"), id);
        return list;
    }

    private void tryFindTexture(List<Path> out, Path dir, String id) {
        if (dir == null || !Files.isDirectory(dir)) return;
        Path direct = dir.resolve(id + ".png");
        addIfExists(out, direct);
        try {
            Files.walk(dir, 4).filter(p -> p.getFileName().toString().equalsIgnoreCase(id + ".png"))
                    .forEach(p -> addIfExists(out, p));
        } catch (IOException ignored) {
        }
    }

    private static void addIfExists(List<Path> list, Path path) {
        if (path != null && Files.isRegularFile(path) && !list.contains(path)) {
            list.add(path);
        }
    }

    private static String sanitize(String input) {
        if (input == null || input.isBlank()) return "unknown";
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }
}