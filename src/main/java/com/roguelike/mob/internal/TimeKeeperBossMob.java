package com.roguelike.mob.internal;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.mob.InternalMob;
import com.roguelike.util.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class TimeKeeperBossMob implements InternalMob {
    private static final String ID = "time_keeper_boss";

    private final RoguelikePlugin plugin;
    private final NamespacedKey mobKey;
    private final NamespacedKey nextBlinkKey;
    private final NamespacedKey nextBladeStormKey;

    public TimeKeeperBossMob(RoguelikePlugin plugin) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, "internal_mob");
        this.nextBlinkKey = new NamespacedKey(plugin, "time_keeper_next_blink");
        this.nextBladeStormKey = new NamespacedKey(plugin, "time_keeper_next_blade_storm");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSpawn(LivingEntity entity) {
    }

    @Override
    public LivingEntity spawn(Location location) {
        Skeleton boss = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);
        apply(boss, ConfigManager.getTimeKeeperBossConfig());
        return boss;
    }

    private void apply(Skeleton boss, ConfigManager.BossConfig config) {
        boss.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, ID);
        boss.customName(Message.toComponent(config.name()));
        boss.setCustomNameVisible(false);
        boss.setRemoveWhenFarAway(false);

        var health = boss.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(config.health());
            boss.setHealth(config.health());
        }
        var damage = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(config.damage());
        }
        var speed = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * config.speedMultiplier());
        }

        EntityEquipment equipment = boss.getEquipment();
        if (equipment == null) return;
        equipment.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        equipment.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
        equipment.setItemInOffHand(new ItemStack(Material.CLOCK));
        equipment.setHelmetDropChance(0.01f);
        equipment.setChestplateDropChance(0.01f);
        equipment.setLeggingsDropChance(0.01f);
        equipment.setBootsDropChance(0.01f);
        equipment.setItemInMainHandDropChance(0.05f);
        equipment.setItemInOffHandDropChance(0.03f);
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker) || !isMob(attacker)) return;
        event.setDamage(ConfigManager.getTimeKeeperBossConfig().damage());
        if (event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 0));
        }
    }

    @Override
    public void tick() {
        ConfigManager.BossConfig config = ConfigManager.getTimeKeeperBossConfig();
        if (!config.enabled()) return;
        for (var world : plugin.getServer().getWorlds()) {
            for (Skeleton boss : world.getEntitiesByClass(Skeleton.class)) {
                if (!isMob(boss) || boss.isDead()) continue;
                LivingEntity target = boss.getTarget();
                if (!(target instanceof Player) || target.isDead()) {
                    target = nearestPlayer(boss, config.detectRange());
                    if (target != null) boss.setTarget(target);
                }
                if (target == null) continue;

                double distance = boss.getLocation().distance(target.getLocation());
                if (distance <= config.detectRange()) {
                    blinkBehind(boss, target, config);
                }
                if (distance <= config.skillRange()) {
                    bladeStorm(boss, config);
                }
            }
        }
    }

    private Player nearestPlayer(LivingEntity boss, double range) {
        Player nearest = null;
        double best = range * range;
        for (var entity : boss.getWorld().getNearbyEntities(boss.getLocation(), range, range, range)) {
            if (!(entity instanceof Player player) || player.isDead()) continue;
            double distance = player.getLocation().distanceSquared(boss.getLocation());
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void blinkBehind(Skeleton boss, LivingEntity target, ConfigManager.BossConfig config) {
        long now = boss.getWorld().getGameTime();
        Long next = boss.getPersistentDataContainer().get(nextBlinkKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        boss.getPersistentDataContainer().set(nextBlinkKey, PersistentDataType.LONG, now + Math.max(30L, config.skillCooldownTicks()));

        Vector facing = target.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() <= 0.001) return;
        Location destination = target.getLocation().subtract(facing.normalize().multiply(2.2));
        destination.setY(target.getLocation().getY());
        if (!destination.getBlock().isPassable()) return;

        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0, 1, 0), 24, 0.3, 0.6, 0.3);
        boss.teleport(destination);
        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0, 1, 0), 24, 0.3, 0.6, 0.3);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
    }

    private void bladeStorm(Skeleton boss, ConfigManager.BossConfig config) {
        long now = boss.getWorld().getGameTime();
        Long next = boss.getPersistentDataContainer().get(nextBladeStormKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        boss.getPersistentDataContainer().set(nextBladeStormKey, PersistentDataType.LONG, now + Math.max(40L, config.skillCooldownTicks() / 2));

        double radius = config.skillRange();
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
        boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, boss.getLocation().add(0, 1, 0), 8, radius / 2, 0.2, radius / 2);
        for (var entity : boss.getWorld().getNearbyEntities(boss.getLocation(), radius, 2.0, radius)) {
            if (!(entity instanceof LivingEntity target) || target.equals(boss)) continue;
            target.damage(config.skillDamage(), boss);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
    }

    @Override
    public boolean isMob(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        return ID.equals(value);
    }
}
