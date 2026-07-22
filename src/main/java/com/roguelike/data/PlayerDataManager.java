package com.roguelike.data;

import com.roguelike.RoguelikePlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static RoguelikePlugin plugin;
    private static File dataFolder;
    private static File sqliteFile;
    private static PlayerDataStore store;
    private static SqlitePlayerDataStore sqliteStore;
    private static boolean sqliteEnabled;
    private static boolean asyncSaveEnabled;
    private static boolean backupEnabled;
    private static int backupKeep;
    private static long backupIntervalMinutes;
    private static final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private static int saveTaskId = -1;
    private static int backupTaskId = -1;

    public static void init(RoguelikePlugin plugin) {
        PlayerDataManager.plugin = plugin;
        dataFolder = new File(plugin.getDataFolder(), "player_data");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        sqliteEnabled = plugin.getConfig().getString("storage.type", "json").equalsIgnoreCase("sqlite");
        sqliteFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.sqlite-file", "roguelike.db"));
        asyncSaveEnabled = plugin.getConfig().getBoolean("storage.async-save", true);
        backupEnabled = plugin.getConfig().getBoolean("storage.backup.enabled", true);
        backupKeep = Math.max(1, plugin.getConfig().getInt("storage.backup.keep", 24));
        backupIntervalMinutes = Math.max(10L, plugin.getConfig().getLong("storage.backup.interval-minutes", 60L));
        if (sqliteEnabled) {
            initSqliteStore();
        } else {
            store = new JsonPlayerDataStore(plugin, dataFolder);
        }
        configureBackupService();
        startSaveTask();
        startBackupTask();
    }

    public static void reloadSettings() {
        if (plugin == null) return;
        asyncSaveEnabled = plugin.getConfig().getBoolean("storage.async-save", true);
        backupEnabled = plugin.getConfig().getBoolean("storage.backup.enabled", true);
        backupKeep = Math.max(1, plugin.getConfig().getInt("storage.backup.keep", 24));
        backupIntervalMinutes = Math.max(10L, plugin.getConfig().getLong("storage.backup.interval-minutes", 60L));
        configureBackupService();
        stopTasks();
        startSaveTask();
        startBackupTask();
    }

    private static void configureBackupService() {
        PlayerDataBackupService.configure(new PlayerDataBackupService.BackupSettings(
                plugin,
                dataFolder,
                sqliteFile,
                sqliteEnabled,
                backupKeep,
                PlayerDataManager::saveAll,
                target -> sqliteStore.createSnapshot(target)
        ));
    }

    private static void startSaveTask() {
        long interval = Math.max(600L, plugin.getConfig().getLong("storage.save-interval", 1200L));
        saveTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, PlayerDataManager::saveAllScheduled, interval, interval);
    }

    private static void initSqliteStore() {
        try {
            sqliteStore = new SqlitePlayerDataStore(plugin, sqliteFile);
            store = sqliteStore;
        } catch (SQLException e) {
            sqliteEnabled = false;
            sqliteStore = null;
            store = new JsonPlayerDataStore(plugin, dataFolder);
            plugin.getLogger().warning("SQLite 初始化失败，已回退到 JSON 存储: " + e.getMessage());
        }
    }

    public static PlayerData get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> load(uuid));
    }

    private static PlayerData load(UUID uuid) {
        return store.load(uuid);
    }

    public static void save(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) return;
        save(player.getUniqueId(), data);
    }

    private static synchronized void save(UUID uuid, PlayerData data) {
        store.save(uuid, data);
    }

    public static void saveAll() {
        Map<UUID, PlayerData> snapshot = snapshotCache();
        snapshot.forEach(PlayerDataManager::save);
    }

    private static void saveAllScheduled() {
        if (!asyncSaveEnabled) {
            saveAll();
            return;
        }
        Map<UUID, PlayerData> snapshot = snapshotCache();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> snapshot.forEach(PlayerDataManager::save));
    }

    private static Map<UUID, PlayerData> snapshotCache() {
        Map<UUID, PlayerData> snapshot = new HashMap<>();
        cache.forEach((uuid, data) -> snapshot.put(uuid, copyData(data)));
        return snapshot;
    }

    private static PlayerData copyData(PlayerData source) {
        PlayerData copy = new PlayerData();
        copy.setKills(source.getKills());
        copy.setDeaths(source.getDeaths());
        copy.setTotalExp(source.getTotalExp());
        copy.setTicketAUses(source.getTicketAUses());
        copy.setSuperTicketAUses(source.getSuperTicketAUses());
        copy.setTicketBUses(source.getTicketBUses());
        copy.setTicketCUses(source.getTicketCUses());
        copy.setMinedBlocks(source.getMinedBlocks());
        copy.setEatenItems(source.getEatenItems());
        return copy;
    }

    private static void startBackupTask() {
        if (!backupEnabled) return;
        long intervalTicks = backupIntervalMinutes * 60L * 20L;
        backupTaskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, PlayerDataManager::createBackup, intervalTicks, intervalTicks).getTaskId();
    }

    private static void stopTasks() {
        if (saveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        if (backupTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(backupTaskId);
            backupTaskId = -1;
        }
    }

    public static void createBackup() {
        PlayerDataBackupService.createBackup();
    }

    public static void unload(Player player) {
        save(player);
        cache.remove(player.getUniqueId());
    }

    public static void reset(Player player) {
        UUID uuid = player.getUniqueId();
        cache.remove(uuid);
        store.delete(uuid);
        cache.put(uuid, new PlayerData());
        save(uuid, cache.get(uuid));
    }

    public static void shutdown() {
        saveAll();
        stopTasks();
        store.close();
    }
}
