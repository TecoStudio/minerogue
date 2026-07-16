package com.roguelike.mob;

import com.roguelike.config.DefaultWeapons;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobManagerTest {
    @Test
    void modifierWeaponsUseTemplateMaterial() {
        assertEquals("IRON_AXE", modifierWeaponType("frost_cleaver"));
        assertEquals("TRIDENT", modifierWeaponType("storm_spear"));
        assertEquals("GOLDEN_SWORD", modifierWeaponType("plague_saber"));
        assertEquals("DIAMOND_SWORD", modifierWeaponType("echo_blade"));
        assertEquals("TRIDENT", MobManager.modifierWeaponMaterialName(DefaultWeapons.create().get("storm_spear")));
        assertEquals("IRON_SWORD", MobManager.modifierWeaponMaterialName(null));
    }

    private static String modifierWeaponType(String id) {
        return MobManager.modifierWeaponMaterialName(DefaultWeapons.create().get(id));
    }

    @Test
    void internalMobRosterIncludesElitesAndBosses() {
        List<String> ids = MobManager.defaultInternalMobIds();

        assertTrue(ids.contains("skeleton_elite"));
        assertTrue(ids.contains("zombie_elite"));
        assertTrue(ids.contains("spider_elite"));
        assertTrue(ids.contains("concierge_boss"));
        assertTrue(ids.contains("time_keeper_boss"));
    }

    @Test
    void internalMobNameplatesAreNotAlwaysVisibleThroughWalls() {
        assertFalse(MobManager.shouldForceInternalMobNameVisible());
    }
}
