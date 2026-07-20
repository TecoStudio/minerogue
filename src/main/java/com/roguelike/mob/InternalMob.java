package com.roguelike.mob;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

public interface InternalMob {
    String id();

    default List<String> aliases() {
        return List.of();
    }

    default boolean spawnable() {
        return true;
    }

    void onSpawn(LivingEntity entity);

    LivingEntity spawn(Location location);

    void onDamage(EntityDamageByEntityEvent event);

    default void onDeath(LivingEntity entity) {
    }

    void tick();

    default void shutdown() {
    }

    boolean isMob(LivingEntity entity);
}
