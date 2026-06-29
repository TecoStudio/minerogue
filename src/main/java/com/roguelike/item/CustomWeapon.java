package com.roguelike.item;

import java.util.HashMap;
import java.util.Map;

public class CustomWeapon {
    private final String id;
    private final String name;
    private final String description;
    private final String item;
    private final double baseDamage;
    private final double attackSpeed;
    private final int durability;
    private final String rarity;
    private final Map<String, Double> effects;

    public CustomWeapon(String id, String name, String description, String item, double baseDamage,
                        double attackSpeed, int durability, String rarity, Map<String, Double> effects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.item = item;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
        this.durability = durability;
        this.rarity = rarity;
        this.effects = effects != null ? new HashMap<>(effects) : new HashMap<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getItem() { return item; }
    public double getBaseDamage() { return baseDamage; }
    public double getAttackSpeed() { return attackSpeed; }
    public int getDurability() { return durability; }
    public String getRarity() { return rarity; }

    public Map<String, Double> getEffects() {
        return new HashMap<>(effects);
    }

    public double getEffect(String key) {
        return effects.getOrDefault(key, 0.0);
    }

    public double getEffect(String key, double defaultValue) {
        return effects.getOrDefault(key, defaultValue);
    }

    public double getFireDamage() { return getEffect("fire_damage", 0.0); }
    public double getFireDuration() { return getEffect("fire_duration", 0.0); }
    public double getLightningChance() { return getEffect("lightning_chance", 0.0); }
    public double getStunDuration() { return getEffect("stun_duration", 0.0); }

    public double getAttackRange() { return getEffect("attack_range", 3.0); }
    public double getLifestealPercent() { return getEffect("lifesteal_percent", 0.0); }
    public double getLifestealFlat() { return getEffect("lifesteal_flat", 0.0); }
    public double getSlowDuration() { return getEffect("slow_duration", 0.0); }
    public int getSlowLevel() { return (int) getEffect("slow_level", 0.0); }
    public int getChainTargets() { return (int) getEffect("chain_targets", 0.0); }
    public double getChainRange() { return getEffect("chain_range", 0.0); }
    public double getChainDamagePercent() { return getEffect("chain_damage_percent", 0.0); }
    public double getDamageStorePercent() { return getEffect("damage_store_percent", 0.0); }
    public double getDamageStoreMax() { return getEffect("damage_store_max", 0.0); }
    public double getCritChance() { return getEffect("crit_chance", 0.0); }
    public double getCritDamage() { return getEffect("crit_damage", 1.5); }
    public double getBleedChance() { return getEffect("bleed_chance", 0.0); }
    public double getBleedDamage() { return getEffect("bleed_damage", 0.0); }
    public double getBleedDuration() { return getEffect("bleed_duration", 0.0); }
}
