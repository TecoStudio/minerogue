package com.roguelike.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossSpawnPlannerTest {
    @Test
    void distanceCheckRequiresAtLeastConfiguredMinimumBlocks() {
        assertFalse(BossSpawnPlanner.isHorizontalDistanceAtLeast(0.0, 0.0, 47.9, 0.0, 48));
        assertTrue(BossSpawnPlanner.isHorizontalDistanceAtLeast(0.0, 0.0, 48.0, 0.0, 48));
    }
}
