package com.roguelike.boss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BossStructureService {
    public Location generate(ActiveBossArena arena, World world) {
        if (arena == null || world == null) return null;
        if ("bone_ruins".equalsIgnoreCase(arena.structureId())) {
            generateBoneRuins(arena, world);
        } else {
            generateBloodAltar(arena, world);
        }
        return arena.centerLocation(world).add(0.0, 1.0, 0.0);
    }

    private void generateBloodAltar(ActiveBossArena arena, World world) {
        fillPlatform(world, arena.centerX(), arena.centerY() - 1, arena.centerZ(), 5, Material.NETHER_BRICKS);
        fillPlatform(world, arena.centerX(), arena.centerY(), arena.centerZ(), 2, Material.CRIMSON_NYLIUM);
        setBlock(world, arena.centerX(), arena.centerY() + 1, arena.centerZ(), Material.REDSTONE_BLOCK);
        for (int[] offset : corners(4)) {
            pillar(world, arena.centerX() + offset[0], arena.centerY(), arena.centerZ() + offset[1], 4, Material.POLISHED_BLACKSTONE_BRICKS);
            setBlock(world, arena.centerX() + offset[0], arena.centerY() + 4, arena.centerZ() + offset[1], Material.SOUL_FIRE);
        }
        markBoundary(world, arena, Material.RED_STAINED_GLASS);
    }

    private void generateBoneRuins(ActiveBossArena arena, World world) {
        fillPlatform(world, arena.centerX(), arena.centerY() - 1, arena.centerZ(), 5, Material.CRACKED_STONE_BRICKS);
        setBlock(world, arena.centerX(), arena.centerY(), arena.centerZ(), Material.CHISELED_STONE_BRICKS);
        for (int[] offset : corners(5)) {
            pillar(world, arena.centerX() + offset[0], arena.centerY(), arena.centerZ() + offset[1], 3, Material.BONE_BLOCK);
            setBlock(world, arena.centerX() + offset[0], arena.centerY() + 3, arena.centerZ() + offset[1], Material.SOUL_LANTERN);
        }
        for (int dx = -3; dx <= 3; dx += 3) {
            setBlock(world, arena.centerX() + dx, arena.centerY(), arena.centerZ() + 2, Material.COBWEB);
        }
        markBoundary(world, arena, Material.WHITE_STAINED_GLASS);
    }

    private void fillPlatform(World world, int cx, int y, int cz, int half, Material material) {
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(world, x, y, z, material);
            }
        }
    }

    private void markBoundary(World world, ActiveBossArena arena, Material material) {
        int step = 8;
        for (int x = arena.centerX() - arena.radius(); x <= arena.centerX() + arena.radius(); x += step) {
            boundaryMarker(world, x, arena.centerZ() - arena.radius(), material);
            boundaryMarker(world, x, arena.centerZ() + arena.radius(), material);
        }
        for (int z = arena.centerZ() - arena.radius(); z <= arena.centerZ() + arena.radius(); z += step) {
            boundaryMarker(world, arena.centerX() - arena.radius(), z, material);
            boundaryMarker(world, arena.centerX() + arena.radius(), z, material);
        }
    }

    private void boundaryMarker(World world, int x, int z, Material material) {
        int y = world.getHighestBlockYAt(x, z) + 1;
        setBlock(world, x, y, z, material);
        setBlock(world, x, y + 1, z, Material.TORCH);
    }

    private void pillar(World world, int x, int y, int z, int height, Material material) {
        for (int i = 0; i < height; i++) setBlock(world, x, y + i, z, material);
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
    }

    private int[][] corners(int distance) {
        return new int[][]{{distance, distance}, {distance, -distance}, {-distance, distance}, {-distance, -distance}};
    }
}
