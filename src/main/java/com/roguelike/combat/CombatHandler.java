package com.roguelike.combat;

import com.roguelike.RoguelikePlugin;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CombatHandler {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static boolean internalDamage = false;
    private static RoguelikePlugin plugin;

    public static void init(RoguelikePlugin plugin) {
        CombatHandler.plugin = plugin;
    }

    public static boolean isInternalDamage() {
        return internalDamage;
    }

    public static double processAttack(Player player, LivingEntity target, double baseDamage) {
        CustomWeapon template = WeaponManager.getTemplate(player.getInventory().getItemInMainHand());
        WeaponInstanceData data = WeaponInstanceData.fromItemStack(player.getInventory().getItemInMainHand());
        if (template == null || data == null) return baseDamage;

        double damage = data.getTotalDamage(template);

        // 暴击
        double critChance = data.getTotalEffect(template, "crit_chance", 0.0);
        double critDamage = data.getTotalEffect(template, "crit_damage", 1.5);
        boolean crit = critChance > 0 && RANDOM.nextDouble() < critChance;
        if (crit) {
            damage *= critDamage;
            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            player.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 10, 0.3, 0.3, 0.3);
            Message.action(player, "&c&l暴击！");
        }

        // 伤害存储爆发
        double storePercent = data.getTotalEffect(template, "damage_store_percent", 0.0);
        double storeMax = data.getTotalEffect(template, "damage_store_max", 0.0);
        if (storePercent > 0 && storeMax > 0) {
            data.addStoredDamage(damage * storePercent);
            if (data.getStoredDamage() >= storeMax) {
                double burst = data.getStoredDamage();
                damage += burst;
                data.setStoredDamage(0);
                Message.action(player, "&6&l爆发！ 额外 " + WeaponManager.format(burst, 1) + " 伤害");
                player.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
            }
            data.saveToItemStack(player.getInventory().getItemInMainHand());
        }

        // 吸血
        double lifePercent = data.getTotalEffect(template, "lifesteal_percent", 0.0);
        double lifeFlat = data.getTotalEffect(template, "lifesteal_flat", 0.0);
        double heal = damage * lifePercent + lifeFlat;
        if (heal > 0) {
            var maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : player.getHealth();
            player.setHealth(Math.min(player.getHealth() + heal, maxHealth));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), (int) Math.min(5, heal + 1));
        }

        // 减速
        double slowDuration = data.getTotalEffect(template, "slow_duration", 0.0);
        int slowLevel = (int) data.getTotalEffect(template, "slow_level", 0.0);
        if (slowDuration > 0) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (slowDuration * 20), slowLevel));
        }

        // 流血
        double bleedChance = data.getTotalEffect(template, "bleed_chance", 0.0);
        double bleedDamage = data.getTotalEffect(template, "bleed_damage", 0.0);
        double bleedDuration = data.getTotalEffect(template, "bleed_duration", 0.0);
        if (bleedChance > 0 && bleedDamage > 0 && RANDOM.nextDouble() < bleedChance) {
            applyBleed(target, bleedDamage, (int) bleedDuration);
        }

        // 火焰
        double fireDamage = data.getTotalEffect(template, "fire_damage", 0.0);
        double fireDuration = data.getTotalEffect(template, "fire_duration", 0.0);
        if (fireDuration > 0) {
            target.setFireTicks((int) (fireDuration * 20));
            if (fireDamage > 0) {
                target.damage(fireDamage);
            }
        }

        // 雷电
        double lightning = data.getTotalEffect(template, "lightning_chance", 0.0);
        if (lightning > 0 && RANDOM.nextDouble() < lightning) {
            target.getWorld().strikeLightning(target.getLocation());
        }

        // 眩晕（缓慢+挖掘疲劳）
        double stun = data.getTotalEffect(template, "stun_duration", 0.0);
        if (stun > 0) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (stun * 20), 3));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, (int) (stun * 20), 3));
        }

        // 连锁伤害
        int chainTargets = (int) data.getTotalEffect(template, "chain_targets", 0.0);
        double chainRange = data.getTotalEffect(template, "chain_range", 0.0);
        double chainPercent = data.getTotalEffect(template, "chain_damage_percent", 0.0);
        if (chainTargets > 0 && chainRange > 0 && chainPercent > 0) {
            applyChainDamage(player, target, damage, chainTargets, chainRange, chainPercent);
        }

        WeaponManager.updateLore(player.getInventory().getItemInMainHand(), template, data);
        return damage;
    }

    private static void applyChainDamage(Player player, LivingEntity primary, double baseDamage, int maxTargets, double range, double percent) {
        List<LivingEntity> nearby = new ArrayList<>();
        for (org.bukkit.entity.Entity entity : primary.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity living && !entity.equals(primary) && !entity.equals(player)) {
                nearby.add(living);
                if (nearby.size() >= maxTargets) break;
            }
        }
        double chainDamage = baseDamage * percent;
        for (LivingEntity entity : nearby) {
            internalDamage = true;
            try {
                entity.damage(chainDamage, player);
            } finally {
                internalDamage = false;
            }
            drawParticleLine(primary.getLocation().add(0, 1, 0), entity.getLocation().add(0, 1, 0));
        }
    }

    private static void drawParticleLine(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length == 0) return;
        direction.normalize();
        for (double d = 0; d < length; d += 0.3) {
            Location point = from.clone().add(direction.clone().multiply(d));
            from.getWorld().spawnParticle(Particle.ENCHANTED_HIT, point, 1);
        }
    }

    private static void applyBleed(LivingEntity target, double damagePerSecond, int durationSeconds) {
        if (durationSeconds <= 0) return;
        new BukkitRunnable() {
            int ticks = durationSeconds * 20;
            @Override
            public void run() {
                if (target.isDead() || !target.isValid()) {
                    cancel();
                    return;
                }
                target.damage(damagePerSecond);
                target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 3);
                ticks -= 20;
                if (ticks <= 0) cancel();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
