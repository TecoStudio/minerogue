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
import java.io.IOException;
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
        recipesFile = new File(plugin.getDataFolder(), "forge-recipes.yml");
        exportDefaultsIfMissing();
        reload();
    }

    public static void reload() {
        RECIPES.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ForgeRecipe recipe = parseRecipe(id, section.getConfigurationSection(id));
            if (recipe != null) RECIPES.add(recipe);
        }
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
        if (shape.size() != 3) return null;
        String[] normalizedShape = new String[3];
        for (int i = 0; i < 3; i++) {
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
        if (line.length() > 3) return line.substring(0, 3);
        return String.format("%-3s", line);
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

    private static void exportDefaultsIfMissing() {
        if (recipesFile.exists()) return;
        try {
            saveDefaults(recipesFile);
        } catch (IOException e) {
            RoguelikePlugin.getInstance().getLogger().warning("无法导出默认铸造配方: " + e.getMessage());
        }
    }

    private static void saveDefaults(File file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("""
                Roguelike 铸造台配方。修改后使用 /rw reload 重载。

                shape 必须是 3 行，每行 3 个字符。空格表示空槽。
                ingredients 中的字符对应 shape 里的符号。
                result.type 可选 armor、weapon、material。
                result.id 在 armor 类型下填写插件防具 ID，在 weapon 类型下填写 weapons.yml 武器 ID，在 material 类型下填写原版材料 ID。
                """);
        addDefault(config, "explosive_helmet", List.of("TCT", "CCC", "TCT"), "explosive_helmet");
        addDefault(config, "explosive_chestplate", List.of("CTC", "TCT", "CTC"), "explosive_chestplate");
        addDefault(config, "explosive_leggings", List.of("TCT", "C C", "TCT"), "explosive_leggings");
        addDefault(config, "explosive_boots", List.of("C C", "T T", "T T"), "explosive_boots");
        addWeaponDefault(config, "ember_knife", List.of(" F ", " W ", " S "), "minecraft:stone_sword", 'F', "minecraft:flint", 'S', "minecraft:stick");
        addWeaponDefault(config, "frost_cleaver", List.of(" I ", " W ", " B "), "minecraft:iron_axe", 'I', "minecraft:blue_ice", 'B', "minecraft:iron_block");
        addWeaponDefault(config, "storm_spear", List.of(" C ", " W ", " R "), "minecraft:trident", 'C', "minecraft:copper_ingot", 'R', "minecraft:redstone");
        addWeaponDefault(config, "plague_saber", List.of(" P ", " W ", " G "), "minecraft:golden_sword", 'P', "minecraft:spider_eye", 'G', "minecraft:gold_ingot");
        addWeaponDefault(config, "echo_blade", List.of(" A ", " W ", " D "), "minecraft:diamond_sword", 'A', "minecraft:amethyst_shard", 'D', "minecraft:diamond");
        addWeaponDefault(config, "glass_cannon_hammer", List.of(" N ", " W ", " B "), "minecraft:netherite_axe", 'N', "minecraft:netherite_ingot", 'B', "minecraft:diamond_block");

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent.getAbsolutePath());
        }
        config.save(file);
    }

    static void saveDefaultsForTest(File file) throws IOException {
        saveDefaults(file);
    }

    private static void addDefault(YamlConfiguration config, String id, List<String> shape, String armorId) {
        String path = "recipes." + id + ".";
        config.set(path + "shape", shape);
        config.set(path + "ingredients.C", "minecraft:copper_ingot");
        config.set(path + "ingredients.T", "minecraft:tnt");
        config.set(path + "result.type", "armor");
        config.set(path + "result.id", armorId);
        config.set(path + "result.amount", 1);
    }

    private static void addWeaponDefault(YamlConfiguration config, String id, List<String> shape,
                                         String weaponMaterial, char catalystSymbol, String catalyst,
                                         char baseSymbol, String baseMaterial) {
        String path = "recipes." + id + ".";
        config.set(path + "shape", shape);
        config.set(path + "ingredients.W", weaponMaterial);
        config.set(path + "ingredients." + catalystSymbol, catalyst);
        config.set(path + "ingredients." + baseSymbol, baseMaterial);
        config.set(path + "result.type", "weapon");
        config.set(path + "result.id", id);
        config.set(path + "result.amount", 1);
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
            char symbol = shape[index / 3].charAt(index % 3);
            if (symbol == ' ') return null;
            return ingredients.get(symbol);
        }
    }
}
