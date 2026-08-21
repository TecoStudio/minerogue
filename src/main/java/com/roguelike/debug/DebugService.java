package com.roguelike.debug;

import com.roguelike.RoguelikePlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class DebugService {
    private final RoguelikePlugin plugin;
    private volatile DebugConfig config;
    private volatile DebugBuffer buffer;
    private volatile DebugEventFilter filter;
    private volatile DebugFileSink sink;
    private final DebugStats stats = new DebugStats();
    private final Object lifecycleLock = new Object();
    private volatile BukkitTask flushTask;
    private volatile boolean enabled;

    public DebugService(RoguelikePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = DebugConfig.defaults();
        this.buffer = new DebugBuffer(config.memoryRecords());
        this.filter = new DebugEventFilter(config);
    }

    public static DebugService init(RoguelikePlugin plugin) {
        DebugService service = new DebugService(plugin);
        service.reload();
        return service;
    }

    public synchronized void reload() {
        synchronized (lifecycleLock) {
            DebugConfig nextConfig = DebugConfig.from(plugin.getConfig());
            DebugBuffer previousBuffer = buffer;
            cancelFlushTask();
            DebugFileSink previousSink = sink;
            sink = null;
            if (previousSink != null) previousSink.close();

            config = nextConfig;
            enabled = nextConfig.enabled();
            buffer = migrate(previousBuffer, nextConfig.memoryRecords());
            filter = new DebugEventFilter(nextConfig);
            if (nextConfig.file().enabled()) {
                sink = new DebugFileSink(plugin.getDataFolder().toPath().resolve(nextConfig.file().directory()),
                        nextConfig.file().maxBytes(), nextConfig.file().keepFiles(), 4096);
                scheduleFlush(nextConfig.flushIntervalTicks());
            }
        }
    }

    public synchronized void shutdown() {
        synchronized (lifecycleLock) {
            cancelFlushTask();
            DebugFileSink current = sink;
            sink = null;
            if (current != null) current.close();
            enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        synchronized (lifecycleLock) {
            this.enabled = enabled;
        }
    }

    public void record(DebugRecord record) {
        if (record == null) return;
        synchronized (lifecycleLock) {
            if (!enabled) return;
            try {
                boolean force = record.category() == DebugCategory.ERROR;
                if (!force && filter.excluded(record)) {
                    stats.markFiltered();
                    return;
                }
                if (!force && config.sampleRate() < 1.0
                        && ThreadLocalRandom.current().nextDouble() >= config.sampleRate()) {
                    stats.markSampled();
                    return;
                }
                if (!force && record.durationMs() < config.minDurationMs()) {
                    stats.markFiltered();
                    return;
                }
                buffer.add(record);
                stats.accept(record);
                if (sink != null && !sink.offer(record.format())) stats.markDropped();
            } catch (RuntimeException ignored) {
                stats.markDropped();
            }
        }
    }

    public void event(String event, String message) {
        record(DebugRecord.of(DebugCategory.EVENT, event, message, 0L));
    }

    public void trace(String event, String message, long durationNanos) {
        record(DebugRecord.of(DebugCategory.TRACE, event, message, durationNanos));
    }

    public List<DebugRecord> tail(int count) { return buffer.tail(count); }

    public List<String> statsLines() { return stats.lines(); }

    public void clear() {
        buffer.clear();
        stats.clear();
    }

    public boolean flush() {
        synchronized (lifecycleLock) {
            DebugFileSink current = sink;
            return current == null || current.flush();
        }
    }

    public String status() {
        return "enabled=" + enabled
                + " records=" + buffer.size()
                + " buffer_dropped=" + buffer.droppedCount()
                + " accepted=" + stats.accepted()
                + " dropped=" + stats.dropped();
    }

    public DebugConfig config() { return config; }

    public DebugStats stats() { return stats; }

    private DebugBuffer migrate(DebugBuffer previous, int capacity) {
        DebugBuffer next = new DebugBuffer(capacity);
        if (previous == null) return next;
        for (DebugRecord record : previous.tail(capacity)) next.add(record);
        return next;
    }

    private void scheduleFlush(int intervalTicks) {
        try {
            flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushSafely,
                    intervalTicks, intervalTicks);
        } catch (RuntimeException ignored) {
            flushTask = null;
        }
    }

    private void flushSafely() {
        try {
            if (!flush()) stats.markDropped();
        } catch (RuntimeException ignored) {
            stats.markDropped();
        }
    }

    private void cancelFlushTask() {
        BukkitTask task = flushTask;
        flushTask = null;
        if (task != null) {
            try {
                task.cancel();
            } catch (RuntimeException ignored) {
                stats.markDropped();
            }
        }
    }
}
