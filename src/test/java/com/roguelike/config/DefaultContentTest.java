package com.roguelike.config;

import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultContentTest {
    private static final Set<String> EXISTING_WEAPON_IDS = Set.of(
            "wooden_sword",
            "flame_sword",
            "vampire_dagger",
            "thunder_axe",
            "whirlwind_blade",
            "inferno_greatsword",
            "phase_scythe",
            "special_weapon",
            "rusty_iron_sword",
            "excited_stone_sword"
    );

    private static final Set<String> EXPANSION_WEAPON_IDS = Set.of(
            "ember_knife",
            "frost_cleaver",
            "storm_spear",
            "plague_saber",
            "echo_blade",
            "glass_cannon_hammer"
    );

    @Test
    void defaultWeaponsAreEmptyShellUntilYamlContentIsLoaded() {
        Map<String, CustomWeapon> weapons = DefaultWeapons.create();

        assertTrue(weapons.isEmpty());
    }

    @Test
    void defaultItemsAreEmptyShellUntilYamlContentIsLoaded() {
        Map<String, CustomItem> items = DefaultItems.create();

        assertTrue(items.isEmpty());
    }

    @Test
    void yamlContentDirectoryLoadsOneDefinitionPerFile() throws IOException {
        Path root = Files.createTempDirectory("roguelike-content-test");
        try {
            Files.createDirectories(root.resolve("weapons"));
            Files.createDirectories(root.resolve("items"));
            Files.createDirectories(root.resolve("armor"));
            Files.createDirectories(root.resolve("mobs"));
            Files.writeString(root.resolve("weapons/test_blade.yml"), """
                    id: test_blade
                    item: minecraft:iron_sword
                    name: 测试剑
                    description: 来自外部 YAML
                    base-damage: 7
                    attack-speed: 1.7
                    durability: 250
                    rarity: rare
                    effects:
                      attack_range: 3.5
                    """);
            Files.writeString(root.resolve("items/test_potion.yml"), """
                    id: test_potion
                    item: minecraft:potion
                    name: 测试药水
                    description: 来自外部 YAML
                    item-type: potion
                    rarity: common
                    effects:
                      heal_amount: 12
                    """);
            Files.writeString(root.resolve("armor/thorns_helmet.yml"), """
                    id: thorns_helmet
                    name: 外部荆棘头盔
                    description: 来自外部 YAML
                    rarity: common
                    """);
            Files.writeString(root.resolve("mobs/skeleton-elite.yml"), """
                    type: internal
                    id: skeleton-elite
                    logic: skeleton-elite
                    aliases:
                      - skeleton_elite
                    enabled: true
                    spawn-chance: 0.25
                    name: '&c外部骷髅'
                    weapon-template: test_blade
                    combat-script: |
                      ranged-shot
                      disable melee-burst
                    """);
            Files.writeString(root.resolve("mobs/husk.yml"), """
                    type: modifier
                    id: husk
                    health-multiplier: 1.5
                    damage-multiplier: 1.2
                    speed-multiplier: 0.9
                    weapon-template: test_blade
                    """);
            Files.writeString(root.resolve("mobs/exp-zombie.yml"), """
                    type: experience
                    id: zombie
                    experience: 42
                    """);

            ConfigManager.loadContentDirectory(root.toFile());

            assertAll(
                    () -> assertEquals("测试剑", ConfigManager.getWeapon("test_blade").getName()),
                    () -> assertEquals(3.5, ConfigManager.getWeapon("test_blade").getAttackRange(), 0.001),
                    () -> assertEquals("测试药水", ConfigManager.getItem("test_potion").getName()),
                    () -> assertEquals(12.0, ConfigManager.getItem("test_potion").getEffect("heal_amount"), 0.001),
                    () -> assertEquals("外部荆棘头盔", ConfigManager.getArmorDefinitions().get("thorns_helmet").name()),
                    () -> assertEquals("skeleton-elite", ConfigManager.getInternalMobDefinitions().getFirst().id()),
                    () -> assertEquals("skeleton-elite", ConfigManager.getInternalMobDefinitions().getFirst().logic()),
                    () -> assertTrue(ConfigManager.getInternalMobDefinitions().getFirst().aliases().contains("skeleton_elite")),
                    () -> assertEquals(0.25, ConfigManager.getSkeletonEliteConfig().spawnChance(), 0.001),
                    () -> assertTrue(ConfigManager.getSkeletonEliteConfig().combatScript().contains("disable melee-burst")),
                    () -> assertEquals("test_blade", ConfigManager.getMobConfig("husk").weaponTemplate())
            );
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void defaultWeaponsUseDropEligibleRaritiesExceptSpecialTemplate() {
        Map<String, CustomWeapon> weapons = DefaultWeapons.create();
        Set<String> dropRarities = Set.of("common", "rare", "epic", "legendary");

        for (String id : EXPANSION_WEAPON_IDS) {
            CustomWeapon weapon = weapons.get(id);
            if (weapon == null) continue;
            String rarity = weapon.getRarity();
            assertTrue(dropRarities.contains(rarity), id + " has non-drop rarity " + rarity);
        }
    }

    @Test
    void specialWeaponCanBreakNormalAffixSlotLimit() {
        CustomWeapon special = DefaultWeapons.create().get("special_weapon");

        assertEquals(null, special);
    }

    @Test
    void defaultWeaponEffectsAreDefensiveCopies() {
        CustomWeapon weapon = DefaultWeapons.create().get("ember_knife");
        if (weapon == null) return;

        weapon.getEffects().put("attack_range", 99.0);

        assertEquals(2.7, weapon.getAttackRange(), 0.001);
    }

    @Test
    void defaultItemsKeepHealingPotionAndExposeDistinctExpansionItems() {
        Map<String, CustomItem> items = DefaultItems.create();
        Set<String> expansionItemIds = Set.of("greater_healing_potion", "swift_tonic", "iron_skin_tonic", "burger");

        assertTrue(items.isEmpty());
    }

    @Test
    void defaultItemEffectsAreDefensiveCopies() {
        CustomItem item = DefaultItems.create().get("greater_healing_potion");
        if (item == null) return;

        item.getEffects().put("heal_amount", 99.0);

        assertEquals(20.0, item.getEffect("heal_amount"), 0.001);
    }

    @Test
    void defaultMobModifiersExposeExistingSchemaOnly() {
        Map<String, ConfigManager.MobConfig> modifiers = DefaultMobs.modifiers();
        Map<String, CustomWeapon> weapons = DefaultWeapons.create();

        assertTrue(modifiers.isEmpty());
    }

    @Test
    void defaultMobsAreEmptyShellUntilYamlContentIsLoaded() {
        Map<String, Integer> experience = DefaultMobs.experience();

        assertTrue(experience.isEmpty());
        assertTrue(DefaultMobs.modifiers().isEmpty());
        assertEquals(0, DefaultMobs.defaultExperience());
    }

    @Test
    void defaultScriptedMobsAreDisabledUntilYamlContentIsLoaded() {
        assertEquals(false, DefaultMobs.scriptedMob().enabled());
    }

    private static void assertValidModifier(Map<String, ConfigManager.MobConfig> modifiers,
                                            Map<String, CustomWeapon> weapons,
                                            String id) {
        ConfigManager.MobConfig modifier = modifiers.get(id);
        assertNotNull(modifier, "missing mob modifier " + id);
        assertTrue(modifier.healthMultiplier() > 0.0, id + " health multiplier must be positive");
        assertTrue(modifier.damageMultiplier() > 0.0, id + " damage multiplier must be positive");
        assertTrue(modifier.speedMultiplier() > 0.0, id + " speed multiplier must be positive");
        assertTrue(modifier.weaponTemplate() == null || modifier.weaponTemplate().isBlank()
                || weapons.containsKey(modifier.weaponTemplate()), id + " references missing weapon " + modifier.weaponTemplate());
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            for (Path current : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(current);
            }
        }
    }
}
