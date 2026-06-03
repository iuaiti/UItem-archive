package com.uitem.resourcepack;

import com.uitem.UItemPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Merges external resource packs (zip or folder) like ItemsAdder external_packs / Oraxen import.
 */
public final class PackImporter {

    private final UItemPlugin plugin;

    public PackImporter(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    public int importInto(Path targetPackRoot, File externalRoot) {
        if (externalRoot == null || !externalRoot.exists()) {
            return 0;
        }
        int files = 0;
        if (externalRoot.isFile() && externalRoot.getName().toLowerCase().endsWith(".zip")) {
            files = importZip(targetPackRoot, externalRoot.toPath());
        } else if (externalRoot.isDirectory()) {
            files = copyTree(externalRoot.toPath(), targetPackRoot);
        }
        return files;
    }

    public List<String> listImportSources(File externalRoot) {
        List<String> names = new ArrayList<>();
        if (externalRoot == null || !externalRoot.exists()) {
            return names;
        }
        File[] children = externalRoot.listFiles();
        if (children == null) return names;
        for (File child : children) {
            names.add(child.getName());
        }
        return names;
    }

    public int importAll(Path targetPackRoot, File externalRoot) {
        int total = 0;
        if (!externalRoot.exists()) {
            externalRoot.mkdirs();
            return 0;
        }
        File[] children = externalRoot.listFiles();
        if (children == null) return 0;
        for (File child : children) {
            if (child.getName().startsWith(".")) continue;
            total += importInto(targetPackRoot, child);
            plugin.getLogger().info("[UItem] Imported external pack: " + child.getName() + " (" + total + " files so far)");
        }
        return total;
    }

    private int importZip(Path targetRoot, Path zipFile) {
        Path temp = targetRoot.resolveSibling(".import-temp-" + System.nanoTime());
        try {
            unzip(zipFile, temp);
            return copyTree(temp, targetRoot);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not import zip " + zipFile, e);
            return 0;
        } finally {
            deleteRecursive(temp);
        }
    }

    private void unzip(Path zip, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = dest.resolve(entry.getName()).normalize();
                if (!out.startsWith(dest)) continue;
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private int copyTree(Path source, Path target) {
        if (!Files.exists(source)) return 0;
        final int[] count = {0};
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path rel = source.relativize(dir);
                    if (rel.toString().contains("__MACOSX")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    Path destDir = target.resolve(rel);
                    Files.createDirectories(destDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = source.relativize(file);
                    if (rel.toString().contains("__MACOSX")) return FileVisitResult.CONTINUE;
                    Path dest = target.resolve(rel);
                    Files.createDirectories(dest.getParent());
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Copy pack tree failed: " + e.getMessage(), e);
        }
        return count[0];
    }

    private void deleteRecursive(Path path) {
        if (path == null || !Files.exists(path)) return;
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
    }
}