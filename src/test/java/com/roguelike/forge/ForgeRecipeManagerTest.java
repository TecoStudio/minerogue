package com.roguelike.forge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeRecipeManagerTest {
    @Test
    void bundledRecipeSourceUsesTwoByTwoShapes() {
        File file = Path.of("content", "recipes", "forge-recipes.yml").toFile();
        assertTrue(file.isFile(), "missing content recipe source");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection recipes = config.getConfigurationSection("recipes");
        assertNotNull(recipes);
        assertFalse(recipes.getKeys(false).isEmpty());
        recipes.getKeys(false).forEach(id -> {
            List<String> shape = config.getStringList("recipes." + id + ".shape");
            assertEquals(2, shape.size(), id + " must have 2 shape rows");
            assertEquals(2, shape.get(0).length(), id + " first row must have 2 chars");
            assertEquals(2, shape.get(1).length(), id + " second row must have 2 chars");
        });
    }

    @Test
    void recipeDownloadCanBeControlledInConfig() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        assertTrue(config.getBoolean("content.github-sync.download-recipes"));
    }
}
