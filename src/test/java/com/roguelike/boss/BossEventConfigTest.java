package com.roguelike.boss;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossEventConfigTest {
    @Test
    void defaultsMatchDocumentedFortyEightHourCycleAndThreeChunkMinimum() {
        BossEventConfig config = BossEventConfig.defaults();

        assertEquals(48.0, config.intervalHours(), 0.001);
        assertEquals(3, config.spawn().minDistanceChunks());
        assertEquals(48, config.spawn().minDistanceBlocks());
        assertTrue(config.enabled());
        assertEquals("world", config.worldName());
    }

    @Test
    void yamlParsesBossWeightsAndArenaProtection() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("boss-events.interval-hours", 0.1);
        yaml.set("boss-events.spawn.min-distance-chunks", 4);
        yaml.set("boss-events.arena.block-place", false);
        yaml.set("boss-events.bosses", java.util.List.of(java.util.Map.of("id", "vagrant", "weight", 7, "structure", "bone_ruins")));

        BossEventConfig config = BossEventConfig.fromYaml(yaml);

        assertEquals(0.1, config.intervalHours(), 0.001);
        assertEquals(64, config.spawn().minDistanceBlocks());
        assertFalse(config.arena().blockPlace());
        assertEquals("vagrant", config.bosses().getFirst().id());
        assertEquals(7, config.bosses().getFirst().weight());
        assertEquals("vagrant", config.bosses().getFirst().mobId());
        assertEquals("bone_ruins", config.bosses().getFirst().structureId());
    }

    @Test
    void yamlParsesBossMobStructureMapAndDropsFromSingleBossFile() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("boss-events.bosses", java.util.List.of(java.util.Map.of(
                "id", "bloodlord",
                "mob", "blood-zombie",
                "weight", 3,
                "structure", java.util.Map.of(
                        "type", "vanilla",
                        "file", "structures/boss/blood_altar.nbt",
                        "offset", java.util.Map.of("x", -8, "y", 1, "z", -8),
                        "rotation", "clockwise_90"
                ),
                "drops", java.util.Map.of("items", java.util.List.of(
                        java.util.Map.of("material", "minecraft:diamond", "amount", 2, "chance", 1.0),
                        java.util.Map.of("weapon-template", "crimson_oath", "amount", 1, "chance", 0.25),
                        java.util.Map.of("item-template", "greater_healing_potion", "amount", 3, "chance", 0.5)
                ))
        )));

        BossEventConfig.BossDefinition boss = BossEventConfig.fromYaml(yaml).bosses().getFirst();

        assertEquals("bloodlord", boss.id());
        assertEquals("blood-zombie", boss.mobId());
        assertEquals("vanilla", boss.structure().type());
        assertEquals("structures/boss/blood_altar.nbt", boss.structure().file());
        assertEquals(-8, boss.structure().offset().x());
        assertEquals(1, boss.structure().offset().y());
        assertEquals(-8, boss.structure().offset().z());
        assertEquals("clockwise_90", boss.structure().rotation());
        assertEquals(3, boss.drops().items().size());
        assertEquals("minecraft:diamond", boss.drops().items().getFirst().material());
        assertEquals(2, boss.drops().items().getFirst().amount());
        assertEquals(1.0, boss.drops().items().getFirst().chance(), 0.001);
    }

    @Test
    void defaultFileCanBeExportedAndReloaded() throws IOException {
        Path dir = Files.createTempDirectory("boss-events-config-test");
        Path file = dir.resolve("boss-events.yml");

        BossEventConfig.writeDefault(file.toFile());
        BossEventConfig config = BossEventConfig.load(file.toFile());

        assertTrue(Files.exists(file));
        assertEquals(2, config.bosses().size());
        assertEquals("blood-zombie", config.bosses().getFirst().id());
    }

    @Test
    void bossCommandUsesConfiguredBossPoolByDefault() {
        assertEquals(java.util.List.of("blood-zombie", "vagrant"), BossEventManager.configuredBossIds());
        assertTrue(BossEventManager.isConfiguredBossId("blood-zombie"));
        assertTrue(BossEventManager.isConfiguredBossId("vagrant"));
        assertFalse(BossEventManager.isConfiguredBossId("123"));
    }
}
