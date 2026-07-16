package com.roguelike.ticket;

import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketManagerTest {
    @Test
    void normalWeaponsCannotDevelopWhenRandomAffixSlotsAreFull() {
        CustomWeapon template = weapon("wooden_sword", "common", 0, false);
        WeaponInstanceData data = new WeaponInstanceData("wooden_sword");
        data.setQuality("base");
        data.setEffectBonus("crit_chance", 0.10);

        assertFalse(TicketManager.canDevelopWeaponAffix(template, data));
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

    private static CustomWeapon weapon(String id, String rarity, int bonusSlots, boolean allowOverflow) {
        return new CustomWeapon(id, id, "", "minecraft:wooden_sword", 4, 1.6, 59, rarity,
                Map.of("attack_range", 3.0), bonusSlots, allowOverflow, "");
    }
}