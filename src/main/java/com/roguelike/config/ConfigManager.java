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
    private static ScriptedMobConfig scriptedMobConfig = DefaultMobs.scriptedMob();

    private static final Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
    private static final Map<String, CustomItem> items = new LinkedHashMap<>();
    private static final Map<String, ArmorDefinition> armorDefinitions = new LinkedHashMap<>();
    private static final Map<String, MobConfig> mobs = new LinkedHashMap<>();
    private static final Map<String, InternalMobDefinition> internalMobDefinitions = new LinkedHashMap<>();
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
        scriptedMobConfigs.clear();
        MobExperienceConfig.clear();

        weapons.putAll(DefaultWeapons.create());
        items.putAll(DefaultItems.create());
        armorDefinitions.putAll(DefaultArmor.create());
        internalMonsterSystemEnabled = DefaultMobs.internalSystemEnabled();
        scriptedMobConfig = DefaultMobs.scriptedMob();
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

        ConfigurationSection internal = config.getConfigurationSection("internal");
        if (internal != null) {
            for (String key : internal.getKeys(false)) {
                if ("enabled".equalsIgnoreCase(key)) continue;
                ConfigurationSection mob = internal.getConfigurationSection(key);
                if (mob != null) applyInternalMobConfig(key, mob);
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
        String template = section.getString("template", "zombie");
        warnUnknownTemplate(id, template);
        List<String> aliases = section.getStringList("aliases");
        boolean spawnable = section.getBoolean("spawnable", true);
        String weaponTemplate = section.getString("weapon-template", null);
        EquipmentDefinition equipment = parseEquipmentDefinition(section);
        DropConfig drops = parseDropConfig(section);
        List<MobPotionEffectDefinition> potionEffects = parsePotionEffects(section);
        List<ActionDefinition> actions = readActions(section);
        internalMobDefinitions.put(id.toLowerCase(Locale.ROOT), new InternalMobDefinition(
                id, template, aliases, spawnable, weaponTemplate, equipment, drops, potionEffects, actions));
        ScriptedMobConfig config = parseScriptedMobConfig(section);
        scriptedMobConfigs.put(id.toLowerCase(Locale.ROOT), config);
        scriptedMobConfig = config;
    }

    private static EquipmentDefinition parseEquipmentDefinition(ConfigurationSection section) {
        ConfigurationSection equipment = section.getConfigurationSection("equipment");
        if (equipment == null) return EquipmentDefinition.empty();
        return new EquipmentDefinition(
                equipment.getString("helmet", null),
                equipment.getString("chestplate", null),
                equipment.getString("leggings", null),
                equipment.getString("boots", null),
                equipment.getString("main-hand", null),
                equipment.getString("off-hand", null),
                equipment.getString("main-hand-weapon-template", null),
                equipment.getString("off-hand-weapon-template", null),
                parseEquipmentDropChances(equipment.getConfigurationSection("drop-chances"))
        );
    }

    private static EquipmentDropChances parseEquipmentDropChances(ConfigurationSection section) {
        if (section == null) return EquipmentDropChances.zero();
        return new EquipmentDropChances(
                clampChance(section.getDouble("helmet", 0.0)),
                clampChance(section.getDouble("chestplate", 0.0)),
                clampChance(section.getDouble("leggings", 0.0)),
                clampChance(section.getDouble("boots", 0.0)),
                clampChance(section.getDouble("main-hand", 0.0)),
                clampChance(section.getDouble("off-hand", 0.0))
        );
    }

    private static void warnUnknownTemplate(String id, String template) {
        String normalized = template == null ? "" : template.toLowerCase(Locale.ROOT);
        if (normalized.contains("zombie") || normalized.contains("skeleton") || normalized.contains("spider")) return;
        if (plugin != null) plugin.getLogger().warning("未知内置怪物模板 " + template + " (" + id + ")，将回退为 zombie。");
    }

    private static List<ActionDefinition> readActions(ConfigurationSection section) {
        List<ActionDefinition> actions = new ArrayList<>();
        for (Map<?, ?> entry : section.getMapList("actions")) {
            Object whenValue = entry.containsKey("when") ? entry.get("when") : "always";
            Object actionValue = entry.containsKey("do") ? entry.get("do") : entry.get("action");
            String when = String.valueOf(whenValue);
            String action = actionValue == null ? "" : String.valueOf(actionValue);
            int hits = Math.max(1, toInt(entry.get("hits"), 1));
            double chance = clampChance(toDouble(entry.get("chance"), 1.0));
            double durationSeconds = Math.max(0.0, toDouble(entry.get("duration-seconds"), toDouble(entry.get("duration"), 2.5)));
            int level = Math.max(1, toInt(entry.get("level"), toInt(entry.get("slow-level"), 1)));
            long cooldownTicks = Math.max(0L, toLong(entry.get("cooldown-ticks"), 0L));
            double speed = Math.max(0.0, toDouble(entry.get("speed"), 0.0));
            double damage = toDouble(entry.get("damage"), Double.NaN);
            if (!action.isBlank()) {
                warnUnknownAction(action);
                actions.add(new ActionDefinition(when, action, hits, chance, durationSeconds, level, cooldownTicks, speed, damage));
            }
        }
        return actions;
    }

    private static void warnUnknownAction(String action) {
        String normalized = action.toLowerCase(Locale.ROOT).replace('_', '-');
        boolean known = switch (normalized) {
            case "melee-burst", "retreat", "leap", "shockwave", "blink", "blade-storm", "slow-on-hit" -> true;
            default -> false;
        };
        if (!known && plugin != null) plugin.getLogger().warning("未知内置怪物动作 " + action + "，该动作将被忽略。");
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long toLong(Object value, long fallback) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return fallback;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double toDouble(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static MobConfig parseMobConfig(ConfigurationSection section) {
        if (section == null) return new MobConfig(1.0, 1.0, 1.0, null);
        double health = section.getDouble("health-multiplier", 1.0);
        double damage = section.getDouble("damage-multiplier", 1.0);
        double speed = section.getDouble("speed-multiplier", 1.0);
        String weapon = section.getString("weapon-template", null);
        return new MobConfig(health, damage, speed, weapon, parseEquipmentDefinition(section), parseDropConfig(section));
    }

    private static DropConfig parseDropConfig(ConfigurationSection section) {
        if (section == null) return DropConfig.empty();
        ConfigurationSection drops = section.getConfigurationSection("drops");
        if (drops == null) {
            return new DropConfig(clampChance(section.getDouble("held-item-drop-chance", 0.0)), List.of());
        }
        double heldItemChance = clampChance(drops.getDouble("held-item-chance",
                drops.getDouble("held-item-drop-chance", 0.0)));
        List<DropItemDefinition> items = new ArrayList<>();
        for (Map<?, ?> entry : drops.getMapList("items")) {
            String material = toNullableString(entry.get("material"));
            String weaponTemplate = toNullableString(entry.get("weapon-template"));
            String itemTemplate = toNullableString(entry.get("item-template"));
            int amount = Math.max(1, toInt(entry.get("amount"), 1));
            double chance = clampChance(toDouble(entry.get("chance"), 0.0));
            if (chance > 0.0 && (isPresent(material) || isPresent(weaponTemplate) || isPresent(itemTemplate))) {
                items.add(new DropItemDefinition(material, weaponTemplate, itemTemplate, amount, chance));
            }
        }
        return new DropConfig(heldItemChance, items);
    }

    private static List<MobPotionEffectDefinition> parsePotionEffects(ConfigurationSection section) {
        List<MobPotionEffectDefinition> effects = new ArrayList<>();
        if (section == null) return effects;
        for (Map<?, ?> entry : section.getMapList("potion-effects")) {
            String type = toNullableString(entry.containsKey("type") ? entry.get("type") : entry.get("effect"));
            if (!isPresent(type)) continue;
            int level = Math.max(1, toInt(entry.get("level"), 1));
            boolean infinite = toBoolean(entry.get("infinite"), false);
            int durationTicks = infinite ? -1 : toInt(entry.get("duration-ticks"), toInt(entry.get("duration"), -1));
            boolean ambient = toBoolean(entry.get("ambient"), false);
            boolean particles = toBoolean(entry.get("particles"), true);
            effects.add(new MobPotionEffectDefinition(type, level, durationTicks, ambient, particles));
        }
        return effects;
    }

    private static String toNullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean toBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) return bool;
        if (value == null) return fallback;
        return Boolean.parseBoolean(value.toString());
    }

    private static ScriptedMobConfig parseScriptedMobConfig(ConfigurationSection section) {
        ScriptedMobConfig defaults = DefaultMobs.scriptedMob();
        if (section == null) return defaults;
        return new ScriptedMobConfig(
                section.getBoolean("enabled", defaults.enabled()),
                clampChance(section.getDouble("spawn-chance", defaults.spawnChance())),
                section.getString("name", defaults.name()),
                Math.max(1.0, section.getDouble("health", defaults.health())),
                Math.max(0.0, section.getDouble("damage", defaults.damage())),
                Math.max(0.0, section.getDouble("speed-multiplier", defaults.speedMultiplier())),
                Math.max(0.0, section.getDouble("detect-range", defaults.detectRange())),
                Math.max(0.0, section.getDouble("skill-range", defaults.skillRange())),
                Math.max(1L, section.getLong("skill-cooldown-ticks", defaults.skillCooldownTicks())),
                Math.max(0.0, section.getDouble("skill-damage", defaults.skillDamage())),
                section.getBoolean("bossbar", defaults.bossBar())
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
            exportBundledContentDefaults();
        } catch (IOException e) {
            plugin.getLogger().warning("无法导出默认 YAML 配置: " + e.getMessage());
        }
    }

    private static void exportBundledContentDefaults() {
        if (plugin == null) return;
        List<String> files = plugin.getConfig().getStringList("content.github-sync.files");
        for (String relative : files) {
            if (relative == null || relative.isBlank() || relative.contains("..")) continue;
            String normalized = relative.replace('\\', '/');
            File target = new File(contentDirectory, normalized);
            if (target.exists()) continue;
            try {
                plugin.saveResource("content/" + normalized, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("默认内容 YAML 不存在 content/" + normalized + ": " + e.getMessage());
            }
        }
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
        config.set("default-experience", MobExperienceConfig.getDefaultExp());
        MobExperienceConfig.getAllMobExp().forEach((key, value) -> config.set("experience." + key, value));
        mobs.forEach((key, value) -> {
            String path = "modifiers." + key + ".";
            config.set(path + "health-multiplier", value.healthMultiplier());
            config.set(path + "damage-multiplier", value.damageMultiplier());
            config.set(path + "speed-multiplier", value.speedMultiplier());
            config.set(path + "weapon-template", value.weaponTemplate());
            writeDropConfig(config, path + "drops", value.drops());
        });
        saveYaml(config, file);
    }

    private static void writeDropConfig(YamlConfiguration config, String path, DropConfig drops) {
        if (drops == null || (drops.heldItemChance() <= 0.0 && drops.items().isEmpty())) return;
        config.set(path + ".held-item-chance", drops.heldItemChance());
        List<Map<String, Object>> items = new ArrayList<>();
        for (DropItemDefinition drop : drops.items()) {
            Map<String, Object> item = new LinkedHashMap<>();
            if (drop.material() != null && !drop.material().isBlank()) item.put("material", drop.material());
            if (drop.weaponTemplate() != null && !drop.weaponTemplate().isBlank()) item.put("weapon-template", drop.weaponTemplate());
            if (drop.itemTemplate() != null && !drop.itemTemplate().isBlank()) item.put("item-template", drop.itemTemplate());
            item.put("chance", drop.chance());
            item.put("amount", drop.amount());
            items.add(item);
        }
        config.set(path + ".items", items);
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
        if (plugin == null) return 0.0;
        return Math.max(0.0, plugin.getConfig().getDouble("gameplay.weapon-drop-multiplier", 0.0));
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

    private static void saveYaml(YamlConfiguration config, File file) throws IOException {
        Files.createDirectories(file.toPath().getParent());
        config.save(file);
    }

    public static RoguelikePlugin getPlugin() {
        return plugin;
    }

    public record MobConfig(double healthMultiplier, double damageMultiplier, double speedMultiplier,
                            String weaponTemplate, EquipmentDefinition equipment, DropConfig drops) {
        public MobConfig(double healthMultiplier, double damageMultiplier, double speedMultiplier, String weaponTemplate) {
            this(healthMultiplier, damageMultiplier, speedMultiplier, weaponTemplate, EquipmentDefinition.empty(), DropConfig.empty());
        }

        public MobConfig(double healthMultiplier, double damageMultiplier, double speedMultiplier,
                         String weaponTemplate, DropConfig drops) {
            this(healthMultiplier, damageMultiplier, speedMultiplier, weaponTemplate, EquipmentDefinition.empty(), drops);
        }

        public MobConfig {
            equipment = equipment == null ? EquipmentDefinition.empty() : equipment;
            drops = drops == null ? DropConfig.empty() : drops;
        }
    }

    public record InternalMobDefinition(String id, String template, List<String> aliases, boolean spawnable,
                                        String weaponTemplate, EquipmentDefinition equipment, DropConfig drops,
                                        List<MobPotionEffectDefinition> potionEffects, List<ActionDefinition> actions) {
        public InternalMobDefinition(String id, String template, List<String> aliases, boolean spawnable,
                                     String weaponTemplate, List<ActionDefinition> actions) {
            this(id, template, aliases, spawnable, weaponTemplate, EquipmentDefinition.empty(),
                    DropConfig.empty(), List.of(), actions);
        }

        public InternalMobDefinition {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            equipment = equipment == null ? EquipmentDefinition.empty() : equipment;
            drops = drops == null ? DropConfig.empty() : drops;
            potionEffects = potionEffects == null ? List.of() : List.copyOf(potionEffects);
            actions = actions == null ? List.of() : List.copyOf(actions);
        }
    }

    public record EquipmentDefinition(String helmet, String chestplate, String leggings, String boots,
                                      String mainHand, String offHand, String mainHandWeaponTemplate,
                                      String offHandWeaponTemplate, EquipmentDropChances dropChances) {
        public static EquipmentDefinition empty() {
            return new EquipmentDefinition(null, null, null, null, null, null, null, null, EquipmentDropChances.zero());
        }

        public EquipmentDefinition {
            dropChances = dropChances == null ? EquipmentDropChances.zero() : dropChances;
        }
    }

    public record EquipmentDropChances(double helmet, double chestplate, double leggings, double boots,
                                       double mainHand, double offHand) {
        public static EquipmentDropChances zero() {
            return new EquipmentDropChances(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public record DropConfig(double heldItemChance, List<DropItemDefinition> items) {
        public static DropConfig empty() {
            return new DropConfig(0.0, List.of());
        }

        public DropConfig {
            heldItemChance = clampChance(heldItemChance);
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record DropItemDefinition(String material, String weaponTemplate, String itemTemplate,
                                     int amount, double chance) {
        public DropItemDefinition {
            amount = Math.max(1, amount);
            chance = clampChance(chance);
        }
    }

    public record MobPotionEffectDefinition(String type, int level, int durationTicks,
                                            boolean ambient, boolean particles) {
        public MobPotionEffectDefinition {
            level = Math.max(1, level);
        }
    }

    public record ActionDefinition(String when, String action, int hits, double chance, double durationSeconds,
                                   int level, long cooldownTicks, double speed, double damage) {
    }

    public record ScriptedMobConfig(boolean enabled, double spawnChance, String name, double health, double damage, double speedMultiplier,
                                    double detectRange, double skillRange, long skillCooldownTicks, double skillDamage,
                                    boolean bossBar) {
    }

    @FunctionalInterface
    private interface YamlFileLoader {
        void load(File file);
    }
}
