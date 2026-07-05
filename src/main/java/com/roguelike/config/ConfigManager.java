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
    private static File sidebarFile;
    private static String sidebarTitle = "&6统计信息";
    private static List<String> sidebarLines = defaultSidebarLines();
    private static boolean internalMonsterSystemEnabled = true;
    private static double skeletonEliteSpawnChance = 0.12;
    private static SkeletonEliteConfig skeletonEliteConfig = DefaultMobs.skeletonElite();
    private static ZombieEliteConfig zombieEliteConfig = DefaultMobs.zombieElite();
    private static SpiderEliteConfig spiderEliteConfig = DefaultMobs.spiderElite();

    private static final Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
    private static final Map<String, CustomItem> items = new LinkedHashMap<>();
    private static final Map<String, MobConfig> mobs = new LinkedHashMap<>();

    public static void loadAll(RoguelikePlugin plugin) {
        ConfigManager.plugin = plugin;
        File data = plugin.getDataFolder();
        weaponsFile = new File(data, "weapons.yml");
        itemsFile = new File(data, "items.yml");
        mobsFile = new File(data, "mobs.yml");
        sidebarFile = new File(data, "sidebar.yml");

        loadBuiltIns();
        exportYamlDefaults();
        loadWeapons();
        loadItems();
        loadMobs();
        loadSidebar();

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
        spiderEliteConfig = DefaultMobs.spiderElite();
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
        spiderEliteConfig = parseSpiderEliteConfig(config.getConfigurationSection("internal.spider-elite"));
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

    private static SpiderEliteConfig parseSpiderEliteConfig(ConfigurationSection section) {
        SpiderEliteConfig defaults = spiderEliteConfig;
        if (section == null) return defaults;
        return new SpiderEliteConfig(
                section.getBoolean("enabled", defaults.enabled()),
                clampChance(section.getDouble("spawn-chance", defaults.spawnChance())),
                section.getString("name", defaults.name()),
                section.getDouble("health", defaults.health()),
                Math.max(0.0, section.getDouble("speed-multiplier", defaults.speedMultiplier())),
                clampChance(section.getDouble("slow-chance", defaults.slowChance())),
                Math.max(0.0, section.getDouble("slow-duration-seconds", defaults.slowDurationSeconds())),
                Math.max(1, section.getInt("slow-level", section.getInt("slow-amplifier", defaults.slowLevel())))
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
            if (!sidebarFile.exists()) saveSidebarYaml(sidebarFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法导出默认 YAML 配置: " + e.getMessage());
        }
    }

    public static void exportEditableYaml() throws IOException {
        saveWeaponsYaml(weaponsFile, weapons);
        saveItemsYaml(itemsFile, items);
        saveMobsYaml(mobsFile);
        saveSidebarYaml(sidebarFile);
    }

    private static void loadSidebar() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(sidebarFile);
        sidebarTitle = config.getString("title", "&6统计信息");
        List<String> lines = config.getStringList("lines");
        sidebarLines = lines.isEmpty() ? defaultSidebarLines() : lines;
    }

    private static List<String> defaultSidebarLines() {
        return List.of(
                "&f玩家: &e%player%",
                "&f等级: &e%level%",
                "&f经验: &a%exp%&7/&a%exp_next%",
                "&f击杀: &c%kills%",
                "&f死亡: &4%deaths%",
                "%ability_cooldowns%"
        );
    }

    private static void saveSidebarYaml(File file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("Roguelike 内置侧边栏配置。修改后使用 /rw reload 重载。可用占位符: %player%, %level%, %exp%, %exp_next%, %kills%, %deaths%, %ability_cooldowns%。");
        config.setComments("title", List.of("侧边栏标题。"));
        config.set("title", sidebarTitle);
        config.setComments("lines", List.of("侧边栏内容。%ability_cooldowns% 会展开为当前冷却中的武器技能，例如 Dash。"));
        config.set("lines", sidebarLines);
        saveYaml(config, file);
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
        config.options().header("""
                Roguelike 怪物配置。修改后使用 /rw reload 重载。

                概率字段使用 0.0 - 1.0：0.12 = 12%，0.35 = 35%。
                药水等级字段使用游戏内显示等级：1 = I，2 = II，3 = III。
                MythicMobs 集成开启时，本插件内置怪物不会自然生成。
                /rw monster spawn 只生成插件自定义怪物，例如 skeleton_elite、zombie_elite、spider_elite。
                """);
        config.setComments("internal", List.of("是否启用本插件内置怪物系统。"));
        config.set("internal.enabled", internalMonsterSystemEnabled);
        saveSkeletonEliteConfig(config, "internal.skeleton-elite", skeletonEliteConfig);
        saveZombieEliteConfig(config, "internal.zombie-elite", zombieEliteConfig);
        saveSpiderEliteConfig(config, "internal.spider-elite", spiderEliteConfig);
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
        config.setComments(path, List.of("骷髅精英：自然骷髅按概率转化，远程射箭，近身突进三连击。"));
        config.setComments(path + ".enabled", List.of("是否启用该怪物。"));
        config.set(path + ".enabled", value.enabled());
        config.setComments(path + ".spawn-chance", List.of("自然生成骷髅转化为骷髅精英的概率，0.12 = 12%。"));
        config.set(path + ".spawn-chance", value.spawnChance());
        config.setComments(path + ".name", List.of("怪物显示名，支持 & 颜色代码。"));
        config.set(path + ".name", value.name());
        config.setComments(path + ".health", List.of("最大生命值。"));
        config.set(path + ".health", value.health());
        config.setComments(path + ".damage", List.of("攻击和箭矢基础伤害。"));
        config.set(path + ".damage", value.damage());
        config.setComments(path + ".poison-chance", List.of("攻击使目标中毒的概率，0.30 = 30%。"));
        config.set(path + ".poison-chance", value.poisonChance());
        config.setComments(path + ".poisoned-damage-bonus", List.of("目标已中毒时的额外伤害比例，0.10 = +10%。"));
        config.set(path + ".poisoned-damage-bonus", value.poisonedDamageBonus());
        config.setComments(path + ".poison-duration-seconds", List.of("中毒持续秒数。"));
        config.set(path + ".poison-duration-seconds", value.poisonDurationSeconds());
        config.setComments(path + ".weapon-template", List.of("主手武器模板 ID，配置在 weapons.yml。"));
        config.set(path + ".weapon-template", value.weaponTemplate());
        config.setComments(path + ".behavior", List.of("战斗行为参数。距离单位为格，冷却单位为 tick，20 tick = 1 秒。"));
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
        config.setComments(path, List.of("僵尸精英：自然僵尸按概率转化，佩戴铁头盔，使用带锋利 I 的亢奋石剑。"));
        config.setComments(path + ".enabled", List.of("是否启用该怪物。"));
        config.set(path + ".enabled", value.enabled());
        config.setComments(path + ".spawn-chance", List.of("自然生成僵尸转化为僵尸精英的概率，0.12 = 12%。"));
        config.set(path + ".spawn-chance", value.spawnChance());
        config.setComments(path + ".name", List.of("怪物显示名，支持 & 颜色代码。"));
        config.set(path + ".name", value.name());
        config.setComments(path + ".health", List.of("最大生命值。"));
        config.set(path + ".health", value.health());
        config.setComments(path + ".damage", List.of("近战基础伤害。"));
        config.set(path + ".damage", value.damage());
        config.setComments(path + ".weapon-template", List.of("主手武器模板 ID，配置在 weapons.yml。"));
        config.set(path + ".weapon-template", value.weaponTemplate());
    }

    private static void saveSpiderEliteConfig(YamlConfiguration config, String path, SpiderEliteConfig value) {
        config.setComments(path, List.of("精英蜘蛛：自然蜘蛛按概率转化，常驻隐身，攻击时概率附加缓慢。"));
        config.setComments(path + ".enabled", List.of("是否启用该怪物。"));
        config.set(path + ".enabled", value.enabled());
        config.setComments(path + ".spawn-chance", List.of("自然生成蜘蛛转化为精英蜘蛛的概率，0.12 = 12%。"));
        config.set(path + ".spawn-chance", value.spawnChance());
        config.setComments(path + ".name", List.of("怪物显示名，支持 & 颜色代码。"));
        config.set(path + ".name", value.name());
        config.setComments(path + ".health", List.of("最大生命值。"));
        config.set(path + ".health", value.health());
        config.setComments(path + ".speed-multiplier", List.of("移动速度倍率。1.2 = 原版移速的 1.2 倍。"));
        config.set(path + ".speed-multiplier", value.speedMultiplier());
        config.setComments(path + ".slow-chance", List.of("攻击使目标获得缓慢的概率，0.35 = 35%。"));
        config.set(path + ".slow-chance", value.slowChance());
        config.setComments(path + ".slow-duration-seconds", List.of("缓慢持续秒数。"));
        config.set(path + ".slow-duration-seconds", value.slowDurationSeconds());
        config.setComments(path + ".slow-level", List.of("缓慢等级，按游戏内显示填写：1 = 缓慢 I，2 = 缓慢 II。"));
        config.set(path + ".slow-level", value.slowLevel());
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

    public static String getSidebarTitle() {
        return sidebarTitle;
    }

    public static List<String> getSidebarLines() {
        return sidebarLines;
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

    public static SpiderEliteConfig getSpiderEliteConfig() {
        return spiderEliteConfig;
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

    public record SpiderEliteConfig(boolean enabled, double spawnChance, String name, double health,
                                    double speedMultiplier, double slowChance, double slowDurationSeconds,
                                    int slowLevel) {
    }
}
