package com.roguelike.boss;

import java.time.Instant;

public class BossEventState {
    private Instant nextBossSpawnAt;
    private Instant lastBossSpawnAt;
    private ActiveBossArena activeArena;

    public BossEventState(Instant nextBossSpawnAt, Instant lastBossSpawnAt, ActiveBossArena activeArena) {
        this.nextBossSpawnAt = nextBossSpawnAt == null ? Instant.now() : nextBossSpawnAt;
        this.lastBossSpawnAt = lastBossSpawnAt;
        this.activeArena = activeArena;
    }

    public static BossEventState initial(BossEventConfig config) {
        return new BossEventState(Instant.now().plusSeconds(Math.round(config.intervalHours() * 3600.0)), null, null);
    }

    public boolean hasActiveArena() {
        return activeArena != null && activeArena.isActive();
    }

    public Instant nextBossSpawnAt() { return nextBossSpawnAt; }
    public Instant lastBossSpawnAt() { return lastBossSpawnAt; }
    public ActiveBossArena activeArena() { return activeArena; }

    public void setNextBossSpawnAt(Instant nextBossSpawnAt) { this.nextBossSpawnAt = nextBossSpawnAt; }
    public void setLastBossSpawnAt(Instant lastBossSpawnAt) { this.lastBossSpawnAt = lastBossSpawnAt; }
    public void setActiveArena(ActiveBossArena activeArena) { this.activeArena = activeArena; }
    public void clearActiveArena() { this.activeArena = null; }
}
