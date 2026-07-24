package com.roguelike.armor.affix;

import com.roguelike.weapon.affix.WeaponAffixManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorAffixManagerTest {
    @Test
    void dashMovedFromWeaponAffixesToArmorAffixes() {
        assertFalse(WeaponAffixManager.effectIds().contains("dash"));
        assertTrue(ArmorAffixManager.effectIds().contains("dash"));
        assertTrue(ArmorAffixManager.isApplicable("dash", Material.CHAINMAIL_CHESTPLATE));
        assertTrue(ArmorAffixManager.isApplicable("dash", Material.ELYTRA));
        assertFalse(WeaponAffixManager.rollableEffectIds().contains("dash"));
    }

    @Test
    void duplicateVanillaArmorAffixesAreNotRandomPluginAffixes() {
        assertFalse(ArmorAffixManager.effectIds().contains("damage_reduction"));
        assertTrue(ArmorAffixManager.effectIds().contains("thorns"));
        assertTrue(ArmorAffixManager.effectIds().contains("swift"));
        assertTrue(ArmorAffixManager.effectIds().contains("explosive"));
        assertFalse(ArmorAffixManager.effectIds().contains("projectile_protection"));
        assertFalse(ArmorAffixManager.effectIds().contains("fire_protection"));
        assertFalse(ArmorAffixManager.effectIds().contains("feather_falling"));
    }
}
