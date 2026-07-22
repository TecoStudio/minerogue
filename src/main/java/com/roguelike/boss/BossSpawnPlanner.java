package com.roguelike.boss;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class BossSpawnPlanner {
    private final Random random;

    public BossSpawnPlanner(Random random) {
        this.random = random == null ? new Random() : random;
    }

    public BossSpawnPlan plan(Collection<? extends Player> onlinePlayers, World world, BossEventConfig config, ActiveBossArena activeArena) {
        if (world == null || config == null) return null;
        List<Player> anchors = new ArrayList<>();
        for (Player player : onlinePlayers) {
            if (player.getWorld().equals(world)) anchors.add(player);
        }
        if (anchors.isEmpty()) {
            if (config.requireOnlinePlayer()) return null;
            return planAround(new Location(world, 0.5, world.getHighestBlockYAt(0, 0) + 1, 0.5), config, activeArena, null);
        }
        for (int i = 0; i < config.spawn().maxAttempts(); i++) {
            Player anchor = anchors.get(random.nextInt(anchors.size()));
            BossSpawnPlan plan = planAround(anchor.getLocation(), config, activeArena, anchor.getName());
            if (plan != null) return plan;
        }
        return null;
    }

    BossSpawnPlan planAround(Location anchor, BossEventConfig config, ActiveBossArena activeArena, String anchorName) {
        if (anchor == null || anchor.getWorld() == null) return null;
        World world = anchor.getWorld();
        int min = config.spawn().minDistanceBlocks();
        int max = Math.max(min, config.spawn().maxDistanceBlocks());
        int attempts = Math.max(1, config.spawn().maxAttempts());
        for (int i = 0; i < attempts; i++) {
            double distance = min + random.nextDouble() * (max - min);
            double angle = random.nextDouble() * Math.PI * 2.0;
            int x = anchor.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = anchor.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (distance2d(anchor.getX(), anchor.getZ(), x + 0.5, z + 0.5) < min) continue;
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (!isReasonableY(y)) continue;
            ActiveBossArena candidateArena = ActiveBossArena.active("candidate", world.getName(), x, y, z,
                    config.arena().radius(), "candidate", "candidate", true);
            if (activeArena != null && activeArena.isActive() && candidateArena.overlaps(activeArena)) continue;
            Location spawn = new Location(world, x + 0.5, y, z + 0.5);
            return new BossSpawnPlan(spawn, anchorName, distance2d(anchor.getX(), anchor.getZ(), spawn.getX(), spawn.getZ()));
        }
        return null;
    }

    static boolean isAtLeastMinDistance(Location anchor, Location spawn, int minDistanceBlocks) {
        if (anchor == null || spawn == null || anchor.getWorld() == null || spawn.getWorld() == null) return false;
        if (!anchor.getWorld().equals(spawn.getWorld())) return false;
        return isHorizontalDistanceAtLeast(anchor.getX(), anchor.getZ(), spawn.getX(), spawn.getZ(), minDistanceBlocks);
    }

    static boolean isHorizontalDistanceAtLeast(double anchorX, double anchorZ, double spawnX, double spawnZ, int minDistanceBlocks) {
        return distance2d(anchorX, anchorZ, spawnX, spawnZ) >= minDistanceBlocks;
    }

    private static boolean isReasonableY(int y) {
        return y > -64 && y < 320;
    }

    private static double distance2d(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public record BossSpawnPlan(Location spawnLocation, String anchorPlayerName, double distanceFromAnchor) {
    }
}
