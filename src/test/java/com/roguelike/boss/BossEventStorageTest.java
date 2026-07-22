package com.roguelike.boss;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BossEventStorageTest {
    @Test
    void activeArenaRoundTripsThroughYaml() throws Exception {
        Path dir = Files.createTempDirectory("boss-events-state-test");
        BossEventStorage storage = new BossEventStorage(dir.resolve("boss-events-state.yml").toFile());
        UUID uuid = UUID.randomUUID();
        ActiveBossArena arena = new ActiveBossArena("boss-20260722-120000", "world", 1200, 72, -830, 32,
                "blood-zombie", uuid, "blood_altar", Instant.parse("2026-07-22T12:00:00Z"), null,
                true, ActiveBossArena.State.ACTIVE);
        BossEventState state = new BossEventState(Instant.parse("2026-07-24T12:00:00Z"),
                Instant.parse("2026-07-22T12:00:00Z"), arena);

        storage.save(state);
        BossEventState loaded = storage.load(BossEventConfig.defaults());

        assertEquals(Instant.parse("2026-07-24T12:00:00Z"), loaded.nextBossSpawnAt());
        assertNotNull(loaded.activeArena());
        assertEquals("boss-20260722-120000", loaded.activeArena().id());
        assertEquals("world", loaded.activeArena().worldName());
        assertEquals(1200, loaded.activeArena().centerX());
        assertEquals(-830, loaded.activeArena().centerZ());
        assertEquals(uuid, loaded.activeArena().bossEntityUuid());
        assertEquals("blood_altar", loaded.activeArena().structureId());
    }
}
