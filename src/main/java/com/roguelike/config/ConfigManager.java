package com.roguelike.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.roguelike.RoguelikePlugin;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RoguelikePlugin plugin;
    private static File weaponsDir;
    private static File itemsDir;
    private static File mobsDir;

    private static final Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
    private static final Map<String, CustomItem> items = new LinkedHashMap<>();
    private static final Map<String, MobConfig> mobs = new HashMap<>();

    public static void loadAll(RoguelikePlugin plugin) {
        ConfigManager.plugin = plugin;
        File data = plugin.getDataFolder();
        weaponsDir = new File(data, "weapons");
        itemsDir = new File(data, "items");
        mobsDir = new File(data, "mobs");

        loadBuiltIns();
        loadWeapons();
        loadItems();
        loadMobs();

        plugin.getLogger().info("加载了 " + weapons.size() + " 个武器模板, " + items.size() + " 个物品, " + mobs.size() + " 个怪物配置。");
    }

    private static void loadBuiltIns() {
        weapons.clear();
        items.clear();
        mobs.clear();
        MobExperienceConfig.clear();

        loadBuiltInWeapons();
        loadBuiltInItems();
        loadBuiltInMobs();
    }

    private static void loadBuiltInWeapons() {
        addWeapon(weapon("wooden_sword", "minecraft:wooden_sword", "木剑", "最基础的武器", 4, 1.6, 59, "common",
                "{\"attack_range\":3.0,\"lifesteal_percent\":0.0,\"slow_duration\":0.0,\"chain_targets\":0,\"chain_range\":0.0,\"chain_damage_percent\":0.0,\"damage_store_percent\":0.0,\"damage_store_max\":0.0,\"crit_chance\":0.05,\"crit_damage\":1.5,\"bleed_chance\":0.0}"));
        addWeapon(weapon("flame_sword", "minecraft:diamond_sword", "烈焰之剑", "燃烧敌人的剑", 10, 1.4, 800, "epic",
                "{\"attack_range\":3.2,\"fire_damage\":4.0,\"fire_duration\":3.0,\"crit_chance\":0.1,\"crit_damage\":1.75}"));
        addWeapon(weapon("vampire_dagger", "minecraft:iron_sword", "吸血匕首", "从敌人身上吸血", 5, 2.4, 600, "rare",
                "{\"attack_range\":2.8,\"lifesteal_percent\":0.15,\"crit_chance\":0.08,\"crit_damage\":1.6}"));
        addWeapon(weapon("thunder_axe", "minecraft:diamond_axe", "雷霆战斧", "几率召唤雷电", 14, 0.9, 1200, "legendary",
                "{\"attack_range\":3.0,\"lightning_chance\":0.15,\"crit_chance\":0.12,\"crit_damage\":2.0}"));
        addWeapon(weapon("whirlwind_blade", "minecraft:iron_sword", "旋风之刃", "攻击会波及周围敌人", 9, 1.3, 1000, "epic",
                "{\"attack_range\":3.1,\"chain_targets\":3,\"chain_range\":3.0,\"chain_damage_percent\":0.5,\"crit_chance\":0.1}"));
        addWeapon(weapon("inferno_greatsword", "minecraft:netherite_sword", "炼狱巨剑", "大范围火焰伤害", 18, 0.8, 2000, "legendary",
                "{\"attack_range\":3.5,\"fire_damage\":6.0,\"fire_duration\":4.0,\"chain_targets\":4,\"chain_range\":4.0,\"chain_damage_percent\":0.4,\"crit_chance\":0.15,\"crit_damage\":2.0}"));
        addWeapon(weapon("special_weapon", "minecraft:wooden_sword", "特殊武器", "由武器开发券唤醒的武器胚子", 3, 1.6, 250, "special",
                "{\"attack_range\":3.0}"));
    }

    private static CustomWeapon weapon(String id, String item, String name, String desc, double damage, double speed, int durability, String rarity, String effectsJson) {
        JsonObject w = new JsonObject();
        w.addProperty("id", id);
        w.addProperty("item", item);
        w.addProperty("name", name);
        w.addProperty("description", desc);
        w.addProperty("base_damage", damage);
        w.addProperty("attack_speed", speed);
        w.addProperty("durability", durability);
        w.addProperty("rarity", rarity);
        w.add("effects", GSON.fromJson(effectsJson, JsonObject.class));
        return parseWeapon(w);
    }

    private static void loadBuiltInItems() {
        JsonObject potion = new JsonObject();
        potion.addProperty("id", "healing_potion");
        potion.addProperty("name", "治疗药水");
        potion.addProperty("description", "恢复生命值");
        potion.addProperty("item_type", "potion");
        potion.addProperty("rarity", "common");
        JsonObject eff = new JsonObject();
        eff.addProperty("heal_amount", 10);
        potion.add("effects", eff);
        addItem(parseItem(potion));
    }

    private static void loadBuiltInMobs() {
        MobExperienceConfig.setDefaultExp(10);
        MobExperienceConfig.setMobExp("zombie", 15);
        MobExperienceConfig.setMobExp("skeleton", 15);
        MobExperienceConfig.setMobExp("creeper", 20);
        MobExperienceConfig.setMobExp("spider", 12);
        MobExperienceConfig.setMobExp("enderman", 25);
        MobExperienceConfig.setMobExp("blaze", 22);
        MobExperienceConfig.setMobExp("warden", 100);
        MobExperienceConfig.setMobExp("ender_dragon", 500);
        MobExperienceConfig.setMobExp("wither", 300);
        mobs.put("zombie", new MobConfig(1.5, 1.2, 1.0, "wooden_sword"));
    }

    private static void loadWeapons() {
        weapons.clear();
        File[] files = weaponsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (!root.has("weapons")) continue;
                JsonArray arr = root.getAsJsonArray("weapons");
                for (int i = 0; i < arr.size(); i++) {
                    addWeapon(parseWeapon(arr.get(i).getAsJsonObject()));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("无法加载武器文件: " + file.getName());
            }
        }
    }

    private static void addWeapon(CustomWeapon weapon) {
        weapons.put(weapon.getId().toLowerCase(), weapon);
    }

    private static CustomWeapon parseWeapon(JsonObject json) {
        String id = json.get("id").getAsString();
        String item = json.has("item") ? json.get("item").getAsString() : id;
        String name = json.has("name") ? json.get("name").getAsString() : id;
        String desc = json.has("description") ? json.get("description").getAsString() : "";
        double damage = json.has("base_damage") ? json.get("base_damage").getAsDouble() : 5.0;
        double speed = json.has("attack_speed") ? json.get("attack_speed").getAsDouble() : 1.6;
        int durability = json.has("durability") ? json.get("durability").getAsInt() : 250;
        String rarity = json.has("rarity") ? json.get("rarity").getAsString() : "common";
        Map<String, Double> effects = new HashMap<>();
        if (json.has("effects")) {
            JsonObject effectsJson = json.getAsJsonObject("effects");
            effectsJson.entrySet().forEach(e -> {
                if (e.getValue().isJsonPrimitive()) {
                    effects.put(e.getKey(), e.getValue().getAsDouble());
                }
            });
        }
        return new CustomWeapon(id, name, desc, item, damage, speed, durability, rarity, effects);
    }

    private static void loadItems() {
        items.clear();
        File[] files = itemsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (!root.has("items")) continue;
                JsonArray arr = root.getAsJsonArray("items");
                for (int i = 0; i < arr.size(); i++) {
                    addItem(parseItem(arr.get(i).getAsJsonObject()));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("无法加载物品文件: " + file.getName());
            }
        }
    }

    private static void addItem(CustomItem item) {
        items.put(item.getId().toLowerCase(), item);
    }

    private static CustomItem parseItem(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.has("name") ? json.get("name").getAsString() : id;
        String desc = json.has("description") ? json.get("description").getAsString() : "";
        String type = json.has("item_type") ? json.get("item_type").getAsString() : "misc";
        String rarity = json.has("rarity") ? json.get("rarity").getAsString() : "common";
        Map<String, Double> effects = new HashMap<>();
        if (json.has("effects")) {
            JsonObject effectsJson = json.getAsJsonObject("effects");
            effectsJson.entrySet().forEach(e -> {
                if (e.getValue().isJsonPrimitive()) {
                    effects.put(e.getKey(), e.getValue().getAsDouble());
                }
            });
        }
        return new CustomItem(id, name, desc, type, rarity, effects);
    }

    private static void loadMobs() {
        File[] files = mobsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root.has("default_exp")) {
                    MobExperienceConfig.setDefaultExp(root.get("default_exp").getAsInt());
                }
                if (root.has("mobs")) {
                    JsonObject map = root.getAsJsonObject("mobs");
                    map.entrySet().forEach(e -> {
                        if (e.getValue().isJsonPrimitive()) {
                            MobExperienceConfig.setMobExp(e.getKey(), e.getValue().getAsInt());
                        }
                    });
                }
                if (root.has("modifiers")) {
                    JsonObject mods = root.getAsJsonObject("modifiers");
                    mods.entrySet().forEach(e -> {
                        if (e.getValue().isJsonObject()) {
                            mobs.put(e.getKey().toLowerCase(), parseMobConfig(e.getValue().getAsJsonObject()));
                        }
                    });
                }
            } catch (IOException e) {
                plugin.getLogger().warning("无法加载怪物文件: " + file.getName());
            }
        }
    }

    private static MobConfig parseMobConfig(JsonObject json) {
        double health = json.has("health_multiplier") ? json.get("health_multiplier").getAsDouble() : 1.0;
        double damage = json.has("damage_multiplier") ? json.get("damage_multiplier").getAsDouble() : 1.0;
        double speed = json.has("speed_multiplier") ? json.get("speed_multiplier").getAsDouble() : 1.0;
        String weapon = json.has("weapon_template") ? json.get("weapon_template").getAsString() : null;
        return new MobConfig(health, damage, speed, weapon);
    }

    public static List<CustomWeapon> getWeapons() {
        return new ArrayList<>(weapons.values());
    }

    public static CustomWeapon getWeapon(String id) {
        return id == null ? null : weapons.get(id.toLowerCase());
    }

    public static List<CustomItem> getItems() {
        return new ArrayList<>(items.values());
    }

    public static CustomItem getItem(String id) {
        return id == null ? null : items.get(id.toLowerCase());
    }

    public static MobConfig getMobConfig(String entityType) {
        return mobs.get(entityType.toLowerCase());
    }

    public static void reload() {
        loadAll(plugin);
    }

    public static FileConfiguration getPluginConfig() {
        return plugin.getConfig();
    }

    public static RoguelikePlugin getPlugin() {
        return plugin;
    }

    public record MobConfig(double healthMultiplier, double damageMultiplier, double speedMultiplier, String weaponTemplate) {
    }
}
