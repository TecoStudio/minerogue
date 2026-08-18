package com.roguelike.combat;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageTestDummyManagerTest {
    @Test
    void testDummyHealthIsSetToOneThousand() {
        assertEquals(1000.0, DamageTestDummyManager.TEST_DUMMY_HEALTH, 0.001);
    }

    @Test
    void recognizesWoolMaterialsAsTestDummyBase() {
        assertTrue(DamageTestDummyManager.isWool(Material.WHITE_WOOL));
        assertTrue(DamageTestDummyManager.isWool(Material.RED_WOOL));
        assertFalse(DamageTestDummyManager.isWool(Material.STONE));
    }
}
