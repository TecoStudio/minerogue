package com.roguelike.combat;

import com.roguelike.RoguelikePlugin;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponAbilityManager;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
        damage = WeaponAbilityManager.applySmash(player, player.getInventory().getItemInMainHand(), template, data, damage);

        boolean wasBurning = target.getFireTicks() > 0;
        boolean wasPoisoned = target.hasPotionEffect(PotionEffectType.POISON);
        double burningBonus = data.getTotalEffect(template, "burning_target_damage_percent", 0.0);
        double poisonedBonus = data.getTotalEffect(template, "poisoned_target_damage_percent", 0.0);
        if (wasBurning && burningBonus > 0) {
            damage *= 1 + burningBonus;
        }
        if (wasPoisoned && poisonedBonus > 0) {
            damage *= 1 + poisonedBonus;
        }

        // 暴击
        double critChance = chance(data.getTotalEffect(template, "crit_chance", 0.0));
        double critDamage = data.getTotalEffect(template, "crit_damage", 1.5);
        boolean crit = critChance > 0 && RANDOM.nextDouble() < critChance;
        if (crit) {
            damage *= critDamage;
            WeaponAbilityManager.applyHyper(player, template, data, true);
            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            player.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 10, 0.3, 0.3, 0.3);
            Message.send(player, "&c&l暴击！ &f" + WeaponManager.format(damage, 1) + " 伤害");
        }

        // 伤害存储爆发：按攻击次数触发，默认 20 下，最低 5 下。
        double storePercent = data.getTotalEffect(template, "damage_store_percent", 0.0);
        int storeHitReduction = (int) data.getTotalEffect(template, "damage_store_hit_reduction", 0.0);
        int requiredHits = Math.max(5, 20 - storeHitReduction);
        if (storePercent > 0) {
            data.addStoredDamage(damage * storePercent);
            data.incrementStoredDamageHits();
            if (data.getStoredDamageHits() >= requiredHits) {
                double burst = data.getStoredDamage();
                damage += burst;
                data.setStoredDamage(0);
                data.setStoredDamageHits(0);
                Message.send(player, "&6&l爆发！ 额外 " + WeaponManager.format(burst, 1) + " 伤害");
                player.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
            }
            sendStoreProgress(player, data.getStoredDamageHits(), requiredHits);
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

        // 火焰
        double fireDamage = data.getTotalEffect(template, "fire_damage", 0.0);
        double fireDuration = data.getTotalEffect(template, "fire_duration", 0.0);
        if (fireDuration > 0) {
            target.setFireTicks((int) (fireDuration * 20));
            if (fireDamage > 0) {
                target.damage(fireDamage, player);
            }
        }

        // 中毒
        double poisonChance = chance(data.getTotalEffect(template, "poison_chance", 0.0));
        if (poisonChance > 0 && RANDOM.nextDouble() < poisonChance) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
        }

        // 雷电
        double lightning = chance(data.getTotalEffect(template, "lightning_chance", 0.0));
        if (lightning > 0 && RANDOM.nextDouble() < lightning) {
            target.getWorld().strikeLightning(target.getLocation());
        }

        // 爆炸
        double explosionChance = chance(data.getTotalEffect(template, "explosion_chance", 0.0));
        if (explosionChance > 0 && RANDOM.nextDouble() < explosionChance) {
            target.getWorld().createExplosion(target.getLocation(), 2.0f, false, false, player);
        }

        // 大爆炸：TNT 级别爆炸，会点火并破坏方块。
        double bigExplosionChance = chance(data.getTotalEffect(template, "big_explosion_chance", 0.0));
        if (bigExplosionChance > 0 && RANDOM.nextDouble() < bigExplosionChance) {
            target.getWorld().createExplosion(target.getLocation(), 4.0f, true, true, player);
        }

        // 连锁伤害
        int chainTargets = (int) data.getTotalEffect(template, "chain_targets", 0.0);
        double chainRange = data.getTotalEffect(template, "chain_range", 0.0);
        double chainPercent = data.getTotalEffect(template, "chain_damage_percent", 0.0);
        if (chainTargets > 0 && chainRange > 0 && chainPercent > 0) {
            applyChainDamage(player, target, damage, chainTargets, chainRange, chainPercent);
        }

        WeaponManager.updateLore(player.getInventory().getItemInMainHand(), template, data);
        Message.send(player, "&7造成伤害: &f" + WeaponManager.format(damage, 1));
        return damage;
    }

    private static void sendStoreProgress(Player player, int hits, int requiredHits) {
        int percent = requiredHits <= 0 ? 0 : (int) Math.min(100, Math.round(hits / (double) requiredHits * 100));
        int filled = Math.min(10, Math.max(0, percent / 10));
        String bar = "§6[" + "#".repeat(filled) + "§7" + "-".repeat(10 - filled) + "§6]§f" + percent + "%";
        player.sendActionBar(Message.toComponent("§e伤害储存 " + bar + " §7" + hits + "/" + requiredHits));
    }

    private static double chance(double value) {
        return Math.max(0.0, Math.min(1.0, value));
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

}
