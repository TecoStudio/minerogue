package com.roguelike.ticket;

import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketManagerTest {
    @Test
    void normalWeaponsCanDevelopPastFormerRandomAffixSlotLimit() {
        CustomWeapon template = weapon("wooden_sword", "common", 0, false);
        WeaponInstanceData data = new WeaponInstanceData("wooden_sword");
        data.setQuality("base");
        data.setEffectBonus("crit_chance", 0.10);

        assertTrue(TicketManager.canDevelopWeaponAffix(template, data));
    }

    @Test
    void specialOverflowWeaponsCanKeepDevelopingPastNormalSlotLimit() {
        CustomWeapon template = weapon("special_weapon", "special", 0, true);
        WeaponInstanceData data = new WeaponInstanceData("special_weapon");
        data.setQuality("base");
        data.setEffectBonus("crit_chance", 0.10);
        data.setEffectBonus("bleed_chance", 0.10);

        assertTrue(TicketManager.canDevelopWeaponAffix(template, data));
    }

    @Test
    void strengtheningSuccessRateDropsHarderAtHigherUseCounts() {
        assertEquals(1.00, TicketManager.calculateSuccessRate(0), 0.001);
        assertEquals(0.90, TicketManager.calculateSuccessRate(2), 0.001);
        assertEquals(0.50, TicketManager.calculateSuccessRate(5), 0.001);
        assertEquals(0.10, TicketManager.calculateSuccessRate(8), 0.001);
        assertEquals(0.05, TicketManager.calculateSuccessRate(9), 0.001);
        assertEquals(0.01, TicketManager.calculateSuccessRate(20), 0.001);
    }

    @Test
    void sixteenBestCaseBaseDamageStrengthensDoNotReachOneHundred() {
        double damage = 12.0;
        for (int i = 0; i < 15; i++) {
            damage *= 1.14;
        }
        assertTrue(damage < 100.0, "基础伤害至少需要 16 次最高倍率强化才可能达到 100");

        damage *= 1.14;
        assertTrue(damage >= 90.0, "16 次最高倍率强化后才接近或达到 100 伤害门槛");
    }

    @Test
    void commonStrengthenRangeIsDisplayedAsNerfedValues() {
        assertEquals("1.06x - 1.18x", TicketManager.formatStrengthenRangeForTesting("common"));
    }

    private static CustomWeapon weapon(String id, String rarity, int bonusSlots, boolean allowOverflow) {
        return new CustomWeapon(id, id, "", "minecraft:wooden_sword", 4, 1.6, 59, rarity,
                Map.of("attack_range", 3.0), bonusSlots, allowOverflow, "");
    }
}