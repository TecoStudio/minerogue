package com.roguelike.boss;

import org.bukkit.block.structure.StructureRotation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossLootPlannerTest {
    @Test
    void bossLootRollsConfiguredDropsWithoutBukkitEntityMocks() {
        BossEventConfig.DropConfig drops = new BossEventConfig.DropConfig(List.of(
                new BossEventConfig.DropItemDefinition("minecraft:diamond", null, null, 2, 1.0),
                new BossEventConfig.DropItemDefinition(null, "crimson_oath", null, 1, 0.0),
                new BossEventConfig.DropItemDefinition(null, null, "greater_healing_potion", 3, 1.0)
        ));

        var rolled = BossLootPlanner.rollDrops(drops, new Random(1));

        assertEquals(2, rolled.size());
        assertEquals("minecraft:diamond", rolled.get(0).material());
        assertEquals(2, rolled.get(0).amount());
        assertEquals("greater_healing_potion", rolled.get(1).itemTemplate());
        assertEquals(3, rolled.get(1).amount());
    }

    @Test
    void structureRotationAcceptsVanillaFriendlyNames() {
        assertEquals(StructureRotation.NONE, BossStructureService.rotation("none"));
        assertEquals(StructureRotation.CLOCKWISE_90, BossStructureService.rotation("clockwise_90"));
        assertEquals(StructureRotation.CLOCKWISE_180, BossStructureService.rotation("180"));
        assertEquals(StructureRotation.COUNTERCLOCKWISE_90, BossStructureService.rotation("counterclockwise_90"));
    }
}
