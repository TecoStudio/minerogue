package com.roguelike.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolAbilityManagerTest {
    @Test
    void crazyMinerReadyOnlyAfterCooldown() {
        long now = 10_000L;
        assertTrue(ToolAbilityManager.crazyMinerReady(now, now));
        assertTrue(ToolAbilityManager.crazyMinerReady(now, now - 1));
        assertFalse(ToolAbilityManager.crazyMinerReady(now, now + 1));
        assertFalse(ToolAbilityManager.crazyMinerReady(now, now + 30_000L));
    }

    @Test
    void crazyMinerCooldownLineShownWhileActiveAndHiddenWhenReady() {
        long now = 10_000L;
        assertEquals("§e疯狂矿工: §f30s", ToolAbilityManager.crazyMinerCooldownLine(now, now + 30_000L));
        assertEquals("§e疯狂矿工: §f1s", ToolAbilityManager.crazyMinerCooldownLine(now, now + 1));
        assertNull(ToolAbilityManager.crazyMinerCooldownLine(now, now));
        assertNull(ToolAbilityManager.crazyMinerCooldownLine(now, now - 1));
    }
}
