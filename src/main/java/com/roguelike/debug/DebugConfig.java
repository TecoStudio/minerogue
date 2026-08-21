package com.roguelike.debug;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record DebugConfig(boolean enabled, int memoryRecords, File file, int flushIntervalTicks,
                          double sampleRate, double minDurationMs, List<String> excludeEvents) {
    public static final int DEFAULT_MEMORY_RECORDS = 1000;
    public static final int MIN_MEMORY_RECORDS = 10;
    public static final int MAX_MEMORY_RECORDS = 100_000;
    public static final long MIN_FILE_BYTES = 4_096L;
    public static final long MAX_FILE_BYTES = 100L * 1024 * 1024;

    public DebugConfig {
        memoryRecords = memoryRecords < MIN_MEMORY_RECORDS ? DEFAULT_MEMORY_RECORDS : Math.min(MAX_MEMORY_RECORDS, memoryRecords);
        flushIntervalTicks = Math.max(1, flushIntervalTicks);
        sampleRate = clamp(sampleRate, 0.0, 1.0);
        minDurationMs = Math.max(0.0, minDurationMs);
        excludeEvents = List.copyOf(excludeEvents == null ? List.of() : excludeEvents);
        file = file == null ? File.defaults() : file;
    }

    public static DebugConfig defaults() {
        return new DebugConfig(false, DEFAULT_MEMORY_RECORDS, File.defaults(), 20, 1.0, 0.0, List.of("player_move"));
    }

    public static DebugConfig from(FileConfiguration config) {
        return from((ConfigurationSection) config.getConfigurationSection("debug"));
    }

    public static DebugConfig from(ConfigurationSection section) {
        if (section == null) return defaults();
        ConfigurationSection file = section.getConfigurationSection("file");
        List<String> excludes = new ArrayList<>();
        boolean hasExcludeEvents = section.contains("exclude-events");
        for (String value : section.getStringList("exclude-events")) {
            if (value != null && !value.isBlank()) excludes.add(value.trim().toLowerCase(Locale.ROOT));
        }
        if (!hasExcludeEvents) excludes.add("player_move");
        return new DebugConfig(section.getBoolean("enabled", false),
                section.getInt("memory-records", DEFAULT_MEMORY_RECORDS),
                new File(file != null && file.getBoolean("enabled", false),
                        file == null ? "debug" : file.getString("directory", "debug"),
                        file == null ? 1_048_576L : file.getLong("max-bytes", 1_048_576L),
                        file == null ? 5 : file.getInt("keep-files", 5)),
                section.getInt("flush-interval-ticks", 20), section.getDouble("sample-rate", 1.0),
                section.getDouble("min-duration-ms", 0.0), excludes);
    }

    public DebugConfig withExcludeEvents(List<String> values) { return new DebugConfig(enabled, memoryRecords, file, flushIntervalTicks, sampleRate, minDurationMs, values); }
    public DebugConfig withMinDurationMs(double value) { return new DebugConfig(enabled, memoryRecords, file, flushIntervalTicks, sampleRate, value, excludeEvents); }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) { return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min; }

    public record File(boolean enabled, String directory, long maxBytes, int keepFiles) {
        public File {
            directory = directory == null || directory.isBlank() ? "debug" : directory.trim();
            maxBytes = Math.max(MIN_FILE_BYTES, Math.min(MAX_FILE_BYTES, maxBytes));
            keepFiles = Math.max(1, Math.min(100, keepFiles));
        }
        static File defaults() { return new File(false, "debug", 1_048_576L, 5); }
    }
}
