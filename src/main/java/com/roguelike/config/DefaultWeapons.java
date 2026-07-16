package com.roguelike.config;

import com.roguelike.item.CustomWeapon;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultWeapons {
    private DefaultWeapons() {
    }

    public static Map<String, CustomWeapon> create() {
        Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
        add(weapons, weapon("wooden_sword", "minecraft:wooden_sword", "木剑", "最基础的武器", 4, 1.6, 59, "common",
                effects("attack_range", 3.0, "lifesteal_percent", 0.0, "slow_duration", 0.0, "chain_targets", 0.0,
                        "chain_range", 0.0, "chain_damage_percent", 0.0, "damage_store_percent", 0.0,
                        "damage_store_hit_reduction", 0.0, "crit_chance", 0.05, "crit_damage", 1.5)));
        add(weapons, weapon("flame_sword", "minecraft:diamond_sword", "烈焰之剑", "燃烧敌人的剑", 10, 1.4, 800, "epic",
                effects("attack_range", 3.2, "fire_damage", 4.0, "fire_duration", 3.0, "crit_chance", 0.1, "crit_damage", 1.75)));
        add(weapons, weapon("vampire_dagger", "minecraft:iron_sword", "吸血匕首", "从敌人身上吸血", 5, 2.4, 600, "rare",
                effects("attack_range", 2.8, "lifesteal_percent", 0.15, "crit_chance", 0.08, "crit_damage", 1.6)));
        add(weapons, weapon("ember_knife", "minecraft:stone_sword", "余烬短刃", "快速点燃敌人的轻型短刃", 5, 2.0, 180, "common",
                effects("attack_range", 2.7, "fire_damage", 2.0, "fire_duration", 2.0, "crit_chance", 0.08)));
        add(weapons, weapon("frost_cleaver", "minecraft:iron_axe", "霜裂斧", "减缓敌人的沉重战斧", 11, 0.9, 520, "rare",
                effects("attack_range", 3.0, "slow_duration", 2.5, "slow_level", 1.0, "crit_damage", 1.8)));
        add(weapons, weapon("storm_spear", "minecraft:trident", "风暴长矛", "延伸攻击距离并引雷的长矛", 8, 1.2, 650, "rare",
                effects("attack_range", 4.0, "lightning_chance", 0.10, "crit_chance", 0.08)));
        add(weapons, weapon("plague_saber", "minecraft:golden_sword", "疫毒军刀", "对中毒目标造成更高伤害", 9, 1.5, 500, "epic",
                effects("attack_range", 3.0, "poison_chance", 0.35, "poisoned_target_damage_percent", 0.30,
                        "crit_chance", 0.12, "crit_damage", 1.7)));
        add(weapons, weapon("echo_blade", "minecraft:diamond_sword", "回响之刃", "攻击会向周围敌人回响", 10, 1.3, 900, "epic",
                effects("attack_range", 3.2, "chain_targets", 4.0, "chain_range", 3.5, "chain_damage_percent", 0.45,
                        "crit_chance", 0.10)));
        add(weapons, weapon("glass_cannon_hammer", "minecraft:netherite_axe", "玻璃重锤", "攻速很慢但爆发极高的重锤", 20, 0.7, 1600, "legendary",
                effects("attack_range", 3.1, "crit_chance", 0.20, "crit_damage", 2.4, "damage_store_percent", 0.20,
                        "damage_store_hit_reduction", 4.0)));
        add(weapons, weapon("thunder_axe", "minecraft:diamond_axe", "雷霆战斧", "几率召唤雷电", 14, 0.9, 1200, "legendary",
                effects("attack_range", 3.0, "lightning_chance", 0.15, "crit_chance", 0.12, "crit_damage", 2.0)));
        add(weapons, weapon("whirlwind_blade", "minecraft:iron_sword", "旋风之刃", "攻击会波及周围敌人", 9, 1.3, 1000, "epic",
                effects("attack_range", 3.1, "chain_targets", 3.0, "chain_range", 3.0, "chain_damage_percent", 0.5, "crit_chance", 0.1)));
        add(weapons, weapon("inferno_greatsword", "minecraft:netherite_sword", "炼狱巨剑", "大范围火焰伤害", 18, 0.8, 2000, "legendary",
                effects("attack_range", 3.5, "fire_damage", 6.0, "fire_duration", 4.0, "chain_targets", 4.0,
                        "chain_range", 4.0, "chain_damage_percent", 0.4, "crit_chance", 0.15, "crit_damage", 2.0)));
        add(weapons, weapon("phase_scythe", "minecraft:iron_hoe", "相位之镰", "撕裂相位的史诗镰刀", 7, 3.2, 250, "epic",
                effects("attack_range", 3.0, "dash", 1.0, "hyper", 1.0, "crit_chance", 0.30)));
        add(weapons, specialWeapon("special_weapon", "minecraft:wooden_sword", "特殊武器", "由开发券唤醒的武器胚子", 3, 1.6, 250, "special",
                effects("attack_range", 3.0), 0, true));
        add(weapons, weapon("rusty_iron_sword", "minecraft:iron_sword", "生锈的铁剑", "骷髅精英使用的破旧铁剑", 5, 1.6, 250, "common",
                effects("attack_range", 3.0, "poison_chance", 0.30, "poisoned_target_damage_percent", 0.10)));
        add(weapons, weapon("excited_stone_sword", "minecraft:stone_sword", "石剑", "僵尸精英使用的亢奋石剑", 5, 1.6, 131, "common",
                effects("attack_range", 3.0, "crit_chance", 0.10, "hyper", 1.0)));
        return weapons;
    }

    private static void add(Map<String, CustomWeapon> weapons, CustomWeapon weapon) {
        weapons.put(weapon.getId().toLowerCase(), weapon);
    }

    private static CustomWeapon weapon(String id, String item, String name, String description, double damage,
                                       double attackSpeed, int durability, String rarity, Map<String, Double> effects) {
        return new CustomWeapon(id, name, description, item, damage, attackSpeed, durability, rarity, effects);
    }

    private static CustomWeapon specialWeapon(String id, String item, String name, String description, double damage,
                                              double attackSpeed, int durability, String rarity, Map<String, Double> effects,
                                              int bonusAffixSlots, boolean allowOverflowAffixes) {
        return new CustomWeapon(id, name, description, item, damage, attackSpeed, durability, rarity, effects,
                bonusAffixSlots, allowOverflowAffixes, "");
    }

    private static Map<String, Double> effects(Object... values) {
        Map<String, Double> effects = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            effects.put((String) values[i], ((Number) values[i + 1]).doubleValue());
        }
        return effects;
    }
}
