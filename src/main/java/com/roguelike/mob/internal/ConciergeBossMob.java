package com.roguelike.mob.internal;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.mob.InternalMob;
import com.roguelike.mob.MobManager;
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
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;

public class ConciergeBossMob implements InternalMob {
    private static final String ID = "concierge";

    private final RoguelikePlugin plugin;
    private final NamespacedKey mobKey;
    private final NamespacedKey nextShockwaveKey;
    private final NamespacedKey nextLeapKey;

    public ConciergeBossMob(RoguelikePlugin plugin) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, "internal_mob");
        this.nextShockwaveKey = new NamespacedKey(plugin, "concierge_next_shockwave");
        this.nextLeapKey = new NamespacedKey(plugin, "concierge_next_leap");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<String> aliases() {
        return List.of("concierge_boss");
    }

    @Override
    public void onSpawn(LivingEntity entity) {
    }

    @Override
    public LivingEntity spawn(Location location) {
        Zombie boss = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        apply(boss, ConfigManager.getConciergeBossConfig());
        return boss;
    }

    private void apply(Zombie boss, ConfigManager.BossConfig config) {
        boss.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, ID);
        boss.customName(Message.toComponent(config.name()));
        boss.setCustomNameVisible(false);
        boss.setAdult();
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
        equipment.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        equipment.setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
        equipment.setHelmetDropChance(0.01f);
        equipment.setChestplateDropChance(0.01f);
        equipment.setLeggingsDropChance(0.01f);
        equipment.setBootsDropChance(0.01f);
        equipment.setItemInMainHandDropChance(0.05f);
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker) || !isMob(attacker)) return;
        if (event.getEntity() instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) {
            event.setCancelled(true);
            return;
        }
        event.setDamage(ConfigManager.getConciergeBossConfig().damage());
    }

    @Override
    public void tick() {
        ConfigManager.BossConfig config = ConfigManager.getConciergeBossConfig();
        if (!config.enabled()) return;
        for (var world : plugin.getServer().getWorlds()) {
            for (Zombie boss : world.getEntitiesByClass(Zombie.class)) {
                if (!isMob(boss) || boss.isDead()) continue;
                LivingEntity target = boss.getTarget();
                if (target instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) {
                    boss.setTarget(null);
                    target = null;
                }
                if (!(target instanceof Player) || target.isDead()) {
                    target = nearestPlayer(boss, config.detectRange());
                    if (target != null) boss.setTarget(target);
                }
                if (target == null) continue;

                double distance = boss.getLocation().distance(target.getLocation());
                if (distance <= config.skillRange()) {
                    shockwave(boss, config);
                } else if (distance <= config.detectRange()) {
                    leapToward(boss, target, config);
                }
            }
        }
    }

    private Player nearestPlayer(LivingEntity boss, double range) {
        Player nearest = null;
        double best = range * range;
        for (var entity : boss.getWorld().getNearbyEntities(boss.getLocation(), range, range, range)) {
            if (!(entity instanceof Player player) || !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) continue;
            double distance = player.getLocation().distanceSquared(boss.getLocation());
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void shockwave(Zombie boss, ConfigManager.BossConfig config) {
        long now = boss.getWorld().getGameTime();
        Long next = boss.getPersistentDataContainer().get(nextShockwaveKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        boss.getPersistentDataContainer().set(nextShockwaveKey, PersistentDataType.LONG, now + config.skillCooldownTicks());

        Location origin = boss.getLocation();
        boss.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.75f);
        boss.getWorld().spawnParticle(Particle.EXPLOSION, origin.add(0, 0.1, 0), 1);
        double radius = config.skillRange();
        for (var entity : boss.getWorld().getNearbyEntities(boss.getLocation(), radius, 1.8, radius)) {
            if (!(entity instanceof LivingEntity target) || target.equals(boss)) continue;
            if (target instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) continue;
            target.damage(config.skillDamage(), boss);
            Vector knockback = target.getLocation().toVector().subtract(boss.getLocation().toVector());
            if (knockback.lengthSquared() > 0.001) {
                target.setVelocity(knockback.normalize().multiply(0.9).setY(0.35));
            }
        }
    }

    private void leapToward(Zombie boss, LivingEntity target, ConfigManager.BossConfig config) {
        long now = boss.getWorld().getGameTime();
        Long next = boss.getPersistentDataContainer().get(nextLeapKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        boss.getPersistentDataContainer().set(nextLeapKey, PersistentDataType.LONG, now + Math.max(20L, config.skillCooldownTicks() / 2));

        Vector direction = target.getLocation().toVector().subtract(boss.getLocation().toVector());
        if (direction.lengthSquared() <= 0.001) return;
        boss.setVelocity(direction.normalize().multiply(0.85).setY(0.35));
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 0.7f, 0.8f);
    }

    @Override
    public boolean isMob(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        return MobManager.matchesInternalMobValue(this, value);
    }
}
