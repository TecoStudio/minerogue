package com.roguelike.combat;

import org.bukkit.Location;
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
    void testDummyRegenerationUsesLevelFour() {
        assertEquals(3, DamageTestDummyManager.TEST_DUMMY_REGENERATION_AMPLIFIER);
    }
    @Test
    void recognizesWoolMaterialsAsTestDummyBase() {
        assertTrue(DamageTestDummyManager.isWool(Material.WHITE_WOOL));
        assertTrue(DamageTestDummyManager.isWool(Material.RED_WOOL));
        assertFalse(DamageTestDummyManager.isWool(Material.STONE));
    }


    @Test
    void protectionUsesCurrentWoolWhenAnchorIsMissing() {
        assertTrue(DamageTestDummyManager.isProtectionActive(false, false, true));
        assertFalse(DamageTestDummyManager.isProtectionActive(false, false, false));
        assertTrue(DamageTestDummyManager.isProtectionActive(true, true, false));
        assertFalse(DamageTestDummyManager.isProtectionActive(true, false, true));
    }

    @Test
    void calculatesReturnLocationAboveWoolAnchor() {
        Location wool = new Location(null, 10.0, 64.0, -4.0);

        Location returnLocation = DamageTestDummyManager.returnLocationAbove(wool);

        assertEquals(10.5, returnLocation.getX(), 0.001);
        assertEquals(65.0, returnLocation.getY(), 0.001);
        assertEquals(-3.5, returnLocation.getZ(), 0.001);
    }
}
