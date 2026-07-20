package com.roguelike.mob.internal;

import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.config.ConfigManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptedInternalMobTest {
    @Test
    void weaponTemplateCombatUsesWeaponEffectsDirectly() {
        CustomWeapon template = new CustomWeapon("rusty_iron_sword", "生锈的铁剑", "", "minecraft:iron_sword",
                5.0, 1.6, 250, "common", Map.of(
                "poison_chance", 0.30,
                "poisoned_target_damage_percent", 0.10
        ));
        WeaponInstanceData data = new WeaponInstanceData("rusty_iron_sword");

        assertEquals(data.getTotalDamage(template), ScriptedInternalMob.weaponDamage(template, data, 1.0, false), 0.001);
        assertEquals(data.getTotalDamage(template) * 1.10, ScriptedInternalMob.weaponDamage(template, data, 1.0, true), 0.001);
        assertEquals(0.30, ScriptedInternalMob.weaponPoisonChance(template, data), 0.001);
    }

    @Test
    void chainedAfterConditionOnlyMatchesCompletedAction() {
        ConfigManager.ScriptedMobConfig config = new ConfigManager.ScriptedMobConfig(
                true, 0.0, "test", 20.0, 2.0, 1.0, 10.0, 3.0, 20L, 4.0, false);

        assertFalse(ScriptedInternalMob.conditionMatches("after melee-burst", 2.0, config, Set.of()));
        assertTrue(ScriptedInternalMob.conditionMatches("after melee-burst", 2.0, config, Set.of("melee-burst")));
        assertTrue(ScriptedInternalMob.conditionMatches("target_close", 2.0, config, Set.of()));
        assertFalse(ScriptedInternalMob.conditionMatches("target_close", 4.0, config, Set.of()));
    }
}
