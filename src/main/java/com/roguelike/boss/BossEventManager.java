package com.roguelike.boss;

import com.roguelike.RoguelikePlugin;
import com.roguelike.mob.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BossEventManager {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());
    private static RoguelikePlugin plugin;
    private static BossEventConfig config;
    private static BossEventState state;
    private static BossEventStorage storage;
    private static BossSpawnPlanner spawnPlanner;
    private static BossStructureService structureService;
    private static BossLeashService leashService;
    private static BukkitTask tickTask;

    private BossEventManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        shutdown();
        BossEventManager.plugin = plugin;
        reload();
        plugin.getServer().getPluginManager().registerEvents(new BossArenaProtectionListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BossEventListener(), plugin);
    }

    public static void reload() {
        if (plugin == null) return;
        cancelTickTask();
        File configFile = new File(plugin.getDataFolder(), "boss-events.yml");
        config = BossEventConfig.load(configFile);
        storage = new BossEventStorage(new File(plugin.getDataFolder(), "boss-events-state.yml"));
        state = storage.load(config);
        spawnPlanner = new BossSpawnPlanner(new Random());
        structureService = new BossStructureService();
        if (leashService == null) leashService = new BossLeashService();
        leashService.start(plugin, config);
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, BossEventManager::tick, 20L * 60L, 20L * 60L);
    }

    public static void shutdown() {
        cancelTickTask();
        if (leashService != null) leashService.stop();
        if (storage != null && state != null) storage.save(state);
        plugin = null;
    }

    private static void cancelTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private static void tick() {
        if (config == null || state == null || !config.enabled()) return;
        if (state.hasActiveArena()) return;
        if (Instant.now().isBefore(state.nextBossSpawnAt())) return;
        if (!trySpawnScheduledBoss()) {
            state.setNextBossSpawnAt(Instant.now().plus(Duration.ofMinutes(30)));
            saveQuietly();
        }
    }

    public static boolean forceSpawn() {
        return trySpawnScheduledBoss();
    }

    private static boolean trySpawnScheduledBoss() {
        if (plugin == null || config == null || state == null) return false;
        if (state.hasActiveArena()) return false;
        World world = Bukkit.getWorld(config.worldName());
        if (world == null) return false;
        BossSpawnPlanner.BossSpawnPlan plan = spawnPlanner.plan(Bukkit.getOnlinePlayers(), world, config, state.activeArena());
        if (plan == null) return false;
        BossEventConfig.BossDefinition boss = chooseBoss(config.bosses());
        String id = "boss-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault()).format(Instant.now());
        ActiveBossArena arena = ActiveBossArena.active(id, world.getName(), plan.spawnLocation().getBlockX(), plan.spawnLocation().getBlockY(),
                plan.spawnLocation().getBlockZ(), config.arena().radius(), boss.mobId(), boss.structureId(), config.arena().protectBlocksWhileActive());
        var spawnLocation = structureService.generate(arena, world, plugin.getDataFolder(), boss.structure());
        LivingEntity entity = MobManager.spawnInternalMob(boss.mobId(), spawnLocation == null ? plan.spawnLocation() : spawnLocation);
        if (entity == null) return false;
        arena.setBossEntityUuid(entity.getUniqueId());
        state.setActiveArena(arena);
        state.setLastBossSpawnAt(Instant.now());
        scheduleNextBoss();
        saveQuietly();
        if (config.broadcast().onSpawn()) broadcastSpawn(arena, plan);
        return true;
    }

    public static void clearActiveArena(boolean removeEntity) {
        endActiveArena(ActiveBossArena.State.CLEARED, removeEntity);
    }

    public static void endActiveArena(ActiveBossArena.State terminalState, boolean removeEntity) {
        if (state == null) return;
        ActiveBossArena arena = state.activeArena();
        if (arena == null) return;
        if (removeEntity && arena.bossEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(arena.bossEntityUuid());
            if (entity != null) entity.remove();
        }
        arena.setState(terminalState);
        state.clearActiveArena();
        saveQuietly();
        if (config != null && config.broadcast().onDeath() && terminalState == ActiveBossArena.State.COMPLETED) {
            Bukkit.broadcast(com.roguelike.util.Message.toComponent("&6周期 Boss 已被击败，事件结束。"));
        }
    }

    public static void setNextSpawnHours(double hours) {
        if (state == null) return;
        long seconds = Math.max(1L, Math.round(hours * 3600.0));
        state.setNextBossSpawnAt(Instant.now().plusSeconds(seconds));
        saveQuietly();
    }

    public static List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&6&l═══ 周期 Boss 事件 ═══");
        lines.add("&e启用: &f" + (config != null && config.enabled()));
        lines.add("&e世界: &f" + (config == null ? "world" : config.worldName()));
        lines.add("&e下一次: &f" + (state == null ? "未知" : TIME_FORMAT.format(state.nextBossSpawnAt())));
        ActiveBossArena arena = activeArena();
        if (arena == null) {
            lines.add("&e活动事件: &7无");
        } else {
            lines.add("&e活动事件: &a" + arena.id());
            lines.add("&eBoss: &f" + arena.bossMobId() + " &7(" + (isBossAlive(arena) ? "存活" : "未找到") + ")");
            lines.add("&e区域: &f" + arena.worldName() + " " + arena.centerX() + ", " + arena.centerY() + ", " + arena.centerZ() + " &7半径 " + arena.radius());
        }
        return lines;
    }

    public static ActiveBossArena activeArena() {
        return state == null ? null : state.activeArena();
    }

    public static BossEventConfig config() {
        return config;
    }

    public static boolean hasActiveArena() {
        return state != null && state.hasActiveArena();
    }

    public static boolean isBossAlive(ActiveBossArena arena) {
        LivingEntity boss = BossLeashService.findBoss(arena);
        return boss != null && boss.isValid() && !boss.isDead();
    }

    public static List<String> configuredBossIds() {
        BossEventConfig source = config == null ? BossEventConfig.defaults() : config;
        return source.bosses().stream()
                .map(BossEventConfig.BossDefinition::id)
                .toList();
    }

    public static boolean isConfiguredBossId(String id) {
        if (id == null || id.isBlank()) return false;
        for (String bossId : configuredBossIds()) {
            if (bossId.equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    public static String configuredMobId(String bossId) {
        BossEventConfig source = config == null ? BossEventConfig.defaults() : config;
        for (BossEventConfig.BossDefinition boss : source.bosses()) {
            if (boss.id().equalsIgnoreCase(bossId)) return boss.mobId();
        }
        return bossId;
    }

    public static BossEventConfig.BossDefinition activeBossDefinition() {
        ActiveBossArena arena = activeArena();
        if (arena == null || config == null) return null;
        for (BossEventConfig.BossDefinition boss : config.bosses()) {
            if (boss.mobId().equalsIgnoreCase(arena.bossMobId()) || boss.id().equalsIgnoreCase(arena.bossMobId())) {
                return boss;
            }
        }
        return null;
    }

    private static void scheduleNextBoss() {
        state.setNextBossSpawnAt(Instant.now().plusSeconds(Math.round(config.intervalHours() * 3600.0)));
    }

    private static BossEventConfig.BossDefinition chooseBoss(List<BossEventConfig.BossDefinition> bosses) {
        int total = bosses.stream().mapToInt(BossEventConfig.BossDefinition::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int cursor = 0;
        for (BossEventConfig.BossDefinition boss : bosses) {
            cursor += boss.weight();
            if (roll < cursor) return boss;
        }
        return bosses.getFirst();
    }

    private static void broadcastSpawn(ActiveBossArena arena, BossSpawnPlanner.BossSpawnPlan plan) {
        String message = "&c周期 Boss " + arena.bossMobId() + " 已在主世界苏醒。";
        if (config.broadcast().showCoordinates()) {
            message += " &7坐标: " + arena.centerX() + ", " + arena.centerY() + ", " + arena.centerZ();
        } else if (config.broadcast().showDirectionFromAnchor() && plan.anchorPlayerName() != null) {
            message += " &7距离 " + plan.anchorPlayerName() + " 约 " + Math.round(plan.distanceFromAnchor()) + " 格。";
        }
        Bukkit.broadcast(com.roguelike.util.Message.toComponent(message));
    }

    private static void saveQuietly() {
        if (storage == null || state == null) return;
        try {
            storage.save(state);
        } catch (RuntimeException e) {
            if (plugin != null) plugin.getLogger().warning(e.getMessage());
        }
    }
}
