package com.roguelike.mob;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobManager {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static RoguelikePlugin plugin;

    public static void init(RoguelikePlugin plugin) {
        MobManager.plugin = plugin;
    }

    public static void applyToMob(LivingEntity entity) {
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

    public static void handleDrop(LivingEntity entity) {
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
