package com.roguelike.boss;

import com.roguelike.RoguelikePlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

public class BossBeaconService {
    private BukkitTask task;
    private String arenaId;

    public void start(RoguelikePlugin plugin, ActiveBossArena arena, World world) {
        stop();
        if (plugin == null || arena == null || world == null || !arena.isActive()) return;
        arenaId = arena.id();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            ActiveBossArena active = BossEventManager.activeArena();
            if (active == null || !active.isActive() || !arenaId.equals(active.id())) {
                stop();
                return;
            }
            spawnRedBeam(world, active.centerLocation(world).add(0.0, 0.2, 0.0));
        }, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        arenaId = null;
    }

    private void spawnRedBeam(World world, Location base) {
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 2.5f);
        for (int y = 0; y <= 96; y++) {
            world.spawnParticle(Particle.DUST, base.clone().add(0.0, y, 0.0), 3, 0.08, 0.0, 0.08, 0.0, red, true);
        }
    }
}
