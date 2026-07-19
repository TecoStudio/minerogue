package com.roguelike.item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemStackFactoryTest {
    @Test
    void potionItemsResolveToPotionMaterialInsteadOfPaper() {
        assertEquals("POTION", materialName("healing_potion"));
        assertEquals("POTION", materialName("greater_healing_potion"));
        assertEquals("POTION", materialName("swift_tonic"));
        assertEquals("POTION", materialName("iron_skin_tonic"));
        assertEquals("PLAYER_HEAD", materialName("burger"));
    }

    @Test
    void effectLoreDescribesConfiguredPotionEffects() {
        List<String> healingLore = CustomItemStackFactory.effectLore(item("greater_healing_potion", "minecraft:potion", "potion", Map.of("heal_amount", 20.0)));
        List<String> speedLore = CustomItemStackFactory.effectLore(item("swift_tonic", "minecraft:potion", "tonic", Map.of("speed_level", 1.0, "duration_seconds", 12.0)));
        List<String> resistanceLore = CustomItemStackFactory.effectLore(item("iron_skin_tonic", "minecraft:potion", "tonic", Map.of("resistance_level", 1.0, "duration_seconds", 10.0)));
        List<String> burgerLore = CustomItemStackFactory.effectLore(item("burger", "minecraft:player_head", "food", Map.of("heal_percent", 0.30, "full_saturation", 1.0)));

        assertTrue(healingLore.contains("恢复生命: 20"));
        assertTrue(speedLore.contains("速度等级: 1"));
        assertTrue(speedLore.contains("持续时间: 12秒"));
        assertTrue(resistanceLore.contains("抗性等级: 1"));
        assertTrue(resistanceLore.contains("持续时间: 10秒"));
        assertTrue(burgerLore.contains("恢复生命: 30%"));
        assertTrue(burgerLore.contains("补满饱食度"));
    }


    private static String materialName(String id) {
        return switch (id) {
            case "burger" -> CustomItemStackFactory.materialName(item(id, "minecraft:player_head", "food", Map.of()));
            case "swift_tonic", "iron_skin_tonic" -> CustomItemStackFactory.materialName(item(id, "minecraft:potion", "tonic", Map.of()));
            default -> CustomItemStackFactory.materialName(item(id, "minecraft:potion", "potion", Map.of()));
        };
    }

    private static CustomItem item(String id, String material, String type, Map<String, Double> effects) {
        return new CustomItem(id, material, id, "", type, "common", effects);
    }
}
