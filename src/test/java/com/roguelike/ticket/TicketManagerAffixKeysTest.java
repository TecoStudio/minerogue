package com.roguelike.ticket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketManagerAffixKeysTest {
    @Test
    void playerFacingEffectStatsExcludeDisabledOilAffixes() {
        assertFalse(TicketManager.getEffectStatKeys().contains("oil_chance"));
        assertFalse(TicketManager.getEffectStatKeys().contains("oiled_target_fire_damage_percent"));
        assertTrue(TicketManager.getEffectStatKeys().contains("bleed_chance"));
        assertTrue(TicketManager.getEffectStatKeys().contains("victim_explosion_chance"));
    }
}
