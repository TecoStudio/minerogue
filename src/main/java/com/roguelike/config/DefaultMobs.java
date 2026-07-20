package com.roguelike.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultMobs {
    private DefaultMobs() {
    }

    public static int defaultExperience() {
        return 0;
    }

    public static boolean internalSystemEnabled() {
        return false;
    }

    public static ConfigManager.ScriptedMobConfig scriptedMob() {
        return new ConfigManager.ScriptedMobConfig(false, 0.0, "", 1.0, 0.0, 1.0, 0.0, 0.0, 20L, 0.0, false);
    }

    public static Map<String, Integer> experience() {
        return new LinkedHashMap<>();
    }

    public static Map<String, ConfigManager.MobConfig> modifiers() {
        return new LinkedHashMap<>();
    }
}
