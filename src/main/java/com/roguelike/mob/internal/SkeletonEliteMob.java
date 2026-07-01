package com.roguelike.mob.internal;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.mob.InternalMob;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SkeletonEliteMob implements InternalMob {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static final String ID = "skeleton_elite";

    private final RoguelikePlugin plugin;
    private final NamespacedKey mobKey;
    private final NamespacedKey nextShotKey;
    private final NamespacedKey nextBurstKey;

    public SkeletonEliteMob(RoguelikePlugin plugin) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, "internal_mob");
        this.nextShotKey = new NamespacedKey(plugin, "elite_next_shot");
        this.nextBurstKey = new NamespacedKey(plugin, "elite_next_burst");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSpawn(LivingEntity entity) {
        ConfigManager.SkeletonEliteConfig config = ConfigManager.getSkeletonEliteConfig();
        if (!config.enabled()) return;
        if (!(entity instanceof Skeleton skeleton)) return;
        if (skeleton.getEntitySpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (RANDOM.nextDouble() >= config.spawnChance()) return;

        apply(skeleton, config);
    }

    @Override
    public LivingEntity spawn(Location location) {
        Skeleton skeleton = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);
        apply(skeleton, ConfigManager.getSkeletonEliteConfig());
        return skeleton;
    }

    private void apply(Skeleton skeleton, ConfigManager.SkeletonEliteConfig config) {
        skeleton.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, ID);
        skeleton.customName(Message.toComponent(config.name()));
        skeleton.setCustomNameVisible(true);

        var health = skeleton.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(config.health());
            skeleton.setHealth(config.health());
        }
        var damage = skeleton.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(config.damage());
        }

        EntityEquipment equipment = skeleton.getEquipment();
        if (equipment == null) return;
        equipment.setHelmet(redLeatherHelmet());
        equipment.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.IRON_BOOTS));
        equipment.setItemInMainHand(configuredSword(config.weaponTemplate()));
        equipment.setItemInOffHand(eliteBow());

        equipment.setHelmetDropChance(0.02f);
        equipment.setChestplateDropChance(0.02f);
        equipment.setLeggingsDropChance(0.02f);
        equipment.setBootsDropChance(0.02f);
        equipment.setItemInMainHandDropChance(0.10f);
        equipment.setItemInOffHandDropChance(0.05f);
    }

    private ItemStack redLeatherHelmet() {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        if (helmet.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.RED);
            meta.displayName(Message.toComponent("&c骷髅精英头盔"));
            helmet.setItemMeta(meta);
        }
        return helmet;
    }

    private ItemStack configuredSword(String weaponTemplate) {
        CustomWeapon template = ConfigManager.getWeapon(weaponTemplate);
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

    private ItemStack eliteBow() {
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

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        LivingEntity attacker = null;
        if (event.getDamager() instanceof LivingEntity living) {
            attacker = living;
        } else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof LivingEntity shooter) {
            attacker = shooter;
        }

        if (attacker == null || !isMob(attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ConfigManager.SkeletonEliteConfig config = ConfigManager.getSkeletonEliteConfig();
        double damage = config.damage();
        if (target.hasPotionEffect(PotionEffectType.POISON)) {
            damage *= 1 + config.poisonedDamageBonus();
        }
        event.setDamage(damage);

        if (RANDOM.nextDouble() < config.poisonChance()) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (config.poisonDurationSeconds() * 20), 0));
        }
    }

    @Override
    public void tick() {
        ConfigManager.SkeletonEliteConfig config = ConfigManager.getSkeletonEliteConfig();
        if (!config.enabled()) return;

        for (var world : plugin.getServer().getWorlds()) {
            for (Skeleton skeleton : world.getEntitiesByClass(Skeleton.class)) {
                if (!isMob(skeleton) || skeleton.isDead()) continue;
                LivingEntity target = skeleton.getTarget();
                if (!(target instanceof Player player) || target.isDead()) continue;
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                    skeleton.setTarget(null);
                    continue;
                }
                if (target == null) continue;

                double distance = skeleton.getLocation().distance(target.getLocation());
                if (distance <= config.meleeRange()) {
                    startMeleeBurst(skeleton, target, config);
                } else {
                    keepRangeAndShoot(skeleton, target, distance, config);
                }
            }
        }
    }

    private void keepRangeAndShoot(Skeleton skeleton, LivingEntity target, double distance, ConfigManager.SkeletonEliteConfig config) {
        if (distance < config.keepDistance()) {
            Vector away = skeleton.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(config.retreatSpeed());
            away.setY(Math.max(0.0, skeleton.getVelocity().getY()));
            skeleton.setVelocity(away);
        }

        long now = skeleton.getWorld().getGameTime();
        Long nextShot = skeleton.getPersistentDataContainer().get(nextShotKey, PersistentDataType.LONG);
        if (nextShot != null && nextShot > now) return;
        skeleton.getPersistentDataContainer().set(nextShotKey, PersistentDataType.LONG, now + config.shotCooldownTicks());

        Vector direction = target.getEyeLocation().toVector().subtract(skeleton.getEyeLocation().toVector()).normalize();
        Arrow arrow = skeleton.launchProjectile(Arrow.class, direction.multiply(config.arrowSpeed()));
        arrow.setShooter(skeleton);
        arrow.setDamage(config.damage());
        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 1.0f);
    }

    private void startMeleeBurst(Skeleton skeleton, LivingEntity target, ConfigManager.SkeletonEliteConfig config) {
        long now = skeleton.getWorld().getGameTime();
        Long nextBurst = skeleton.getPersistentDataContainer().get(nextBurstKey, PersistentDataType.LONG);
        if (nextBurst != null && nextBurst > now) return;
        skeleton.getPersistentDataContainer().set(nextBurstKey, PersistentDataType.LONG, now + config.burstCooldownTicks());

        Vector lunge = target.getLocation().toVector().subtract(skeleton.getLocation().toVector()).normalize().multiply(config.lungeSpeed());
        lunge.setY(0.25);
        skeleton.setVelocity(lunge);
        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 1.35f);

        for (int i = 0; i < 3; i++) {
            int delay = 6 + i * 8;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> strikeIfClose(skeleton, target, config), delay);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!skeleton.isDead() && !target.isDead()) {
                Vector away = skeleton.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(config.postBurstRetreatSpeed());
                away.setY(0.18);
                skeleton.setVelocity(away);
            }
        }, 34L);
    }

    private void strikeIfClose(Skeleton skeleton, LivingEntity target, ConfigManager.SkeletonEliteConfig config) {
        if (skeleton.isDead() || target.isDead() || !skeleton.getWorld().equals(target.getWorld())) return;
        double range = Math.max(config.meleeRange(), 3.5);
        if (skeleton.getLocation().distanceSquared(target.getLocation()) > range * range) return;

        target.damage(config.damage(), skeleton);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1.0, 0), 1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.2f);
    }

    @Override
    public boolean isMob(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        return ID.equals(value);
    }
}
