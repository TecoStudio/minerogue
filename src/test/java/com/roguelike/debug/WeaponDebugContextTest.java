package com.roguelike.debug;

import com.roguelike.item.WeaponInstanceData;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponDebugContextTest {
    @Test
    void capturesOnlySafeImmutableWeaponSummary() {
        WeaponInstanceData data = new WeaponInstanceData("flame_sword");
        data.setEffectBonus("crit_chance", 0.25);
        data.setEffectBonus("smash", 1.0);
        data.setStoredDamage(12.5);

        WeaponDebugContext context = WeaponDebugContext.from(Material.IRON_SWORD, data);

        assertEquals("flame_sword", context.baseWeaponId());
        assertEquals(data.getInstanceId(), context.instanceId());
        assertEquals("IRON_SWORD", context.material());
        assertEquals(List.of("crit_chance", "smash"), context.effectKeys());
        assertEquals(12.5, context.storedDamage(), 0.001);
        assertEquals(2, context.bonusCount());
        assertFalse(context.missing());
        assertFalse(context.invalid());
        assertNotSame(data.getEffectBonuses().keySet(), context.effectKeys());
    }

    @Test
    void missingAndInvalidContextsAreSafeAndDoNotExposePayload() {
        WeaponDebugContext missing = WeaponDebugContext.missing(Material.AIR);
        WeaponDebugContext invalid = WeaponDebugContext.invalid(Material.BOW, IllegalArgumentException.class);

        assertTrue(missing.missing());
        assertFalse(missing.invalid());
        assertEquals("AIR", missing.material());
        assertTrue(invalid.invalid());
        assertFalse(invalid.missing());
        assertEquals(IllegalArgumentException.class.getSimpleName(), invalid.errorType());
        assertFalse(invalid.toString().contains("{"));
    }
}
