package com.roguelike.mob.internal;

import com.roguelike.RoguelikePlugin;
import com.roguelike.combat.CombatHandler;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.mob.InternalMob;
import com.roguelike.mob.MobManager;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ScriptedInternalMob implements InternalMob {
    private static final java.util.Random RANDOM = java.util.concurrent.ThreadLocalRandom.current();
    private static boolean scriptedSkillDamage = false;

    private final RoguelikePlugin plugin;
    private final ConfigManager.InternalMobDefinition definition;
    private final NamespacedKey mobKey;
    private final NamespacedKey nextPrimaryKey;
    private final NamespacedKey nextSecondaryKey;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public ScriptedInternalMob(RoguelikePlugin plugin, ConfigManager.InternalMobDefinition definition) {
        this.plugin = plugin;
        this.definition = definition;
        this.mobKey = new NamespacedKey(plugin, "internal_mob");
        this.nextPrimaryKey = new NamespacedKey(plugin, "scripted_next_primary_" + safeKey(definition.id()));
        this.nextSecondaryKey = new NamespacedKey(plugin, "scripted_next_secondary_" + safeKey(definition.id()));
    }

    @Override
    public String id() {
        return definition.id();
    }

    @Override
    public List<String> aliases() {
        return definition.aliases();
    }

    @Override
    public boolean spawnable() {
        return definition.spawnable() && ConfigManager.getScriptedMobConfig(id()).enabled();
    }

    @Override
    public void onSpawn(LivingEntity entity) {
        ConfigManager.ScriptedMobConfig config = ConfigManager.getScriptedMobConfig(id());
        if (!config.enabled() || config.spawnChance() <= 0.0) return;
        if (entity.getEntitySpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (entity.getPersistentDataContainer().has(mobKey, PersistentDataType.STRING)) return;
        if (entity.getType() != templateEntityType(definition.template())) return;
        if (RANDOM.nextDouble() >= config.spawnChance()) return;
        apply(entity, config);
    }

    @Override
    public LivingEntity spawn(Location location) {
        ConfigManager.ScriptedMobConfig config = ConfigManager.getScriptedMobConfig(id());
        if (!config.enabled()) return null;
        EntityType type = templateEntityType(definition.template());
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        apply(entity, config);
        return entity;
    }

    private void apply(LivingEntity entity, ConfigManager.ScriptedMobConfig config) {
        entity.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, id());
        entity.customName(Message.toComponent(config.name()));
        entity.setCustomNameVisible(false);
        entity.setRemoveWhenFarAway(false);
        if (config.bossBar()) {
            bossBars.put(entity.getUniqueId(), Bukkit.createBossBar(displayName(config.name()), BarColor.RED, BarStyle.SEGMENTED_10));
        }

        var health = entity.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(config.health());
            entity.setHealth(config.health());
        }
        var damage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(config.damage());
        var speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(speed.getBaseValue() * config.speedMultiplier());
        equipTemplate(entity, config);
        applyConfiguredPotionEffects(entity);
    }

    private void equipTemplate(LivingEntity entity, ConfigManager.ScriptedMobConfig config) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;
        ConfigManager.EquipmentDefinition equipmentDefinition = definition.equipment();
        setSlot(equipment::setHelmet, equipmentDefinition.helmet());
        setSlot(equipment::setChestplate, equipmentDefinition.chestplate());
        setSlot(equipment::setLeggings, equipmentDefinition.leggings());
        setSlot(equipment::setBoots, equipmentDefinition.boots());
        setSlot(equipment::setItemInMainHand, equipmentDefinition.mainHand());
        setSlot(equipment::setItemInOffHand, equipmentDefinition.offHand());
        setWeaponSlot(equipment::setItemInMainHand, equipmentDefinition.mainHandWeaponTemplate());
        setWeaponSlot(equipment::setItemInOffHand, equipmentDefinition.offHandWeaponTemplate());
        equipConfiguredWeapon(equipment);
        ConfigManager.EquipmentDropChances dropChances = equipmentDefinition.dropChances();
        equipment.setHelmetDropChance((float) dropChances.helmet());
        equipment.setChestplateDropChance((float) dropChances.chestplate());
        equipment.setLeggingsDropChance((float) dropChances.leggings());
        equipment.setBootsDropChance((float) dropChances.boots());
        equipment.setItemInMainHandDropChance((float) dropChances.mainHand());
        equipment.setItemInOffHandDropChance((float) dropChances.offHand());
    }

    private void setSlot(java.util.function.Consumer<ItemStack> setter, String materialName) {
        if (materialName == null || materialName.isBlank()) return;
        Material material = material(materialName);
        if (material == null || material.isAir() || !material.isItem()) return;
        setter.accept(new ItemStack(material));
    }

    private void setWeaponSlot(java.util.function.Consumer<ItemStack> setter, String weaponTemplate) {
        if (weaponTemplate == null || weaponTemplate.isBlank()) return;
        CustomWeapon template = ConfigManager.getWeapon(weaponTemplate);
        if (template == null) return;
        setter.accept(WeaponManager.createWeaponStack(template, weaponMaterial(template)));
    }

    private void equipConfiguredWeapon(EntityEquipment equipment) {
        ConfigManager.EquipmentDefinition equipmentDefinition = definition.equipment();
        if (isConfigured(equipmentDefinition.mainHandWeaponTemplate()) || isConfigured(equipmentDefinition.offHandWeaponTemplate())) return;
        String weaponTemplate = definition.weaponTemplate();
        if (weaponTemplate == null || weaponTemplate.isBlank()) return;
        CustomWeapon template = ConfigManager.getWeapon(weaponTemplate);
        if (template == null) return;
        ItemStack weapon = WeaponManager.createWeaponStack(template, null);
        Material material = weaponMaterial(template);
        if (equipment.getItemInMainHand().getType() == Material.BOW && material != Material.BOW) {
            equipment.setItemInOffHand(weapon);
        } else {
            equipment.setItemInMainHand(weapon);
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private void applyConfiguredPotionEffects(LivingEntity entity) {
        for (ConfigManager.MobPotionEffectDefinition effect : definition.potionEffects()) {
            PotionEffectType type = potionEffectType(effect.type());
            if (type == null) continue;
            int duration = effect.durationTicks() < 0 ? PotionEffect.INFINITE_DURATION : Math.max(1, effect.durationTicks());
            entity.addPotionEffect(new PotionEffect(type, duration, Math.max(0, effect.level() - 1), effect.ambient(), effect.particles()));
        }
    }

    private PotionEffectType potionEffectType(String type) {
        if (type == null || type.isBlank()) return null;
        String key = type.toLowerCase(Locale.ROOT).replace('_', '-');
        return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key));
    }

    private Material weaponMaterial(CustomWeapon template) {
        String materialName = template.getItem() == null ? "" : template.getItem().trim().toUpperCase(Locale.ROOT);
        if (materialName.startsWith("MINECRAFT:")) materialName = materialName.substring("MINECRAFT:".length());
        Material material = Material.matchMaterial(materialName);
        return material == null || material.isAir() ? Material.IRON_SWORD : material;
    }

    private Material material(String materialName) {
        String normalized = materialName.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) normalized = normalized.substring("MINECRAFT:".length());
        return Material.matchMaterial(normalized);
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        LivingEntity attacker = damageSource(event);
        if (attacker == null || !isMob(attacker)) return;
        if (event.getEntity() instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) {
            event.setCancelled(true);
            return;
        }
        if (scriptedSkillDamage || CombatHandler.isInternalDamage()) return;

        ConfigManager.ScriptedMobConfig config = ConfigManager.getScriptedMobConfig(id());
        double damage = config.damage();
        WeaponInstanceData data = weaponData(attacker);
        CustomWeapon template = data == null ? null : ConfigManager.getWeapon(data.getBaseWeaponId());
        boolean wasPoisoned = event.getEntity() instanceof LivingEntity target && target.hasPotionEffect(PotionEffectType.POISON);
        if (template != null && data != null) {
            damage = weaponDamage(template, data, damage, wasPoisoned);
        }
        event.setDamage(damage);
        if (event.getEntity() instanceof LivingEntity target) {
            applyHitActions(attacker, target, config);
            if (template != null && data != null && RANDOM.nextDouble() < weaponPoisonChance(template, data)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            }
        }
    }

    @Override
    public void onDeath(LivingEntity entity) {
        MobManager.handleConfiguredDrops(entity, definition.drops());
    }

    private LivingEntity damageSource(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity living) return living;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    private WeaponInstanceData weaponData(LivingEntity attacker) {
        EntityEquipment equipment = attacker.getEquipment();
        if (equipment == null) return null;
        WeaponInstanceData main = WeaponInstanceData.fromItemStack(equipment.getItemInMainHand());
        return main != null ? main : WeaponInstanceData.fromItemStack(equipment.getItemInOffHand());
    }

    private void applyHitActions(LivingEntity attacker, LivingEntity target, ConfigManager.ScriptedMobConfig config) {
        double distance = attacker.getLocation().distance(target.getLocation());
        for (ConfigManager.ActionDefinition action : definition.actions()) {
            if (!"slow-on-hit".equals(normalizedAction(action))) continue;
            if (!conditionMatches(action.when(), distance, config, Set.of())) continue;
            if (RANDOM.nextDouble() >= action.chance()) continue;
            int amplifier = Math.max(0, action.level() - 1);
            int durationTicks = Math.max(1, (int) Math.round(action.durationSeconds() * 20.0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, amplifier));
        }
    }

    static double weaponDamage(CustomWeapon template, WeaponInstanceData data, double fallbackDamage, boolean targetPoisoned) {
        double damage = Math.max(fallbackDamage, data.getTotalDamage(template));
        double poisonedBonus = data.getTotalEffect(template, "poisoned_target_damage_percent", 0.0);
        return targetPoisoned && poisonedBonus > 0.0 ? damage * (1.0 + poisonedBonus) : damage;
    }

    static double weaponPoisonChance(CustomWeapon template, WeaponInstanceData data) {
        return Math.max(0.0, Math.min(1.0, data.getTotalEffect(template, "poison_chance", 0.0)));
    }

    @Override
    public void tick() {
        ConfigManager.ScriptedMobConfig config = ConfigManager.getScriptedMobConfig(id());
        if (!config.enabled()) return;
        Set<UUID> detected = new HashSet<>();
        for (var world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (!(entity instanceof Mob mob) || !isMob(entity)) continue;
                detected.add(entity.getUniqueId());
                if (!isValid(entity)) {
                    removeBossBar(entity);
                    continue;
                }
                updateBossBar(entity, config);
                LivingEntity target = mob.getTarget();
                if (target instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) {
                    mob.setTarget(null);
                    target = null;
                }
                if (!(target instanceof Player) || target.isDead()) {
                    target = nearestPlayer(entity, config.detectRange());
                    if (target != null) mob.setTarget(target);
                }
                if (target == null) continue;
                runScriptedActions(entity, target, config);
            }
        }
        cleanupMissingBossBars(detected);
    }

    private void runScriptedActions(LivingEntity entity, LivingEntity target, ConfigManager.ScriptedMobConfig config) {
        double distance = entity.getLocation().distance(target.getLocation());
        Set<String> completedActions = new HashSet<>();
        for (ConfigManager.ActionDefinition action : definition.actions()) {
            if (!conditionMatches(action.when(), distance, config, completedActions)) continue;
            if (runAction(action, entity, target, config)) completedActions.add(normalizedAction(action));
        }
    }

    static boolean conditionMatches(String when, double distance, ConfigManager.ScriptedMobConfig config, Set<String> completedActions) {
        String condition = normalize(when).replace('_', '-');
        if (condition.startsWith("after ")) {
            return completedActions.contains(condition.substring("after ".length()).trim());
        }
        return switch (condition) {
            case "target-close" -> distance <= config.skillRange();
            case "target-far" -> distance > config.skillRange() && distance <= config.detectRange();
            case "target-detected" -> distance <= config.detectRange();
            case "always" -> true;
            default -> false;
        };
    }

    private boolean runAction(ConfigManager.ActionDefinition action, LivingEntity entity, LivingEntity target,
                              ConfigManager.ScriptedMobConfig config) {
        return switch (normalizedAction(action)) {
            case "melee-burst" -> meleeBurst(entity, target, config, action);
            case "retreat" -> retreat(entity, target, action);
            case "leap" -> leapToward(entity, target, config, action);
            case "shockwave" -> shockwave(entity, config, action);
            case "blink" -> blinkBehind(entity, target, config, action);
            case "blade-storm" -> bladeStorm(entity, config, action);
            case "slow-on-hit" -> false;
            default -> false;
        };
    }

    private static String normalizedAction(ConfigManager.ActionDefinition action) {
        return normalize(action.action()).replace('_', '-');
    }

    private Player nearestPlayer(LivingEntity entity, double range) {
        Player nearest = null;
        double best = range * range;
        for (var nearby : entity.getWorld().getNearbyEntities(entity.getLocation(), range, range, range)) {
            if (!(nearby instanceof Player player) || !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) continue;
            double distance = player.getLocation().distanceSquared(entity.getLocation());
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void updateBossBar(LivingEntity entity, ConfigManager.ScriptedMobConfig config) {
        if (!config.bossBar()) {
            removeBossBar(entity);
            return;
        }
        BossBar bar = bossBars.computeIfAbsent(entity.getUniqueId(), ignored -> Bukkit.createBossBar(displayName(config.name()), BarColor.RED, BarStyle.SEGMENTED_10));
        bar.setTitle(displayName(config.name()));
        var health = entity.getAttribute(Attribute.MAX_HEALTH);
        double maximum = Math.max(1.0, health == null ? config.health() : health.getValue());
        bar.setProgress(Math.max(0.0, Math.min(1.0, entity.getHealth() / maximum)));
        bar.removeAll();
        double visibleDistance = config.detectRange() * config.detectRange();
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= visibleDistance) bar.addPlayer(player);
        }
    }

    private boolean meleeBurst(LivingEntity entity, LivingEntity target, ConfigManager.ScriptedMobConfig config, ConfigManager.ActionDefinition action) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextPrimaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return false;
        entity.getPersistentDataContainer().set(nextPrimaryKey, PersistentDataType.LONG, now + cooldown(action, config.skillCooldownTicks()));
        int count = Math.max(1, action.hits());
        for (int i = 0; i < count; i++) {
            applySkillDamage(target, actionDamage(action, config), entity);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
        return true;
    }

    private boolean retreat(LivingEntity entity, LivingEntity target, ConfigManager.ActionDefinition action) {
        Vector direction = entity.getLocation().toVector().subtract(target.getLocation().toVector());
        if (direction.lengthSquared() <= 0.001) return false;
        double speed = action.speed() > 0.0 ? action.speed() : 0.65;
        entity.setVelocity(direction.normalize().multiply(speed).setY(0.25));
        return true;
    }

    private boolean shockwave(LivingEntity entity, ConfigManager.ScriptedMobConfig config, ConfigManager.ActionDefinition action) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextPrimaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return false;
        entity.getPersistentDataContainer().set(nextPrimaryKey, PersistentDataType.LONG, now + cooldown(action, config.skillCooldownTicks()));
        Location origin = entity.getLocation();
        entity.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.75f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, origin.add(0, 0.1, 0), 1);
        double radius = config.skillRange();
        for (var nearby : entity.getWorld().getNearbyEntities(entity.getLocation(), radius, 1.8, radius)) {
            if (!(nearby instanceof LivingEntity target) || target.equals(entity)) continue;
            if (target instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) continue;
            applySkillDamage(target, actionDamage(action, config), entity);
            Vector knockback = target.getLocation().toVector().subtract(entity.getLocation().toVector());
            if (knockback.lengthSquared() > 0.001) target.setVelocity(knockback.normalize().multiply(0.9).setY(0.35));
        }
        return true;
    }

    private boolean leapToward(LivingEntity entity, LivingEntity target, ConfigManager.ScriptedMobConfig config, ConfigManager.ActionDefinition action) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextSecondaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return false;
        entity.getPersistentDataContainer().set(nextSecondaryKey, PersistentDataType.LONG, now + cooldown(action, Math.max(20L, config.skillCooldownTicks() / 2)));
        Vector direction = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        if (direction.lengthSquared() <= 0.001) return false;
        double speed = action.speed() > 0.0 ? action.speed() : 0.85;
        entity.setVelocity(direction.normalize().multiply(speed).setY(0.35));
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 0.7f, 0.8f);
        return true;
    }

    private boolean blinkBehind(LivingEntity entity, LivingEntity target, ConfigManager.ScriptedMobConfig config, ConfigManager.ActionDefinition action) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextPrimaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return false;
        entity.getPersistentDataContainer().set(nextPrimaryKey, PersistentDataType.LONG, now + cooldown(action, Math.max(30L, config.skillCooldownTicks())));
        Vector facing = target.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() <= 0.001) return false;
        Location destination = target.getLocation().subtract(facing.normalize().multiply(2.2));
        destination.setY(target.getLocation().getY());
        if (!destination.getBlock().isPassable()) return false;
        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 24, 0.3, 0.6, 0.3);
        entity.teleport(destination);
        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 24, 0.3, 0.6, 0.3);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
        return true;
    }

    private boolean bladeStorm(LivingEntity entity, ConfigManager.ScriptedMobConfig config, ConfigManager.ActionDefinition action) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextSecondaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return false;
        entity.getPersistentDataContainer().set(nextSecondaryKey, PersistentDataType.LONG, now + cooldown(action, Math.max(40L, config.skillCooldownTicks() / 2)));
        double radius = config.skillRange();
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
        entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, entity.getLocation().add(0, 1, 0), 8, radius / 2, 0.2, radius / 2);
        for (var nearby : entity.getWorld().getNearbyEntities(entity.getLocation(), radius, 2.0, radius)) {
            if (!(nearby instanceof LivingEntity target) || target.equals(entity)) continue;
            if (target instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) continue;
            applySkillDamage(target, actionDamage(action, config), entity);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
        return true;
    }

    private double actionDamage(ConfigManager.ActionDefinition action, ConfigManager.ScriptedMobConfig config) {
        return Double.isNaN(action.damage()) ? config.skillDamage() : Math.max(0.0, action.damage());
    }

    private long cooldown(ConfigManager.ActionDefinition action, long fallback) {
        return action.cooldownTicks() > 0L ? action.cooldownTicks() : fallback;
    }

    private void applySkillDamage(LivingEntity target, double damage, LivingEntity source) {
        scriptedSkillDamage = true;
        try {
            target.damage(damage, source);
        } finally {
            scriptedSkillDamage = false;
        }
    }

    private void cleanupMissingBossBars(Set<UUID> detected) {
        bossBars.entrySet().removeIf(entry -> {
            if (detected.contains(entry.getKey())) return false;
            entry.getValue().removeAll();
            return true;
        });
    }

    private void removeBossBar(LivingEntity entity) {
        BossBar bar = bossBars.remove(entity.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    @Override
    public void shutdown() {
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
    }

    private boolean isValid(LivingEntity entity) {
        return entity != null && entity.isValid() && !entity.isDead() && entity.getWorld() != null;
    }

    private String displayName(String configuredName) {
        String name = configuredName == null || configuredName.isBlank() ? id() : configuredName;
        return name.replace('&', '§');
    }

    private EntityType templateEntityType(String template) {
        String normalized = normalize(template);
        if (normalized.contains("skeleton")) return EntityType.SKELETON;
        if (normalized.contains("spider")) return EntityType.SPIDER;
        return EntityType.ZOMBIE;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String safeKey(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_/.-]", "_");
    }

    @Override
    public boolean isMob(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        return MobManager.matchesInternalMobValue(this, value);
    }
}
