package com.roguelike.boss;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public class BossEventStorage {
    private final File file;

    public BossEventStorage(File file) {
        this.file = file;
    }

    public BossEventState load(BossEventConfig config) {
        if (!file.exists()) return BossEventState.initial(config);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Instant next = parseInstant(yaml.getString("next-boss-spawn-at"), Instant.now());
        Instant last = parseInstant(yaml.getString("last-boss-spawn-at"), null);
        ActiveBossArena arena = readArena(yaml.getConfigurationSection("active-arena"));
        return new BossEventState(next, last, arena != null && arena.isActive() ? arena : null);
    }

    public void save(BossEventState state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-boss-spawn-at", format(state.nextBossSpawnAt()));
        yaml.set("last-boss-spawn-at", format(state.lastBossSpawnAt()));
        writeArena(yaml, "active-arena", state.activeArena());
        try {
            Files.createDirectories(file.toPath().getParent());
            yaml.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("无法保存 Boss 事件状态: " + e.getMessage(), e);
        }
    }

    static void writeArena(YamlConfiguration yaml, String path, ActiveBossArena arena) {
        if (arena == null) {
            yaml.set(path, null);
            return;
        }
        yaml.set(path + ".id", arena.id());
        yaml.set(path + ".world", arena.worldName());
        yaml.set(path + ".center.x", arena.centerX());
        yaml.set(path + ".center.y", arena.centerY());
        yaml.set(path + ".center.z", arena.centerZ());
        yaml.set(path + ".radius", arena.radius());
        yaml.set(path + ".boss-id", arena.bossMobId());
        yaml.set(path + ".boss-entity-uuid", arena.bossEntityUuid() == null ? null : arena.bossEntityUuid().toString());
        yaml.set(path + ".structure", arena.structureId());
        yaml.set(path + ".created-at", format(arena.createdAt()));
        yaml.set(path + ".expires-at", format(arena.expiresAt()));
        yaml.set(path + ".protected-while-active", arena.protectedWhileActive());
        yaml.set(path + ".state", arena.state().name());
    }

    static ActiveBossArena readArena(ConfigurationSection section) {
        if (section == null) return null;
        String id = section.getString("id", "boss-unknown");
        String world = section.getString("world", "world");
        int x = section.getInt("center.x");
        int y = section.getInt("center.y", 64);
        int z = section.getInt("center.z");
        int radius = section.getInt("radius", 32);
        String bossId = section.getString("boss-id", "blood-zombie");
        UUID uuid = parseUuid(section.getString("boss-entity-uuid"));
        String structure = section.getString("structure", "blood_altar");
        Instant created = parseInstant(section.getString("created-at"), Instant.now());
        Instant expires = parseInstant(section.getString("expires-at"), null);
        boolean protectedWhileActive = section.getBoolean("protected-while-active", true);
        ActiveBossArena.State state = ActiveBossArena.parseState(section.getString("state"));
        return new ActiveBossArena(id, world, x, y, z, radius, bossId, uuid, structure, created, expires, protectedWhileActive, state);
    }

    private static String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
