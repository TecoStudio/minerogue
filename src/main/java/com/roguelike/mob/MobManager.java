package com.roguelike.mob;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomItemStackFactory;
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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class MobManager {
    private static RoguelikePlugin plugin;
    private static final List<InternalMob> internalMobs = new ArrayList<>();
    private static BukkitTask behaviorTask;

    public static void init(RoguelikePlugin plugin) {
        shutdown();
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
        for (InternalMob mob : internalMobs) {
            mob.shutdown();
        }
        internalMobs.clear();
    }

    public static void reload() {
        if (plugin != null) init(plugin);
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

        applyConfiguredEquipment(entity, config, type);
    }

    private static void applyConfiguredEquipment(LivingEntity entity, ConfigManager.MobConfig config, String type) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;

        ConfigManager.EquipmentDefinition equipmentDefinition = config.equipment();
        setSlot(equipment::setHelmet, equipmentDefinition.helmet());
        setSlot(equipment::setChestplate, equipmentDefinition.chestplate());
        setSlot(equipment::setLeggings, equipmentDefinition.leggings());
        setSlot(equipment::setBoots, equipmentDefinition.boots());
        setSlot(equipment::setItemInMainHand, equipmentDefinition.mainHand());
        setSlot(equipment::setItemInOffHand, equipmentDefinition.offHand());
        setWeaponSlot(equipment::setItemInMainHand, equipmentDefinition.mainHandWeaponTemplate());
        setWeaponSlot(equipment::setItemInOffHand, equipmentDefinition.offHandWeaponTemplate());
        equipLegacyWeaponTemplate(equipment, config, type, equipmentDefinition);

        ConfigManager.EquipmentDropChances dropChances = equipmentDefinition.dropChances();
        equipment.setHelmetDropChance((float) dropChances.helmet());
        equipment.setChestplateDropChance((float) dropChances.chestplate());
        equipment.setLeggingsDropChance((float) dropChances.leggings());
        equipment.setBootsDropChance((float) dropChances.boots());
        equipment.setItemInMainHandDropChance((float) dropChances.mainHand());
        equipment.setItemInOffHandDropChance((float) dropChances.offHand());
    }

    private static void equipLegacyWeaponTemplate(EntityEquipment equipment, ConfigManager.MobConfig config,
                                                  String type, ConfigManager.EquipmentDefinition equipmentDefinition) {
        if ("zombie".equals(type) || config.weaponTemplate() == null || config.weaponTemplate().isBlank()) return;
        if (isConfigured(equipmentDefinition.mainHand()) || isConfigured(equipmentDefinition.mainHandWeaponTemplate())) return;
        CustomWeapon template = ConfigManager.getWeapon(config.weaponTemplate());
        if (template == null) return;
        ItemStack weapon = WeaponManager.createWeaponStack(template, modifierWeaponMaterial(template));
        equipment.setItemInMainHand(weapon);
    }

    private static void setSlot(java.util.function.Consumer<ItemStack> setter, String materialName) {
        if (!isConfigured(materialName)) return;
        Material material = Material.matchMaterial(normalizedMaterialName(materialName));
        if (material == null || material.isAir() || !material.isItem()) return;
        setter.accept(new ItemStack(material));
    }

    private static void setWeaponSlot(java.util.function.Consumer<ItemStack> setter, String weaponTemplate) {
        if (!isConfigured(weaponTemplate)) return;
        CustomWeapon template = ConfigManager.getWeapon(weaponTemplate);
        if (template == null) return;
        setter.accept(WeaponManager.createWeaponStack(template, modifierWeaponMaterial(template)));
    }

    private static boolean isConfigured(String value) {
        return value != null && !value.isBlank();
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
            if (mob.spawnable()) {
                ids.add(mob.id());
                ids.addAll(mob.aliases());
            }
        }
        return ids;
    }

    public static List<String> defaultInternalMobIds() {
        return ConfigManager.getInternalMobDefinitions().stream()
                .filter(definition -> definition.spawnable() && ConfigManager.getScriptedMobConfig(definition.id()).enabled())
                .map(ConfigManager.InternalMobDefinition::id)
                .toList();
    }

    public static boolean shouldForceInternalMobNameVisible() {
        return false;
    }

    public static boolean shouldBossAffectPlayer(GameMode gameMode, boolean dead) {
        return !dead && gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR;
    }

    public static double defaultRandomWeaponDropMultiplier() {
        return 0.0;
    }

    public static LivingEntity spawnInternalMob(String id, Location location) {
        for (InternalMob mob : internalMobs) {
            if (mob.spawnable() && matchesId(mob, id)) {
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
            if (mob.spawnable() && matchesId(mob, id)) return true;
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
            if (mob.isMob(entity)) {
                mob.onDeath(entity);
                return;
            }
        }
        String type = entity.getType().name().toLowerCase();
        ConfigManager.MobConfig config = ConfigManager.getMobConfig(type);
        if (config == null) return;
        handleConfiguredDrops(entity, config.drops());
    }

    public static void handleConfiguredDrops(LivingEntity entity, ConfigManager.DropConfig drops) {
        if (entity == null || drops == null) return;
        if (drops.heldItemChance() > 0.0 && entity.getEquipment() != null) {
            dropHeldItem(entity, entity.getEquipment().getItemInMainHand(), drops.heldItemChance());
            dropHeldItem(entity, entity.getEquipment().getItemInOffHand(), drops.heldItemChance());
        }
        for (ConfigManager.DropItemDefinition drop : drops.items()) {
            if (ThreadLocalRandom.current().nextDouble() >= drop.chance()) continue;
            ItemStack stack = createConfiguredDrop(drop);
            if (stack != null && !stack.getType().isAir()) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), stack);
            }
        }
    }

    private static void dropHeldItem(LivingEntity entity, ItemStack held, double chance) {
        if (held == null || held.getType().isAir()) return;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        ItemStack stack = held.clone();
        stack.setAmount(1);
        entity.getWorld().dropItemNaturally(entity.getLocation(), stack);
    }

    private static ItemStack createConfiguredDrop(ConfigManager.DropItemDefinition drop) {
        if (drop.weaponTemplate() != null && !drop.weaponTemplate().isBlank()) {
            CustomWeapon template = ConfigManager.getWeapon(drop.weaponTemplate());
            if (template == null) return null;
            ItemStack stack = WeaponManager.createWeaponStack(template, modifierWeaponMaterial(template));
            stack.setAmount(drop.amount());
            return stack;
        }
        if (drop.itemTemplate() != null && !drop.itemTemplate().isBlank()) {
            CustomItem item = ConfigManager.getItem(drop.itemTemplate());
            if (item == null) return null;
            ItemStack stack = CustomItemStackFactory.createItemStack(item);
            stack.setAmount(drop.amount());
            return stack;
        }
        if (drop.material() != null && !drop.material().isBlank()) {
            Material material = Material.matchMaterial(normalizedMaterialName(drop.material()));
            if (material == null || material.isAir() || !material.isItem()) return null;
            return new ItemStack(material, drop.amount());
        }
        return null;
    }

    private static String normalizedMaterialName(String material) {
        String normalized = material.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) normalized = normalized.substring("MINECRAFT:".length());
        return normalized;
    }

    private static void handleRandomWeaponDrop(LivingEntity entity) {
        if (!(entity instanceof Monster)) return;
        String rarity = rollWeaponRarity();
        if (rarity == null) return;

        Collection<CustomWeapon> weapons = ConfigManager.getWeapons();
        List<CustomWeapon> candidates = new ArrayList<>();
        for (CustomWeapon weapon : weapons) {
            if ("special".equalsIgnoreCase(weapon.getRarity())) continue;
            if (rarity.equalsIgnoreCase(weapon.getRarity())) candidates.add(weapon);
        }
        if (candidates.isEmpty()) return;

        CustomWeapon template = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
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
        return ThreadLocalRandom.current().nextDouble() < chance;
    }
}
