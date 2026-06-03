package com.uitem.util;

import java.util.regex.Pattern;

/**
 * Loc / nhan dien dong lore ky thuat (ID, material...) khong hien cho nguoi choi.
 */
public final class ItemDisplayUtil {

    private static final Pattern TECHNICAL_LORE = Pattern.compile(
            "^(id|material|cmd|click\\s|────────).*$",
            Pattern.CASE_INSENSITIVE);

    private ItemDisplayUtil() {
    }

    public static boolean isTechnicalLoreLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String plain = TextUtil.stripMiniMessageTags(line).trim();
        if (plain.isEmpty()) {
            return false;
        }
        if (TECHNICAL_LORE.matcher(plain).matches()) {
            return true;
        }
        return plain.regionMatches(true, 0, "id:", 0, 3)
                || plain.regionMatches(true, 0, "material:", 0, 9)
                || plain.regionMatches(true, 0, "cmd:", 0, 4);
    }

    public static String sanitizeLoreLineForPlayers(String line) {
        return isTechnicalLoreLine(line) ? null : line;
    }
}