package com.roguelike.config;

import com.roguelike.item.CustomWeapon;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultWeapons {
    private DefaultWeapons() {
    }

    public static Map<String, CustomWeapon> create() {
        return new LinkedHashMap<>();
    }
}
