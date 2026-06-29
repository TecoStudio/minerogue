package com.roguelike.util;

import com.roguelike.RoguelikePlugin;

public class DevLog {
    private static RoguelikePlugin plugin;

    public static void init(RoguelikePlugin plugin) {
        DevLog.plugin = plugin;
    }

    public static boolean isEnabled() {
        return plugin != null && plugin.getConfig().getBoolean("debug.enabled", false);
    }

    public static void info(String message) {
        if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }

    public static void debug(String message) {
        if (isEnabled()) {
            plugin.getLogger().info("[Debug] " + message);
        }
    }

    public static void warn(String message) {
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }
}
