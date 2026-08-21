package com.roguelike.debug;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public final class DebugStats {
    private final LongAdder accepted = new LongAdder();
    private final LongAdder filtered = new LongAdder();
    private final LongAdder sampled = new LongAdder();
    private final LongAdder dropped = new LongAdder();
    private final LongAdder totalDurationNanos = new LongAdder();
    private final Map<String, AtomicLong> eventCounts = new java.util.concurrent.ConcurrentHashMap<>();

    public void accept(DebugRecord record) {
        if (record == null) return;
        accepted.increment();
        totalDurationNanos.add(record.durationNanos());
        eventCounts.computeIfAbsent(record.event(), ignored -> new AtomicLong()).incrementAndGet();
    }

    public void markFiltered() { filtered.increment(); }
    public void markSampled() { sampled.increment(); }
    public void markDropped() { dropped.increment(); }
    public long accepted() { return accepted.sum(); }
    public long filtered() { return filtered.sum(); }
    public long sampled() { return sampled.sum(); }
    public long dropped() { return dropped.sum(); }
    public long eventCount(String event) { return eventCounts.getOrDefault(event, new AtomicLong()).get(); }
    public double averageDurationMs() {
        long count = accepted();
        return count == 0 ? 0.0 : totalDurationNanos.sum() / 1_000_000.0 / count;
    }
    public void clear() {
        accepted.reset();
        filtered.reset();
        sampled.reset();
        dropped.reset();
        totalDurationNanos.reset();
        eventCounts.clear();
    }
    public java.util.List<String> lines() {
        return java.util.List.of(
                "accepted=" + accepted() + " filtered=" + filtered() + " sampled=" + sampled() + " dropped=" + dropped(),
                "averageDurationMs=" + String.format(java.util.Locale.ROOT, "%.3f", averageDurationMs()),
                "events=" + eventCounts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")));
    }
}
