package com.roguelike.debug;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugEventSummaryTest {
    @Test
    void createsStructuredSummaryWithoutNullFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("player", "Alex");
        fields.put("world", null);
        fields.put("x", 12.5);

        DebugRecord record = DebugEventSummary.create("PlayerJoinEvent", "joined", fields);

        assertEquals("playerjoinevent", record.event());
        assertEquals("joined", record.message());
        assertEquals("Alex", record.fields().get("player"));
        assertEquals("12.5", record.fields().get("x"));
        assertFalse(record.fields().containsKey("world"));
        assertTrue(record.format().contains("player=Alex"));
    }

    @Test
    void nullInputsAreSafeAndSummaryExceptionsAreContained() {
        DebugRecord record = DebugEventSummary.create(null, null, null);

        assertEquals("unknown", record.event());
        assertEquals("", record.message());
        assertTrue(record.fields().isEmpty());
    }

    @Test
    void eventSummariesKeepOnlySafeScalarFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", "SUCCESSFULLY_LOADED");
        fields.put("block_count", 3);
        fields.put("cancelled", true);
        fields.put("player_uuid", "00000000-0000-0000-0000-000000000001");
        fields.put("location_x", 12.5);
        fields.put("item", new Object());

        DebugRecord record = DebugEventSummary.create(
                "EntityExplodeEvent", "entity explosion", fields);

        assertEquals("SUCCESSFULLY_LOADED", record.fields().get("status"));
        assertEquals("3", record.fields().get("block_count"));
        assertEquals("true", record.fields().get("cancelled"));
        assertEquals("12.5", record.fields().get("location_x"));
        assertFalse(record.fields().containsKey("item"));
    }

    @Test
    void eventListenerHelperSummariesIncludeCoordinatesAndCancellation() {
        Map<String, Object> resourcePack = DebugEventListener.resourcePackFields(
                null, org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.DECLINED);
        Map<String, Object> bucket = DebugEventListener.bucketFields(
                null, org.bukkit.Material.WATER, null);
        Map<String, Object> explosion = DebugEventListener.explosionFields(
                null, 4, true);
        Map<String, Object> blockExplosion = DebugEventListener.blockExplosionFields(
                null, 2, false);

        assertEquals("DECLINED", resourcePack.get("status"));
        assertEquals("WATER", bucket.get("bucket"));
        assertEquals(4, explosion.get("block_count"));
        assertEquals(true, explosion.get("cancelled"));
        assertEquals(2, blockExplosion.get("block_count"));
        assertEquals(false, blockExplosion.get("cancelled"));
        assertFalse(resourcePack.containsKey("player_uuid"));
        assertFalse(bucket.containsKey("location_x"));
    }
}
