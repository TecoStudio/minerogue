package com.roguelike.mob.internal;

import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
