package com.roguelike.weapon;

import org.junit.jupiter.api.Test;
import org.bukkit.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BowAbilityManagerTest {
    @Test
    void overchargeStartsAfterVanillaFullDrawAndScalesWithProgress() {
        assertEquals(0.0, BowAbilityManager.overchargeProgress(10), 0.001);
        assertEquals(0.0, BowAbilityManager.overchargeProgress(20), 0.001);
        assertEquals(0.5, BowAbilityManager.overchargeProgress(30), 0.001);
        assertEquals(1.0, BowAbilityManager.overchargeProgress(40), 0.001);

        assertEquals(1.0, BowAbilityManager.chargeMultiplier(0, 1.0), 0.001);
        assertEquals(1.0, BowAbilityManager.chargeMultiplier(5, 0.0), 0.001);
        assertEquals(1.10, BowAbilityManager.chargeMultiplier(1, 0.5), 0.001);
        assertEquals(2.00, BowAbilityManager.chargeMultiplier(5, 1.0), 0.001);
    }

    @Test
    void scatterConfiguredValueRepresentsTotalArrowCount() {
        assertEquals(0, BowAbilityManager.extraScatterProjectiles(0));
        assertEquals(1, BowAbilityManager.extraScatterProjectiles(2));
        assertEquals(4, BowAbilityManager.extraScatterProjectiles(5));
        assertEquals(4, BowAbilityManager.extraScatterProjectiles(9));
    }

    @Test
    void chargeMultiplierAppliesToProcessedBowDamage() {
        assertEquals(12.0, BowAbilityManager.applyChargeMultiplier(10.0, 1.2), 0.001);
        assertEquals(10.0, BowAbilityManager.applyChargeMultiplier(10.0, 1.0), 0.001);
    }

    @Test
    void scatterKeepsMainArrowMomentumMagnitude() {
        Vector source = new Vector(0.35, 0.12, 2.75);

        Vector first = BowAbilityManager.scatterVelocity(source, 0);
        Vector second = BowAbilityManager.scatterVelocity(source, 1);

        assertEquals(source.length(), first.length(), 0.001);
        assertEquals(source.length(), second.length(), 0.001);
    }
}
