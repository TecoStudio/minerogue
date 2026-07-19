package com.roguelike.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultArmor {
    private DefaultArmor() {
    }

    public static Map<String, ArmorDefinition> create() {
        return new LinkedHashMap<>();
    }
}
