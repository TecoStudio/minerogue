package com.roguelike.item;

import java.util.HashMap;
import java.util.Map;

public class CustomItem {
    private final String id;
    private final String item;
    private final String name;
    private final String description;
    private final String itemType;
    private final String rarity;
    private final Map<String, Double> effects;

    public CustomItem(String id, String item, String name, String description, String itemType, String rarity, Map<String, Double> effects) {
        this.id = id;
        this.item = item;
        this.name = name;
        this.description = description;
        this.itemType = itemType;
        this.rarity = rarity;
        this.effects = effects != null ? new HashMap<>(effects) : new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getItemType() {
        return itemType;
    }

    public String getRarity() {
        return rarity;
    }

    public Map<String, Double> getEffects() {
        return new HashMap<>(effects);
    }

    public double getEffect(String key) {
        return effects.getOrDefault(key, 0.0);
    }

    public double getEffect(String key, double defaultValue) {
        return effects.getOrDefault(key, defaultValue);
    }
}
