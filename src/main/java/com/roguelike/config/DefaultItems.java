package com.roguelike.config;

import com.roguelike.item.CustomItem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultItems {
    private DefaultItems() {
    }

    public static Map<String, CustomItem> create() {
        Map<String, CustomItem> items = new LinkedHashMap<>();
        add(items, item("healing_potion", "minecraft:potion", "治疗药水", "恢复生命值", "potion", "common",
                effects("heal_amount", 10.0)));
        add(items, item("greater_healing_potion", "minecraft:potion", "强效治疗药水", "恢复更多生命值", "potion", "rare",
                effects("heal_amount", 20.0)));
        add(items, item("swift_tonic", "minecraft:potion", "迅捷药剂", "可配置为短时间提升移动节奏的模板物品", "tonic", "rare",
                effects("speed_level", 1.0, "duration_seconds", 12.0)));
        add(items, item("iron_skin_tonic", "minecraft:potion", "铁肤药剂", "可配置为短时间提升生存能力的模板物品", "tonic", "epic",
                effects("resistance_level", 1.0, "duration_seconds", 10.0)));
        return items;
    }

    private static void add(Map<String, CustomItem> items, CustomItem item) {
        items.put(item.getId().toLowerCase(), item);
    }

    private static CustomItem item(String id, String item, String name, String description, String itemType,
                                   String rarity, Map<String, Double> effects) {
        return new CustomItem(id, item, name, description, itemType, rarity, effects);
    }

    private static Map<String, Double> effects(Object... values) {
        Map<String, Double> effects = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            effects.put((String) values[i], ((Number) values[i + 1]).doubleValue());
        }
        return effects;
    }
}
