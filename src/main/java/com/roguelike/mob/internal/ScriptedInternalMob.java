package com.roguelike.mob.internal;

import com.roguelike.RoguelikePlugin;
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
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
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
        return definition.spawnable();
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
        EntityType type = templateEntityType(definition.template());
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        apply(entity, ConfigManager.getScriptedMobConfig(id()));
        return entity;
    }

    private void apply(LivingEntity entity, ConfigManager.ScriptedMobConfig config) {
        entity.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, id());
        entity.customName(Message.toComponent(config.name()));
        entity.setCustomNameVisible(false);
        entity.setRemoveWhenFarAway(false);
        bossBars.put(entity.getUniqueId(), Bukkit.createBossBar(displayName(config.name()), BarColor.RED, BarStyle.SEGMENTED_10));

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
    }

    private void equipTemplate(LivingEntity entity, ConfigManager.ScriptedMobConfig config) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;
        String template = normalize(definition.template());
        if (template.contains("skeleton")) {
            equipment.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            equipment.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            equipment.setItemInOffHand(new ItemStack(Material.CLOCK));
        } else {
            equipment.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            equipment.setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
        }
        equipConfiguredWeapon(equipment);
        equipment.setHelmetDropChance(0.01f);
        equipment.setChestplateDropChance(0.01f);
        equipment.setLeggingsDropChance(0.01f);
        equipment.setBootsDropChance(0.01f);
        equipment.setItemInMainHandDropChance(0.05f);
        equipment.setItemInOffHandDropChance(0.03f);
    }

    private void equipConfiguredWeapon(EntityEquipment equipment) {
        String weaponTemplate = definition.weaponTemplate();
        if (weaponTemplate == null || weaponTemplate.isBlank()) return;
        CustomWeapon template = ConfigManager.getWeapon(weaponTemplate);
        if (template == null) return;
        equipment.setItemInMainHand(WeaponManager.createWeaponStack(template, null));
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker) || !isMob(attacker)) return;
        if (event.getEntity() instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) {
            event.setCancelled(true);
            return;
        }
        ConfigManager.ScriptedMobConfig config = ConfigManager.getScriptedMobConfig(id());
        double damage = config.damage();
        WeaponInstanceData data = WeaponInstanceData.fromItemStack(attacker.getEquipment() == null ? null : attacker.getEquipment().getItemInMainHand());
        CustomWeapon template = data == null ? null : ConfigManager.getWeapon(data.getBaseWeaponId());
        boolean wasPoisoned = event.getEntity() instanceof LivingEntity target && target.hasPotionEffect(PotionEffectType.POISON);
        if (template != null && data != null) {
            damage = weaponDamage(template, data, damage, wasPoisoned);
        }
        event.setDamage(damage);
        if (event.getEntity() instanceof LivingEntity target) {
            if (hasAction("slow-on-hit")) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 0));
            }
            if (template != null && data != null && RANDOM.nextDouble() < weaponPoisonChance(template, data)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            }
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
        for (ConfigManager.ActionDefinition action : definition.actions()) {
            if (!conditionMatches(action.when(), distance, config)) continue;
            runAction(action, entity, target, config);
        }
    }

    private boolean hasAction(String name) {
        for (ConfigManager.ActionDefinition action : definition.actions()) {
            if (name.equalsIgnoreCase(action.action())) return true;
        }
        return false;
    }

    private boolean conditionMatches(String when, double distance, ConfigManager.ScriptedMobConfig config) {
        String condition = normalize(when).replace('_', '-');
        return switch (condition) {
            case "target-close" -> distance <= config.skillRange();
            case "target-far" -> distance > config.skillRange() && distance <= config.detectRange();
            case "target-detected" -> distance <= config.detectRange();
            case "always", "after melee-burst" -> true;
            default -> false;
        };
    }

    private void runAction(ConfigManager.ActionDefinition action, LivingEntity entity, LivingEntity target,
                           ConfigManager.ScriptedMobConfig config) {
        switch (normalize(action.action()).replace('_', '-')) {
            case "melee-burst" -> meleeBurst(entity, target, config, action.hits());
            case "retreat" -> retreat(entity, target);
            case "leap" -> leapToward(entity, target, config);
            case "shockwave" -> shockwave(entity, config);
            case "blink" -> blinkBehind(entity, target, config);
            case "blade-storm" -> bladeStorm(entity, config);
            default -> {
            }
        }
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

    private void meleeBurst(LivingEntity entity, LivingEntity target, ConfigManager.ScriptedMobConfig config, int hits) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextPrimaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        entity.getPersistentDataContainer().set(nextPrimaryKey, PersistentDataType.LONG, now + config.skillCooldownTicks());
        int count = Math.max(1, hits);
        for (int i = 0; i < count; i++) {
            target.damage(config.skillDamage(), entity);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
    }

    private void retreat(LivingEntity entity, LivingEntity target) {
        Vector direction = entity.getLocation().toVector().subtract(target.getLocation().toVector());
        if (direction.lengthSquared() <= 0.001) return;
        entity.setVelocity(direction.normalize().multiply(0.65).setY(0.25));
    }

    private void shockwave(LivingEntity entity, ConfigManager.ScriptedMobConfig config) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextPrimaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        entity.getPersistentDataContainer().set(nextPrimaryKey, PersistentDataType.LONG, now + config.skillCooldownTicks());
        Location origin = entity.getLocation();
        entity.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.75f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, origin.add(0, 0.1, 0), 1);
        double radius = config.skillRange();
        for (var nearby : entity.getWorld().getNearbyEntities(entity.getLocation(), radius, 1.8, radius)) {
            if (!(nearby instanceof LivingEntity target) || target.equals(entity)) continue;
            if (target instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) continue;
            target.damage(config.skillDamage(), entity);
            Vector knockback = target.getLocation().toVector().subtract(entity.getLocation().toVector());
            if (knockback.lengthSquared() > 0.001) target.setVelocity(knockback.normalize().multiply(0.9).setY(0.35));
        }
    }

    private void leapToward(LivingEntity entity, LivingEntity target, ConfigManager.ScriptedMobConfig config) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextSecondaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        entity.getPersistentDataContainer().set(nextSecondaryKey, PersistentDataType.LONG, now + Math.max(20L, config.skillCooldownTicks() / 2));
        Vector direction = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        if (direction.lengthSquared() <= 0.001) return;
        entity.setVelocity(direction.normalize().multiply(0.85).setY(0.35));
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 0.7f, 0.8f);
    }

    private void blinkBehind(LivingEntity entity, LivingEntity target, ConfigManager.ScriptedMobConfig config) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextPrimaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        entity.getPersistentDataContainer().set(nextPrimaryKey, PersistentDataType.LONG, now + Math.max(30L, config.skillCooldownTicks()));
        Vector facing = target.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() <= 0.001) return;
        Location destination = target.getLocation().subtract(facing.normalize().multiply(2.2));
        destination.setY(target.getLocation().getY());
        if (!destination.getBlock().isPassable()) return;
        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 24, 0.3, 0.6, 0.3);
        entity.teleport(destination);
        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 24, 0.3, 0.6, 0.3);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
    }

    private void bladeStorm(LivingEntity entity, ConfigManager.ScriptedMobConfig config) {
        long now = entity.getWorld().getGameTime();
        Long next = entity.getPersistentDataContainer().get(nextSecondaryKey, PersistentDataType.LONG);
        if (next != null && next > now) return;
        entity.getPersistentDataContainer().set(nextSecondaryKey, PersistentDataType.LONG, now + Math.max(40L, config.skillCooldownTicks() / 2));
        double radius = config.skillRange();
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
        entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, entity.getLocation().add(0, 1, 0), 8, radius / 2, 0.2, radius / 2);
        for (var nearby : entity.getWorld().getNearbyEntities(entity.getLocation(), radius, 2.0, radius)) {
            if (!(nearby instanceof LivingEntity target) || target.equals(entity)) continue;
            if (target instanceof Player player && !MobManager.shouldBossAffectPlayer(player.getGameMode(), player.isDead())) continue;
            target.damage(config.skillDamage(), entity);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
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
