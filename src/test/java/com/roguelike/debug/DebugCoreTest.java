package com.roguelike.debug;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugCoreTest {
    @Test
    void configUsesSafeDefaultsAndClampsInvalidValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("debug.enabled", true);
        yaml.set("debug.memory-records", -10);
        yaml.set("debug.file.enabled", true);
        yaml.set("debug.file.max-bytes", 1);
        yaml.set("debug.file.keep-files", 0);
        yaml.set("debug.flush-interval-ticks", 0);
        yaml.set("debug.sample-rate", 4.0);
        yaml.set("debug.min-duration-ms", -3.0);
        yaml.set("debug.exclude-events", List.of(" BlockBreak ", "", "combat"));

        DebugConfig config = DebugConfig.from(yaml);

        assertTrue(config.enabled());
        assertEquals(DebugConfig.DEFAULT_MEMORY_RECORDS, config.memoryRecords());
        assertEquals(DebugConfig.MIN_FILE_BYTES, config.file().maxBytes());
        assertEquals(1, config.file().keepFiles());
        assertEquals(1, config.flushIntervalTicks());
        assertEquals(1.0, config.sampleRate());
        assertEquals(0.0, config.minDurationMs());
        assertEquals(List.of("blockbreak", "combat"), List.copyOf(config.excludeEvents()));
    }

    @Test
    void defaultsKeepDebugAndFileLoggingDisabled() {
        DebugConfig config = DebugConfig.from(new YamlConfiguration());

        assertFalse(config.enabled());
        assertFalse(config.file().enabled());
        assertEquals(List.of("player_move"), config.excludeEvents());
    }

    @Test
    void explicitEmptyExcludeEventsEnablesPlayerMoveRecording() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("debug.enabled", true);
        yaml.set("debug.exclude-events", List.of());

        DebugConfig config = DebugConfig.from(yaml);

        assertTrue(config.enabled());
        assertFalse(config.file().enabled());
        assertTrue(config.excludeEvents().isEmpty());
    }

    @Test
    void configExcludesPlayerMoveByDefault() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("debug.enabled", true);

        DebugConfig config = DebugConfig.from(yaml);

        assertEquals(List.of("player_move"), config.excludeEvents());
    }

    @Test
    void bufferRetainsNewestRecordsAndReportsDroppedCount() {
        DebugBuffer buffer = new DebugBuffer(2);
        buffer.add(record("one"));
        buffer.add(record("two"));
        buffer.add(record("three"));

        assertEquals(2, buffer.size());
        assertEquals(List.of("two", "three"), buffer.tail(10).stream().map(DebugRecord::message).toList());
        assertEquals(1, buffer.droppedCount());
    }

    @Test
    void filterExcludesEventsAndRequiresMinimumDuration() {
        DebugConfig config = DebugConfig.defaults().withExcludeEvents(List.of("combat")).withMinDurationMs(10.0);
        DebugEventFilter filter = new DebugEventFilter(config);

        assertFalse(filter.accepts(record("ignored", "combat", 20)));
        assertFalse(filter.accepts(record("fast", "movement", 5)));
        assertTrue(filter.accepts(record("slow", "movement", 20)));
    }

    @Test
    void recordIsImmutableAndFormatsStructuredFields() {
        DebugRecord record = new DebugRecord(Instant.parse("2026-01-02T03:04:05Z"),
                DebugCategory.EVENT, "block_break", "player broke block", 12_500_000L,
                Map.of("player", "Alex", "count", "2"));

        assertEquals(12.5, record.durationMs(), 0.001);
        assertTrue(record.format().contains("EVENT"));
        assertTrue(record.format().contains("block_break"));
        assertTrue(record.format().contains("player=Alex"));
    }

    @Test
    void clearingBufferAlsoClearsDroppedCount() {
        DebugBuffer buffer = new DebugBuffer(1);
        buffer.add(record("one"));
        buffer.add(record("two"));

        buffer.clear();

        assertEquals(0, buffer.size());
        assertEquals(0, buffer.droppedCount());
    }

    @Test
    void formattedDurationUsesStableSnakeCaseFieldName() {
        DebugRecord record = record("slow", "trace", 12.5);

        assertTrue(record.format().contains("duration_ms=12.500"));
    }

    @Test
    void statsAggregateAcceptedFilteredAndDroppedRecords() {
        DebugStats stats = new DebugStats();
        stats.accept(record("a", "event_a", 2));
        stats.accept(record("b", "event_a", 8));
        stats.markFiltered();
        stats.markSampled();
        stats.markDropped();

        assertEquals(2, stats.accepted());
        assertEquals(1, stats.filtered());
        assertEquals(1, stats.sampled());
        assertEquals(1, stats.dropped());
        assertEquals(2, stats.eventCount("event_a"));
        assertTrue(stats.lines().stream().anyMatch(line -> line.contains("accepted=2")));
    }

    private static DebugRecord record(String message) {
        return record(message, "event", 0);
    }

    private static DebugRecord record(String message, String event, double durationMs) {
        return DebugRecord.of(DebugCategory.EVENT, event, message, (long) (durationMs * 1_000_000L));
    }
}
