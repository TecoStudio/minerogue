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

    public static double skeletonEliteSpawnChance() {
        return 0.0;
    }

    public static ConfigManager.SkeletonEliteConfig skeletonElite() {
        return new ConfigManager.SkeletonEliteConfig(
                false, 0.0, "", 20.0, 0.0, 0.0, 0.0, 0.0, null,
                0.0, 0.0, 0.0, 20L, 20L, 1.0, 0.0, 0.0, 0.0, ""
        );
    }

    public static ConfigManager.ZombieEliteConfig zombieElite() {
        return new ConfigManager.ZombieEliteConfig(false, 0.0, "", 20.0, 0.0, null, "");
    }

    public static ConfigManager.SpiderEliteConfig spiderElite() {
        return new ConfigManager.SpiderEliteConfig(false, 0.0, "", 20.0, 1.0, 0.0, 0.0, 1, "");
    }

    public static ConfigManager.ScriptedMobConfig scriptedMob() {
        return new ConfigManager.ScriptedMobConfig(false, "", 1.0, 0.0, 1.0, 0.0, 0.0, 20L, 0.0);
    }

    public static Map<String, Integer> experience() {
        return new LinkedHashMap<>();
    }

    public static Map<String, ConfigManager.MobConfig> modifiers() {
        return new LinkedHashMap<>();
    }
}
