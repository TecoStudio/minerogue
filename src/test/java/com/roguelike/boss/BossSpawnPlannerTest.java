package com.roguelike.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossSpawnPlannerTest {
    @Test
    void distanceCheckRequiresAtLeastConfiguredMinimumBlocks() {
        assertFalse(BossSpawnPlanner.isHorizontalDistanceAtLeast(0.0, 0.0, 79.9, 0.0, 80));
        assertTrue(BossSpawnPlanner.isHorizontalDistanceAtLeast(0.0, 0.0, 80.0, 0.0, 80));
    }
}
