package com.uitem.chat;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages capturing chat input from a player.
 * When a player needs to enter text (e.g. an item name, material...),
 * register a callback and intercept that player's next chat message.
 */
public class ChatInputManager implements Listener {

    private final UItemPlugin plugin;
    private final Map<UUID, ChatInputRequest> pendingInputs;

    public ChatInputManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.pendingInputs = new ConcurrentHashMap<>();
    }

    /**
     * Request a player to enter text via chat.
     *
     * @param player   the player who must enter input
     * @param prompt   the instruction message
     * @param callback the callback receiving the entered text (runs on the main thread)
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        requestInput(player, prompt, callback, null);
    }

    /**
     * Request a player to enter text via chat, with a cancel callback.
     *
     * @param player   the player who must enter input
     * @param prompt   the instruction message
     * @param callback the callback receiving the entered text (runs on the main thread)
     * @param onCancel runs when the user types 'cancel' (usually reopens the GUI)
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback, Runnable onCancel) {
        // Default cancel action: reopen whatever UItem GUI was open when the
        // prompt started, so 'cancel' always takes the player back instead of
        // leaving them staring at chat.
        Runnable cancelAction = onCancel;
        if (cancelAction == null) {
            var holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof com.uitem.gui.AbstractGui gui) {
                cancelAction = gui::reopen;
            }
        }
        pendingInputs.put(player.getUniqueId(), new ChatInputRequest(callback, cancelAction));

        player.closeInventory();
        player.sendMessage(TextUtil.prefixed(prompt));
        player.sendMessage(TextUtil.info("Type 'cancel' to abort."));
    }

    /**
     * Check whether a player has a pending input.
     */
    public boolean hasPendingInput(UUID uuid) {
        return pendingInputs.containsKey(uuid);
    }

    /**
     * Cancel a pending input.
     */
    public void cancelInput(UUID uuid) {
        pendingInputs.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatInputRequest request = pendingInputs.remove(player.getUniqueId());

        if (request == null) return;

        // Block the chat message
        event.setCancelled(true);

        // Get the plain text
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Run on the player's entity scheduler — on Folia, opening an inventory
        // from the global region thread throws.
        SchedulerUtil.runForEntity(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(TextUtil.info("Cancelled."));
                if (request.onCancel() != null) {
                    request.onCancel().run();
                }
                return;
            }
            request.callback().accept(input);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
        // Clean up SearchManager query for this player (null-safe)
        var inst = com.uitem.UItemPlugin.getInstance();
        if (inst != null) {
            inst.getSearchManager().clearQuery(event.getPlayer());
        }
    }

    private record ChatInputRequest(Consumer<String> callback, Runnable onCancel) {
        ChatInputRequest(Consumer<String> callback) {
            this(callback, null);
        }
    }
}
