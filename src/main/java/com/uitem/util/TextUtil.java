package com.uitem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Utility class for text and components.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
    }

    /**
     * Parse MiniMessage string into Component.
     */
    public static Component parse(String miniMessageStr) {
        if (miniMessageStr == null || miniMessageStr.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessageStr)
                .decoration(TextDecoration.ITALIC, false); // Disable default italic for lore
    }

    /**
     * Create a simple Component with color.
     */
    public static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Prefix for plugin messages.
     */
    public static Component prefix() {
        return MINI_MESSAGE.deserialize("<gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <dark_gray>│ ");
    }

    /**
     * Message with prefix.
     */
    public static Component prefixed(String message) {
        return prefix().append(MINI_MESSAGE.deserialize(message));
    }

    /**
     * Error message.
     */
    public static Component error(String message) {
        return prefix().append(Component.text(message, NamedTextColor.RED));
    }

    /**
     * Success message.
     */
    public static Component success(String message) {
        return prefix().append(Component.text(message, NamedTextColor.GREEN));
    }

    /**
     * Info message.
     */
    public static Component info(String message) {
        return prefix().append(Component.text(message, NamedTextColor.GRAY));
    }
}
