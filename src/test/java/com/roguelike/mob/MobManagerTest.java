package com.roguelike.mob;

import com.roguelike.item.CustomWeapon;
import com.roguelike.config.ConfigManager;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        assertEquals("TRIDENT", MobManager.modifierWeaponMaterialName(weapon("storm_spear", "minecraft:trident")));
        assertEquals("IRON_SWORD", MobManager.modifierWeaponMaterialName(null));
    }

    private static String modifierWeaponType(String id) {
        return MobManager.modifierWeaponMaterialName(switch (id) {
            case "frost_cleaver" -> weapon(id, "minecraft:iron_axe");
            case "storm_spear" -> weapon(id, "minecraft:trident");
            case "plague_saber" -> weapon(id, "minecraft:golden_sword");
            case "echo_blade" -> weapon(id, "minecraft:diamond_sword");
            default -> null;
        });
    }

    private static CustomWeapon weapon(String id, String material) {
        return new CustomWeapon(id, id, "", material, 1.0, 1.0, 100, "common", Map.of());
    }

    @Test
    void internalMobRosterIsYamlDrivenAndEmptyBeforeContentLoad() {
        List<String> ids = MobManager.defaultInternalMobIds();

        assertEquals(ConfigManager.getInternalMobDefinitions().stream()
                .filter(ConfigManager.InternalMobDefinition::spawnable)
                .map(ConfigManager.InternalMobDefinition::id)
                .toList(), ids);
    }

    @Test
    void yamlAliasesRemainAccepted() {
        InternalMob mob = new StubMob("blood-zombie", List.of("blood_zombie"));

        assertTrue(MobManager.matchesId(mob, "blood-zombie"));
        assertTrue(MobManager.matchesId(mob, "blood_zombie"));
        assertFalse(MobManager.matchesId(mob, "vagrant"));
        assertFalse(MobManager.matchesId(null, "blood-zombie"));
        assertFalse(MobManager.matchesId(mob, null));
    }

    @Test
    void internalMobValueMatchingIgnoresMissingPersistentDataValue() {
        InternalMob mob = new StubMob("blood-zombie", List.of("blood_zombie"));

        assertFalse(MobManager.matchesInternalMobValue(mob, null));
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

    @Test
    void randomPluginWeaponDropsAreDisabledByDefault() {
        assertEquals(0.0, MobManager.defaultRandomWeaponDropMultiplier(), 0.001);
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
