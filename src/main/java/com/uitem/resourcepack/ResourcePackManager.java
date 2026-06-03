package com.uitem.resourcepack;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Resource pack engine cho UItem.
 *
 * Chuẩn đường dẫn:
 * - Base model: assets/minecraft/models/item/<material>.json
 * - Custom model: assets/uitem/models/item/<itemId>.json
 * - Texture: assets/uitem/textures/item/<itemId>.png
 *
 * Texture nguồn đặt trong:
 * - <plugins>/UItem/textures/<itemId>.png
 * Nếu thiếu texture, plugin sẽ sinh placeholder để pack vẫn build được.
 */
public class ResourcePackManager {

    private static final int SHA1_LENGTH = 20;

    private final UItemPlugin plugin;
    private final UItemManager itemManager;
    private final File rpDir;
    private File cmdConfigFile;
    private final PackImporter packImporter;

    private ResourcePackSettings settings;
    private TextureResolver textureResolver;

    private final Object cmdLock = new Object();
    private org.bukkit.configuration.file.FileConfiguration cmdConfig;
    private final Map<String, Integer> cmdCache = new LinkedHashMap<>();
    private int nextCmd;

    private volatile File lastZipFile;
    private volatile byte[] lastPackHash;

    public ResourcePackManager(UItemPlugin plugin, UItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.rpDir = new File(plugin.getDataFolder(), "resourcepack");
        this.packImporter = new PackImporter(plugin);
        reloadFromConfig(plugin.getConfig());
        loadCmdConfig();
    }

    /**
     * Đọc lại các trường cấu hình từ config (dùng khi reload).
     */
    public void reloadFromConfig(org.bukkit.configuration.file.FileConfiguration config) {
        this.settings = new ResourcePackSettings(plugin);
        this.textureResolver = new TextureResolver(plugin, settings);
        this.cmdConfigFile = new File(plugin.getDataFolder(), settings.cmdStorageFile());
        loadCmdConfig();
        plugin.getLogger().info("[UItem] ResourcePackManager reloaded config.");
    }

    public ResourcePackSettings getSettings() {
        return settings;
    }

    public File getResourcePackDirectory() {
        return rpDir;
    }

    public File getCmdConfigFile() {
        return cmdConfigFile;
    }

    public File getLastZipFile() {
        return lastZipFile;
    }

    public byte[] getLastPackHash() {
        return lastPackHash == null ? null : lastPackHash.clone();
    }

    public String getLastPackHashHex() {
        return lastPackHash == null ? null : toHex(lastPackHash);
    }

    public synchronized void reload() {
        loadCmdConfig();
    }

    private void loadCmdConfig() {
        synchronized (cmdLock) {
            if (!cmdConfigFile.exists()) {
                File parent = cmdConfigFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                try {
                    cmdConfigFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not create CMD config file: " + e.getMessage());
                }
            }
            cmdConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(cmdConfigFile);
            nextCmd = Math.max(1001, cmdConfig.getInt("next_cmd", 1001));

            cmdCache.clear();
            var section = cmdConfig.getConfigurationSection("items");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    int value = section.getInt(key, -1);
                    if (value > 0) {
                        cmdCache.put(key.toLowerCase(Locale.ROOT), value);
                    }
                }
            }
        }
    }

    private void saveCmdConfig() {
        synchronized (cmdLock) {
            cmdConfig.set("next_cmd", nextCmd);
            for (Map.Entry<String, Integer> entry : cmdCache.entrySet()) {
                cmdConfig.set("items." + entry.getKey(), entry.getValue());
            }
            try {
                cmdConfig.save(cmdConfigFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save CMD config: " + e.getMessage());
            }
        }
    }

    /**
     * Lấy hoặc cấp CustomModelData cho item, đảm bảo không trùng.
     */
    public int getOrAssignCmd(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return -1;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        synchronized (cmdLock) {
            Integer existing = cmdCache.get(normalized);
            if (existing != null && existing > 0) {
                return existing;
            }
            int cmd = nextCmd++;
            cmdCache.put(normalized, cmd);
            saveCmdConfig();
            return cmd;
        }
    }

    /**
     * Đồng bộ CMD vào UItemData nếu item chưa có CMD hợp lệ.
     */
    public void syncCmdToItem(UItemData data) {
        if (data == null || data.getId() == null || data.getId().isBlank()) {
            return;
        }
        if (data.getCustomModelData() > 0) {
            synchronized (cmdLock) {
                cmdCache.put(data.getId().toLowerCase(Locale.ROOT), data.getCustomModelData());
                saveCmdConfig();
            }
            return;
        }
        int cmd = getOrAssignCmd(data.getId());
        data.setCustomModelData(cmd);
    }

    /**
     * Đồng bộ CMD cache vào các item đang load trong RAM.
     * Dùng trên main thread sau khi load/reload để tránh item stack bị thiếu CMD.
     */
    public void syncCmdsToLiveItems() {
        boolean changed = false;
        synchronized (cmdLock) {
            for (UItemData data : itemManager.getAllItems().values()) {
                if (data == null || data.getId() == null || data.getId().isBlank()) {
                    continue;
                }

                String id = data.getId().toLowerCase(Locale.ROOT);
                int currentCmd = data.getCustomModelData();
                Integer cached = cmdCache.get(id);

                if (currentCmd > 0) {
                    if (cached == null || cached != currentCmd) {
                        cmdCache.put(id, currentCmd);
                        changed = true;
                    }
                    continue;
                }

                if (cached != null && cached > 0) {
                    data.setCustomModelData(cached);
                    continue;
                }

                int assigned = nextCmd++;
                cmdCache.put(id, assigned);
                data.setCustomModelData(assigned);
                changed = true;
            }

            if (changed) {
                saveCmdConfig();
            }
        }

        if (changed) {
            plugin.getStorageManager().saveAllNow(itemManager);
        }
    }

    /**
     * Full pipeline: sync CMD → generate folder → export zip (Oraxen / ItemsAdder style build).
     */
    public synchronized File rebuildAll() throws IOException {
        syncCmdsToLiveItems();
        generatePack();
        return exportZip();
    }

    public PackStatus getStatus() {
        File zip = lastZipFile;
        var server = plugin.getPackServer();
        String url = settings.manualUrl();
        if ((url == null || url.isBlank()) && server != null && server.isRunning()) {
            url = server.getPackUrl();
        }
        return new PackStatus(
                rpDir.getAbsolutePath(),
                settings.externalPacksDir().getAbsolutePath(),
                zip == null ? null : zip.getAbsolutePath(),
                getLastPackHashHex(),
                url,
                server != null && server.isRunning(),
                itemManager.getAllItems().size(),
                cmdCache.size()
        );
    }

    public record PackStatus(
            String packDirectory,
            String importDirectory,
            String lastZip,
            String sha1,
            String packUrl,
            boolean hostRunning,
            int itemCount,
            int cmdAssignments
    ) {
    }

    /**
     * Generate toàn bộ pack đúng cấu trúc (merge external + UItem overrides).
     */
    public synchronized void generatePack() {
        clearGeneratedPack();
        createDirectories();
        settings.sourceTextureDir().mkdirs();
        settings.externalPacksDir().mkdirs();

        if (settings.importExternalPacks()) {
            int imported = packImporter.importAll(rpDir.toPath(), settings.externalPacksDir());
            plugin.getLogger().info("[UItem] Merged " + imported + " file(s) from external packs.");
        }

        Map<String, List<ModelJsonHelper.OverrideEntry>> uitemOverrides = new LinkedHashMap<>();
        Map<String, UItemData> snapshot = itemManager.snapshotAll();
        List<Map.Entry<String, UItemData>> sortedItems = new ArrayList<>(snapshot.entrySet());
        sortedItems.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));

        for (Map.Entry<String, UItemData> entry : sortedItems) {
            String itemId = entry.getKey();
            UItemData data = entry.getValue();
            if (data == null || data.isExcludeFromPack()) {
                continue;
            }

            int cmd = data.getCustomModelData();
            if (cmd <= 0) {
                cmd = getOrAssignCmd(itemId);
            }
            if (cmd <= 0) {
                continue;
            }

            Material material = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
            String baseMaterial = material.name().toLowerCase(Locale.ROOT);
            String modelPath = settings.namespace() + ":item/" + sanitizeFileName(itemId);

            uitemOverrides.computeIfAbsent(baseMaterial, k -> new ArrayList<>())
                    .add(new ModelJsonHelper.OverrideEntry(cmd, modelPath));

            copyOrGenerateTexture(data);
        }

        for (Map.Entry<String, List<ModelJsonHelper.OverrideEntry>> entry : uitemOverrides.entrySet()) {
            writeBaseModelWithMerge(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, UItemData> entry : sortedItems) {
            if (entry.getValue() != null && !entry.getValue().isExcludeFromPack()) {
                writeCustomModelJson(entry.getKey(), entry.getValue());
            }
        }

        generatePackMcmeta();
        copyDefaultPackIcon();
        plugin.getLogger().info("[UItem] Resource pack generated at: " + rpDir.getAbsolutePath());
    }

    private void writeBaseModelWithMerge(String baseMaterial, List<ModelJsonHelper.OverrideEntry> uitemEntries) {
        File outFile = new File(rpDir, settings.baseModelFolder() + "/" + baseMaterial + ".json");
        List<ModelJsonHelper.OverrideEntry> existing = settings.mergeVanillaOverrides()
                ? ModelJsonHelper.readOverrides(outFile.toPath())
                : List.of();
        Map<Integer, ModelJsonHelper.OverrideEntry> merged =
                ModelJsonHelper.mergeOverrides(existing, uitemEntries);
        writeText(outFile, ModelJsonHelper.buildBaseModelJson(baseMaterial, merged));
    }

    private void writeCustomModelJson(String itemId, UItemData data) {
        String json = ModelJsonHelper.buildCustomModelJson(data, settings.namespace(), itemId);
        File outFile = new File(rpDir, settings.modelFolder() + "/" + sanitizeFileName(itemId) + ".json");
        writeText(outFile, json);
    }

    private void clearGeneratedPack() {
        if (!rpDir.exists()) {
            return;
        }
        try (Stream<Path> stream = Files.walk(rpDir.toPath())) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(rpDir.toPath()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            plugin.getLogger().warning("Could not delete old file: " + path + " | " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            plugin.getLogger().warning("Could not clean old resource pack: " + e.getMessage());
        }
    }

    private void createDirectories() {
        new File(rpDir, settings.baseModelFolder()).mkdirs();
        new File(rpDir, settings.modelFolder()).mkdirs();
        new File(rpDir, settings.textureFolder()).mkdirs();
        settings.sourceTextureDir().mkdirs();
    }

    private void copyOrGenerateTexture(UItemData data) {
        String safeId = sanitizeFileName(data.getId());
        Path target = new File(rpDir, settings.textureFolder()).toPath().resolve(safeId + ".png");

        boolean copied = textureResolver.copyToPack(data, target);
        if (!copied) {
            plugin.getLogger().warning("[UItem] Missing texture for '" + data.getId()
                    + "' — generating placeholder. Put PNG in textures/ or set resourcepack.texture in item file.");
            try {
                generatePlaceholderTexture(target, data.getId());
            } catch (IOException e) {
                plugin.getLogger().warning("[UItem] Placeholder failed for " + data.getId() + ": " + e.getMessage());
            }
        }
    }

    private void generatePlaceholderTexture(Path target, String itemId) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int seed = Math.abs(itemId == null ? 0 : itemId.hashCode());
            Color c1 = new Color((seed >> 16) & 0xFF, (seed >> 8) & 0xFF, seed & 0xFF, 255);
            Color c2 = new Color(40, 40, 40, 255);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean alt = ((x + y) & 1) == 0;
                    g.setColor(alt ? c1 : c2);
                    g.fillRect(x, y, 1, 1);
                }
            }
            g.setColor(new Color(255, 255, 255, 120));
            g.drawRect(0, 0, 15, 15);
        } finally {
            g.dispose();
        }
        Files.createDirectories(target.getParent());
        ImageIO.write(image, "PNG", target.toFile());
    }

    private void generatePackMcmeta() {
        String description = settings.descriptionEn() + " | EN\n" + settings.descriptionVi() + " | VI";
        String json = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": " + settings.packFormat() + ",\n" +
                "    \"description\": \"" + escapeJson(description) + "\"\n" +
                "  }\n" +
                "}\n";
        writeText(new File(rpDir, "pack.mcmeta"), json);
    }

    private void copyDefaultPackIcon() {
        File target = new File(rpDir, "pack.png");
        if (target.exists()) return;
        try {
            var stream = plugin.getResource("resourcepack/pack.png");
            if (stream != null) {
                Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void sendPackToAllOnline() {
        String url = resolvePackUrl();
        if (url == null || url.isBlank()) {
            plugin.getLogger().warning("[UItem] No pack URL — export pack and start host or set resourcepack.url");
            return;
        }
        byte[] hash = getLastPackHash();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            applyResourcePack(p, url, hash);
        }
    }

    public void sendPackTo(Player player) {
        String url = resolvePackUrl();
        if (url == null || url.isBlank() || player == null) return;
        applyResourcePack(player, url, getLastPackHash());
    }

    public String resolvePackUrl() {
        if (settings.manualUrl() != null && !settings.manualUrl().isBlank()) {
            return settings.manualUrl();
        }
        var server = plugin.getPackServer();
        if (server != null && server.isRunning()) {
            return server.getPackUrl();
        }
        return null;
    }

    /**
     * Xuất toàn bộ pack thành file zip.
     */
    public File exportZip() throws IOException {
        generatePack();

        File zipFile = new File(plugin.getDataFolder(), settings.outputZipName());
        if (zipFile.exists() && !zipFile.delete()) {
            throw new IOException("Could not delete old zip file: " + zipFile.getAbsolutePath());
        }

        Path rpPath = rpDir.toPath();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
             Stream<Path> paths = Files.walk(rpPath)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String entryName = rpPath.relativize(file).toString().replace('\\', '/');
                try {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException io) {
                throw io;
            }
            throw ex;
        }

        lastZipFile = zipFile;
        lastPackHash = calculateSHA1(zipFile);
        plugin.getLogger().info("[UItem] Exported resource pack: " + zipFile.getAbsolutePath());
        return zipFile;
    }

    /**
     * Tính SHA-1 cho file zip.
     */
    public byte[] calculateSHA1(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist to calculate SHA-1");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 not available", e);
        }
    }

    public String sha1Hex(File file) throws IOException {
        return toHex(calculateSHA1(file));
    }

    public byte[] parseHexHash(String hex) {
        if (hex == null) {
            return null;
        }
        String clean = hex.trim().replace(" ", "").toLowerCase(Locale.ROOT);
        if (clean.length() != SHA1_LENGTH * 2 || (clean.length() & 1) != 0) {
            return null;
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            int hi = Character.digit(clean.charAt(i), 16);
            int lo = Character.digit(clean.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }

    /**
     * Gửi resource pack cho player theo URL.
     */
    public void applyResourcePack(Player player, String url) {
        applyResourcePack(player, url, (byte[]) null);
    }

    public void applyResourcePack(Player player, String url, String hashHex) {
        byte[] hash = parseHexHash(hashHex);
        if (hash == null && hashHex != null && !hashHex.isBlank()) {
            plugin.getLogger().warning("[UItem] Invalid hash, will use cached hash or send pack without hash.");
        }
        applyResourcePack(player, url, hash);
    }

    public void applyResourcePack(Player player, String url, byte[] hash) {
        if (player == null || url == null || url.isBlank()) {
            return;
        }

        byte[] finalHash = hash;
        if (finalHash == null || finalHash.length != SHA1_LENGTH) {
            if (lastPackHash != null && lastPackHash.length == SHA1_LENGTH) {
                finalHash = lastPackHash.clone();
            }
        }

        byte[] useHash = finalHash == null ? null : finalHash.clone();
        if (!player.isOnline()) {
            return;
        }
        SchedulerUtil.runForEntity(plugin, player, () -> {
            if (useHash != null) {
                player.setResourcePack(url, useHash);
            } else {
                player.setResourcePack(url);
            }
        });
    }

    private void writeText(File file, String content) {
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write file: " + file.getAbsolutePath() + " | " + e.getMessage());
        }
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

}
