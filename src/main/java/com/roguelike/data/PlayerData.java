package com.roguelike.data;

import com.roguelike.level.LevelManager;

public class PlayerData {
    private int kills;
    private int deaths;
    private long totalExp;
    private int cachedLevel;

    public PlayerData() {
        this.kills = 0;
        this.deaths = 0;
        this.totalExp = 0;
        recalculateLevel();
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKill() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void addDeath() {
        this.deaths++;
    }

    public long getTotalExp() {
        return totalExp;
    }

    public void setTotalExp(long totalExp) {
        this.totalExp = Math.max(0, totalExp);
        recalculateLevel();
    }

    public void addExp(long amount) {
        if (amount <= 0) return;
        this.totalExp += amount;
        recalculateLevel();
    }

    public int getLevel() {
        return cachedLevel;
    }

    private void recalculateLevel() {
        this.cachedLevel = LevelManager.calculateLevel(totalExp);
    }

    public long getExpForCurrentLevel() {
        return totalExp - LevelManager.totalExpForLevel(cachedLevel);
    }

    public long getExpToNextLevel() {
        return LevelManager.expToNextLevel(totalExp);
    }
}
