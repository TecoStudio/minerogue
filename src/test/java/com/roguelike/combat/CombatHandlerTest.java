package com.roguelike.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatHandlerTest {
    @Test
    void damageFormulaChatTextKeepsDetailedParts() {
        assertEquals("§7伤害: §a5.0 §8= §f5.0",
                CombatHandler.formatDamageChatText("§a5.0", 5.0));
    }

    @Test
    void damageActionBarShowsFormulaFinalDamageAndExtraTriggers() {
        assertEquals("§7伤害: §a5.0 &8x §b1.50 §8= §f7.5 §8| §e流血触发",
                CombatHandler.formatDamageActionBar("§a5.0 &8x §b1.50", 7.5, java.util.List.of("流血触发")));
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
}
