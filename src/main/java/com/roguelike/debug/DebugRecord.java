package com.roguelike.debug;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public record DebugRecord(Instant timestamp, DebugCategory category, String event,
                          String message, long durationNanos, Map<String, String> fields) {
    public DebugRecord {
        timestamp = Objects.requireNonNullElse(timestamp, Instant.now());
        category = Objects.requireNonNullElse(category, DebugCategory.GENERAL);
        event = normalize(event, "general");
        message = Objects.requireNonNullElse(message, "");
        durationNanos = Math.max(0L, durationNanos);
        fields = Map.copyOf(fields == null ? Map.of() : new LinkedHashMap<>(fields));
    }

    public static DebugRecord of(DebugCategory category, String event, String message, long durationNanos) {
        return new DebugRecord(Instant.now(), category, event, message, durationNanos, Map.of());
    }

    public double durationMs() {
        return durationNanos / 1_000_000.0;
    }

    public String format() {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(timestamp.toString());
        joiner.add("[" + category + "]");
        joiner.add(event);
        if (durationNanos > 0) joiner.add("duration_ms=" + String.format(java.util.Locale.ROOT, "%.3f", durationMs()));
        if (!message.isBlank()) joiner.add(message.replace('\n', ' '));
        fields.forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
