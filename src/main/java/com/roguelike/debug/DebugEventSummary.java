package com.roguelike.debug;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds deliberately small, scalar-only summaries for plugin business events. */
public final class DebugEventSummary {
    private DebugEventSummary() {
    }

    public static DebugRecord create(String event, String message, Map<String, ?> values) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null) return;
                String safe = scalar(value);
                if (safe != null) fields.put(key, safe);
            });
        }
        return new DebugRecord(null, DebugCategory.EVENT, event == null ? "unknown" : event,
                message, 0L, fields);
    }

    public static void record(DebugService service, String event, String message, Map<String, ?> values) {
        if (service == null) return;
        try {
            service.record(create(event, message, values));
        } catch (RuntimeException ignored) {
            // Debug collection must never affect the original event.
        }
    }

    private static String scalar(Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Character) return value.toString();
        if (value instanceof Enum<?> enumValue) return enumValue.name();
        return null;
    }
}
