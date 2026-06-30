package com.roguelike.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultMobs {
    private DefaultMobs() {
    }

    public static int defaultExperience() {
        return 10;
    }

    public static boolean internalSystemEnabled() {
        return true;
    }

    public static double skeletonEliteSpawnChance() {
        return 0.12;
    }

    public static Map<String, Integer> experience() {
        Map<String, Integer> experience = new LinkedHashMap<>();
        experience.put("zombie", 15);
        experience.put("skeleton", 15);
        experience.put("creeper", 20);
        experience.put("spider", 12);
        experience.put("enderman", 25);
        experience.put("blaze", 22);
        experience.put("warden", 100);
        experience.put("ender_dragon", 500);
        experience.put("wither", 300);
        return experience;
    }

    public static Map<String, ConfigManager.MobConfig> modifiers() {
        Map<String, ConfigManager.MobConfig> modifiers = new LinkedHashMap<>();
        modifiers.put("zombie", new ConfigManager.MobConfig(1.5, 1.2, 1.0, "wooden_sword"));
        return modifiers;
    }
}
