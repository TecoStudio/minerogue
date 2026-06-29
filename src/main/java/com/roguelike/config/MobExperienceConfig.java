package com.roguelike.config;

import java.util.HashMap;
import java.util.Map;

public class MobExperienceConfig {
    private static final Map<String, Integer> mobExpMap = new HashMap<>();
    private static int defaultExp = 10;

    public static void setMobExp(String entityType, int exp) {
        mobExpMap.put(entityType, exp);
    }

    public static int getMobExp(String entityType) {
        return mobExpMap.getOrDefault(entityType, defaultExp);
    }

    public static int getDefaultExp() {
        return defaultExp;
    }

    public static void setDefaultExp(int exp) {
        defaultExp = exp;
    }

    public static Map<String, Integer> getAllMobExp() {
        return new HashMap<>(mobExpMap);
    }

    public static void clear() {
        mobExpMap.clear();
    }
}
