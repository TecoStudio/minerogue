package com.roguelike.boss;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record BossEventConfig(
        boolean enabled,
        String worldName,
        double intervalHours,
        boolean requireOnlinePlayer,
        Spawn spawn,
        Arena arena,
        Leash leash,
        Broadcast broadcast,
        List<BossDefinition> bosses
) {
    public BossEventConfig {
        worldName = blankDefault(worldName, "world");
        intervalHours = Math.max(0.01, intervalHours);
        spawn = spawn == null ? Spawn.defaults() : spawn;
        arena = arena == null ? Arena.defaults() : arena;
        leash = leash == null ? Leash.defaults() : leash;
        broadcast = broadcast == null ? Broadcast.defaults() : broadcast;
        bosses = bosses == null || bosses.isEmpty() ? defaultBosses() : List.copyOf(bosses);
    }

    public long intervalTicks() {
        return Math.max(1L, Math.round(intervalHours * 60.0 * 60.0 * 20.0));
    }

    public static BossEventConfig defaults() {
        return new BossEventConfig(true, "world", 48.0, true,
                Spawn.defaults(), Arena.defaults(), Leash.defaults(), Broadcast.defaults(), defaultBosses());
    }

    public static BossEventConfig load(File file) {
        ensureDefaultFile(file);
        return fromYaml(YamlConfiguration.loadConfiguration(file));
    }

    static BossEventConfig fromYaml(YamlConfiguration yaml) {
        ConfigurationSection root = yaml.getConfigurationSection("boss-events");
        if (root == null) root = yaml;
        Spawn spawn = new Spawn(
                root.getInt("spawn.min-distance-chunks", 3),
                root.getInt("spawn.max-distance-blocks", 192),
                root.getInt("spawn.max-attempts", 32),
                root.getInt("spawn.avoid-spawn-radius-blocks", 128)
        );
        Arena arena = new Arena(
                root.getInt("arena.radius", 32),
                root.getBoolean("arena.protect-blocks-while-active", true),
                root.getBoolean("arena.block-break", true),
                root.getBoolean("arena.block-place", true),
                root.getBoolean("arena.block-explosions", true),
                root.getBoolean("arena.block-buckets", true),
                root.getBoolean("arena.keep-structure-after-death", true)
        );
        Leash leash = new Leash(
                root.getBoolean("leash.enabled", true),
                root.getLong("leash.check-interval-ticks", 20L),
                root.getInt("leash.max-distance-from-center", 32),
                root.getInt("leash.teleport-back-distance", 40)
        );
        Broadcast broadcast = new Broadcast(
                root.getBoolean("broadcast.on-spawn", true),
                root.getBoolean("broadcast.on-death", true),
                root.getBoolean("broadcast.show-coordinates", false),
                root.getBoolean("broadcast.show-direction-from-anchor", true)
        );
        return new BossEventConfig(
                root.getBoolean("enabled", true),
                root.getString("world", "world"),
                root.getDouble("interval-hours", 48.0),
                root.getBoolean("require-online-player", true),
                spawn,
                arena,
                leash,
                broadcast,
                readBosses(root)
        );
    }

    static void writeDefault(File file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().header("Roguelike 周期 Boss 事件配置。修改后使用 /rw reload 重载。");
        yaml.set("boss-events.enabled", true);
        yaml.set("boss-events.world", "world");
        yaml.set("boss-events.interval-hours", 48.0);
        yaml.set("boss-events.require-online-player", true);
        yaml.set("boss-events.spawn.min-distance-chunks", 3);
        yaml.set("boss-events.spawn.max-distance-blocks", 192);
        yaml.set("boss-events.spawn.max-attempts", 32);
        yaml.set("boss-events.spawn.avoid-spawn-radius-blocks", 128);
        yaml.set("boss-events.arena.radius", 32);
        yaml.set("boss-events.arena.protect-blocks-while-active", true);
        yaml.set("boss-events.arena.block-break", true);
        yaml.set("boss-events.arena.block-place", true);
        yaml.set("boss-events.arena.block-explosions", true);
        yaml.set("boss-events.arena.block-buckets", true);
        yaml.set("boss-events.arena.keep-structure-after-death", true);
        yaml.set("boss-events.leash.enabled", true);
        yaml.set("boss-events.leash.check-interval-ticks", 20L);
        yaml.set("boss-events.leash.max-distance-from-center", 32);
        yaml.set("boss-events.leash.teleport-back-distance", 40);
        yaml.set("boss-events.broadcast.on-spawn", true);
        yaml.set("boss-events.broadcast.on-death", true);
        yaml.set("boss-events.broadcast.show-coordinates", false);
        yaml.set("boss-events.broadcast.show-direction-from-anchor", true);
        List<java.util.Map<String, Object>> bosses = new ArrayList<>();
        bosses.add(defaultBossMap("blood-zombie", 60, "blood_altar", List.of(
                Map.of("material", "minecraft:diamond", "amount", 1, "chance", 0.35),
                Map.of("weapon-template", "crimson_oath", "amount", 1, "chance", 0.05)
        )));
        bosses.add(defaultBossMap("vagrant", 40, "bone_ruins", List.of(
                Map.of("item-template", "greater_healing_potion", "amount", 2, "chance", 0.45),
                Map.of("weapon-template", "echo_blade", "amount", 1, "chance", 0.05)
        )));
        yaml.set("boss-events.bosses", bosses);
        Files.createDirectories(file.toPath().getParent());
        yaml.save(file);
    }

    private static Map<String, Object> defaultBossMap(String id, int weight, String structureId, List<Map<String, Object>> loot) {
        Map<String, Object> boss = new LinkedHashMap<>();
        boss.put("id", id);
        boss.put("weight", weight);
        boss.put("mob", id);
        Map<String, Object> structure = new LinkedHashMap<>();
        structure.put("type", "builtin");
        structure.put("id", structureId);
        boss.put("structure", structure);
        Map<String, Object> drops = new LinkedHashMap<>();
        drops.put("items", loot);
        boss.put("drops", drops);
        return boss;
    }

    private static void ensureDefaultFile(File file) {
        if (file.exists()) return;
        try {
            writeDefault(file);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建 boss-events.yml: " + e.getMessage(), e);
        }
    }

    private static List<BossDefinition> readBosses(ConfigurationSection root) {
        List<BossDefinition> result = new ArrayList<>();
        for (Object value : root.getList("bosses", List.of())) {
            if (value instanceof java.util.Map<?, ?> map) {
                String id = String.valueOf(map.containsKey("id") ? map.get("id") : "");
                int weight = parseInt(map.get("weight"), 1);
                String mobId = String.valueOf(map.containsKey("mob") ? map.get("mob") : id);
                StructureDefinition structure = parseStructure(map.get("structure"));
                DropConfig drops = parseDropConfig(map);
                if (!id.isBlank()) result.add(new BossDefinition(id, weight, mobId, structure, drops));
            }
        }
        return result;
    }

    private static StructureDefinition parseStructure(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            String type = stringValue(map.get("type"), "builtin");
            String id = stringValue(map.containsKey("id") ? map.get("id") : map.get("name"), "blood_altar");
            String file = stringValue(map.get("file"), "");
            String rotation = stringValue(map.get("rotation"), "none");
            Offset offset = parseOffset(map.get("offset"));
            return new StructureDefinition(type, id, file, offset, rotation);
        }
        return StructureDefinition.builtin(stringValue(raw, "blood_altar"));
    }

    private static Offset parseOffset(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return new Offset(parseInt(map.get("x"), 0), parseInt(map.get("y"), 0), parseInt(map.get("z"), 0));
        }
        return Offset.zero();
    }

    private static DropConfig parseDropConfig(Map<?, ?> boss) {
        Object raw = boss.containsKey("drops") ? boss.get("drops") : boss.get("loot");
        if (!(raw instanceof Map<?, ?> drops)) return DropConfig.empty();
        List<DropItemDefinition> items = new ArrayList<>();
        Object rawItems = drops.get("items");
        if (rawItems instanceof List<?> list) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> map)) continue;
                String material = stringValue(map.get("material"), null);
                String weaponTemplate = stringValue(map.get("weapon-template"), null);
                String itemTemplate = stringValue(map.get("item-template"), null);
                int amount = parseInt(map.get("amount"), 1);
                double chance = parseDouble(map.get("chance"), 0.0);
                if (chance > 0.0 && (isPresent(material) || isPresent(weaponTemplate) || isPresent(itemTemplate))) {
                    items.add(new DropItemDefinition(material, weaponTemplate, itemTemplate, amount, chance));
                }
            }
        }
        return new DropConfig(items);
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<BossDefinition> defaultBosses() {
        return List.of(
                new BossDefinition("blood-zombie", 60, "blood-zombie", StructureDefinition.builtin("blood_altar"), DropConfig.empty()),
                new BossDefinition("vagrant", 40, "vagrant", StructureDefinition.builtin("bone_ruins"), DropConfig.empty())
        );
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) return fallback;
        String string = String.valueOf(value);
        return string.isBlank() ? fallback : string;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    public record Spawn(int minDistanceChunks, int maxDistanceBlocks, int maxAttempts, int avoidSpawnRadiusBlocks) {
        public Spawn {
            minDistanceChunks = Math.max(1, minDistanceChunks);
            maxDistanceBlocks = Math.max(minDistanceChunks * 16, maxDistanceBlocks);
            maxAttempts = Math.max(1, maxAttempts);
            avoidSpawnRadiusBlocks = Math.max(0, avoidSpawnRadiusBlocks);
        }
        public int minDistanceBlocks() {
            return minDistanceChunks * 16;
        }
        static Spawn defaults() {
            return new Spawn(3, 192, 32, 128);
        }
    }

    public record Arena(int radius, boolean protectBlocksWhileActive, boolean blockBreak, boolean blockPlace,
                        boolean blockExplosions, boolean blockBuckets, boolean keepStructureAfterDeath) {
        public Arena { radius = Math.max(4, radius); }
        static Arena defaults() { return new Arena(32, true, true, true, true, true, true); }
    }

    public record Leash(boolean enabled, long checkIntervalTicks, int maxDistanceFromCenter, int teleportBackDistance) {
        public Leash {
            checkIntervalTicks = Math.max(1L, checkIntervalTicks);
            maxDistanceFromCenter = Math.max(1, maxDistanceFromCenter);
            teleportBackDistance = Math.max(maxDistanceFromCenter, teleportBackDistance);
        }
        static Leash defaults() { return new Leash(true, 20L, 32, 40); }
    }

    public record Broadcast(boolean onSpawn, boolean onDeath, boolean showCoordinates, boolean showDirectionFromAnchor) {
        static Broadcast defaults() { return new Broadcast(true, true, false, true); }
    }

    public record BossDefinition(String id, int weight, String mobId, StructureDefinition structure, DropConfig drops) {
        public BossDefinition {
            id = blankDefault(id, "blood-zombie");
            weight = Math.max(1, weight);
            mobId = blankDefault(mobId, id);
            structure = structure == null ? StructureDefinition.builtin("blood_altar") : structure;
            drops = drops == null ? DropConfig.empty() : drops;
        }

        public String structureId() {
            return structure.id();
        }
    }

    public record StructureDefinition(String type, String id, String file, Offset offset, String rotation) {
        public StructureDefinition {
            type = blankDefault(type, "builtin").toLowerCase(Locale.ROOT);
            id = blankDefault(id, "blood_altar");
            file = file == null ? "" : file;
            offset = offset == null ? Offset.zero() : offset;
            rotation = blankDefault(rotation, "none");
        }

        static StructureDefinition builtin(String id) {
            return new StructureDefinition("builtin", id, "", Offset.zero(), "none");
        }
    }

    public record Offset(int x, int y, int z) {
        static Offset zero() { return new Offset(0, 0, 0); }
    }

    public record DropConfig(List<DropItemDefinition> items) {
        static DropConfig empty() { return new DropConfig(List.of()); }
        public DropConfig { items = items == null ? List.of() : List.copyOf(items); }
    }

    public record DropItemDefinition(String material, String weaponTemplate, String itemTemplate, int amount, double chance) {
        public DropItemDefinition {
            amount = Math.max(1, amount);
            chance = Math.max(0.0, Math.min(1.0, chance));
        }
    }
}
