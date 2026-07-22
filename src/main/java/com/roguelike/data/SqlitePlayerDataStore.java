package com.roguelike.data;

import com.roguelike.RoguelikePlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

final class SqlitePlayerDataStore implements PlayerDataStore {
    private final RoguelikePlugin plugin;
    private final File sqliteFile;
    private final Connection connection;

    SqlitePlayerDataStore(RoguelikePlugin plugin, File sqliteFile) throws SQLException {
        this.plugin = plugin;
        this.sqliteFile = sqliteFile;
        File parent = sqliteFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
        initSchema();
        plugin.getLogger().info("SQLite storage enabled: " + sqliteFile.getAbsolutePath());
    }

    @Override
    public synchronized PlayerData load(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT kills, deaths, total_exp, ticket_a_uses, super_ticket_a_uses, ticket_b_uses, ticket_c_uses, weapon_development_uses, mined_blocks, eaten_items FROM player_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return new PlayerData();
                return readData(result);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("无法从 SQLite 加载玩家数据: " + uuid + " - " + e.getMessage());
            return new PlayerData();
        }
    }

    @Override
    public synchronized void save(UUID uuid, PlayerData data) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO player_data(uuid, kills, deaths, total_exp, ticket_a_uses, super_ticket_a_uses, ticket_b_uses, ticket_c_uses, weapon_development_uses, mined_blocks, eaten_items) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET kills = excluded.kills, deaths = excluded.deaths, total_exp = excluded.total_exp, ticket_a_uses = excluded.ticket_a_uses, super_ticket_a_uses = excluded.super_ticket_a_uses, ticket_b_uses = excluded.ticket_b_uses, ticket_c_uses = excluded.ticket_c_uses, weapon_development_uses = excluded.weapon_development_uses, mined_blocks = excluded.mined_blocks, eaten_items = excluded.eaten_items")) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, data.getKills());
            statement.setInt(3, data.getDeaths());
            statement.setLong(4, data.getTotalExp());
            statement.setInt(5, data.getTicketAUses());
            statement.setInt(6, data.getSuperTicketAUses());
            statement.setInt(7, data.getTicketBUses());
            statement.setInt(8, data.getTicketCUses());
            statement.setInt(9, 0);
            statement.setLong(10, data.getMinedBlocks());
            statement.setLong(11, data.getEatenItems());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("无法保存 SQLite 玩家数据: " + uuid + " - " + e.getMessage());
        }
    }

    @Override
    public synchronized void delete(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM player_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("无法重置 SQLite 玩家数据: " + uuid + " - " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("关闭 SQLite 连接失败: " + e.getMessage());
        }
    }

    synchronized File createSnapshot(File target) throws IOException {
        if (target.exists() && !target.delete()) throw new IOException("无法覆盖 SQLite 快照: " + target.getAbsolutePath());
        String escaped = target.getAbsolutePath().replace("'", "''");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("VACUUM INTO '" + escaped + "'");
            return target;
        } catch (SQLException e) {
            throw new IOException("无法创建 SQLite 一致性快照: " + e.getMessage(), e);
        }
    }

    File sqliteFile() {
        return sqliteFile;
    }

    private void initSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid TEXT PRIMARY KEY, kills INTEGER NOT NULL, deaths INTEGER NOT NULL, total_exp INTEGER NOT NULL, ticket_a_uses INTEGER NOT NULL DEFAULT 0, super_ticket_a_uses INTEGER NOT NULL DEFAULT 0, ticket_b_uses INTEGER NOT NULL DEFAULT 0, ticket_c_uses INTEGER NOT NULL DEFAULT 0, weapon_development_uses INTEGER NOT NULL DEFAULT 0, mined_blocks INTEGER NOT NULL DEFAULT 0, eaten_items INTEGER NOT NULL DEFAULT 0)");
            addColumnIfMissing(statement, "ticket_a_uses");
            addColumnIfMissing(statement, "super_ticket_a_uses");
            addColumnIfMissing(statement, "ticket_b_uses");
            addColumnIfMissing(statement, "ticket_c_uses");
            addColumnIfMissing(statement, "weapon_development_uses");
            addColumnIfMissing(statement, "mined_blocks");
            addColumnIfMissing(statement, "eaten_items");
        }
    }

    private void addColumnIfMissing(Statement statement, String column) {
        try {
            statement.executeUpdate("ALTER TABLE player_data ADD COLUMN " + column + " INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("duplicate column")) {
                plugin.getLogger().warning("SQLite 字段迁移失败: " + column + " - " + e.getMessage());
            }
        }
    }

    private PlayerData readData(ResultSet result) throws SQLException {
        PlayerData data = new PlayerData();
        data.setKills(result.getInt("kills"));
        data.setDeaths(result.getInt("deaths"));
        data.setTotalExp(result.getLong("total_exp"));
        data.setTicketAUses(result.getInt("ticket_a_uses"));
        data.setSuperTicketAUses(result.getInt("super_ticket_a_uses"));
        data.setTicketBUses(result.getInt("ticket_b_uses") + result.getInt("weapon_development_uses"));
        data.setTicketCUses(result.getInt("ticket_c_uses"));
        data.setMinedBlocks(result.getLong("mined_blocks"));
        data.setEatenItems(result.getLong("eaten_items"));
        return data;
    }
}
