package com.roguelike.integration;

import com.roguelike.RoguelikePlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class IntegrationManager {
    private static RoguelikePlugin plugin;
    private static boolean placeholderApi;
    private static boolean mythicMobs;
    private static boolean nova;

    public static void init(RoguelikePlugin plugin) {
        IntegrationManager.plugin = plugin;
        placeholderApi = isEnabled("PlaceholderAPI") && configEnabled("placeholderapi");
        mythicMobs = isEnabled("MythicMobs") && configEnabled("mythicmobs");
        nova = isEnabled("Nova") && configEnabled("nova");

        logStatus("PlaceholderAPI", placeholderApi);
        logStatus("MythicMobs", mythicMobs);
        logStatus("Nova", nova);

        if (placeholderApi) {
            PlaceholderBridge.register(plugin);
        }
    }

    private static boolean isEnabled(String name) {
        Plugin dependency = Bukkit.getPluginManager().getPlugin(name);
        return dependency != null && dependency.isEnabled();
    }

    private static boolean configEnabled(String key) {
        return plugin.getConfig().getBoolean("integrations." + key + ".enabled", true);
    }

    private static void logStatus(String name, boolean enabled) {
        plugin.getLogger().info(name + " integration: " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isPlaceholderApiEnabled() {
        return placeholderApi;
    }

    public static boolean isMythicMobsEnabled() {
        return mythicMobs;
    }

    public static boolean isNovaEnabled() {
        return nova;
    }
}
