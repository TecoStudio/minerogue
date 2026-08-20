package com.roguelike.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatHandlerTest {
    @Test
    void damageActionBarShowsOnlyFinalDamage() {
        assertEquals("§7伤害: §f7.5", CombatHandler.formatDamageActionBar(7.5));
    }

    @Test
    void damageChatTextKeepsFormulaAndFinalDamage() {
        assertEquals("§7伤害: §a5.0 §8= §f5.0",
                CombatHandler.formatDamageChatText("§a5.0", 5.0));
    }

    @Test
    void protectedDummyProcessesVanillaWeaponAttacksForFormulaFeedback() {
        assertTrue(CombatHandler.shouldProcessAttack(false, true));
        assertFalse(CombatHandler.shouldProcessAttack(false, false));
    }
    @Test
    void pluginWeaponDamageReplacesOnePointEventDamage() {
        assertEquals(8.0, CombatHandler.resolveWeaponBaseDamage(1.0, 8.0, false), 0.001);
    }

    @Test
    void vanillaBonusPreservesOnlyDamageAboveWeaponValue() {
        assertEquals(2.0, CombatHandler.vanillaBonus(10.0, 8.0, false), 0.001);
        assertEquals(4.0, CombatHandler.vanillaBonus(8.0, 8.0, true), 0.001);
    }

    @Test
    void attackSpeedAtOrBelowTickCapKeepsDamageUnchanged() {
        assertEquals(1.0, CombatHandler.attackSpeedOverflowMultiplier(20.0), 0.001);
        assertEquals(1.0, CombatHandler.attackSpeedOverflowMultiplier(1.6), 0.001);
    }

    @Test
    void attackSpeedAboveTickCapConvertsOverflowToDamageProportionally() {
        assertEquals(1.5, CombatHandler.attackSpeedOverflowMultiplier(30.0), 0.001);
        assertEquals(2.0, CombatHandler.attackSpeedOverflowMultiplier(40.0), 0.001);
    }

    @Test
    void momentumResetsStacksAfterDecayWindow() {
        assertEquals(1, CombatHandler.resolveMomentumStacks(5, 3000));
    }

    @Test
    void momentumStacksIncrementWithinDecayWindow() {
        assertEquals(4, CombatHandler.resolveMomentumStacks(3, 500));
    }

    @Test
    void momentumCapsAtMaxStacks() {
        assertEquals(20, CombatHandler.resolveMomentumStacks(19, 500));
        assertEquals(20, CombatHandler.resolveMomentumStacks(25, 500));
    }
}
