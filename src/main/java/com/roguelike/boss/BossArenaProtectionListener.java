package com.roguelike.boss;

import com.roguelike.util.Message;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class BossArenaProtectionListener implements Listener {
    private static final String PROTECTED_MESSAGE = "&cBoss 区域受事件力量保护，无法破坏。";

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        BossEventConfig config = BossEventManager.config();
        if (config == null || !config.arena().blockBreak()) return;
        if (isProtected(event.getBlock())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), PROTECTED_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        BossEventConfig config = BossEventManager.config();
        if (config == null || !config.arena().blockPlace()) return;
        if (isProtected(event.getBlock())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), PROTECTED_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        BossEventConfig config = BossEventManager.config();
        if (config == null || !config.arena().blockBuckets()) return;
        if (isProtected(event.getBlock())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), PROTECTED_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        BossEventConfig config = BossEventManager.config();
        if (config == null || !config.arena().blockBuckets()) return;
        if (isProtected(event.getBlock())) {
            event.setCancelled(true);
            Message.send(event.getPlayer(), PROTECTED_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        BossEventConfig config = BossEventManager.config();
        if (config == null || !config.arena().blockExplosions()) return;
        event.blockList().removeIf(BossArenaProtectionListener::isProtected);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        BossEventConfig config = BossEventManager.config();
        if (config == null || !config.arena().blockExplosions()) return;
        event.blockList().removeIf(BossArenaProtectionListener::isProtected);
    }

    static boolean isProtected(Block block) {
        ActiveBossArena arena = BossEventManager.activeArena();
        return arena != null && arena.isActive() && arena.protectedWhileActive() && arena.contains(block);
    }
}
