package com.roguelike.equipment.affix;

import java.util.Map;

/**
 * 静态机制说明文本，供词条图鉴 GUI 展示。
 * 武器词条的实际 lore 是按武器数据动态拼接的，这里提供一句精简机制说明。
 */
public final class AffixDescriptions {
    private static final Map<String, String> WEAPON = Map.ofEntries(
            Map.entry("lifesteal_percent", "造成伤害时按伤害百分比回复自身生命。"),
            Map.entry("lifesteal_flat", "造成伤害时回复固定数值生命。"),
            Map.entry("chain_targets", "命中时连锁攻击附近额外目标，目标数越多范围越广。"),
            Map.entry("chain_range", "连锁命中的最大作用距离。"),
            Map.entry("chain_damage_percent", "连锁目标受到的伤害比例。"),
            Map.entry("crit_chance", "攻击触发暴击的概率。暴击自身受到伤害 200%。"),
            Map.entry("crit_damage", "暴击造成的伤害倍率。"),
            Map.entry("crit_lifesteal_percent", "仅在暴击时吸取伤害一定比例生命，与普通吸血叠加。"),
            Map.entry("fire_damage", "命中附加火焰伤害并点燃目标。自身受到伤害 200%。"),
            Map.entry("fire_duration", "火焰点燃的持续时间。"),
            Map.entry("lightning_chance", "攻击概率召唤雷电劈击目标。自身受到伤害 200%。"),
            Map.entry("slow_duration", "命中减速目标的时长。"),
            Map.entry("slow_level", "减速效果的等级。"),
            Map.entry("damage_store_percent", "储存部分受到的伤害，攒够次数后释放。自身受到伤害 200%。"),
            Map.entry("damage_store_hit_reduction", "减少伤害储存所需的命中次数。"),
            Map.entry("burning_target_damage_percent", "对正在燃烧的目标额外增伤。自身受到伤害 200%。"),
            Map.entry("poisoned_target_damage_percent", "对中毒目标额外增伤。自身受到伤害 200%。"),
            Map.entry("poison_chance", "攻击概率使目标中毒。"),
            Map.entry("explosion_chance", "攻击概率引发爆炸。自身受到伤害 200%。"),
            Map.entry("big_explosion_chance", "攻击概率引发大范围爆炸。自身受到伤害 200%。"),
            Map.entry("victim_explosion_chance", "击杀敌人时概率引爆其尸体，适合高爆发清怪。"),
            Map.entry("smash", "主动：3倍伤害且力量效果翻倍，使用后冷却 7 秒。自身受到伤害 200%。"),
            Map.entry("hyper", "暴击后获得速度与急迫，等级越高效果越强，持续 3 秒。"),
            Map.entry("gift", "击杀后 7 秒内回复 50% 生命并获得抗性提升。"),
            Map.entry("momentum", "连击叠层，每层提升伤害（上限 20 层），受击或 2 秒未命中清零。"),
            Map.entry("durability_restore", "耐久消耗时概率返还耐久，等级越高概率越高。"),
            Map.entry("ore_highlight", "挖掘时概率高亮附近矿物。"),
            Map.entry("crazy_miner", "仅镐：手持时永久急迫 III，挖方块概率获得饱和；触发后 30 秒冷却。"),
            Map.entry("scatter_shot", "仅弓：一次拉弓发射多支箭。"),
            Map.entry("rapid_shot", "仅弓：射出后短延迟追加多支箭。"),
            Map.entry("charge_power", "仅弓：满弓后继续蓄力提高伤害，等级越高提升越大。"),
            Map.entry("contract_damage_200", "对敌伤害 +200%，但自身受到伤害 +200%。"),
            Map.entry("contract_speed_200", "移动速度 +200%，但自身受到伤害 +200%。"),
            Map.entry("contract_attack_speed_200", "攻击速度 +200%，但自身受到伤害 +200%。"),
            Map.entry("contract_range_200", "攻击距离 +200%，但自身受到伤害 +200%。"),
            Map.entry("contract_crit_chance_100", "暴击率 +100%，但自身受到伤害 +200%。"),
            Map.entry("contract_crit_damage_300", "暴击伤害 +300%，但自身受到伤害 +200%。"),
            Map.entry("contract_lifesteal_100", "吸血 +100%，但自身受到伤害 +200%。"),
            Map.entry("contract_thunder_100", "攻击必定雷击，但自身受到伤害 +200%。"),
            Map.entry("contract_explosion_100", "攻击必定爆炸，但自身受到伤害 +200%。"),
            Map.entry("contract_berserk_self_harm", "对敌伤害 +300%，但每次命中自损最大生命 10%。")
    );

    private static final Map<String, String> ARMOR = Map.ofEntries(
            Map.entry("dash", "仅护腿：空中潜行突进，2 次充能 / 5 秒冷却。"),
            Map.entry("thorns", "受击时反弹伤害，按套装件数提升反伤。"),
            Map.entry("swift", "提升移动速度，4 件套强化 Dash。"),
            Map.entry("explosive", "受击时概率爆炸反击周围敌人。"),
            Map.entry("guardian", "每件 6% 插件减伤，4 件套上限 60%。"),
            Map.entry("vampire", "击杀敌人回复生命。"),
            Map.entry("storm", "攻击时概率召唤雷击目标。"),
            Map.entry("protection", "原版保护附魔入口，减少各类伤害。")
    );

    private static final Map<String, String> BASE = Map.of(
            "damage", "武器基础伤害，决定每次命中造成的伤害。",
            "attack_speed", "攻击速度，影响每秒攻击次数（上限 20/秒，超出转为额外伤害）。",
            "attack_range", "攻击距离，近战可触及的范围。"
    );

    private AffixDescriptions() {
    }

    public static String weapon(String id) {
        return WEAPON.getOrDefault(id, "—");
    }

    public static String armor(String id) {
        return ARMOR.getOrDefault(id, "—");
    }

    public static String base(String stat) {
        return BASE.getOrDefault(stat, "—");
    }
}
