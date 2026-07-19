package com.roguelike.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BowAbilityManagerTest {
    @Test
    void chargeMultiplierRequiresFullDrawAndScalesWithLevel() {
        assertEquals(1.0, BowAbilityManager.chargeMultiplier(0.5f, 5), 0.001);
        assertEquals(1.0, BowAbilityManager.chargeMultiplier(1.0f, 0), 0.001);
        assertEquals(1.20, BowAbilityManager.chargeMultiplier(1.0f, 1), 0.001);
        assertEquals(2.00, BowAbilityManager.chargeMultiplier(1.0f, 5), 0.001);
    }
}
