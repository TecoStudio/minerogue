package com.roguelike.boss;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;

public class BossLeashService {
    private BukkitTask task;

    public void start(com.roguelike.RoguelikePlugin plugin, BossEventConfig config) {
        stop();
        if (plugin == null || config == null || !config.leash().enabled()) return;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick,
                config.leash().checkIntervalTicks(), config.leash().checkIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        BossEventConfig config = BossEventManager.config();
        ActiveBossArena arena = BossEventManager.activeArena();
        if (config == null || arena == null || !arena.isActive()) return;
        LivingEntity boss = findBoss(arena);
        if (boss == null || boss.isDead() || !boss.isValid()) {
            BossEventManager.endActiveArena(ActiveBossArena.State.EXPIRED, true);
            return;
        }
        if (arena.horizontalDistanceSquared(boss.getLocation()) <= (double) config.leash().maxDistanceFromCenter() * config.leash().maxDistanceFromCenter()) {
            return;
        }
        World world = Bukkit.getWorld(arena.worldName());
        if (world == null) return;
        boss.teleport(arena.centerLocation(world).add(0.0, 1.0, 0.0));
        if (boss instanceof Mob mob) mob.setTarget(null);
    }

    static LivingEntity findBoss(ActiveBossArena arena) {
        if (arena == null || arena.bossEntityUuid() == null) return null;
        Entity entity = Bukkit.getEntity(arena.bossEntityUuid());
        return entity instanceof LivingEntity living ? living : null;
    }
}
