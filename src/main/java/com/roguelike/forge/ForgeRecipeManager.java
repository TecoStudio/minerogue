package com.roguelike.forge;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.ArmorSetManager;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ForgeRecipeManager {
    private static final List<ForgeRecipe> RECIPES = new ArrayList<>();
    private static File recipesFile;

    private ForgeRecipeManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        recipesFile = new File(plugin.getDataFolder(), "content/recipes/forge-recipes.yml");
        reload();
    }

    public static void reload() {
        RECIPES.clear();
        if (!recipesFile.isFile()) {
            logRecipeCount();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null) {
            logRecipeCount();
            return;
        }

        for (String id : section.getKeys(false)) {
            ForgeRecipe recipe = parseRecipe(id, section.getConfigurationSection(id));
            if (recipe != null) RECIPES.add(recipe);
        }
        logRecipeCount();
    }

    private static void logRecipeCount() {
        RoguelikePlugin.getInstance().getLogger().info("加载了 " + RECIPES.size() + " 份配方。");
    }

    public static ForgeRecipe match(Inventory inventory, int[] inputSlots) {
        for (ForgeRecipe recipe : RECIPES) {
            if (recipe.matches(inventory, inputSlots)) return recipe;
        }
        return null;
    }

    public static int count() {
        return RECIPES.size();
    }

    private static ForgeRecipe parseRecipe(String id, ConfigurationSection section) {
        if (section == null) return null;
        List<String> shape = section.getStringList("shape");
        if (shape.size() == 3) {
            RoguelikePlugin.getInstance().getLogger().warning("配方 " + id + " 使用旧版 3x3 格式，已跳过。请将 GitHub 内容源中的 content/recipes/forge-recipes.yml 改为 2x2 格式后执行 /rw reload。");
            return null;
        }
        if (shape.size() != 2) return null;
        String[] normalizedShape = new String[2];
        for (int i = 0; i < 2; i++) {
            normalizedShape[i] = normalizeShapeLine(shape.get(i));
        }

        Map<Character, Material> ingredients = new LinkedHashMap<>();
        ConfigurationSection ingredientSection = section.getConfigurationSection("ingredients");
        if (ingredientSection != null) {
            for (String key : ingredientSection.getKeys(false)) {
                if (key.isEmpty()) continue;
                Material material = parseMaterial(ingredientSection.getString(key));
                if (material != null && !material.isAir()) ingredients.put(key.charAt(0), material);
            }
        }

        ItemStack result = parseResult(section.getConfigurationSection("result"));
        if (result == null || result.getType().isAir()) return null;
        return new ForgeRecipe(id, normalizedShape, ingredients, result);
    }

    private static String normalizeShapeLine(String line) {
        if (line == null) line = "";
        if (line.length() > 2) return line.substring(0, 2);
        return String.format("%-2s", line);
    }

    private static Material parseMaterial(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) normalized = normalized.substring("MINECRAFT:".length());
        return Material.matchMaterial(normalized);
    }

    private static ItemStack parseResult(ConfigurationSection section) {
        if (section == null) return null;
        String type = section.getString("type", "material").toLowerCase(Locale.ROOT);
        String id = section.getString("id", "");
        int amount = Math.max(1, section.getInt("amount", 1));

        ItemStack result = switch (type) {
            case "armor" -> ArmorSetManager.createSetItem(id);
            case "weapon" -> {
                CustomWeapon weapon = ConfigManager.getWeapon(id);
                yield weapon == null ? null : WeaponManager.createWeaponStack(weapon, null);
            }
            case "material" -> {
                Material material = parseMaterial(id);
                yield material == null ? null : new ItemStack(material);
            }
            default -> null;
        };
        if (result != null) result.setAmount(amount);
        return result;
    }

    public record ForgeRecipe(String id, String[] shape, Map<Character, Material> ingredients, ItemStack result) {
        boolean matches(Inventory inventory, int[] inputSlots) {
            for (int i = 0; i < inputSlots.length; i++) {
                Material expected = materialAt(i);
                ItemStack actual = inventory.getItem(inputSlots[i]);
                if (expected == null) {
                    if (actual != null && !actual.getType().isAir()) return false;
                } else if (actual == null || actual.getType() != expected || actual.getAmount() < 1) {
                    return false;
                }
            }
            return true;
        }

        void consume(Inventory inventory, int[] inputSlots) {
            for (int i = 0; i < inputSlots.length; i++) {
                if (materialAt(i) == null) continue;
                ItemStack stack = inventory.getItem(inputSlots[i]);
                if (stack == null) continue;
                stack.setAmount(stack.getAmount() - 1);
                if (stack.getAmount() <= 0) inventory.setItem(inputSlots[i], null);
            }
        }

        private Material materialAt(int index) {
            char symbol = shape[index / 2].charAt(index % 2);
            if (symbol == ' ') return null;
            return ingredients.get(symbol);
        }
    }
}
