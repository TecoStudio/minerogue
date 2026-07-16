package com.roguelike.item;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponInstanceDataTest {
    @Test
    void newWeaponsDefaultToBaseQualityWithOneRandomAffixSlot() {
        WeaponInstanceData data = new WeaponInstanceData("wooden_sword");
        CustomWeapon template = weapon("wooden_sword", "common", 0, false);

        assertEquals(1, data.getGearLevel());
        assertEquals("base", data.getQuality());
        assertEquals(1, data.getQualityPowerBonus());
        assertEquals(1, data.getRandomAffixSlotLimit(template));
        assertTrue(data.hasOpenRandomAffixSlot(template));
    }

    @Test
    void qualityControlsRandomAffixSlotsAndGearPower() {
        WeaponInstanceData data = new WeaponInstanceData("flame_sword");
        CustomWeapon template = weapon("flame_sword", "epic", 0, false);

        data.setGearLevel(4);
        data.setQuality("plusplus");
        data.setEffectBonus("crit_chance", 0.12);
        data.setEffectBonus("bleed_chance", 0.10);

        assertEquals(8, data.getGearPower());
        assertEquals(3, data.getRandomAffixSlotLimit(template));
        assertEquals(2, data.getRandomAffixCount());
        assertTrue(data.hasOpenRandomAffixSlot(template));
    }

    @Test
    void specialWeaponsCanOverflowNormalSlotLimit() {
        WeaponInstanceData data = new WeaponInstanceData("special_weapon");
        CustomWeapon template = weapon("special_weapon", "special", 0, true);

        data.setQuality("base");
        data.setEffectBonus("crit_chance", 0.12);
        data.setEffectBonus("bleed_chance", 0.10);
        data.setEffectBonus("victim_explosion_chance", 0.10);

        assertEquals(1, data.getRandomAffixSlotLimit(template));
        assertEquals(3, data.getRandomAffixCount());
        assertTrue(data.hasOpenRandomAffixSlot(template));
        assertTrue(data.isOverflowingRandomAffixSlots(template));
    }

    @Test
    void bonusSlotsExtendNormalLimitWithoutFullOverflow() {
        WeaponInstanceData data = new WeaponInstanceData("echo_blade");
        CustomWeapon template = weapon("echo_blade", "epic", 2, false);

        data.setQuality("plus");
        data.setEffectBonus("crit_chance", 0.12);
        data.setEffectBonus("bleed_chance", 0.10);
        data.setEffectBonus("victim_explosion_chance", 0.10);
        data.setEffectBonus("chain_targets", 2.0);

        assertEquals(4, data.getRandomAffixSlotLimit(template));
        assertFalse(data.hasOpenRandomAffixSlot(template));
        assertFalse(data.isOverflowingRandomAffixSlots(template));
    }

    @Test
    void gearPowerScalesDamageWithoutChangingAttackSpeed() {
        WeaponInstanceData data = new WeaponInstanceData("flame_sword");
        CustomWeapon template = weapon("flame_sword", "epic", 0, false);

        data.setGearLevel(4);
        data.setQuality("plusplus");

        assertEquals(6.56, data.getScaledBaseDamage(template), 0.001);
        assertEquals(6.56, data.getTotalDamage(template), 0.001);
        assertEquals(1.6, data.getTotalAttackSpeed(template), 0.001);
    }

    private static CustomWeapon weapon(String id, String rarity, int bonusSlots, boolean allowOverflow) {
        return new CustomWeapon(id, id, "", "minecraft:wooden_sword", 4, 1.6, 59, rarity,
                Map.of("attack_range", 3.0), bonusSlots, allowOverflow, "");
    }
}