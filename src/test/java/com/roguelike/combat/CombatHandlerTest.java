package com.roguelike.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatHandlerTest {
    @Test
    void vanillaCriticalBonusIsPreservedEvenWhenEventDamageHasNoExtraSwordBonus() {
        assertEquals(4.0, CombatHandler.vanillaBonus(8.0, 8.0, true), 0.001);
    }

    @Test
    void vanillaBonusStillKeepsExistingEnchantOrCriticalDamageWhenLarger() {
        assertEquals(6.0, CombatHandler.vanillaBonus(14.0, 8.0, true), 0.001);
        assertEquals(2.0, CombatHandler.vanillaBonus(10.0, 8.0, false), 0.001);
    }

    @Test
    void vanillaCriticalBonusAddsToSmallExistingEnchantBonusWhenCriticalWasMissing() {
        assertEquals(5.0, CombatHandler.vanillaBonus(9.0, 8.0, true), 0.001);
    }
}