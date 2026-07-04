package com.roguelike.mob;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.integration.IntegrationManager;
import com.roguelike.mob.internal.SkeletonEliteMob;
import com.roguelike.mob.internal.SpiderEliteMob;
import com.roguelike.mob.internal.ZombieEliteMob;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobManager {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static RoguelikePlugin plugin;
    private static final List<InternalMob> internalMobs = new ArrayList<>();
    private static BukkitTask behaviorTask;

    public static void init(RoguelikePlugin plugin) {
        MobManager.plugin = plugin;
        internalMobs.clear();
        internalMobs.add(new SkeletonEliteMob(plugin));
        internalMobs.add(new ZombieEliteMob(plugin));
        internalMobs.add(new SpiderEliteMob(plugin));
        behaviorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, MobManager::tickInternalMobs, 20L, 10L);
    }

    public static void shutdown() {
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
    }

    public static void applyToMob(LivingEntity entity) {
        if (IntegrationManager.isMythicMobsEnabled() || !ConfigManager.isInternalMonsterSystemEnabled()) return;
        for (InternalMob mob : internalMobs) {
            mob.onSpawn(entity);
            if (mob.isMob(entity)) {
                return;
            }
        }

        String type = entity.getType().name().toLowerCase();
        ConfigManager.MobConfig config = ConfigManager.getMobConfig(type);
        if (config == null) return;

        if (config.healthMultiplier() != 1.0) {
            var attr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                double newMax = attr.getBaseValue() * config.healthMultiplier();
                attr.setBaseValue(newMax);
                entity.setHealth(newMax);
            }
        }

        if (config.damageMultiplier() != 1.0) {
            var attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attr != null) {
                attr.setBaseValue(attr.getBaseValue() * config.damageMultiplier());
            }
        }

        if (config.speedMultiplier() != 1.0) {
            var attr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attr != null) {
                attr.setBaseValue(attr.getBaseValue() * config.speedMultiplier());
            }
        }

        if (!"zombie".equals(type) && config.weaponTemplate() != null && entity.getEquipment() != null) {
            CustomWeapon template = ConfigManager.getWeapon(config.weaponTemplate());
            if (template != null) {
                ItemStack weapon = WeaponManager.createWeaponStack(template, org.bukkit.Material.IRON_SWORD);
                entity.getEquipment().setItemInMainHand(weapon);
                if (entity instanceof Monster monster) {
                    entity.getEquipment().setItemInMainHandDropChance(0.15f);
                }
            }
        }
    }

    public static void handleDamage(EntityDamageByEntityEvent event) {
        if (IntegrationManager.isMythicMobsEnabled() || !ConfigManager.isInternalMonsterSystemEnabled()) return;
        for (InternalMob mob : internalMobs) {
            mob.onDamage(event);
        }
    }

    public static List<String> getSpawnableMobIds() {
        List<String> ids = new ArrayList<>();
        for (InternalMob mob : internalMobs) {
            ids.add(mob.id());
        }
        return ids;
    }

    public static LivingEntity spawnInternalMob(String id, Location location) {
        for (InternalMob mob : internalMobs) {
            if (mob.id().equalsIgnoreCase(id)) {
                return mob.spawn(location);
            }
        }
        return null;
    }

    private static void tickInternalMobs() {
        if (plugin == null || IntegrationManager.isMythicMobsEnabled() || !ConfigManager.isInternalMonsterSystemEnabled()) return;
        for (InternalMob mob : internalMobs) {
            mob.tick();
        }
    }

    public static void handleDrop(LivingEntity entity) {
        handleRandomWeaponDrop(entity);
        for (InternalMob mob : internalMobs) {
            if (mob.isMob(entity)) return;
        }
        String type = entity.getType().name().toLowerCase();
        ConfigManager.MobConfig config = ConfigManager.getMobConfig(type);
        if (config == null || config.weaponTemplate() == null) return;
        if (RANDOM.nextDouble() > 0.15) return;
        CustomWeapon template = ConfigManager.getWeapon(config.weaponTemplate());
        if (template == null) return;
        ItemStack weapon = WeaponManager.createWeaponStack(template, org.bukkit.Material.IRON_SWORD);
        entity.getWorld().dropItemNaturally(entity.getLocation(), weapon);
    }

    private static void handleRandomWeaponDrop(LivingEntity entity) {
        if (!(entity instanceof Monster)) return;
        String rarity = rollWeaponRarity();
        if (rarity == null) return;

        List<CustomWeapon> candidates = new ArrayList<>();
        for (CustomWeapon weapon : ConfigManager.getWeapons()) {
            if ("special".equalsIgnoreCase(weapon.getRarity())) continue;
            if (rarity.equalsIgnoreCase(weapon.getRarity())) candidates.add(weapon);
        }
        if (candidates.isEmpty()) return;

        CustomWeapon template = candidates.get(RANDOM.nextInt(candidates.size()));
        ItemStack weapon = WeaponManager.createWeaponStack(template, null);
        entity.getWorld().dropItemNaturally(entity.getLocation(), weapon);
    }

    private static String rollWeaponRarity() {
        if (RANDOM.nextDouble() < 0.002) return "legendary";
        if (RANDOM.nextDouble() < 0.005) return "epic";
        if (RANDOM.nextDouble() < 0.010) return "rare";
        if (RANDOM.nextDouble() < 0.020) return "common";
        return null;
    }
}
