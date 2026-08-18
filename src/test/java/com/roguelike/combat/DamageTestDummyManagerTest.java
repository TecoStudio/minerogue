package com.roguelike.combat;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageTestDummyManagerTest {
    @Test
    void recognizesWoolMaterialsAsTestDummyBase() {
        assertTrue(DamageTestDummyManager.isWool(Material.WHITE_WOOL));
        assertTrue(DamageTestDummyManager.isWool(Material.RED_WOOL));
        assertFalse(DamageTestDummyManager.isWool(Material.STONE));
    }
}
