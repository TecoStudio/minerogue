package com.roguelike.config;

import com.roguelike.RoguelikePlugin;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {
    private static RoguelikePlugin plugin;
    private static File weaponsFile;
    private static File itemsFile;
    private static File contentDirectory;
    private static File mobsFile;
    private static File sidebarFile;
    private static String sidebarTitle = "&6统计信息";
    private static List<String> sidebarLines = defaultSidebarLines();
    private static boolean internalMonsterSystemEnabled = true;
    private static double skeletonEliteSpawnChance = 0.12;
    private static SkeletonEliteConfig skeletonEliteConfig = DefaultMobs.skeletonElite();
    private static ZombieEliteConfig zombieEliteConfig = DefaultMobs.zombieElite();
    private static SpiderEliteConfig spiderEliteConfig = DefaultMobs.spiderElite();
    private static ScriptedMobConfig scriptedMobConfig = DefaultMobs.scriptedMob();

    private static final Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
    private static final Map<String, CustomItem> items = new LinkedHashMap<>();
    private static final Map<String, ArmorDefinition> armorDefinitions = new LinkedHashMap<>();
    private static final Map<String, MobConfig> mobs = new LinkedHashMap<>();
    private static final Map<String, InternalMobDefinition> internalMobDefinitions = new LinkedHashMap<>();
    private static final Map<String, SkeletonEliteConfig> skeletonEliteConfigs = new LinkedHashMap<>();
    private static final Map<String, ZombieEliteConfig> zombieEliteConfigs = new LinkedHashMap<>();
    private static final Map<String, SpiderEliteConfig> spiderEliteConfigs = new LinkedHashMap<>();
    private static final Map<String, ScriptedMobConfig> scriptedMobConfigs = new LinkedHashMap<>();

    public static void loadAll(RoguelikePlugin plugin) {
        ConfigManager.plugin = plugin;
        File data = plugin.getDataFolder();
        weaponsFile = new File(data, "weapons.yml");
        itemsFile = new File(data, "items.yml");
        contentDirectory = new File(data, "content");
        mobsFile = new File(data, "mobs.yml");
        sidebarFile = new File(data, "sidebar.yml");

        loadBuiltIns();
        exportYamlDefaults();
        syncGithubContentIfEnabled();
        loadWeapons();
        loadItems();
        loadMobs();
        loadContentDirectory(contentDirectory);
        loadSidebar();

        plugin.getLogger().info("加载了 " + weapons.size() + " 个武器模板, " + items.size() + " 个物品, " + armorDefinitions.size() + " 个防具定义, " + mobs.size() + " 个怪物配置。");
    }

    private static void loadBuiltIns() {
        weapons.clear();
        items.clear();
        armorDefinitions.clear();
        mobs.clear();
        internalMobDefinitions.clear();
        skeletonEliteConfigs.clear();
        zombieEliteConfigs.clear();
        spiderEliteConfigs.clear();
        scriptedMobConfigs.clear();
        MobExperienceConfig.clear();

        weapons.putAll(DefaultWeapons.create());
        items.putAll(DefaultItems.create());
        armorDefinitions.putAll(DefaultArmor.create());
        internalMonsterSystemEnabled = DefaultMobs.internalSystemEnabled();
        skeletonEliteConfig = DefaultMobs.skeletonElite();
        zombieEliteConfig = DefaultMobs.zombieElite();
        spiderEliteConfig = DefaultMobs.spiderElite();
        scriptedMobConfig = DefaultMobs.scriptedMob();
        skeletonEliteSpawnChance = skeletonEliteConfig.spawnChance();
        MobExperienceConfig.setDefaultExp(DefaultMobs.defaultExperience());
        DefaultMobs.experience().forEach(MobExperienceConfig::setMobExp);
        mobs.putAll(DefaultMobs.modifiers());
    }

    private static void loadWeapons() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(weaponsFile);
        loadWeaponsFromConfiguration(config);
    }

    public static void loadContentDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return;
        loadYamlFiles(new File(directory, "weapons"), ConfigManager::loadWeaponsFromFile);
        loadYamlFiles(new File(directory, "items"), ConfigManager::loadItemsFromFile);
        loadYamlFiles(new File(directory, "armor"), ConfigManager::loadArmorFromFile);
        loadYamlFiles(new File(directory, "mobs"), ConfigManager::loadMobsFromFile);
    }

    private static void loadYamlFiles(File directory, YamlFileLoader loader) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return;
        try (var stream = Files.walk(directory.toPath())) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(ConfigManager::isYamlFile)
                    .sorted(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)))
                    .toList();
            for (Path path : files) loader.load(path.toFile());
        } catch (IOException e) {
            if (plugin != null) plugin.getLogger().warning("读取内容目录失败 " + directory.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private static boolean isYamlFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private static void loadWeaponsFromFile(File file) {
        loadWeaponsFromConfiguration(YamlConfiguration.loadConfiguration(file), contentId(file));
    }

    private static void loadWeaponsFromConfiguration(YamlConfiguration config) {
        loadWeaponsFromConfiguration(config, null);
    }

    private static void loadWeaponsFromConfiguration(YamlConfiguration config, String defaultId) {
        ConfigurationSection section = config.getConfigurationSection("weapons");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                CustomWeapon weapon = parseWeapon(id, section.getConfigurationSection(id));
                if (weapon != null) addWeapon(weapon);
            }
            return;
        }
        String id = config.getString("id", defaultId);
        if (id != null && !id.isBlank()) {
            CustomWeapon weapon = parseWeapon(id, config);
            if (weapon != null) addWeapon(weapon);
        }
    }

    private static void loadItemsFromFile(File file) {
        loadItemsFromConfiguration(YamlConfiguration.loadConfiguration(file), contentId(file));
    }

    private static void loadArmorFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("armor");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ArmorDefinition definition = parseArmorDefinition(id, section.getConfigurationSection(id));
                if (definition != null) armorDefinitions.put(id.toLowerCase(Locale.ROOT), definition);
            }
            return;
        }
        String id = config.getString("id", contentId(file));
        if (id != null && !id.isBlank()) {
            ArmorDefinition definition = parseArmorDefinition(id, config);
            if (definition != null) armorDefinitions.put(id.toLowerCase(Locale.ROOT), definition);
        }
    }

    private static void loadMobsFromFile(File file) {
        loadMobsFromConfiguration(YamlConfiguration.loadConfiguration(file), contentId(file));
    }

    private static String contentId(File file) {
        if (file == null) return null;
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
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
        int bonusAffixSlots = Math.max(0, section.getInt("bonus-affix-slots", 0));
        boolean allowOverflowAffixes = section.getBoolean("allow-overflow-affixes", "special".equalsIgnoreCase(rarity));
        String legendaryAffix = section.getString("legendary-affix", "");
        Map<String, Double> effects = readDoubleMap(section.getConfigurationSection("effects"));
        return new CustomWeapon(id, name, desc, item, damage, speed, durability, rarity, effects,
                bonusAffixSlots, allowOverflowAffixes, legendaryAffix);
    }

    private static void loadItems() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        loadItemsFromConfiguration(config);
    }

    private static void loadItemsFromConfiguration(YamlConfiguration config) {
        loadItemsFromConfiguration(config, null);
    }

    private static void loadItemsFromConfiguration(YamlConfiguration config, String defaultId) {
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                CustomItem item = parseItem(id, section.getConfigurationSection(id));
                if (item != null) addItem(item);
            }
            return;
        }
        String id = config.getString("id", defaultId);
        if (id != null && !id.isBlank()) {
            CustomItem item = parseItem(id, config);
            if (item != null) addItem(item);
        }
    }

    private static void addItem(CustomItem item) {
        items.put(item.getId().toLowerCase(), item);
    }

    private static CustomItem parseItem(String id, ConfigurationSection section) {
        if (section == null) return DefaultItems.create().get(id.toLowerCase());
        String type = section.getString("item-type", "misc");
        String item = section.getString("item", defaultItemMaterial(type));
        String name = section.getString("name", id);
        String desc = section.getString("description", "");
        String rarity = section.getString("rarity", "common");
        Map<String, Double> effects = readDoubleMap(section.getConfigurationSection("effects"));
        return new CustomItem(id, item, name, desc, type, rarity, effects);
    }

    private static String defaultItemMaterial(String itemType) {
        if ("potion".equalsIgnoreCase(itemType) || "tonic".equalsIgnoreCase(itemType)) return "minecraft:potion";
        return "minecraft:paper";
    }

    private static ArmorDefinition parseArmorDefinition(String id, ConfigurationSection section) {
        if (section == null) return null;
        String name = section.getString("name", id);
        String desc = section.getString("description", "");
        String rarity = section.getString("rarity", "common");
        return new ArmorDefinition(name, desc, rarity);
    }

    private static void loadMobs() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(mobsFile);
        loadMobsFromConfiguration(config);
    }

    private static void loadMobsFromConfiguration(YamlConfiguration config) {
        loadMobsFromConfiguration(config, null);
    }

    private static void loadMobsFromConfiguration(YamlConfiguration config, String defaultId) {
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

        String type = config.getString("type", "").toLowerCase(Locale.ROOT);
        String id = config.getString("id", defaultId);
        if (id == null || id.isBlank()) return;
        switch (type) {
            case "experience" -> {
                MobExperienceConfig.setMobExp(id, config.getInt("experience", config.getInt("value", MobExperienceConfig.getMobExp(id))));
            }
            case "modifier" -> mobs.put(id.toLowerCase(Locale.ROOT), parseMobConfig(config));
            case "internal" -> applyInternalMobConfig(id, config);
            case "settings" -> {
                internalMonsterSystemEnabled = config.getBoolean("internal-system-enabled", internalMonsterSystemEnabled);
                MobExperienceConfig.setDefaultExp(config.getInt("default-experience", MobExperienceConfig.getDefaultExp()));
            }
            default -> {
                if (config.isSet("health-multiplier") || config.isSet("damage-multiplier") || config.isSet("speed-multiplier")) {
                    mobs.put(id.toLowerCase(Locale.ROOT), parseMobConfig(config));
                }
            }
        }
    }

    private static void applyInternalMobConfig(String id, ConfigurationSection section) {
        String normalized = id.toLowerCase(Locale.ROOT).replace('_', '-');
        String logic = section.getString("logic", legacyInternalMobLogic(normalized));
        if (logic == null || logic.isBlank()) return;
        String logicKind = internalLogicKind(logic);
        List<String> aliases = section.getStringList("aliases");
        boolean spawnable = section.getBoolean("spawnable", true);
        internalMobDefinitions.put(id.toLowerCase(Locale.ROOT), new InternalMobDefinition(id, logic, aliases, spawnable));
        switch (logicKind) {
            case "scripted" -> {
                ScriptedMobConfig config = parseScriptedMobConfig(section);
                scriptedMobConfigs.put(id.toLowerCase(Locale.ROOT), config);
                scriptedMobConfig = config;
            }
            case "skeleton-elite" -> {
                SkeletonEliteConfig config = parseSkeletonEliteConfig(section);
                skeletonEliteConfigs.put(id.toLowerCase(Locale.ROOT), config);
                skeletonEliteConfig = config;
                skeletonEliteSpawnChance = config.spawnChance();
            }
            case "zombie-elite" -> {
                ZombieEliteConfig config = parseZombieEliteConfig(section);
                zombieEliteConfigs.put(id.toLowerCase(Locale.ROOT), config);
                zombieEliteConfig = config;
            }
            case "spider-elite" -> {
                SpiderEliteConfig config = parseSpiderEliteConfig(section);
                spiderEliteConfigs.put(id.toLowerCase(Locale.ROOT), config);
                spiderEliteConfig = config;
            }
            default -> {
            }
        }
    }

    private static String legacyInternalMobLogic(String normalizedId) {
        return switch (normalizedId) {
            case "skeleton-elite" -> "skeleton-elite";
            case "zombie-elite" -> "zombie-elite";
            case "spider-elite" -> "spider-elite";
            case "blood-zombie" -> "use template zombie\nif target_far then leap\nelse shockwave";
            case "vagrant" -> "use template skeleton\nif target_detected then blink\nif target_close then blade-storm";
            default -> null;
        };
    }

    private static String internalLogicKind(String logic) {
        String normalized = logic.toLowerCase(Locale.ROOT).replace('_', '-').trim();
        return switch (normalized) {
            case "skeleton-elite", "zombie-elite", "spider-elite" -> normalized;
            default -> "scripted";
        };
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
                Math.max(0.0, section.getDouble("behavior.post-burst-retreat-speed", defaults.postBurstRetreatSpeed())),
                readCombatScript(section, defaults.combatScript())
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
                section.getString("weapon-template", defaults.weaponTemplate()),
                readCombatScript(section, defaults.combatScript())
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
                Math.max(1, section.getInt("slow-level", section.getInt("slow-amplifier", defaults.slowLevel()))),
                readCombatScript(section, defaults.combatScript())
        );
    }

    private static ScriptedMobConfig parseScriptedMobConfig(ConfigurationSection section) {
        ScriptedMobConfig defaults = scriptedMobConfig;
        if (section == null) return defaults;
        return new ScriptedMobConfig(
                section.getBoolean("enabled", defaults.enabled()),
                section.getString("name", defaults.name()),
                Math.max(1.0, section.getDouble("health", defaults.health())),
                Math.max(0.0, section.getDouble("damage", defaults.damage())),
                Math.max(0.0, section.getDouble("speed-multiplier", defaults.speedMultiplier())),
                Math.max(0.0, section.getDouble("detect-range", defaults.detectRange())),
                Math.max(0.0, section.getDouble("skill-range", defaults.skillRange())),
                Math.max(1L, section.getLong("skill-cooldown-ticks", defaults.skillCooldownTicks())),
                Math.max(0.0, section.getDouble("skill-damage", defaults.skillDamage()))
        );
    }

    private static String readCombatScript(ConfigurationSection section, String fallback) {
        String script = section.getString("combat-script", null);
        if (script != null) return script;
        List<String> lines = section.getStringList("combat-script");
        return lines.isEmpty() ? fallback : String.join("\n", lines);
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
        saveArmorYaml(new File(plugin.getDataFolder(), "armor.yml"), armorDefinitions);
        saveMobsYaml(mobsFile);
        saveSidebarYaml(sidebarFile);
    }

    private static void syncGithubContentIfEnabled() {
        if (plugin == null || !plugin.getConfig().getBoolean("content.github-sync.enabled", true)) return;
        String baseUrl = plugin.getConfig().getString("content.github-sync.base-url", "");
        List<String> files = plugin.getConfig().getStringList("content.github-sync.files");
        if (baseUrl == null || baseUrl.isBlank() || files.isEmpty()) return;
        boolean overwrite = plugin.getConfig().getBoolean("content.github-sync.overwrite-existing", true);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        for (String relative : files) {
            if (relative == null || relative.isBlank() || relative.contains("..")) continue;
            Path target = contentDirectory.toPath().resolve(relative.replace('\\', '/')).normalize();
            if (!target.startsWith(contentDirectory.toPath())) continue;
            if (!overwrite && Files.exists(target)) continue;
            try {
                Files.createDirectories(target.getParent());
                HttpRequest request = HttpRequest.newBuilder(URI.create(joinUrl(baseUrl, relative.replace('\\', '/'))))
                        .timeout(Duration.ofSeconds(20))
                        .GET()
                        .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    Files.write(target, response.body());
                } else {
                    plugin.getLogger().warning("拉取内容 YAML 失败 " + relative + ": HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("拉取内容 YAML 失败 " + relative + ": " + e.getMessage());
            }
        }
    }

    private static String joinUrl(String baseUrl, String relative) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = relative.startsWith("/") ? relative.substring(1) : relative;
        return base + "/" + path;
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
            if (weapon.getBonusAffixSlots() > 0) config.set(path + "bonus-affix-slots", weapon.getBonusAffixSlots());
            if (weapon.allowsOverflowAffixes()) config.set(path + "allow-overflow-affixes", true);
            if (!weapon.getLegendaryAffix().isBlank()) config.set(path + "legendary-affix", weapon.getLegendaryAffix());
            weapon.getEffects().forEach((key, value) -> config.set(path + "effects." + key, value));
        }
        saveYaml(config, file);
    }

    private static void saveItemsYaml(File file, Map<String, CustomItem> source) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("Roguelike 物品配置。修改后使用 /rw reload 重载。");
        for (CustomItem item : source.values()) {
            String path = "items." + item.getId() + ".";
            config.set(path + "item", item.getItem());
            config.set(path + "name", item.getName());
            config.set(path + "description", item.getDescription());
            config.set(path + "item-type", item.getItemType());
            config.set(path + "rarity", item.getRarity());
            item.getEffects().forEach((key, value) -> config.set(path + "effects." + key, value));
        }
        saveYaml(config, file);
    }

    private static void saveArmorYaml(File file, Map<String, ArmorDefinition> source) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().header("Roguelike 防具定义配置。修改后使用 /rw reload 重载。");
        for (Map.Entry<String, ArmorDefinition> entry : source.entrySet()) {
            String path = "armor." + entry.getKey() + ".";
            ArmorDefinition definition = entry.getValue();
            config.set(path + "name", definition.name());
            config.set(path + "description", definition.description());
            config.set(path + "rarity", definition.rarity());
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
                /rw monster spawn 只生成 content/mobs/*.yml 中 type: internal 且 spawnable: true 的怪物。
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

    public static CustomWeapon getWeapon(String id) {
        return id == null ? null : weapons.get(id.toLowerCase());
    }

    public static List<CustomWeapon> getWeapons() {
        return new ArrayList<>(weapons.values());
    }

    public static List<CustomItem> getItems() {
        return new ArrayList<>(items.values());
    }

    public static CustomItem getItem(String id) {
        return id == null ? null : items.get(id.toLowerCase());
    }

    public static Map<String, ArmorDefinition> getArmorDefinitions() {
        return new LinkedHashMap<>(armorDefinitions);
    }

    public static List<InternalMobDefinition> getInternalMobDefinitions() {
        return new ArrayList<>(internalMobDefinitions.values());
    }

    public static SkeletonEliteConfig getSkeletonEliteConfig(String id) {
        return configOrDefault(skeletonEliteConfigs, id, skeletonEliteConfig);
    }

    public static ZombieEliteConfig getZombieEliteConfig(String id) {
        return configOrDefault(zombieEliteConfigs, id, zombieEliteConfig);
    }

    public static SpiderEliteConfig getSpiderEliteConfig(String id) {
        return configOrDefault(spiderEliteConfigs, id, spiderEliteConfig);
    }

    public static ScriptedMobConfig getScriptedMobConfig(String id) {
        return configOrDefault(scriptedMobConfigs, id, scriptedMobConfig);
    }

    private static <T> T configOrDefault(Map<String, T> source, String id, T fallback) {
        if (id == null) return fallback;
        return source.getOrDefault(id.toLowerCase(Locale.ROOT), fallback);
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

    public static double getExpMultiplier() {
        if (plugin == null) return 1.0;
        return Math.max(0.0, plugin.getConfig().getDouble("gameplay.exp-multiplier", 1.0));
    }

    public static double getProgressionExpMultiplier() {
        if (plugin == null) return 1.0;
        return Math.max(0.0, plugin.getConfig().getDouble("gameplay.progression-exp-multiplier", getExpMultiplier()));
    }

    public static double getWeaponDropMultiplier() {
        if (plugin == null) return 1.0;
        return Math.max(0.0, plugin.getConfig().getDouble("gameplay.weapon-drop-multiplier", 1.0));
    }

    public static double getConfiguredWeaponDropChance(String rarity, double fallback) {
        if (plugin == null) return fallback;
        String path = "gameplay.weapon-drop-chances." + rarity.toLowerCase();
        return clampChance(plugin.getConfig().getDouble(path, fallback));
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

    private static void saveYaml(YamlConfiguration config, File file) throws IOException {
        Files.createDirectories(file.toPath().getParent());
        config.save(file);
    }

    public static RoguelikePlugin getPlugin() {
        return plugin;
    }

    public record MobConfig(double healthMultiplier, double damageMultiplier, double speedMultiplier, String weaponTemplate) {
    }

    public record InternalMobDefinition(String id, String logic, List<String> aliases, boolean spawnable) {
        public InternalMobDefinition {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }

    public record SkeletonEliteConfig(boolean enabled, double spawnChance, String name, double health, double damage,
                                      double poisonChance, double poisonedDamageBonus, double poisonDurationSeconds,
                                      String weaponTemplate, double detectRange, double keepDistance, double meleeRange,
                                      long shotCooldownTicks, long burstCooldownTicks, double arrowSpeed,
                                      double retreatSpeed, double lungeSpeed, double postBurstRetreatSpeed,
                                      String combatScript) {
    }

    public record ZombieEliteConfig(boolean enabled, double spawnChance, String name, double health, double damage,
                                    String weaponTemplate, String combatScript) {
    }

    public record SpiderEliteConfig(boolean enabled, double spawnChance, String name, double health,
                                    double speedMultiplier, double slowChance, double slowDurationSeconds,
                                    int slowLevel, String combatScript) {
    }

    public record ScriptedMobConfig(boolean enabled, String name, double health, double damage, double speedMultiplier,
                                    double detectRange, double skillRange, long skillCooldownTicks, double skillDamage) {
    }

    @FunctionalInterface
    private interface YamlFileLoader {
        void load(File file);
    }
}
