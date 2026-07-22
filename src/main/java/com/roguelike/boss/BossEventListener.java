package com.roguelike.boss;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public class BossEventListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveBossArena arena = BossEventManager.activeArena();
        if (arena == null || arena.bossEntityUuid() == null) return;
        if (event.getEntity().getUniqueId().equals(arena.bossEntityUuid())) {
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
        }
    }
}
