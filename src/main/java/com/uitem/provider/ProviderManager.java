package com.uitem.provider;

import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry for {@link ItemProvider}s. Replaces the legacy
 * IntegrationManager as the single source of "where does this ItemStack come
 * from".
 * <p>
 * Design rules:
 * <ul>
 *   <li>Providers register by namespace. The namespace is the prefix used in
 *       config strings like {@code ia:ruby_sword}.</li>
 *   <li>All 3rd-party plugins are optional — missing plugins simply mean the
 *       provider is not registered. No errors.</li>
 *   <li>Vanilla is always registered.</li>
 * </ul>
 */
public class ProviderManager {

    private final UItemPlugin plugin;
    private final Map<String, ItemProvider> providers = new LinkedHashMap<>();

    public ProviderManager(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Re-detect installed providers. Safe to call multiple times (e.g. after a softdepend loads). */
    public void detect() {
        providers.clear();
        // Vanilla always first
        register(new VanillaProvider());

        // Optional hooks — silently skipped if missing
        tryRegister("ItemsAdder", ItemsAdderProvider::new, "ItemsAdder");
        tryRegister("Oraxen", OraxenProvider::new, "Oraxen");
        // Future providers plug in here without touching callers (Open/Closed).
    }

    private void tryRegister(String pluginName, ProviderFactory factory, String displayNameForLog) {
        Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
        if (p == null || !p.isEnabled()) {
            plugin.getLogger().info("  ○ " + displayNameForLog + " — not detected");
            return;
        }
        try {
            ItemProvider provider = factory.create();
            register(provider);
            plugin.getLogger().info("  ✔ " + displayNameForLog + " v" + p.getPluginMeta().getVersion() + " — hooked");
        } catch (Throwable t) {
            plugin.getLogger().warning("  ○ " + displayNameForLog + " found but API hook failed: " + t.getMessage());
        }
    }

    /** Register (or replace) a provider. */
    public void register(ItemProvider provider) {
        if (provider == null) return;
        providers.put(provider.getNamespace().toLowerCase(Locale.ROOT), provider);
    }

    public ItemProvider getProvider(String namespace) {
        if (namespace == null) return null;
        return providers.get(namespace.toLowerCase(Locale.ROOT));
    }

    public Collection<ItemProvider> all() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /** Providers that are currently available AND browseable (e.g. for "pick source" GUI). */
    public List<ItemProvider> browseable() {
        List<ItemProvider> out = new ArrayList<>();
        for (ItemProvider p : providers.values()) {
            if (p.isAvailable()) out.add(p);
        }
        return out;
    }

    /** Parse {@code "namespace:id"} (or "namespace:namespace:id") into parts. */
    public static String[] split(String source) {
        if (source == null || source.isBlank()) return null;
        int idx = source.indexOf(':');
        if (idx < 0) return null;
        String ns = source.substring(0, idx).toLowerCase(Locale.ROOT);
        String id = source.substring(idx + 1);
        return new String[]{ns, id};
    }

    /** Resolve a {@code namespace:id} config string to an {@link Optional} ItemStack. */
    public Optional<ItemStack> build(String source) {
        String[] parts = split(source);
        if (parts == null) return Optional.empty();
        ItemProvider p = getProvider(parts[0]);
        if (p == null || !p.isAvailable()) {
            plugin.getLogger().warning("[UItem] Unknown or unavailable provider '" + parts[0]
                    + "' for source '" + source + "'");
            return Optional.empty();
        }
        return Optional.ofNullable(p.getItem(parts[1]));
    }

    /** True if the source can be resolved (namespace known + exists). */
    public boolean canResolve(String source) {
        String[] parts = split(source);
        if (parts == null) return false;
        ItemProvider p = getProvider(parts[0]);
        return p != null && p.isAvailable() && p.exists(parts[1]);
    }

    /**
     * Auto-detect the provider + id behind an ItemStack the admin is holding.
     * Iterates registered providers (highest priority first = registration
     * order preserved as insertion-ordered LinkedHashMap).
     *
     * @return "namespace:id" string (e.g. "ia:ruby_sword") or null if no provider recognized it.
     */
    public String detectSource(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        for (ItemProvider p : providers.values()) {
            if (!p.isAvailable()) continue;
            String id = null;
            if (p instanceof ItemsAdderProvider ia) {
                id = ia.extractLocalId(stack);
            } else if (p instanceof OraxenProvider oraxen) {
                id = oraxen.extractLocalId(stack);
            }
            if (id != null && p.exists(id)) {
                return p.getNamespace() + ":" + id;
            }
        }
        // Fall back to vanilla
        ItemProvider vanilla = getProvider(VanillaProvider.NAMESPACE);
        if (vanilla != null && vanilla.exists(stack.getType().name().toLowerCase(Locale.ROOT))) {
            return vanilla.getNamespace() + ":" + stack.getType().name().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    @FunctionalInterface
    private interface ProviderFactory {
        ItemProvider create() throws Exception;
    }
}
