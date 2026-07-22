package com.roguelike.boss;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class ActiveBossArena {
    public enum State {
        ACTIVE,
        COMPLETED,
        EXPIRED,
        CLEARED
    }

    private final String id;
    private final String worldName;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;
    private final String bossMobId;
    private UUID bossEntityUuid;
    private final String structureId;
    private final Instant createdAt;
    private Instant expiresAt;
    private final boolean protectedWhileActive;
    private State state;

    public ActiveBossArena(String id, String worldName, int centerX, int centerY, int centerZ, int radius,
                           String bossMobId, UUID bossEntityUuid, String structureId, Instant createdAt,
                           Instant expiresAt, boolean protectedWhileActive, State state) {
        this.id = id;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = Math.max(1, radius);
        this.bossMobId = bossMobId;
        this.bossEntityUuid = bossEntityUuid;
        this.structureId = structureId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.expiresAt = expiresAt;
        this.protectedWhileActive = protectedWhileActive;
        this.state = state == null ? State.ACTIVE : state;
    }

    public static ActiveBossArena active(String id, String worldName, int centerX, int centerY, int centerZ,
                                         int radius, String bossMobId, String structureId, boolean protectedWhileActive) {
        return new ActiveBossArena(id, worldName, centerX, centerY, centerZ, radius, bossMobId, null,
                structureId, Instant.now(), null, protectedWhileActive, State.ACTIVE);
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public boolean contains(Block block) {
        if (block == null || block.getWorld() == null) return false;
        return contains(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) return false;
        return contains(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean contains(String world, int x, int y, int z) {
        if (!worldName.equalsIgnoreCase(world)) return false;
        return Math.abs(x - centerX) <= radius
                && Math.abs(z - centerZ) <= radius
                && y >= centerY - 16
                && y <= centerY + 64;
    }

    public boolean overlaps(ActiveBossArena other) {
        if (other == null || !worldName.equalsIgnoreCase(other.worldName)) return false;
        int combined = radius + other.radius;
        long dx = (long) centerX - other.centerX;
        long dz = (long) centerZ - other.centerZ;
        return dx * dx + dz * dz <= (long) combined * combined;
    }

    public double horizontalDistanceSquared(Location location) {
        if (location == null || location.getWorld() == null || !worldName.equalsIgnoreCase(location.getWorld().getName())) {
            return Double.MAX_VALUE;
        }
        double dx = location.getX() - centerX;
        double dz = location.getZ() - centerZ;
        return dx * dx + dz * dz;
    }

    public Location centerLocation(World world) {
        return new Location(world, centerX + 0.5, centerY, centerZ + 0.5);
    }

    public String id() { return id; }
    public String worldName() { return worldName; }
    public int centerX() { return centerX; }
    public int centerY() { return centerY; }
    public int centerZ() { return centerZ; }
    public int radius() { return radius; }
    public String bossMobId() { return bossMobId; }
    public UUID bossEntityUuid() { return bossEntityUuid; }
    public String structureId() { return structureId; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public boolean protectedWhileActive() { return protectedWhileActive; }
    public State state() { return state; }

    public void setBossEntityUuid(UUID bossEntityUuid) { this.bossEntityUuid = bossEntityUuid; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setState(State state) { this.state = state == null ? State.ACTIVE : state; }

    public static State parseState(String value) {
        if (value == null || value.isBlank()) return State.ACTIVE;
        try {
            return State.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return State.ACTIVE;
        }
    }
}
