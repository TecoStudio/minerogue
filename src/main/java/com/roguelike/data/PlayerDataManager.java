package com.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguelike.RoguelikePlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RoguelikePlugin plugin;
    private static File dataFolder;
    private static Connection sqlite;
    private static boolean sqliteEnabled;
    private static final Map<UUID, PlayerData> cache = new HashMap<>();
    private static int saveTaskId = -1;

    public static void init(RoguelikePlugin plugin) {
        PlayerDataManager.plugin = plugin;
        dataFolder = new File(plugin.getDataFolder(), "player_data");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        sqliteEnabled = plugin.getConfig().getString("storage.type", "json").equalsIgnoreCase("sqlite");
        if (sqliteEnabled) {
            initSqlite();
        }
        long interval = Math.max(600L, plugin.getConfig().getLong("storage.save-interval", 1200L));
        saveTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, PlayerDataManager::saveAll, interval, interval);
    }

    private static void initSqlite() {
        try {
            File database = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.sqlite-file", "roguelike.db"));
            File parent = database.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            sqlite = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
            try (Statement statement = sqlite.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid TEXT PRIMARY KEY, kills INTEGER NOT NULL, deaths INTEGER NOT NULL, total_exp INTEGER NOT NULL, ticket_a_uses INTEGER NOT NULL DEFAULT 0, super_ticket_a_uses INTEGER NOT NULL DEFAULT 0, ticket_b_uses INTEGER NOT NULL DEFAULT 0, ticket_c_uses INTEGER NOT NULL DEFAULT 0, weapon_development_uses INTEGER NOT NULL DEFAULT 0)");
                addColumnIfMissing(statement, "ticket_a_uses");
                addColumnIfMissing(statement, "super_ticket_a_uses");
                addColumnIfMissing(statement, "ticket_b_uses");
                addColumnIfMissing(statement, "ticket_c_uses");
                addColumnIfMissing(statement, "weapon_development_uses");
            }
            plugin.getLogger().info("SQLite storage enabled: " + database.getAbsolutePath());
        } catch (SQLException e) {
            sqliteEnabled = false;
            plugin.getLogger().warning("SQLite 初始化失败，已回退到 JSON 存储: " + e.getMessage());
        }
    }

    private static void addColumnIfMissing(Statement statement, String column) {
        try {
            statement.executeUpdate("ALTER TABLE player_data ADD COLUMN " + column + " INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException ignored) {
        }
    }

    public static PlayerData get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> load(uuid));
    }

    private static PlayerData load(UUID uuid) {
        if (sqliteEnabled) {
            return loadSqlite(uuid);
        }
        return loadJson(uuid);
    }

    private static PlayerData loadJson(UUID uuid) {
        File file = new File(dataFolder, uuid + ".json");
        if (!file.exists()) return new PlayerData();
        try (FileReader reader = new FileReader(file)) {
            PlayerData data = GSON.fromJson(reader, PlayerData.class);
            return data != null ? data : new PlayerData();
        } catch (IOException e) {
            plugin.getLogger().warning("无法加载玩家数据: " + uuid + " - " + e.getMessage());
            return new PlayerData();
        }
    }

    private static PlayerData loadSqlite(UUID uuid) {
            try (PreparedStatement statement = sqlite.prepareStatement("SELECT kills, deaths, total_exp, ticket_a_uses, super_ticket_a_uses, ticket_b_uses, ticket_c_uses, weapon_development_uses FROM player_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return new PlayerData();
                PlayerData data = new PlayerData();
                data.setKills(result.getInt("kills"));
                data.setDeaths(result.getInt("deaths"));
                data.setTotalExp(result.getLong("total_exp"));
                for (int i = 0; i < result.getInt("ticket_a_uses"); i++) data.addTicketUse("ticket_a");
                for (int i = 0; i < result.getInt("super_ticket_a_uses"); i++) data.addTicketUse("super_ticket_a");
                for (int i = 0; i < result.getInt("ticket_b_uses"); i++) data.addTicketUse("ticket_b");
                for (int i = 0; i < result.getInt("ticket_c_uses"); i++) data.addTicketUse("ticket_c");
                for (int i = 0; i < result.getInt("weapon_development_uses"); i++) data.addTicketUse("ticket_b");
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("无法从 SQLite 加载玩家数据: " + uuid + " - " + e.getMessage());
            return new PlayerData();
        }
    }

    public static void save(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) return;
        save(player.getUniqueId(), data);
    }

    private static void save(UUID uuid, PlayerData data) {
        if (sqliteEnabled) {
            saveSqlite(uuid, data);
        } else {
            saveJson(uuid, data);
        }
    }

    private static void saveJson(UUID uuid, PlayerData data) {
        File file = new File(dataFolder, uuid + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存玩家数据: " + uuid + " - " + e.getMessage());
        }
    }

    private static void saveSqlite(UUID uuid, PlayerData data) {
        try (PreparedStatement statement = sqlite.prepareStatement("INSERT INTO player_data(uuid, kills, deaths, total_exp, ticket_a_uses, super_ticket_a_uses, ticket_b_uses, ticket_c_uses, weapon_development_uses) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET kills = excluded.kills, deaths = excluded.deaths, total_exp = excluded.total_exp, ticket_a_uses = excluded.ticket_a_uses, super_ticket_a_uses = excluded.super_ticket_a_uses, ticket_b_uses = excluded.ticket_b_uses, ticket_c_uses = excluded.ticket_c_uses, weapon_development_uses = excluded.weapon_development_uses")) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, data.getKills());
            statement.setInt(3, data.getDeaths());
            statement.setLong(4, data.getTotalExp());
            statement.setInt(5, data.getTicketAUses());
            statement.setInt(6, data.getSuperTicketAUses());
            statement.setInt(7, data.getTicketBUses());
            statement.setInt(8, data.getTicketCUses());
            statement.setInt(9, 0);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("无法保存 SQLite 玩家数据: " + uuid + " - " + e.getMessage());
        }
    }

    public static void saveAll() {
        cache.forEach(PlayerDataManager::save);
    }

    public static void unload(Player player) {
        save(player);
        cache.remove(player.getUniqueId());
    }

    public static void shutdown() {
        saveAll();
        if (saveTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        if (sqlite != null) {
            try {
                sqlite.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭 SQLite 连接失败: " + e.getMessage());
            }
        }
    }
}
