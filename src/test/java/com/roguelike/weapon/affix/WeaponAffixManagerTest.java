package com.roguelike.weapon.affix;

import com.roguelike.equipment.EquipmentKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponAffixManagerTest {
    @Test
    void removedBombAffixIsNotAvailable() {
        assertFalse(WeaponAffixManager.effectIds().contains("bomb"));
        assertFalse(WeaponAffixManager.rollableEffectIds().contains("bomb"));
    }
    @Test
    void chanceAffixesGenerateWithinConfiguredRange() {
        Random random = new Random(1234);

        for (String id : List.of("victim_explosion_chance")) {
            double value = WeaponAffixManager.generateBaseValue(id, random);
            assertTrue(value > 0.0, id + " should generate a positive chance");
            assertTrue(value <= 0.30, id + " should stay in low-proc affix range");
            assertTrue(WeaponAffixManager.format(id, value).endsWith("%"));
        }
    }


    @Test
    void oilAffixesAreRemovedBecauseMinecraftHasNoClearOilState() {
        List<String> rollable = WeaponAffixManager.rollableEffectIds();

        assertFalse(rollable.contains("oil_chance"));
        assertFalse(rollable.contains("oiled_target_fire_damage_percent"));
        assertFalse(rollable.contains("bleed_chance"));
        assertFalse(rollable.contains("bleeding_target_damage_percent"));
        assertTrue(rollable.contains("contract_damage_200"));
        assertEquals("通用词条", WeaponAffixManager.category("oil_chance"));
    }

    @Test
    void toolAndBowAffixesHaveSeparatePools() {
        assertTrue(WeaponAffixManager.toolOnlyEffectIds().contains("ore_highlight"));
        assertFalse(WeaponAffixManager.toolOnlyEffectIds().contains("crit_chance"));

        assertTrue(WeaponAffixManager.isApplicable("ore_highlight", EquipmentKind.TOOL));
        assertFalse(WeaponAffixManager.isApplicable("ore_highlight", EquipmentKind.WEAPON));
        assertTrue(WeaponAffixManager.isApplicable("crit_chance", EquipmentKind.TOOL));
        assertFalse(WeaponAffixManager.isApplicable("crit_chance", EquipmentKind.BOW));
        assertTrue(WeaponAffixManager.isApplicable("scatter_shot", EquipmentKind.BOW));
        assertFalse(WeaponAffixManager.isApplicable("scatter_shot", EquipmentKind.WEAPON));
    }

    @Test
    void crazyMinerIsPickaxeOnlyToolAffix() {
        assertTrue(WeaponAffixManager.toolOnlyEffectIds().contains("crazy_miner"));
        assertTrue(WeaponAffixManager.isApplicable("crazy_miner", org.bukkit.Material.DIAMOND_PICKAXE));
        assertFalse(WeaponAffixManager.isApplicable("crazy_miner", org.bukkit.Material.DIAMOND_AXE));
        assertFalse(WeaponAffixManager.isApplicable("crazy_miner", org.bukkit.Material.DIAMOND_SWORD));
        assertFalse(WeaponAffixManager.isApplicable("crazy_miner", org.bukkit.Material.BOW));
    }
    @Test
    void scatterShotUsesExplicitArrowCountsFromTwoToFive() {
        Random random = new Random(42);

        for (int i = 0; i < 20; i++) {
            double value = WeaponAffixManager.generateBaseValue("scatter_shot", random);
            assertTrue(value >= 2.0 && value <= 5.0, "scatter count must be 2-5, got " + value);
            assertEquals((int) value + "支", WeaponAffixManager.format("scatter_shot", value));
        }
    }

    @Test
    void momentumAndCritLifestealAreWeaponOnlyPercentageAffixes() {
        assertTrue(WeaponAffixManager.effectIds().contains("momentum"));
        assertTrue(WeaponAffixManager.effectIds().contains("crit_lifesteal_percent"));

        assertTrue(WeaponAffixManager.isApplicable("momentum", EquipmentKind.WEAPON));
        assertFalse(WeaponAffixManager.isApplicable("momentum", EquipmentKind.BOW));
        assertTrue(WeaponAffixManager.isApplicable("crit_lifesteal_percent", EquipmentKind.WEAPON));
        assertFalse(WeaponAffixManager.isApplicable("crit_lifesteal_percent", EquipmentKind.BOW));

        Random random = new Random(7);
        for (int i = 0; i < 20; i++) {
            double momentum = WeaponAffixManager.generateBaseValue("momentum", random);
            assertTrue(momentum >= 0.02 && momentum <= 0.05, "momentum must be 0.02-0.05, got " + momentum);
            assertTrue(WeaponAffixManager.format("momentum", momentum).endsWith("%"));
        }
        for (int i = 0; i < 20; i++) {
            double critLife = WeaponAffixManager.generateBaseValue("crit_lifesteal_percent", random);
            assertTrue(critLife >= 0.10 && critLife <= 0.25, "crit_lifesteal must be 0.10-0.25, got " + critLife);
            assertTrue(WeaponAffixManager.format("crit_lifesteal_percent", critLife).endsWith("%"));
        }
    }
}
