package com.roguelike.forge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ForgeRecipeManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultForgeRecipesExposeExpansionWeaponRecipes() throws IOException {
        File file = tempDir.resolve("forge-recipes.yml").toFile();

        ForgeRecipeManager.saveDefaultsForTest(file);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, String> recipes = Map.of(
                "ember_knife", "minecraft:stone_sword",
                "frost_cleaver", "minecraft:iron_axe",
                "storm_spear", "minecraft:trident",
                "plague_saber", "minecraft:golden_sword",
                "echo_blade", "minecraft:diamond_sword",
                "glass_cannon_hammer", "minecraft:netherite_axe"
        );

        assertAll(recipes.entrySet().stream()
                .map(entry -> () -> assertWeaponRecipe(config, entry.getKey(), entry.getValue())));
    }


    private static void assertWeaponRecipe(YamlConfiguration config, String id, String coreMaterial) {
        String path = "recipes." + id + ".";
        ConfigurationSection section = config.getConfigurationSection("recipes." + id);
        assertNotNull(section, "missing forge recipe " + id);
        assertEquals("weapon", config.getString(path + "result.type"));
        assertEquals(id, config.getString(path + "result.id"));
        assertEquals(1, config.getInt(path + "result.amount"));
        assertEquals(coreMaterial, config.getString(path + "ingredients.W"));
    }
}
