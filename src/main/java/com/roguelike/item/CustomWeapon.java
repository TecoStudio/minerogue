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
    private final int bonusAffixSlots;
    private final boolean allowOverflowAffixes;
    private final String legendaryAffix;

    public CustomWeapon(String id, String name, String description, String item, double baseDamage,
                        double attackSpeed, int durability, String rarity, Map<String, Double> effects) {
        this(id, name, description, item, baseDamage, attackSpeed, durability, rarity, effects, 0, false, "");
    }

    public CustomWeapon(String id, String name, String description, String item, double baseDamage,
                        double attackSpeed, int durability, String rarity, Map<String, Double> effects,
                        int bonusAffixSlots, boolean allowOverflowAffixes, String legendaryAffix) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.item = item;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
        this.durability = durability;
        this.rarity = rarity;
        this.effects = effects != null ? new HashMap<>(effects) : new HashMap<>();
        this.bonusAffixSlots = Math.max(0, bonusAffixSlots);
        this.allowOverflowAffixes = allowOverflowAffixes;
        this.legendaryAffix = legendaryAffix == null ? "" : legendaryAffix;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getItem() { return item; }
    public double getBaseDamage() { return baseDamage; }
    public double getAttackSpeed() { return attackSpeed; }
    public int getDurability() { return durability; }
    public String getRarity() { return rarity; }
    public int getBonusAffixSlots() { return bonusAffixSlots; }
    public boolean allowsOverflowAffixes() { return allowOverflowAffixes; }
    public String getLegendaryAffix() { return legendaryAffix; }

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

    public double getAttackRange() { return getEffect("attack_range", 3.0); }
    public double getLifestealPercent() { return getEffect("lifesteal_percent", 0.0); }
    public double getLifestealFlat() { return getEffect("lifesteal_flat", 0.0); }
    public double getSlowDuration() { return getEffect("slow_duration", 0.0); }
    public int getSlowLevel() { return (int) getEffect("slow_level", 0.0); }
    public int getChainTargets() { return (int) getEffect("chain_targets", 0.0); }
    public double getChainRange() { return getEffect("chain_range", 0.0); }
    public double getChainDamagePercent() { return getEffect("chain_damage_percent", 0.0); }
    public double getDamageStorePercent() { return getEffect("damage_store_percent", 0.0); }
    public int getDamageStoreHitReduction() { return (int) getEffect("damage_store_hit_reduction", 0.0); }
    public double getCritChance() { return getEffect("crit_chance", 0.0); }
    public double getCritDamage() { return getEffect("crit_damage", 1.5); }
    public double getBurningTargetDamagePercent() { return getEffect("burning_target_damage_percent", 0.0); }
    public double getPoisonedTargetDamagePercent() { return getEffect("poisoned_target_damage_percent", 0.0); }
    public double getPoisonChance() { return getEffect("poison_chance", 0.0); }
    public double getExplosionChance() { return getEffect("explosion_chance", 0.0); }
    public double getBigExplosionChance() { return getEffect("big_explosion_chance", 0.0); }
}
