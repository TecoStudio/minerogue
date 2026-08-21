package com.roguelike.boss;

import com.roguelike.RoguelikePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;
import java.util.UUID;

public class BossEventListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveBossArena arena = BossEventManager.activeArena();
        if (arena == null || arena.bossEntityUuid() == null) return;
        if (event.getEntity().getUniqueId().equals(arena.bossEntityUuid())) {
            UUID bossUuid = arena.bossEntityUuid();
            String bossName = BossDamageTracker.bossDisplayNameFor(bossUuid, event.getEntity());
            BossEventConfig.BossDefinition boss = BossEventManager.activeBossDefinition();
            if (boss != null) {
                BossLootPlanner.rollDrops(boss.drops(), new Random()).forEach(drop -> {
                    var stack = com.roguelike.mob.MobManager.createConfiguredDrop(drop);
                    if (stack != null && !stack.getType().isAir()) {
                        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), stack);
                    }
                });
            }
            BossEventManager.endActiveArena(ActiveBossArena.State.COMPLETED, false);
            // 广播伤害总榜，延迟恢复侧边栏并清空记录，让玩家短暂看到终榜。
            BossDamageScoreboard.broadcastLeaderboard(bossUuid, bossName);
            Bukkit.getScheduler().runTaskLater(RoguelikePlugin.getInstance(), () -> {
                BossDamageScoreboard.restoreAll();
                BossDamageTracker.clear(bossUuid);
            }, 100L);
        }
    }
}
