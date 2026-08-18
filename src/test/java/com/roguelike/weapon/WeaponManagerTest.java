package com.roguelike.weapon;

import com.roguelike.item.CustomWeapon;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponManagerTest {
    @Test
    void specialWeaponKeepsOriginalDisplayAndDoesNotUseResourcePackModel() {
        CustomWeapon template = weapon("special_weapon", "special");

        assertFalse(WeaponManager.usesTemplateDisplay(template));
        assertFalse(WeaponManager.usesResourcePackModel(template));
    }

    @Test
    void vanillaMainHandDamageUsesMaterialDefaults() {
        assertEquals(4.0, WeaponManager.getVanillaMainHandDamage(org.bukkit.Material.WOODEN_SWORD), 0.001);
        assertEquals(8.0, WeaponManager.getVanillaMainHandDamage(org.bukkit.Material.NETHERITE_SWORD), 0.001);
    }

    @Test
    void yamlWeaponUsesTemplateDisplayAndResourcePackModel() {
        CustomWeapon template = weapon("flame_sword", "epic");

        assertTrue(WeaponManager.usesTemplateDisplay(template));
        assertTrue(WeaponManager.usesResourcePackModel(template));
    }

    private static CustomWeapon weapon(String id, String rarity) {
        return new CustomWeapon(id, id, "", "minecraft:wooden_sword", 4, 1.6, 59, rarity,
                Map.of("attack_range", 3.0), 0, false, "");
    }
}
