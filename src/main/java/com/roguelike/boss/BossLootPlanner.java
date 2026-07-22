package com.roguelike.boss;

import com.roguelike.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class BossLootPlanner {
    private BossLootPlanner() {
    }

    public static List<ConfigManager.DropItemDefinition> rollDrops(BossEventConfig.DropConfig drops, Random random) {
        List<ConfigManager.DropItemDefinition> result = new ArrayList<>();
        if (drops == null || random == null) return result;
        for (BossEventConfig.DropItemDefinition drop : drops.items()) {
            if (random.nextDouble() >= drop.chance()) continue;
            result.add(new ConfigManager.DropItemDefinition(
                    drop.material(), drop.weaponTemplate(), drop.itemTemplate(), drop.amount(), 1.0));
        }
        return result;
    }
}
