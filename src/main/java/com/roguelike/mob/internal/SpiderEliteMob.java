package com.roguelike.mob.internal;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.mob.InternalMob;
import com.roguelike.util.Message;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Spider;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SpiderEliteMob implements InternalMob {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private final ConfigManager.InternalMobDefinition definition;
    private final NamespacedKey mobKey;

    public SpiderEliteMob(RoguelikePlugin plugin, ConfigManager.InternalMobDefinition definition) {
        this.definition = definition;
        this.mobKey = new NamespacedKey(plugin, "internal_mob");
    }

    @Override
    public String id() {
        return definition.id();
    }

    @Override
    public java.util.List<String> aliases() {
        return definition.aliases();
    }

    @Override
    public boolean spawnable() {
        return definition.spawnable();
    }

    @Override
    public void onSpawn(LivingEntity entity) {
        ConfigManager.SpiderEliteConfig config = ConfigManager.getSpiderEliteConfig(id());
        if (!config.enabled()) return;
        if (!(entity instanceof Spider spider)) return;
        if (spider.getEntitySpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (RANDOM.nextDouble() >= config.spawnChance()) return;

        apply(spider, config);
    }

    @Override
    public LivingEntity spawn(Location location) {
        Spider spider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        apply(spider, ConfigManager.getSpiderEliteConfig(id()));
        return spider;
    }

    private void apply(Spider spider, ConfigManager.SpiderEliteConfig config) {
        spider.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, id());
        spider.customName(Message.toComponent(config.name()));
        spider.setCustomNameVisible(false);
        spider.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));

        var health = spider.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(config.health());
            spider.setHealth(config.health());
        }

        var speed = spider.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * config.speedMultiplier());
        }
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker) || !isMob(attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ConfigManager.SpiderEliteConfig config = ConfigManager.getSpiderEliteConfig(id());
        if (!MobCombatScript.actionEnabled(config.combatScript(), "slow-on-hit", true)) return;
        if (RANDOM.nextDouble() < config.slowChance()) {
            int amplifier = Math.max(0, config.slowLevel() - 1);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    (int) (config.slowDurationSeconds() * 20), amplifier));
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isMob(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        return com.roguelike.mob.MobManager.matchesInternalMobValue(this, value);
    }
}
