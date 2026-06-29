package com.roguelike.mob;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.integration.IntegrationManager;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobManager {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static final String SKELETON_ELITE_ID = "skeleton_elite";
    private static final double SKELETON_ELITE_HEALTH = 30.0;
    private static final double SKELETON_ELITE_DAMAGE = 5.0;
    private static final double SKELETON_ELITE_POISON_CHANCE = 0.30;
    private static final double SKELETON_ELITE_POISONED_BONUS = 0.10;
    private static RoguelikePlugin plugin;
    private static NamespacedKey mobKey;
    private static BukkitTask behaviorTask;

    public static void init(RoguelikePlugin plugin) {
        MobManager.plugin = plugin;
        mobKey = new NamespacedKey(plugin, "internal_mob");
        behaviorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, MobManager::tickEliteSkeletons, 20L, 10L);
    }

    public static void shutdown() {
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
    }

    public static void applyToMob(LivingEntity entity) {
        if (IntegrationManager.isMythicMobsEnabled() || !ConfigManager.isInternalMonsterSystemEnabled()) return;
        if (entity instanceof Skeleton skeleton && skeleton.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            if (RANDOM.nextDouble() < ConfigManager.getSkeletonEliteSpawnChance()) {
                applySkeletonElite(skeleton);
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

        if (config.weaponTemplate() != null && entity.getEquipment() != null) {
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

    private static void applySkeletonElite(Skeleton skeleton) {
        skeleton.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, SKELETON_ELITE_ID);
        skeleton.customName(Message.toComponent("&c骷髅精英"));
        skeleton.setCustomNameVisible(true);

        var health = skeleton.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(SKELETON_ELITE_HEALTH);
            skeleton.setHealth(SKELETON_ELITE_HEALTH);
        }
        var damage = skeleton.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(SKELETON_ELITE_DAMAGE);
        }

        EntityEquipment equipment = skeleton.getEquipment();
        if (equipment == null) return;
        equipment.setHelmet(redLeatherHelmet());
        equipment.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.IRON_BOOTS));
        equipment.setItemInMainHand(rustyIronSword());
        equipment.setItemInOffHand(eliteBow());

        equipment.setHelmetDropChance(0.02f);
        equipment.setChestplateDropChance(0.02f);
        equipment.setLeggingsDropChance(0.02f);
        equipment.setBootsDropChance(0.02f);
        equipment.setItemInMainHandDropChance(0.10f);
        equipment.setItemInOffHandDropChance(0.05f);
    }

    private static ItemStack redLeatherHelmet() {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        if (helmet.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.RED);
            meta.displayName(Message.toComponent("&c骷髅精英头盔"));
            helmet.setItemMeta(meta);
        }
        return helmet;
    }

    private static ItemStack rustyIronSword() {
        CustomWeapon template = ConfigManager.getWeapon("rusty_iron_sword");
        if (template != null) {
            return WeaponManager.createWeaponStack(template, Material.IRON_SWORD);
        }

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        var meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(Message.toComponent("&7生锈的铁剑"));
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private static ItemStack eliteBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (unbreaking != null) {
            bow.addUnsafeEnchantment(unbreaking, 3);
        }
        var meta = bow.getItemMeta();
        if (meta != null) {
            meta.displayName(Message.toComponent("&7骷髅精英的弓"));
            bow.setItemMeta(meta);
        }
        return bow;
    }

    public static void handleDamage(EntityDamageByEntityEvent event) {
        LivingEntity attacker = null;
        if (event.getDamager() instanceof LivingEntity living) {
            attacker = living;
        } else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof LivingEntity shooter) {
            attacker = shooter;
        }

        if (attacker == null || !isInternalMob(attacker, SKELETON_ELITE_ID)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        double damage = SKELETON_ELITE_DAMAGE;
        boolean wasPoisoned = target.hasPotionEffect(PotionEffectType.POISON);
        if (wasPoisoned) {
            damage *= 1 + SKELETON_ELITE_POISONED_BONUS;
        }
        event.setDamage(damage);

        if (RANDOM.nextDouble() < SKELETON_ELITE_POISON_CHANCE) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
        }
    }

    private static void tickEliteSkeletons() {
        if (plugin == null || IntegrationManager.isMythicMobsEnabled() || !ConfigManager.isInternalMonsterSystemEnabled()) return;
        for (var world : plugin.getServer().getWorlds()) {
            for (Skeleton skeleton : world.getEntitiesByClass(Skeleton.class)) {
                if (!isInternalMob(skeleton, SKELETON_ELITE_ID) || skeleton.isDead()) continue;
                LivingEntity target = skeleton.getTarget();
                if (!(target instanceof Player) || target.isDead()) {
                    target = nearestPlayer(skeleton, 18.0);
                    if (target != null) skeleton.setTarget(target);
                }
                if (target == null) continue;

                double distance = skeleton.getLocation().distance(target.getLocation());
                if (distance <= 3.2) {
                    startMeleeBurst(skeleton, target);
                } else {
                    keepRangeAndShoot(skeleton, target, distance);
                }
            }
        }
    }

    private static Player nearestPlayer(Skeleton skeleton, double range) {
        Player nearest = null;
        double nearestDistance = range * range;
        for (Player player : skeleton.getWorld().getPlayers()) {
            if (player.isDead() || !player.getLocation().getWorld().equals(skeleton.getWorld())) continue;
            double distance = player.getLocation().distanceSquared(skeleton.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void keepRangeAndShoot(Skeleton skeleton, LivingEntity target, double distance) {
        if (distance < 8.0) {
            Vector away = skeleton.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.22);
            away.setY(Math.max(0.0, skeleton.getVelocity().getY()));
            skeleton.setVelocity(away);
        }

        long now = skeleton.getWorld().getGameTime();
        Long nextShot = skeleton.getPersistentDataContainer().get(new NamespacedKey(plugin, "elite_next_shot"), PersistentDataType.LONG);
        if (nextShot != null && nextShot > now) return;
        skeleton.getPersistentDataContainer().set(new NamespacedKey(plugin, "elite_next_shot"), PersistentDataType.LONG, now + 35L);

        Vector direction = target.getEyeLocation().toVector().subtract(skeleton.getEyeLocation().toVector()).normalize();
        Arrow arrow = skeleton.launchProjectile(Arrow.class, direction.multiply(1.9));
        arrow.setShooter(skeleton);
        arrow.setDamage(SKELETON_ELITE_DAMAGE);
        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 1.0f);
    }

    private static void startMeleeBurst(Skeleton skeleton, LivingEntity target) {
        long now = skeleton.getWorld().getGameTime();
        Long nextBurst = skeleton.getPersistentDataContainer().get(new NamespacedKey(plugin, "elite_next_burst"), PersistentDataType.LONG);
        if (nextBurst != null && nextBurst > now) return;
        skeleton.getPersistentDataContainer().set(new NamespacedKey(plugin, "elite_next_burst"), PersistentDataType.LONG, now + 100L);

        Vector lunge = target.getLocation().toVector().subtract(skeleton.getLocation().toVector()).normalize().multiply(0.75);
        lunge.setY(0.25);
        skeleton.setVelocity(lunge);
        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 1.35f);

        for (int i = 0; i < 3; i++) {
            int delay = 6 + i * 8;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> strikeIfClose(skeleton, target), delay);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!skeleton.isDead() && !target.isDead()) {
                Vector away = skeleton.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.65);
                away.setY(0.18);
                skeleton.setVelocity(away);
            }
        }, 34L);
    }

    private static void strikeIfClose(Skeleton skeleton, LivingEntity target) {
        if (skeleton.isDead() || target.isDead() || !skeleton.getWorld().equals(target.getWorld())) return;
        if (skeleton.getLocation().distanceSquared(target.getLocation()) > 12.25) return;

        target.damage(SKELETON_ELITE_DAMAGE, skeleton);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1.0, 0), 1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.2f);
    }

    private static boolean isInternalMob(LivingEntity entity, String id) {
        if (mobKey == null) return false;
        String value = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        return id.equals(value);
    }

    public static void handleDrop(LivingEntity entity) {
        if (isInternalMob(entity, SKELETON_ELITE_ID)) return;
        String type = entity.getType().name().toLowerCase();
        ConfigManager.MobConfig config = ConfigManager.getMobConfig(type);
        if (config == null || config.weaponTemplate() == null) return;
        if (RANDOM.nextDouble() > 0.15) return;
        CustomWeapon template = ConfigManager.getWeapon(config.weaponTemplate());
        if (template == null) return;
        ItemStack weapon = WeaponManager.createWeaponStack(template, org.bukkit.Material.IRON_SWORD);
        entity.getWorld().dropItemNaturally(entity.getLocation(), weapon);
    }
}
