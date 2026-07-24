package com.roguelike.config;

import java.util.List;

public record ArmorDefinition(
        String name,
        String description,
        String rarity,
        String set,
        String piece,
        String material,
        String affix,
        int affixLevel,
        List<String> lore,
        List<ArmorEnchantmentDefinition> enchantments
) {
    public ArmorDefinition(String name, String description, String rarity) {
        this(name, description, rarity, "", "", "", "", 0, List.of(), List.of());
    }

    public record ArmorEnchantmentDefinition(String id, int level) {
    }
}
