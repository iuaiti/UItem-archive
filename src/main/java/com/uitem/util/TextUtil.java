package com.uitem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class cho text và component.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    /** Spacer so the first visible lore line is not hidden by the client tooltip. */
    private static final Component LORE_LEADING_SPACER = Component.text(" ", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false);

    private TextUtil() {
    }

    /**
     * Parse MiniMessage string thành Component.
     */
    public static Component parse(String miniMessageStr) {
        if (miniMessageStr == null || miniMessageStr.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessageStr)
                .decoration(TextDecoration.ITALIC, false); // Tắt italic mặc định của lore
    }

    /**
     * Split lore entries on newlines and wrap very long single lines so tooltips use multiple rows.
     */
    public static List<String> expandLoreLines(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            if (line == null) {
                out.add("");
                continue;
            }
            String normalized = line.replace("\r\n", "\n").replace('\r', '\n');
            if (!normalized.contains("\n")) {
                out.addAll(wrapLongLoreLine(normalized));
                continue;
            }
            for (String part : normalized.split("\n", -1)) {
                out.addAll(wrapLongLoreLine(part));
            }
        }
        return out;
    }

    private static List<String> wrapLongLoreLine(String line) {
        if (line == null) {
            return List.of("");
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return List.of("");
        }
        final int maxLen = 38;
        if (trimmed.length() <= maxLen) {
            return List.of(line);
        }
        if (trimmed.contains(" | ")) {
            List<String> parts = new ArrayList<>();
            for (String segment : trimmed.split(" \\| ")) {
                parts.addAll(wrapLongLoreLine(segment.trim()));
            }
            return parts;
        }
        List<String> wrapped = new ArrayList<>();
        String[] words = trimmed.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (current.isEmpty()) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxLen) {
                current.append(' ').append(word);
            } else {
                wrapped.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            wrapped.add(current.toString());
        }
        return wrapped.isEmpty() ? List.of(trimmed) : wrapped;
    }

    /**
     * Build item lore with a leading spacer so the first line is visible in-game.
     */
    public static List<Component> buildLore(List<String> lines) {
        return buildLoreFromExpanded(expandLoreLines(lines));
    }

    public static List<Component> buildLore(String... lines) {
        return buildLore(List.of(lines));
    }

    private static List<Component> buildLoreFromExpanded(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<Component> lore = new ArrayList<>(lines.size() + 1);
        lore.add(LORE_LEADING_SPACER);
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                lore.add(Component.empty());
            } else {
                lore.add(parse(line));
            }
        }
        return List.copyOf(lore);
    }

    /**
     * Prepend spacer to existing lore (e.g. when appending to an item that already has lore).
     */
    public static List<Component> withLeadingSpacer(List<Component> lore) {
        if (lore == null || lore.isEmpty()) {
            return List.of(LORE_LEADING_SPACER);
        }
        if (isLeadingSpacer(lore.getFirst())) {
            return lore;
        }
        List<Component> padded = new ArrayList<>(lore.size() + 1);
        padded.add(LORE_LEADING_SPACER);
        padded.addAll(lore);
        return List.copyOf(padded);
    }

    private static boolean isLeadingSpacer(Component line) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(line).trim();
        return plain.isEmpty();
    }

    /**
     * Tạo Component đơn giản với màu.
     */
    public static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Prefix cho tin nhắn plugin.
     */
    public static Component prefix() {
        return MINI_MESSAGE.deserialize("<gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <dark_gray>│ ");
    }

    /**
     * Tin nhắn với prefix.
     */
    public static Component prefixed(String message) {
        return prefix().append(MINI_MESSAGE.deserialize(message));
    }

    /**
     * Tin nhắn lỗi.
     */
    public static Component error(String message) {
        return prefix().append(Component.text(message, NamedTextColor.RED));
    }

    /**
     * Tin nhắn thành công.
     */
    public static Component success(String message) {
        return prefix().append(Component.text(message, NamedTextColor.GREEN));
    }

    /**
     * Tin nhắn thông tin.
     */
    public static Component info(String message) {
        return prefix().append(Component.text(message, NamedTextColor.GRAY));
    }

    /** Bo tag MiniMessage de tim kiem / so sanh text thuan. */
    public static String stripMiniMessageTags(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replaceAll("<[^>]+>", "").toLowerCase();
    }
}
