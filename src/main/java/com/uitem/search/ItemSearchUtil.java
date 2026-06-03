package com.uitem.search;

import com.uitem.UItemData;
import com.uitem.util.TextUtil;

/**
 * Lọc item trong Item Library theo từ khóa.
 */
public final class ItemSearchUtil {

    private ItemSearchUtil() {
    }

    public static boolean matches(UItemData data, String query) {
        if (data == null || query == null || query.isBlank()) {
            return true;
        }
        String lower = query.toLowerCase().trim();
        if (data.getId().toLowerCase().contains(lower)) {
            return true;
        }
        if (data.getRarity() != null && data.getRarity().toLowerCase().contains(lower)) {
            return true;
        }
        if (data.getMaterial() != null && data.getMaterial().name().toLowerCase().replace('_', ' ').contains(lower)) {
            return true;
        }
        String plainName = TextUtil.stripMiniMessageTags(data.getDisplayName());
        if (plainName.contains(lower)) {
            return true;
        }
        if (data.getLore() != null) {
            for (String line : data.getLore()) {
                if (TextUtil.stripMiniMessageTags(line).contains(lower)) {
                    return true;
                }
            }
        }
        return false;
    }
}