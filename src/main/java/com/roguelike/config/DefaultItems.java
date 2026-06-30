package com.roguelike.config;

import com.roguelike.item.CustomItem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultItems {
    private DefaultItems() {
    }

    public static Map<String, CustomItem> create() {
        Map<String, CustomItem> items = new LinkedHashMap<>();
        Map<String, Double> effects = new LinkedHashMap<>();
        effects.put("heal_amount", 10.0);
        CustomItem potion = new CustomItem("healing_potion", "治疗药水", "恢复生命值", "potion", "common", effects);
        items.put(potion.getId().toLowerCase(), potion);
        return items;
    }
}
