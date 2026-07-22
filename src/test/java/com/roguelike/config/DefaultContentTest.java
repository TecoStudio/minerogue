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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                    template: skeleton
                    aliases:
                      - skeleton_elite
                    enabled: true
                    spawn-chance: 0.25
                    name: '&c外部骷髅'
                    weapon-template: test_blade
                    equipment:
                      main-hand: minecraft:bow
                      off-hand-weapon-template: test_blade
                    drops:
                      held-item-chance: 0.25
                      items:
                        - weapon-template: test_blade
                          chance: 0.40
                          amount: 1
                    skill-range: 3.0
                    actions:
                      - when: target_close
                        do: melee-burst
                        hits: 3
                      - when: after melee-burst
                        do: retreat
                    """);
            Files.writeString(root.resolve("mobs/husk.yml"), """
                    type: modifier
                    id: husk
                    health-multiplier: 1.5
                    damage-multiplier: 1.2
                    speed-multiplier: 0.9
                    weapon-template: test_blade
                    equipment:
                      helmet: minecraft:iron_helmet
                      chestplate: minecraft:iron_chestplate
                      main-hand: minecraft:stone_axe
                      off-hand-weapon-template: test_blade
                      drop-chances:
                        helmet: 0.05
                        chestplate: 0.04
                        main-hand: 0.03
                        off-hand: 0.02
                    """);
            Files.writeString(root.resolve("mobs/exp-zombie.yml"), """
                    type: experience
                    id: zombie
                    experience: 42
                    """);

            ConfigManager.loadContentDirectory(root.toFile());

            ConfigManager.InternalMobDefinition skeleton = ConfigManager.getInternalMobDefinitions().stream()
                    .filter(definition -> definition.id().equals("skeleton-elite"))
                    .findFirst()
                    .orElseThrow();

            assertAll(
                    () -> assertEquals("测试剑", ConfigManager.getWeapon("test_blade").getName()),
                    () -> assertEquals(3.5, ConfigManager.getWeapon("test_blade").getAttackRange(), 0.001),
                    () -> assertEquals("测试药水", ConfigManager.getItem("test_potion").getName()),
                    () -> assertEquals(12.0, ConfigManager.getItem("test_potion").getEffect("heal_amount"), 0.001),
                    () -> assertEquals("外部荆棘头盔", ConfigManager.getArmorDefinitions().get("thorns_helmet").name()),
                    () -> assertEquals("skeleton-elite", skeleton.id()),
                    () -> assertEquals("skeleton", skeleton.template()),
                    () -> assertEquals("test_blade", skeleton.weaponTemplate()),
                    () -> assertEquals("minecraft:bow", skeleton.equipment().mainHand()),
                    () -> assertEquals("test_blade", skeleton.equipment().offHandWeaponTemplate()),
                    () -> assertEquals(0.25, skeleton.drops().heldItemChance(), 0.001),
                    () -> assertEquals("test_blade", skeleton.drops().items().getFirst().weaponTemplate()),
                    () -> assertEquals(0.40, skeleton.drops().items().getFirst().chance(), 0.001),
                    () -> assertTrue(skeleton.aliases().contains("skeleton_elite")),
                    () -> assertEquals("melee-burst", skeleton.actions().getFirst().action()),
                    () -> assertEquals(3, skeleton.actions().getFirst().hits()),
                    () -> assertEquals("test_blade", ConfigManager.getMobConfig("husk").weaponTemplate()),
                    () -> assertEquals("minecraft:iron_helmet", ConfigManager.getMobConfig("husk").equipment().helmet()),
                    () -> assertEquals("minecraft:stone_axe", ConfigManager.getMobConfig("husk").equipment().mainHand()),
                    () -> assertEquals("test_blade", ConfigManager.getMobConfig("husk").equipment().offHandWeaponTemplate()),
                    () -> assertEquals(0.05, ConfigManager.getMobConfig("husk").equipment().dropChances().helmet(), 0.001),
                    () -> assertEquals(0.02, ConfigManager.getMobConfig("husk").equipment().dropChances().offHand(), 0.001),
                    () -> assertEquals(0.0, ConfigManager.getMobConfig("husk").drops().heldItemChance(), 0.001)
            );
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void internalMobYamlDefaultsDoNotBleedAcrossFiles() throws IOException {
        Path root = Files.createTempDirectory("roguelike-mob-defaults-test");
        try {
            Files.createDirectories(root.resolve("mobs"));
            Files.writeString(root.resolve("mobs/a-spider-default-source.yml"), """
                    type: internal
                    id: a-spider-default-source
                    template: spider
                    enabled: true
                    spawn-chance: 0.45
                    name: '&5Source'
                    health: 44.0
                    damage: 6.0
                    speed-multiplier: 1.7
                    detect-range: 19.0
                    skill-range: 4.0
                    skill-cooldown-ticks: 80
                    skill-damage: 3.0
                    bossbar: true
                    actions:
                      - when: target_close
                        do: slow-on-hit
                    """);
            Files.writeString(root.resolve("mobs/z-boss-default-target.yml"), """
                    type: internal
                    id: z-boss-default-target
                    template: zombie
                    enabled: true
                    name: '&4Target'
                    health: 150.0
                    actions:
                      - when: target_far
                        do: leap
                    """);

            ConfigManager.loadContentDirectory(root.toFile());

            ConfigManager.ScriptedMobConfig source = ConfigManager.getScriptedMobConfig("a-spider-default-source");
            ConfigManager.ScriptedMobConfig target = ConfigManager.getScriptedMobConfig("z-boss-default-target");
            assertAll(
                    () -> assertEquals(0.45, source.spawnChance(), 0.001),
                    () -> assertTrue(source.bossBar()),
                    () -> assertEquals(0.0, target.spawnChance(), 0.001),
                    () -> assertEquals(0.0, target.damage(), 0.001),
                    () -> assertEquals(1.0, target.speedMultiplier(), 0.001),
                    () -> assertEquals(0.0, target.detectRange(), 0.001),
                    () -> assertEquals(0.0, target.skillRange(), 0.001),
                    () -> assertEquals(20L, target.skillCooldownTicks()),
                    () -> assertEquals(0.0, target.skillDamage(), 0.001),
                    () -> assertFalse(target.bossBar())
            );
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void bundledInternalMobYamlMakesEquipmentAndDropsExplicit() throws IOException {
        Path mobDirectory = Path.of("content", "mobs");

        try (var stream = Files.list(mobDirectory)) {
            for (Path file : stream.filter(path -> path.toString().endsWith(".yml")).toList()) {
                String yaml = Files.readString(file);
                if (!yaml.contains("type: internal")) continue;

                assertTrue(yaml.contains("equipment:"), file + " must explicitly configure equipment");
                assertTrue(yaml.contains("drops:"), file + " must explicitly configure drops");
                if (yaml.contains("template: spider")) {
                    assertTrue(yaml.contains("potion-effects:"), file + " must explicitly configure spider invisibility/effects");
                }
            }
        }
    }

    @Test
    void bundledSpiderEliteKeepsDamagingAttackEnabled() throws IOException {
        Path mobDirectory = Path.of("content", "mobs");

        ConfigManager.loadContentDirectory(mobDirectory.getParent().toFile());
        ConfigManager.ScriptedMobConfig spider = ConfigManager.getScriptedMobConfig("spider-elite");
        ConfigManager.InternalMobDefinition definition = ConfigManager.getInternalMobDefinitions().stream()
                .filter(mob -> mob.id().equals("spider-elite"))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertTrue(spider.damage() > 0.0, "spider-elite must not set attack damage to zero"),
                () -> assertEquals("slow-on-hit", definition.actions().getFirst().action())
        );
    }

    @Test
    void bundledZombieEliteUsesFullIronArmor() throws IOException {
        ConfigManager.loadContentDirectory(Path.of("content").toFile());
        ConfigManager.InternalMobDefinition zombie = ConfigManager.getInternalMobDefinitions().stream()
                .filter(mob -> mob.id().equals("zombie-elite"))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals("minecraft:iron_helmet", zombie.equipment().helmet()),
                () -> assertEquals("minecraft:iron_chestplate", zombie.equipment().chestplate()),
                () -> assertEquals("minecraft:iron_leggings", zombie.equipment().leggings()),
                () -> assertEquals("minecraft:iron_boots", zombie.equipment().boots()),
                () -> assertEquals("excited_stone_sword", zombie.equipment().mainHandWeaponTemplate())
        );
    }

    @Test
    void bundledEliteMobsUseOneInFiftySpawnChance() throws IOException {
        ConfigManager.loadContentDirectory(Path.of("content").toFile());

        assertAll(
                () -> assertEquals(0.02, ConfigManager.getScriptedMobConfig("skeleton-elite").spawnChance(), 0.001),
                () -> assertEquals(0.02, ConfigManager.getScriptedMobConfig("zombie-elite").spawnChance(), 0.001),
                () -> assertEquals(0.02, ConfigManager.getScriptedMobConfig("spider-elite").spawnChance(), 0.001)
        );
    }

    @Test
    void bundledMobYamlDoesNotDropPluginItemsByDefault() throws IOException {
        Path mobDirectory = Path.of("content", "mobs");

        try (var stream = Files.list(mobDirectory)) {
            for (Path file : stream.filter(path -> path.toString().endsWith(".yml")).toList()) {
                String drops = topLevelSection(Files.readString(file), "drops");
                if (drops.isBlank()) continue;

                assertFalse(drops.contains("weapon-template:"), file + " must opt out of plugin weapon drops by default");
                assertFalse(drops.contains("item-template:"), file + " must opt out of plugin item drops by default");
                assertFalse(drops.contains("held-item-chance: 0.05"), file + " must not drop plugin-capable held items by default");
                assertFalse(drops.contains("held-item-chance: 0.15"), file + " must not drop plugin-capable held items by default");
            }
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

    private static String topLevelSection(String yaml, String sectionName) {
        StringBuilder section = new StringBuilder();
        boolean inSection = false;
        for (String line : yaml.split("\\R")) {
            if (!inSection) {
                inSection = line.equals(sectionName + ":");
                continue;
            }
            if (!line.isBlank() && !Character.isWhitespace(line.charAt(0))) break;
            section.append(line).append('\n');
        }
        return section.toString();
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
