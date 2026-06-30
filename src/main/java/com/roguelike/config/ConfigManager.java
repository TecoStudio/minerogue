package com.roguelike.config;

import com.roguelike.RoguelikePlugin;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static RoguelikePlugin plugin;
    private static File weaponsFile;
    private static File itemsFile;
    private static File mobsFile;
    private static boolean internalMonsterSystemEnabled = true;
    private static double skeletonEliteSpawnChance = 0.12;
    private static SkeletonEliteConfig skeletonEliteConfig = DefaultMobs.skeletonElite();
    private static ZombieEliteConfig zombieEliteConfig = DefaultMobs.zombieElite();

    private static final Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
    private static final Map<String, CustomItem> items = new LinkedHashMap<>();
    private static final Map<String, MobConfig> mobs = new LinkedHashMap<>();

    public static void loadAll(RoguelikePlugin plugin) {
        ConfigManager.plugin = plugin;
        File data = plugin.getDataFolder();
        weaponsFile = new File(data, "weapons.yml");
        itemsFile = new File(data, "items.yml");
        mobsFile = new File(data, "mobs.yml");

        loadBuiltIns();
        exportYamlDefaults();
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

        weapons.putAll(DefaultWeapons.create());
        items.putAll(DefaultItems.create());
        internalMonsterSystemEnabled = DefaultMobs.internalSystemEnabled();
        skeletonEliteConfig = DefaultMobs.skeletonElite();
        zombieEliteConfig = DefaultMobs.zombieElite();
        skeletonEliteSpawnChance = skeletonEliteConfig.spawnChance();
        MobExperienceConfig.setDefaultExp(DefaultMobs.defaultExperience());
        DefaultMobs.experience().forEach(MobExperienceConfig::setMobExp);
        mobs.putAll(DefaultMobs.modifiers());
    }

    private static void loadWeapons() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(weaponsFile);
        ConfigurationSection section = config.getConfigurationSection("weapons");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            addWeapon(parseWeapon(id, section.getConfigurationSection(id)));
        }
    }

    private static void addWeapon(CustomWeapon weapon) {
        weapons.put(weapon.getId().toLowerCase(), weapon);
    }

    private static CustomWeapon parseWeapon(String id, ConfigurationSection section) {
        if (section == null) return DefaultWeapons.create().get(id.toLowerCase());
        String item = section.getString("item", id);
        String name = section.getString("name", id);
        String desc = section.getString("description", "");
        double damage = section.getDouble("base-damage", 5.0);
        double speed = section.getDouble("attack-speed", 1.6);
        int durability = section.getInt("durability", 250);
        String rarity = section.getString("rarity", "common");
        Map<String, Double> effects = readDoubleMap(section.getConfigurationSection("effects"));
        return new CustomWeapon(id, name, desc, item, damage, speed, durability, rarity, effects);
    }

    private static void loadItems() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            addItem(parseItem(id, section.getConfigurationSection(id)));
        }
    }

    private static void addItem(CustomItem item) {
        items.put(item.getId().toLowerCase(), item);
    }

    private static CustomItem parseItem(String id, ConfigurationSection section) {
        if (section == null) return DefaultItems.create().get(id.toLowerCase());
        String name = section.getString("name", id);
        String desc = section.getString("description", "");
        String type = section.getString("item-type", "misc");
        String rarity = section.getString("rarity", "common");
        Map<String, Double> effects = readDoubleMap(section.getConfigurationSection("effects"));
        return new CustomItem(id, name, desc, type, rarity, effects);
    }

    private static void loadMobs() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(mobsFile);
        internalMonsterSystemEnabled = config.getBoolean("internal.enabled", internalMonsterSystemEnabled);
        skeletonEliteConfig = parseSkeletonEliteConfig(config.getConfigurationSection("internal.skeleton-elite"));
        zombieEliteConfig = parseZombieEliteConfig(config.getConfigurationSection("internal.zombie-elite"));
        skeletonEliteSpawnChance = skeletonEliteConfig.spawnChance();
        MobExperienceConfig.setDefaultExp(config.getInt("default-experience", MobExperienceConfig.getDefaultExp()));
        ConfigurationSection experience = config.getConfigurationSection("experience");
        if (experience != null) {
            for (String id : experience.getKeys(false)) {
                MobExperienceConfig.setMobExp(id, experience.getInt(id));
            }
        }

        ConfigurationSection modifiers = config.getConfigurationSection("modifiers");
        if (modifiers != null) {
            for (String id : modifiers.getKeys(false)) {
                mobs.put(id.toLowerCase(), parseMobConfig(modifiers.getConfigurationSection(id)));
            }
        }
    }

    private static MobConfig parseMobConfig(ConfigurationSection section) {
        if (section == null) return new MobConfig(1.0, 1.0, 1.0, null);
        double health = section.getDouble("health-multiplier", 1.0);
        double damage = section.getDouble("damage-multiplier", 1.0);
        double speed = section.getDouble("speed-multiplier", 1.0);
        String weapon = section.getString("weapon-template", null);
        return new MobConfig(health, damage, speed, weapon);
    }

    private static SkeletonEliteConfig parseSkeletonEliteConfig(ConfigurationSection section) {
        SkeletonEliteConfig defaults = skeletonEliteConfig;
        if (section == null) return defaults;
        return new SkeletonEliteConfig(
                section.getBoolean("enabled", defaults.enabled()),
                clampChance(section.getDouble("spawn-chance", defaults.spawnChance())),
                section.getString("name", defaults.name()),
                section.getDouble("health", defaults.health()),
                section.getDouble("damage", defaults.damage()),
                clampChance(section.getDouble("poison-chance", defaults.poisonChance())),
                Math.max(0.0, section.getDouble("poisoned-damage-bonus", defaults.poisonedDamageBonus())),
                Math.max(0.0, section.getDouble("poison-duration-seconds", defaults.poisonDurationSeconds())),
                section.getString("weapon-template", defaults.weaponTemplate()),
                Math.max(0.0, section.getDouble("behavior.detect-range", defaults.detectRange())),
                Math.max(0.0, section.getDouble("behavior.keep-distance", defaults.keepDistance())),
                Math.max(0.0, section.getDouble("behavior.melee-range", defaults.meleeRange())),
                Math.max(1L, section.getLong("behavior.shot-cooldown-ticks", defaults.shotCooldownTicks())),
                Math.max(1L, section.getLong("behavior.burst-cooldown-ticks", defaults.burstCooldownTicks())),
                Math.max(0.1, section.getDouble("behavior.arrow-speed", defaults.arrowSpeed())),
                Math.max(0.0, section.getDouble("behavior.retreat-speed", defaults.retreatSpeed())),
                Math.max(0.0, section.getDouble("behavior.lunge-speed", defaults.lungeSpeed())),
                Math.max(0.0, section.getDouble("behavior.post-burst-retreat-speed", defaults.postBurstRetreatSpeed()))
        );
    }

    private static ZombieEliteConfig parseZombieEliteConfig(ConfigurationSection section) {
        ZombieEliteConfig defaults = zombieEliteConfig;
        if (section == null) return defaults;
        return new ZombieEliteConfig(
                section.getBoolean("enabled", defaults.enabled()),
                clampChance(section.getDouble("spawn-chance", defaults.spawnChance())),
                section.getString("name", defaults.name()),
                section.getDouble("health", defaults.health()),
                section.getDouble("damage", defaults.damage()),
                section.getString("weapon-template", defaults.weaponTemplate())
        );
    }

    private static Map<String, Double> readDoubleMap(ConfigurationSection section) {
        Map<String, Double> values = new LinkedHashMap<>();
        if (section == null) return values;
        for (String key : section.getKeys(false)) {
            values.put(key, section.getDouble(key));
        }
        return values;
    }

    private static double clampChance(double chance) {
        return Math.max(0.0, Math.min(1.0, chance));
    }

    private static void exportYamlDefaults() {
        try {
            if (!weaponsFile.exists()) saveWeaponsYaml(weaponsFile, DefaultWeapons.create());
            if (!itemsFile.exists()) saveItemsYaml(itemsFile, DefaultItems.create());
            if (!mobsFile.exists()) saveMobsYaml(mobsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法导出默认 YAML 配置: " + e.getMessage());
        }
    }

    public static void exportEditableYaml() throws IOException {
        saveWeaponsYaml(weaponsFile, weapons);
        saveItemsYaml(itemsFile, items);
        saveMobsYaml(mobsFile);
    }

    private static void saveWeaponsYaml(File file, Map<String, CustomWeapon> source) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("Roguelike 武器配置。修改后使用 /rw reload 重载。");
        for (CustomWeapon weapon : source.values()) {
            String path = "weapons." + weapon.getId() + ".";
            config.set(path + "item", weapon.getItem());
            config.set(path + "name", weapon.getName());
            config.set(path + "description", weapon.getDescription());
            config.set(path + "base-damage", weapon.getBaseDamage());
            config.set(path + "attack-speed", weapon.getAttackSpeed());
            config.set(path + "durability", weapon.getDurability());
            config.set(path + "rarity", weapon.getRarity());
            weapon.getEffects().forEach((key, value) -> config.set(path + "effects." + key, value));
        }
        saveYaml(config, file);
    }

    private static void saveItemsYaml(File file, Map<String, CustomItem> source) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("Roguelike 物品配置。修改后使用 /rw reload 重载。");
        for (CustomItem item : source.values()) {
            String path = "items." + item.getId() + ".";
            config.set(path + "name", item.getName());
            config.set(path + "description", item.getDescription());
            config.set(path + "item-type", item.getItemType());
            config.set(path + "rarity", item.getRarity());
            item.getEffects().forEach((key, value) -> config.set(path + "effects." + key, value));
        }
        saveYaml(config, file);
    }

    private static void saveMobsYaml(File file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("Roguelike 怪物配置。修改后使用 /rw reload 重载。");
        config.set("internal.enabled", internalMonsterSystemEnabled);
        saveSkeletonEliteConfig(config, "internal.skeleton-elite", skeletonEliteConfig);
        saveZombieEliteConfig(config, "internal.zombie-elite", zombieEliteConfig);
        config.set("default-experience", MobExperienceConfig.getDefaultExp());
        MobExperienceConfig.getAllMobExp().forEach((key, value) -> config.set("experience." + key, value));
        mobs.forEach((key, value) -> {
            String path = "modifiers." + key + ".";
            config.set(path + "health-multiplier", value.healthMultiplier());
            config.set(path + "damage-multiplier", value.damageMultiplier());
            config.set(path + "speed-multiplier", value.speedMultiplier());
            config.set(path + "weapon-template", value.weaponTemplate());
        });
        saveYaml(config, file);
    }

    private static void saveSkeletonEliteConfig(YamlConfiguration config, String path, SkeletonEliteConfig value) {
        config.set(path + ".enabled", value.enabled());
        config.set(path + ".spawn-chance", value.spawnChance());
        config.set(path + ".name", value.name());
        config.set(path + ".health", value.health());
        config.set(path + ".damage", value.damage());
        config.set(path + ".poison-chance", value.poisonChance());
        config.set(path + ".poisoned-damage-bonus", value.poisonedDamageBonus());
        config.set(path + ".poison-duration-seconds", value.poisonDurationSeconds());
        config.set(path + ".weapon-template", value.weaponTemplate());
        config.set(path + ".behavior.detect-range", value.detectRange());
        config.set(path + ".behavior.keep-distance", value.keepDistance());
        config.set(path + ".behavior.melee-range", value.meleeRange());
        config.set(path + ".behavior.shot-cooldown-ticks", value.shotCooldownTicks());
        config.set(path + ".behavior.burst-cooldown-ticks", value.burstCooldownTicks());
        config.set(path + ".behavior.arrow-speed", value.arrowSpeed());
        config.set(path + ".behavior.retreat-speed", value.retreatSpeed());
        config.set(path + ".behavior.lunge-speed", value.lungeSpeed());
        config.set(path + ".behavior.post-burst-retreat-speed", value.postBurstRetreatSpeed());
    }

    private static void saveZombieEliteConfig(YamlConfiguration config, String path, ZombieEliteConfig value) {
        config.set(path + ".enabled", value.enabled());
        config.set(path + ".spawn-chance", value.spawnChance());
        config.set(path + ".name", value.name());
        config.set(path + ".health", value.health());
        config.set(path + ".damage", value.damage());
        config.set(path + ".weapon-template", value.weaponTemplate());
    }

    private static void saveYaml(YamlConfiguration config, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent.getAbsolutePath());
        }
        config.save(file);
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

    public static boolean isInternalMonsterSystemEnabled() {
        return plugin != null && internalMonsterSystemEnabled;
    }

    public static double getSkeletonEliteSpawnChance() {
        if (plugin == null) return 0.0;
        return skeletonEliteSpawnChance;
    }

    public static SkeletonEliteConfig getSkeletonEliteConfig() {
        return skeletonEliteConfig;
    }

    public static ZombieEliteConfig getZombieEliteConfig() {
        return zombieEliteConfig;
    }

    public static RoguelikePlugin getPlugin() {
        return plugin;
    }

    public record MobConfig(double healthMultiplier, double damageMultiplier, double speedMultiplier, String weaponTemplate) {
    }

    public record SkeletonEliteConfig(boolean enabled, double spawnChance, String name, double health, double damage,
                                      double poisonChance, double poisonedDamageBonus, double poisonDurationSeconds,
                                      String weaponTemplate, double detectRange, double keepDistance, double meleeRange,
                                      long shotCooldownTicks, long burstCooldownTicks, double arrowSpeed,
                                      double retreatSpeed, double lungeSpeed, double postBurstRetreatSpeed) {
    }

    public record ZombieEliteConfig(boolean enabled, double spawnChance, String name, double health, double damage,
                                    String weaponTemplate) {
    }
}
