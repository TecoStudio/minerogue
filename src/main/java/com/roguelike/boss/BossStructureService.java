package com.roguelike.boss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;

public class BossStructureService {
    public Location generate(ActiveBossArena arena, World world) {
        return generate(arena, world, null, BossEventConfig.StructureDefinition.builtin(arena == null ? "blood_altar" : arena.structureId()));
    }

    public Location generate(ActiveBossArena arena, World world, File dataFolder, BossEventConfig.StructureDefinition structure) {
        if (arena == null || world == null) return null;
        BossEventConfig.StructureDefinition definition = structure == null
                ? BossEventConfig.StructureDefinition.builtin(arena.structureId())
                : structure;
        if ("vanilla".equalsIgnoreCase(definition.type()) || "nbt".equalsIgnoreCase(definition.type())) {
            placeVanillaStructure(arena, world, dataFolder, definition);
        } else if ("litematic".equalsIgnoreCase(definition.type()) || "projection".equalsIgnoreCase(definition.type())) {
            Logger.getLogger(BossStructureService.class.getName()).warning("Boss 结构 " + definition.file()
                    + " 是投影文件类型；当前版本不会直接粘贴 .litematic，请先导出/转换为原版 .nbt 结构文件。");
        } else if ("bone_ruins".equalsIgnoreCase(definition.id())) {
            generateBoneRuins(arena, world);
        } else {
            generateBloodAltar(arena, world);
        }
        return arena.centerLocation(world).add(0.0, 1.0, 0.0);
    }

    private void placeVanillaStructure(ActiveBossArena arena, World world, File dataFolder, BossEventConfig.StructureDefinition definition) {
        if (definition.file().isBlank()) return;
        File file = structureFile(dataFolder, definition.file());
        try {
            org.bukkit.structure.Structure structure = Bukkit.getStructureManager().loadStructure(file);
            BossEventConfig.Offset offset = definition.offset();
            Location origin = new Location(world, arena.centerX() + offset.x(), arena.centerY() + offset.y(), arena.centerZ() + offset.z());
            structure.place(origin, true, rotation(definition.rotation()), Mirror.NONE, 0, 1.0f, new Random());
        } catch (IOException | IllegalArgumentException e) {
            Logger.getLogger(BossStructureService.class.getName()).warning("加载 Boss 原版结构失败 " + file + ": " + e.getMessage());
        }
    }

    private File structureFile(File dataFolder, String configuredPath) {
        File file = new File(configuredPath);
        if (file.isAbsolute()) return file;
        File base = dataFolder == null ? new File(".") : dataFolder;
        return new File(base, configuredPath);
    }

    static StructureRotation rotation(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "90", "clockwise_90" -> StructureRotation.CLOCKWISE_90;
            case "180", "clockwise_180" -> StructureRotation.CLOCKWISE_180;
            case "270", "counterclockwise_90", "counter_clockwise_90" -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };
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
