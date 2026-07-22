package com.roguelike.boss;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class BossEventListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveBossArena arena = BossEventManager.activeArena();
        if (arena == null || arena.bossEntityUuid() == null) return;
        if (event.getEntity().getUniqueId().equals(arena.bossEntityUuid())) {
            BossEventManager.endActiveArena(ActiveBossArena.State.COMPLETED, false);
        }
    }
}
