package com.roguelike.mob;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface InternalMob {
    String id();

    void onSpawn(LivingEntity entity);

    LivingEntity spawn(Location location);

    void onDamage(EntityDamageByEntityEvent event);

    void tick();

    boolean isMob(LivingEntity entity);
}
