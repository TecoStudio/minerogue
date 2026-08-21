package com.roguelike.boss;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 监听对 Boss 的伤害，累计到 BossDamageTracker，并把首次造成伤害的玩家切入 Boss 侧边栏。
 * 在 MONITOR 优先级读取 getFinalDamage()，取最接近「实际造成」的数值。
 */
public class BossDamageListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!BossDamageTracker.isBossEntity(target)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        UUID bossUuid = target.getUniqueId();
        double amount = event.getFinalDamage();
        if (amount <= 0) return;

        String displayName = BossDamageTracker.bossDisplayNameFor(bossUuid, target);
        BossDamageTracker.recordDamage(bossUuid, attacker.getUniqueId(), amount, displayName);

        if (!BossDamageScoreboard.isViewing(attacker)) {
            BossDamageScoreboard.show(attacker, bossUuid, displayName);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BossDamageTracker.onPlayerQuit(event.getPlayer());
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
