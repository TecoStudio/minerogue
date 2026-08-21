package com.roguelike.debug;

import java.util.concurrent.ThreadLocalRandom;

public final class DebugEventFilter {
    private final DebugConfig config;
    public DebugEventFilter(DebugConfig config) { this.config = config; }
    public boolean excluded(DebugRecord record) { return config.excludeEvents().contains(record.event()); }
    public boolean accepts(DebugRecord record) {
        return !excluded(record) && record.durationMs() >= config.minDurationMs()
                && (config.sampleRate() >= 1.0 || ThreadLocalRandom.current().nextDouble() < config.sampleRate());
    }
}
