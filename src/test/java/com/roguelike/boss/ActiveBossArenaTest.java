package com.roguelike.boss;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveBossArenaTest {
    @Test
    void squareCylinderContainsBlocksInSameWorldOnly() {
        ActiveBossArena arena = ActiveBossArena.active("boss", "world", 100, 70, -50, 32,
                "bloodlord", "blood-zombie", true);

        assertTrue(arena.contains("world", 132, 70, -18));
        assertFalse(arena.contains("world_nether", 100, 70, -50));
        assertFalse(arena.contains("world", 133, 70, -50));
        assertFalse(arena.contains("world", 100, 140, -50));
    }

    @Test
    void overlappingArenasUseHorizontalRadius() {
        ActiveBossArena first = ActiveBossArena.active("a", "world", 0, 64, 0, 32, "bloodlord", "blood-zombie", true);
        ActiveBossArena near = ActiveBossArena.active("b", "world", 60, 64, 0, 32, "vagrant", "vagrant", true);
        ActiveBossArena far = ActiveBossArena.active("c", "world", 70, 64, 0, 32, "vagrant", "vagrant", true);
        assertTrue(first.overlaps(near));
        assertFalse(first.overlaps(far));
    }

    @Test
    void centerChunkCoordinatesUseArenaCenter() {
        ActiveBossArena arena = ActiveBossArena.active("boss", "world", -17, 64, 31, 32,
                "bloodlord", "blood-zombie", true);

        assertEquals(-2, arena.centerChunkX());
        assertEquals(1, arena.centerChunkZ());
    }

    @Test
    void completedArenaIsNotActive() {
        ActiveBossArena arena = new ActiveBossArena("boss", "world", 0, 64, 0, 32,
                "bloodlord", "blood-zombie", UUID.randomUUID(), Instant.now(), null,
                true, ActiveBossArena.State.COMPLETED);

        assertFalse(arena.isActive());
    }
}
