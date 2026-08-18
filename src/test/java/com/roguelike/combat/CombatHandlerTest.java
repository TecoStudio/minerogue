package com.roguelike.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatHandlerTest {
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
}
