package com.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguelike.RoguelikePlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

final class JsonPlayerDataStore implements PlayerDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final RoguelikePlugin plugin;
    private final File dataFolder;

    JsonPlayerDataStore(RoguelikePlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;
    }

    @Override
    public PlayerData load(UUID uuid) {
        File file = file(uuid);
        if (!file.exists()) return new PlayerData();
        try (FileReader reader = new FileReader(file)) {
            PlayerData data = GSON.fromJson(reader, PlayerData.class);
            return data != null ? data : new PlayerData();
        } catch (IOException e) {
            plugin.getLogger().warning("无法加载玩家数据: " + uuid + " - " + e.getMessage());
            return new PlayerData();
        }
    }

    @Override
    public synchronized void save(UUID uuid, PlayerData data) {
        try (FileWriter writer = new FileWriter(file(uuid))) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存玩家数据: " + uuid + " - " + e.getMessage());
        }
    }

    @Override
    public synchronized void delete(UUID uuid) {
        File file = file(uuid);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("无法删除玩家数据文件: " + file.getAbsolutePath());
        }
    }

    @Override
    public void close() {
    }

    private File file(UUID uuid) {
        return new File(dataFolder, uuid + ".json");
    }
}
