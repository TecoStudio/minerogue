package com.roguelike.mob;

import com.roguelike.config.DefaultWeapons;
import org.bukkit.GameMode;
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
        assertTrue(ids.contains("concierge"));
        assertTrue(ids.contains("time_keeper"));
        assertFalse(ids.contains("concierge_boss"));
        assertFalse(ids.contains("time_keeper_boss"));
    }

    @Test
    void legacyBossIdsRemainAcceptedAsAliases() {
        InternalMob concierge = new StubMob("concierge", List.of("concierge_boss"));
        InternalMob timeKeeper = new StubMob("time_keeper", List.of("time_keeper_boss"));

        assertTrue(MobManager.matchesId(concierge, "concierge"));
        assertTrue(MobManager.matchesId(concierge, "concierge_boss"));
        assertTrue(MobManager.matchesId(timeKeeper, "time_keeper"));
        assertTrue(MobManager.matchesId(timeKeeper, "time_keeper_boss"));
        assertFalse(MobManager.matchesId(concierge, "time_keeper"));
        assertFalse(MobManager.matchesId(null, "concierge"));
        assertFalse(MobManager.matchesId(concierge, null));
    }

    @Test
    void internalMobValueMatchingIgnoresMissingPersistentDataValue() {
        InternalMob concierge = new StubMob("concierge", List.of("concierge_boss"));

        assertFalse(MobManager.matchesInternalMobValue(concierge, null));
    }

    @Test
    void internalMobNameplatesAreNotAlwaysVisibleThroughWalls() {
        assertFalse(MobManager.shouldForceInternalMobNameVisible());
    }

    @Test
    void bossCombatIgnoresCreativeAndSpectatorPlayers() {
        assertTrue(MobManager.shouldBossAffectPlayer(GameMode.SURVIVAL, false));
        assertTrue(MobManager.shouldBossAffectPlayer(GameMode.ADVENTURE, false));
        assertFalse(MobManager.shouldBossAffectPlayer(GameMode.CREATIVE, false));
        assertFalse(MobManager.shouldBossAffectPlayer(GameMode.SPECTATOR, false));
        assertFalse(MobManager.shouldBossAffectPlayer(GameMode.SURVIVAL, true));
    }

    private record StubMob(String id, List<String> aliases) implements InternalMob {
        @Override
        public void onSpawn(org.bukkit.entity.LivingEntity entity) {
        }

        @Override
        public org.bukkit.entity.LivingEntity spawn(org.bukkit.Location location) {
            return null;
        }

        @Override
        public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        }

        @Override
        public void tick() {
        }

        @Override
        public boolean isMob(org.bukkit.entity.LivingEntity entity) {
            return false;
        }
    }
}
