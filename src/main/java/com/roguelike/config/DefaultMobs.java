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

    public static ConfigManager.SkeletonEliteConfig skeletonElite() {
        return new ConfigManager.SkeletonEliteConfig(
                true,
                0.12,
                "&c骷髅精英",
                30.0,
                5.0,
                0.30,
                0.10,
                5.0,
                "rusty_iron_sword",
                18.0,
                8.0,
                3.2,
                35L,
                100L,
                1.9,
                0.22,
                0.75,
                0.65
        );
    }

    public static ConfigManager.ZombieEliteConfig zombieElite() {
        return new ConfigManager.ZombieEliteConfig(
                true,
                0.12,
                "&2僵尸精英",
                35.0,
                5.0,
                "excited_stone_sword"
        );
    }

    public static ConfigManager.SpiderEliteConfig spiderElite() {
        return new ConfigManager.SpiderEliteConfig(
                true,
                0.12,
                "&5精英蜘蛛",
                35.0,
                1.2,
                0.35,
                8.0,
                1
        );
    }

    public static ConfigManager.BossConfig conciergeBoss() {
        return new ConfigManager.BossConfig(
                true,
                "&4沸血僵尸",
                180.0,
                9.0,
                0.95,
                24.0,
                5.0,
                120L,
                8.0
        );
    }

    public static ConfigManager.BossConfig timeKeeperBoss() {
        return new ConfigManager.BossConfig(
                true,
                "&5流浪者",
                150.0,
                7.0,
                1.25,
                26.0,
                4.5,
                90L,
                6.0
        );
    }

    public static Map<String, Integer> experience() {
        Map<String, Integer> experience = new LinkedHashMap<>();
        experience.put("zombie", 15);
        experience.put("skeleton", 15);
        experience.put("creeper", 20);
        experience.put("spider", 12);
        experience.put("enderman", 30);
        experience.put("blaze", 22);
        experience.put("warden", 350);
        experience.put("ender_dragon", 500);
        experience.put("wither", 300);
        return experience;
    }

    public static Map<String, ConfigManager.MobConfig> modifiers() {
        Map<String, ConfigManager.MobConfig> modifiers = new LinkedHashMap<>();
        modifiers.put("husk", new ConfigManager.MobConfig(1.25, 1.10, 0.95, "frost_cleaver"));
        modifiers.put("drowned", new ConfigManager.MobConfig(1.10, 1.05, 1.05, "storm_spear"));
        modifiers.put("pillager", new ConfigManager.MobConfig(1.15, 1.15, 1.00, "echo_blade"));
        modifiers.put("zombified_piglin", new ConfigManager.MobConfig(1.20, 1.20, 1.05, "plague_saber"));
        return modifiers;
    }
}
