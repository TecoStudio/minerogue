package com.roguelike.resourcepack;

import com.roguelike.RoguelikePlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Resolves the current resource-pack hash for join-time dispatch.
 *
 * <p>The nightly release rebuilds the pack only when content changes and always
 * publishes a {@code .sha1} asset next to the {@code .zip}. Rather than hand-maintaining
 * the hash in {@code config.yml}, the server fetches that {@code .sha1} from GitHub
 * asynchronously and caches it in memory. When {@code config.yml} provides an explicit
 * {@code hash} value, that fixed value wins and no network call is made.
 */
public final class ResourcePackManager {
    private static final Pattern SHA1 = Pattern.compile("[0-9a-fA-F]{40}");
    /** Refresh interval for the in-memory hash cache (20 ticks = 1 second). */
    private static final long REFRESH_PERIOD_TICKS = 20L * 60 * 30; // 30 minutes
    private static final long REFRESH_DELAY_TICKS = 20L * 10;       // 10 seconds after start

    private static RoguelikePlugin plugin;
    private static HttpClient client;
    private static BukkitTask refreshTask;
    /** Last successfully fetched hash; null until a fetch succeeds (or when using a fixed hash). */
    private static volatile String cachedHash;

    private ResourcePackManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        ResourcePackManager.plugin = plugin;
        // GitHub release assets 302-redirect to a CDN host, so redirects must be followed
        // or every fetch fails. connectTimeout bounds connection setup.
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        // Pre-fetch immediately (async) so the first joining player gets a hash;
        // the timer keeps it fresh afterward.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, ResourcePackManager::refreshHash);
        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, ResourcePackManager::refreshHash, REFRESH_DELAY_TICKS, REFRESH_PERIOD_TICKS);
    }

    /** Clears the cache and re-fetches; called on /rw reload so a changed url takes effect. */
    public static void reload() {
        cachedHash = null;
        if (plugin != null && client != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, ResourcePackManager::refreshHash);
        }
    }

    public static void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        client = null;
        cachedHash = null;
    }

    /**
     * Returns the hash to send with the resource pack. A non-blank {@code configHash}
     * is returned as-is (fixed override); otherwise the latest fetched GitHub hash is
     * returned (may be null if the fetch has not yet succeeded — the caller then omits
     * the hash so the client re-downloads).
     */
    public static String resolveHash(String configHash) {
        if (configHash != null && !configHash.isBlank()) return configHash.trim();
        return cachedHash;
    }

    /**
     * Reads {@code resource-pack.hash} from config and resolves the effective hash:
     * an explicit config value wins (no network); otherwise the cached GitHub hash
     * is used (may be null). The hash source ({@code url + ".sha1"}) is fetched
     * asynchronously by the refresh task, so this call never blocks the main thread.
     */
    public static String getHash() {
        if (plugin == null) return null;
        String configHash = plugin.getConfig().getString("resource-pack.hash", "");
        return resolveHash(configHash);
    }

    /** Test-only hook to inject a cached hash without network access. */
    static void setCachedHashForTest(String hash) {
        cachedHash = hash;
    }

    private static void refreshHash() {
        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank()) return;
        // An explicit hash in config means no fetch is needed.
        String configHash = plugin.getConfig().getString("resource-pack.hash", "");
        if (configHash != null && !configHash.isBlank()) return;
        String sha1Url = deriveSha1Url(url);
        if (sha1Url == null) return;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sha1Url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String hash = extractSha1(response.body());
                if (hash != null) {
                    cachedHash = hash;
                    plugin.getLogger().info("已从 GitHub 同步资源包 SHA1: " + hash);
                }
            } else {
                plugin.getLogger().warning("同步资源包 SHA1 失败: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("同步资源包 SHA1 失败: " + e.getMessage());
        }
    }

    /** Derives the {@code .sha1} asset URL from the pack {@code .zip} URL. */
    static String deriveSha1Url(String packUrl) {
        if (packUrl == null) return null;
        int query = packUrl.indexOf('?');
        String base = query >= 0 ? packUrl.substring(0, query) : packUrl;
        String suffix = query >= 0 ? packUrl.substring(query) : "";
        if (!base.toLowerCase().endsWith(".zip")) return null;
        return base + ".sha1" + suffix;
    }

    static String extractSha1(String body) {
        if (body == null) return null;
        var matcher = SHA1.matcher(body);
        return matcher.find() ? matcher.group().toLowerCase() : null;
    }
}
