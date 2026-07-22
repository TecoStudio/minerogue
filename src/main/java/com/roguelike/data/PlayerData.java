package com.roguelike.data;

import com.roguelike.level.LevelManager;

public class PlayerData {
    private int kills;
    private int deaths;
    private long totalExp;
    private int ticketAUses;
    private int superTicketAUses;
    private int ticketBUses;
    private int ticketCUses;
    private long minedBlocks;
    private long eatenItems;
    private int cachedLevel;

    public PlayerData() {
        this.kills = 0;
        this.deaths = 0;
        this.totalExp = 0;
        this.ticketAUses = 0;
        this.superTicketAUses = 0;
        this.ticketBUses = 0;
        this.ticketCUses = 0;
        this.minedBlocks = 0;
        this.eatenItems = 0;
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

    public int getTicketAUses() {
        return ticketAUses;
    }

    public int getSuperTicketAUses() {
        return superTicketAUses;
    }

    public int getTicketBUses() {
        return ticketBUses;
    }

    public int getTicketCUses() {
        return ticketCUses;
    }

    public void setTicketAUses(int ticketAUses) {
        this.ticketAUses = Math.max(0, ticketAUses);
    }

    public void setSuperTicketAUses(int superTicketAUses) {
        this.superTicketAUses = Math.max(0, superTicketAUses);
    }

    public void setTicketBUses(int ticketBUses) {
        this.ticketBUses = Math.max(0, ticketBUses);
    }

    public void setTicketCUses(int ticketCUses) {
        this.ticketCUses = Math.max(0, ticketCUses);
    }

    public void addTicketUse(String ticketId) {
        switch (ticketId) {
            case "ticket_a" -> ticketAUses++;
            case "super_ticket_a" -> superTicketAUses++;
            case "ticket_b", "tool_ticket_b", "weapon_development" -> ticketBUses++;
            case "ticket_c" -> ticketCUses++;
            default -> {
            }
        }
    }

    public long getMinedBlocks() {
        return minedBlocks;
    }

    public void setMinedBlocks(long minedBlocks) {
        this.minedBlocks = Math.max(0, minedBlocks);
    }

    public long incrementMinedBlocks() {
        return ++minedBlocks;
    }

    public long getEatenItems() {
        return eatenItems;
    }

    public void setEatenItems(long eatenItems) {
        this.eatenItems = Math.max(0, eatenItems);
    }

    public long incrementEatenItems() {
        return ++eatenItems;
    }
}
