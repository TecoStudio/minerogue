package com.roguelike.config;

import com.roguelike.item.CustomItem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultItems {
    private DefaultItems() {
    }

    public static Map<String, CustomItem> create() {
        return new LinkedHashMap<>();
    }
}
