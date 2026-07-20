package com.roguelike.mob;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.integration.IntegrationManager;
import com.roguelike.mob.internal.ScriptedInternalMob;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        for (ConfigManager.InternalMobDefinition definition : ConfigManager.getInternalMobDefinitions()) {
            InternalMob mob = createInternalMob(plugin, definition);
            if (mob != null) internalMobs.add(mob);
        }
        behaviorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, MobManager::tickInternalMobs, 20L, 10L);
    }

    private static InternalMob createInternalMob(RoguelikePlugin plugin, ConfigManager.InternalMobDefinition definition) {
        return new ScriptedInternalMob(plugin, definition);
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
                ItemStack weapon = WeaponManager.createWeaponStack(template, modifierWeaponMaterial(template));
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
            if (mob.spawnable()) ids.add(mob.id());
        }
        return ids;
    }

    public static List<String> getAcceptedMobIds() {
        List<String> ids = new ArrayList<>();
        for (InternalMob mob : internalMobs) {
            ids.add(mob.id());
            ids.addAll(mob.aliases());
        }
        return ids;
    }

    public static List<String> defaultInternalMobIds() {
        return ConfigManager.getInternalMobDefinitions().stream()
                .filter(ConfigManager.InternalMobDefinition::spawnable)
                .map(ConfigManager.InternalMobDefinition::id)
                .toList();
    }

    public static boolean shouldForceInternalMobNameVisible() {
        return false;
    }

    public static boolean shouldBossAffectPlayer(GameMode gameMode, boolean dead) {
        return !dead && gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR;
    }

    public static LivingEntity spawnInternalMob(String id, Location location) {
        for (InternalMob mob : internalMobs) {
            if (matchesId(mob, id)) {
                return mob.spawn(location);
            }
        }
        return null;
    }

    static boolean matchesId(InternalMob mob, String id) {
        if (mob == null || id == null) return false;
        if (mob.id().equalsIgnoreCase(id)) return true;
        for (String alias : mob.aliases()) {
            if (alias.equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    public static boolean matchesInternalMobValue(InternalMob mob, String value) {
        if (mob == null || value == null) return false;
        return matchesId(mob, value);
    }

    public static boolean isAcceptedMobId(String id) {
        for (InternalMob mob : internalMobs) {
            if (matchesId(mob, id)) return true;
        }
        return false;
    }

    static Material modifierWeaponMaterial(CustomWeapon template) {
        String materialName = modifierWeaponMaterialName(template);
        Material material = Material.matchMaterial(materialName);
        return material == null || material.isAir() ? Material.IRON_SWORD : material;
    }

    static String modifierWeaponMaterialName(CustomWeapon template) {
        if (template == null || template.getItem() == null || template.getItem().isBlank()) return "IRON_SWORD";
        String normalized = template.getItem().trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) normalized = normalized.substring("MINECRAFT:".length());
        return normalized;
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
        ItemStack weapon = WeaponManager.createWeaponStack(template, modifierWeaponMaterial(template));
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
        double multiplier = ConfigManager.getWeaponDropMultiplier();
        if (multiplier <= 0.0) return null;
        if (rollChance("legendary", 0.002, multiplier)) return "legendary";
        if (rollChance("epic", 0.005, multiplier)) return "epic";
        if (rollChance("rare", 0.010, multiplier)) return "rare";
        if (rollChance("common", 0.020, multiplier)) return "common";
        return null;
    }

    private static boolean rollChance(String rarity, double fallback, double multiplier) {
        double chance = Math.min(1.0, ConfigManager.getConfiguredWeaponDropChance(rarity, fallback) * multiplier);
        return RANDOM.nextDouble() < chance;
    }
}
