package com.roguelike.item;

import com.roguelike.config.DefaultItems;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemStackFactoryTest {
    @Test
    void potionItemsResolveToPotionMaterialInsteadOfPaper() {
        assertEquals("POTION", materialName("healing_potion"));
        assertEquals("POTION", materialName("greater_healing_potion"));
        assertEquals("POTION", materialName("swift_tonic"));
        assertEquals("POTION", materialName("iron_skin_tonic"));
    }

    @Test
    void effectLoreDescribesConfiguredPotionEffects() {
        List<String> healingLore = CustomItemStackFactory.effectLore(DefaultItems.create().get("greater_healing_potion"));
        List<String> speedLore = CustomItemStackFactory.effectLore(DefaultItems.create().get("swift_tonic"));
        List<String> resistanceLore = CustomItemStackFactory.effectLore(DefaultItems.create().get("iron_skin_tonic"));

        assertTrue(healingLore.contains("恢复生命: 20"));
        assertTrue(speedLore.contains("速度等级: 1"));
        assertTrue(speedLore.contains("持续时间: 12秒"));
        assertTrue(resistanceLore.contains("抗性等级: 1"));
        assertTrue(resistanceLore.contains("持续时间: 10秒"));
    }


    private static String materialName(String id) {
        return CustomItemStackFactory.materialName(DefaultItems.create().get(id));
    }
}
