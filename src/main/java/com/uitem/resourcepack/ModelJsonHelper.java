package com.uitem.resourcepack;

import com.uitem.UItemData;
import org.bukkit.Material;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModelJsonHelper {

    private static final Pattern CMD_PATTERN = Pattern.compile("\"custom_model_data\"\\s*:\\s*(\\d+)");
    private static final Pattern MODEL_PATTERN = Pattern.compile("\"model\"\\s*:\\s*\"([^\"]+)\"");

    private ModelJsonHelper() {
    }

    public record OverrideEntry(int cmd, String model) {
    }

    public static boolean isHandheld(String material) {
        String m = material == null ? "" : material.toLowerCase(Locale.ROOT);
        return m.contains("sword") || m.contains("axe") || m.contains("pickaxe")
                || m.contains("shovel") || m.contains("hoe") || m.contains("rod")
                || m.contains("trident") || m.contains("mace") || m.contains("spear");
    }

    public static String defaultParent(UItemData data, String baseMaterial) {
        if (data != null && data.getModelParent() != null && !data.getModelParent().isBlank()) {
            String p = data.getModelParent().trim();
            if (!p.contains(":")) {
                return "minecraft:item/" + p;
            }
            return p;
        }
        return isHandheld(baseMaterial) ? "minecraft:item/handheld" : "minecraft:item/generated";
    }

    public static List<OverrideEntry> readOverrides(Path jsonFile) {
        List<OverrideEntry> list = new ArrayList<>();
        if (jsonFile == null || !Files.isRegularFile(jsonFile)) {
            return list;
        }
        try {
            String content = Files.readString(jsonFile, StandardCharsets.UTF_8);
            if (!content.contains("overrides")) {
                return list;
            }
            Matcher cmdMatcher = CMD_PATTERN.matcher(content);
            Matcher modelMatcher = MODEL_PATTERN.matcher(content);
            List<Integer> cmds = new ArrayList<>();
            List<String> models = new ArrayList<>();
            while (cmdMatcher.find()) {
                cmds.add(Integer.parseInt(cmdMatcher.group(1)));
            }
            while (modelMatcher.find()) {
                String model = modelMatcher.group(1);
                if (model.contains("custom_model_data")) continue;
                models.add(model);
            }
            int pairs = Math.min(cmds.size(), models.size());
            for (int i = 0; i < pairs; i++) {
                list.add(new OverrideEntry(cmds.get(i), models.get(i)));
            }
        } catch (IOException ignored) {
        }
        return list;
    }

    public static Map<Integer, OverrideEntry> mergeOverrides(List<OverrideEntry> existing, List<OverrideEntry> uitem) {
        Map<Integer, OverrideEntry> map = new LinkedHashMap<>();
        for (OverrideEntry e : existing) {
            map.put(e.cmd(), e);
        }
        for (OverrideEntry e : uitem) {
            map.put(e.cmd(), e);
        }
        return map;
    }

    public static String buildBaseModelJson(String baseMaterial, Map<Integer, OverrideEntry> overrides) {
        String parent = isHandheld(baseMaterial) ? "minecraft:item/handheld" : "minecraft:item/generated";
        List<OverrideEntry> sorted = new ArrayList<>(overrides.values());
        sorted.sort(Comparator.comparingInt(OverrideEntry::cmd));

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"parent\": \"").append(parent).append("\",\n");
        json.append("  \"textures\": {\n");
        json.append("    \"layer0\": \"minecraft:item/").append(baseMaterial).append("\"\n");
        json.append("  }");
        if (!sorted.isEmpty()) {
            json.append(",\n  \"overrides\": [\n");
            for (int i = 0; i < sorted.size(); i++) {
                OverrideEntry oe = sorted.get(i);
                json.append("    {\n");
                json.append("      \"predicate\": { \"custom_model_data\": ").append(oe.cmd()).append(" },\n");
                json.append("      \"model\": \"").append(oe.model()).append("\"\n");
                json.append("    }");
                if (i < sorted.size() - 1) json.append(',');
                json.append('\n');
            }
            json.append("  ]");
        }
        json.append("\n}\n");
        return json.toString();
    }

    public static String buildCustomModelJson(UItemData data, String namespace, String itemId) {
        String safeId = itemId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        Material mat = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
        String parent = defaultParent(data, mat.name().toLowerCase(Locale.ROOT));

        List<String> layers = new ArrayList<>();
        if (data.getTextureLayers() != null && !data.getTextureLayers().isEmpty()) {
            layers.addAll(data.getTextureLayers());
        } else {
            layers.add(namespace + ":item/" + safeId);
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"parent\": \"").append(parent).append("\",\n");
        json.append("  \"textures\": {\n");
        for (int i = 0; i < layers.size(); i++) {
            String key = i == 0 ? "layer0" : "layer" + i;
            String tex = layers.get(i);
            if (!tex.contains(":")) {
                tex = namespace + ":item/" + tex;
            }
            json.append("    \"").append(key).append("\": \"").append(tex).append("\"");
            if (i < layers.size() - 1) json.append(',');
            json.append('\n');
        }
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }
}