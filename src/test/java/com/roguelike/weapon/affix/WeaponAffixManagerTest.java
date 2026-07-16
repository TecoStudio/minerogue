package com.roguelike.weapon.affix;

import com.roguelike.config.DefaultWeapons;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponAffixManagerTest {
    @Test
    void deadCellsInspiredAffixesAreGroupedAndExposeSynergyHooks() {
        List<String> ids = WeaponAffixManager.effectIds();

        assertFalse(ids.contains("oil_chance"));
        assertFalse(ids.contains("oiled_target_fire_damage_percent"));
        assertTrue(ids.contains("bleed_chance"));
        assertTrue(ids.contains("bleeding_target_damage_percent"));
        assertTrue(ids.contains("victim_explosion_chance"));
        assertEquals("击杀触发", WeaponAffixManager.category("victim_explosion_chance"));
        assertTrue(WeaponAffixManager.synergyHint("bleeding_target_damage_percent").contains("流血"));
    }

    @Test
    void chanceAffixesGenerateWithinConfiguredRange() {
        Random random = new Random(1234);

        for (String id : List.of("bleed_chance", "victim_explosion_chance")) {
            double value = WeaponAffixManager.generateBaseValue(id, random);
            assertTrue(value > 0.0, id + " should generate a positive chance");
            assertTrue(value <= 0.30, id + " should stay in low-proc affix range");
            assertTrue(WeaponAffixManager.format(id, value).endsWith("%"));
        }
    }

    @Test
    void weaponScalingTagsDefaultToDeadCellsStyleColors() {
        CustomWeapon flameSword = DefaultWeapons.create().get("flame_sword");
        CustomWeapon frostCleaver = DefaultWeapons.create().get("frost_cleaver");
        CustomWeapon stormSpear = DefaultWeapons.create().get("storm_spear");
        WeaponInstanceData data = new WeaponInstanceData("test");

        assertTrue(WeaponAffixManager.scalingTags(flameSword, data).contains("暴虐"));
        assertTrue(WeaponAffixManager.scalingTags(frostCleaver, data).contains("生存"));
        assertTrue(WeaponAffixManager.scalingTags(stormSpear, data).contains("战术"));
        assertFalse(WeaponAffixManager.scalingTags(flameSword, data).isEmpty());
    }

    @Test
    void oilAffixesAreRemovedBecauseMinecraftHasNoClearOilState() {
        List<String> rollable = WeaponAffixManager.rollableEffectIds();

        assertFalse(rollable.contains("oil_chance"));
        assertFalse(rollable.contains("oiled_target_fire_damage_percent"));
        assertTrue(rollable.contains("bleed_chance"));
        assertTrue(rollable.contains("bleeding_target_damage_percent"));
        assertEquals("通用词条", WeaponAffixManager.category("oil_chance"));
    }
}
