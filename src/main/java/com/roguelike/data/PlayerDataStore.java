package com.roguelike.data;

import java.util.UUID;

interface PlayerDataStore {
    PlayerData load(UUID uuid);

    void save(UUID uuid, PlayerData data);

    void delete(UUID uuid);

    void close();
}
