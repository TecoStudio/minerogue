package com.roguelike.weapon.affix;

import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.equipment.EquipmentKind;
import com.roguelike.equipment.EquipmentTypeResolver;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WeaponAffixManager {
    private static final Map<String, WeaponAffix> AFFIXES = new LinkedHashMap<>();
    private static final Map<String, Target> TARGETS = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, String> SYNERGY_HINTS = new LinkedHashMap<>();

    private enum Target { WEAPON, BOW, TOOL, ALL }

    static {
        register(percent("lifesteal_percent", "吸血百分比", 0.10, 0.20, (lore, template, data) -> {
            double lifePercent = total(template, data, "lifesteal_percent", 0.0);
            double lifeFlat = total(template, data, "lifesteal_flat", 0.0);
            if (lifePercent > 0 || lifeFlat > 0) {
                String text = "§c❤ 吸血: §f" + WeaponManager.format(lifePercent * 100, 0) + "%";
                if (lifeFlat > 0) text += " +" + WeaponManager.format(lifeFlat, 1);
                lore.add(Message.toComponent(text));
            }
        }));
        register(number("lifesteal_flat", "吸血固定值", 1.0, 3.0, null));
        register(number("chain_targets", "连锁目标", 1, 3, (lore, template, data) -> {
            int chainTargets = (int) total(template, data, "chain_targets", 0.0);
            double chainRange = total(template, data, "chain_range", 0.0);
            if (chainTargets > 0) lore.add(Message.toComponent("§6⚖ 连锁: §f" + chainTargets + "目标 (" + WeaponManager.format(chainRange, 1) + "格)，受到伤害 200%"));
        }, true));
        register(number("chain_range", "连锁范围", 2.0, 4.0, null));
        register(percent("chain_damage_percent", "连锁伤害", 0.30, 0.50, null));
        register(percent("crit_chance", "暴击率", 0.05, 0.15, (lore, template, data) -> {
            double critChance = chance(total(template, data, "crit_chance", 0.0));
            double critDamage = total(template, data, "crit_damage", 1.5);
            if (critChance > 0) lore.add(Message.toComponent("§6⚖ 暴击: §f" + WeaponManager.format(critChance * 100, 0) + "% (" + WeaponManager.format(critDamage, 1) + "x)，受到伤害 200%"));
        }));
        register(number("crit_damage", "暴击倍率", 1.5, 2.0, null));
        register(number("fire_damage", "火焰伤害", 2.0, 5.0, (lore, template, data) -> {
            double fireDamage = total(template, data, "fire_damage", 0.0);
            double fireDuration = total(template, data, "fire_duration", 0.0);
            if (fireDamage > 0) lore.add(Message.toComponent("§6⚖ 火焰: §f" + WeaponManager.format(fireDamage, 1) + "伤害 (" + WeaponManager.format(fireDuration, 1) + "s)，受到伤害 200%"));
        }));
        register(number("fire_duration", "燃烧时长", 2.0, 5.0, null));
        register(percent("lightning_chance", "雷电概率", 0.05, 0.15, (lore, template, data) -> {
            double value = chance(total(template, data, "lightning_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§6⚖ 雷电: §f" + WeaponManager.format(value * 100, 0) + "%，受到伤害 200%"));
        }));
        register(number("slow_duration", "减速时长", 1.0, 3.0, (lore, template, data) -> {
            double slowDuration = total(template, data, "slow_duration", 0.0);
            int slowLevel = (int) total(template, data, "slow_level", 0.0);
            if (slowDuration > 0) lore.add(Message.toComponent("§9❄ 减速: §f" + (slowLevel + 1) + "级 (" + WeaponManager.format(slowDuration, 1) + "s)"));
        }));
        register(number("slow_level", "减速等级", 1, 2, null, true));
        register(percent("damage_store_percent", "伤害存储率", 0.10, 0.25, (lore, template, data) -> {
            double storePercent = total(template, data, "damage_store_percent", 0.0);
            if (storePercent > 0) lore.add(Message.toComponent("§6⚖ 伤害储存: §f" + WeaponManager.format(storePercent * 100, 0) + "% / " + damageStoreRequiredHits(template, data) + "次，受到伤害 200%"));
        }));
        register(new SimpleAffix("damage_store_hit_reduction", "伤害储存次数减少", false, 1, 1, false, null) {
            @Override
            public boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data) {
                return total(template, data, "damage_store_percent", 0.0) > 0.0 && total(template, data, id(), 0.0) < 15.0;
            }

            @Override
            public double strengthen(double currentValue, int useCount, Random random) {
                return Math.min(15, currentValue + 1);
            }
        });
        register(percent("burning_target_damage_percent", "燃烧目标增伤", 0.15, 0.40, (lore, template, data) -> {
            double value = total(template, data, "burning_target_damage_percent", 0.0);
            if (value > 0) lore.add(Message.toComponent("§6⚖ 燃烧增伤: §f" + WeaponManager.format(value * 100, 0) + "%，受到伤害 200%"));
        }));
        register(percent("poisoned_target_damage_percent", "中毒目标增伤", 0.15, 0.40, (lore, template, data) -> {
            double value = total(template, data, "poisoned_target_damage_percent", 0.0);
            if (value > 0) lore.add(Message.toComponent("§6⚖ 中毒增伤: §f" + WeaponManager.format(value * 100, 0) + "%，受到伤害 200%"));
        }));
        register(percent("poison_chance", "中毒概率", 0.10, 0.30, (lore, template, data) -> {
            double value = chance(total(template, data, "poison_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§2☠ 中毒概率: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }), Target.WEAPON, "状态附加");
        register(percent("bleed_chance", "流血概率", 0.08, 0.25, (lore, template, data) -> {
            double value = chance(total(template, data, "bleed_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§4🩸 流血概率: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }), Target.WEAPON, "状态附加");
        register(percent("bleeding_target_damage_percent", "流血目标增伤", 0.15, 0.45, (lore, template, data) -> {
            double value = total(template, data, "bleeding_target_damage_percent", 0.0);
            if (value > 0) lore.add(Message.toComponent("§4⚖ 流血增伤: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }), Target.WEAPON, "协同增伤", "对已流血目标造成更高直接伤害，可与流血概率词条自洽成套。");
        register(percent("explosion_chance", "爆炸概率", 0.05, 0.15, (lore, template, data) -> {
            double value = chance(total(template, data, "explosion_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§6⚖ 爆炸概率: §f" + WeaponManager.format(value * 100, 0) + "%，受到伤害 200%"));
        }));
        register(percent("big_explosion_chance", "大爆炸概率", 0.02, 0.08, (lore, template, data) -> {
            double value = chance(total(template, data, "big_explosion_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§6⚖ 大爆炸概率: §f" + WeaponManager.format(value * 100, 0) + "%，受到伤害 200%"));
        }));
        register(percent("victim_explosion_chance", "击杀爆炸概率", 0.05, 0.20, (lore, template, data) -> {
            double value = chance(total(template, data, "victim_explosion_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§6☄ 击杀爆炸: §f" + WeaponManager.format(value * 100, 0) + "% §7(击杀时触发)"));
        }), Target.WEAPON, "击杀触发", "击杀敌人时有概率引爆尸体，适合高爆发和连锁清怪武器。");
        register(toggle("smash", "猛击", "§6⚖ 猛击: §f3倍伤害，力量效果翻倍，使用后冷却7秒，受到伤害 200%", false));
        register(toggle("bomb", "小心炸弹！", "§6☄ 小心炸弹！: §f潜行投掷常规大爆炸TNT，20格或3秒后爆炸，30秒冷却", false));
        register(new SimpleAffix("hyper", "亢奋", true, 1, 1, true, (lore, template, data) -> {
            int hyper = (int) total(template, data, "hyper", 0.0);
            if (hyper > 0) lore.add(Message.toComponent("§b✦ 亢奋: §f暴击后速度" + hyper + "、急迫" + hyper + " 3秒"));
        }) {
            @Override
            public boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data) {
                double value = total(template, data, id(), 0.0);
                return value > 0 && value < 3;
            }

            @Override
            public double strengthen(double currentValue, int useCount, Random random) {
                return Math.min(3, currentValue + 1);
            }
        });
        register(toggle("gift", "馈赠", "§d❤ 馈赠: §f击杀后7秒回复50%生命并获得抗性提升", false));
        register(level("durability_restore", "用不坏", 1, 5, (lore, template, data) -> {
            int level = (int) total(template, data, "durability_restore", 0.0);
            if (level > 0) lore.add(Message.toComponent("§a✦ 用不坏: §f" + level + "级 (" + WeaponManager.format(durabilityRestoreChance(level) * 100, 0) + "%返还3耐久)"));
        }), Target.ALL);
        register(toggle("ore_highlight", "高亮矿物", "§e✦ 高亮矿物: §f挖掘时10%概率高亮附近矿物1秒", false), Target.TOOL);
        register(new SimpleAffix("scatter_shot", "散射", true, 2, 5, true, (lore, template, data) -> {
            int arrows = (int) total(template, data, "scatter_shot", 0.0);
            if (arrows > 0) lore.add(Message.toComponent("§b➹ 散射: §f总计发射 " + arrows + " 支箭"));
        }) {
            @Override
            public boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data) {
                double value = total(template, data, id(), 0.0);
                return value > 0 && value < 5;
            }

            @Override
            public double strengthen(double currentValue, int useCount, Random random) {
                return Math.min(5, currentValue + 1);
            }

            @Override
            public String format(double value) {
                return (int) value + "支";
            }
        }, Target.BOW);
        register(level("rapid_shot", "连发", 1, 5, (lore, template, data) -> {
            int level = (int) total(template, data, "rapid_shot", 0.0);
            if (level > 0) lore.add(Message.toComponent("§b➹ 连发: §f短延迟追加 " + level + " 支箭"));
        }), Target.BOW);
        register(level("charge_power", "蓄能", 1, 5, (lore, template, data) -> {
            int level = (int) total(template, data, "charge_power", 0.0);
            if (level > 0) lore.add(Message.toComponent("§d➹ 蓄能: §f满弓后继续蓄力提高伤害，等级 " + level));
        }), Target.BOW);
        register(neutral("neutral_damage_200", "狂战契约", "§6⚖ 狂战契约: §f对敌伤害 200%，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_speed_200", "疾行契约", "§6⚖ 疾行契约: §f移动速度 200%，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_attack_speed_200", "急速契约", "§6⚖ 急速契约: §f攻击速度 200%，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_range_200", "远击契约", "§6⚖ 远击契约: §f攻击距离 200%，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_crit_chance_100", "精准契约", "§6⚖ 精准契约: §f暴击率 +100%，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_crit_damage_300", "处刑契约", "§6⚖ 处刑契约: §f暴击伤害 300%，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_lifesteal_100", "鲜血契约", "§6⚖ 鲜血契约: §f吸血 +100%，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_thunder_100", "引雷契约", "§6⚖ 引雷契约: §f攻击必定雷击，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_explosion_100", "爆裂契约", "§6⚖ 爆裂契约: §f攻击必定爆炸，受到伤害 200%"), Target.ALL);
        register(neutral("neutral_berserk_self_harm", "血怒契约", "§6⚖ 血怒契约: §f对敌伤害 300%，每次命中自损最大生命 10%"), Target.ALL);
    }

    public static List<String> effectIds() {
        return List.copyOf(AFFIXES.keySet());
    }

    public static List<String> rollableEffectIds() {
        return effectIds();
    }

    public static List<String> toolOnlyEffectIds() {
        List<String> ids = new ArrayList<>();
        for (String id : AFFIXES.keySet()) {
            if (isToolOnly(id)) ids.add(id);
        }
        return ids;
    }

    public static boolean isToolOnly(String id) {
        return TARGETS.getOrDefault(id, Target.WEAPON) == Target.TOOL;
    }

    public static WeaponAffix get(String id) {
        return AFFIXES.get(id);
    }

    public static String displayName(String id) {
        WeaponAffix affix = get(id);
        return affix == null ? id : affix.displayName();
    }

    public static String category(String id) {
        return CATEGORIES.getOrDefault(id, "通用词条");
    }

    public static String synergyHint(String id) {
        return SYNERGY_HINTS.getOrDefault(id, "");
    }

    public static List<String> scalingTags(CustomWeapon template, WeaponInstanceData data) {
        List<String> tags = new ArrayList<>();
        if (total(template, data, "fire_damage", 0.0) > 0
                || total(template, data, "burning_target_damage_percent", 0.0) > 0
                || total(template, data, "bleed_chance", 0.0) > 0
                || total(template, data, "bleeding_target_damage_percent", 0.0) > 0
                || total(template, data, "lifesteal_percent", 0.0) > 0
                || total(template, data, "smash", 0.0) > 0) {
            tags.add("暴虐");
        }
        if (total(template, data, "lightning_chance", 0.0) > 0
                || total(template, data, "chain_targets", 0.0) > 0
                || total(template, data, "chain_damage_percent", 0.0) > 0
                || total(template, data, "explosion_chance", 0.0) > 0
                || total(template, data, "big_explosion_chance", 0.0) > 0
                || total(template, data, "victim_explosion_chance", 0.0) > 0) {
            tags.add("战术");
        }
        if (total(template, data, "slow_duration", 0.0) > 0
                || total(template, data, "damage_store_percent", 0.0) > 0
                || total(template, data, "poison_chance", 0.0) > 0
                || total(template, data, "poisoned_target_damage_percent", 0.0) > 0
                || total(template, data, "gift", 0.0) > 0) {
            tags.add("生存");
        }
        if (tags.isEmpty()) tags.add("无色");
        return List.copyOf(tags);
    }

    public static String format(String id, double value) {
        WeaponAffix affix = get(id);
        return affix == null ? WeaponManager.format(value, 2) : affix.format(value);
    }

    public static double generateBaseValue(String id, Random random) {
        WeaponAffix affix = get(id);
        return affix == null ? random.nextDouble() * 0.2 : affix.generateBaseValue(random);
    }

    public static boolean isAvailable(CustomWeapon template, WeaponInstanceData data, String id) {
        return AFFIXES.containsKey(id) && template.getEffect(id, 0.0) == 0.0 && data.getEffectBonus(id) == 0.0;
    }

    public static boolean isAvailable(CustomWeapon template, WeaponInstanceData data, String id, Material material) {
        return isApplicable(id, material) && isAvailable(template, data, id);
    }

    public static boolean isApplicable(String id, Material material) {
        return isApplicable(id, EquipmentTypeResolver.resolve(material));
    }

    public static boolean isApplicable(String id, EquipmentKind kind) {
        Target target = TARGETS.getOrDefault(id, Target.WEAPON);
        // Pickaxes and axes are intentionally both tools and weapons in this plugin.
        // Target.WEAPON therefore applies to tools too; Target.TOOL only gates mining-only affixes.
        return target == Target.ALL
                || (target == Target.WEAPON && kind != EquipmentKind.BOW)
                || (target == Target.TOOL && kind == EquipmentKind.TOOL)
                || (target == Target.BOW && kind == EquipmentKind.BOW);
    }

    public static boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data, String id) {
        WeaponAffix affix = get(id);
        return affix != null && affix.isStrengthenable(template, data);
    }

    public static boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data, String id, Material material) {
        return isApplicable(id, material) && isStrengthenable(template, data, id);
    }

    public static double strengthen(String id, double currentValue, int useCount, Random random) {
        WeaponAffix affix = get(id);
        if (affix == null) return strengthenNumber(currentValue, useCount, random);
        return affix.strengthen(currentValue, useCount, random);
    }

    public static void appendLore(List<Component> lore, CustomWeapon template, WeaponInstanceData data) {
        for (WeaponAffix affix : AFFIXES.values()) {
            affix.appendLore(lore, template, data);
        }
    }

    private static void register(WeaponAffix affix) {
        register(affix, Target.WEAPON);
    }

    private static void register(WeaponAffix affix, Target target) {
        register(affix, target, "通用词条", "");
    }

    private static void register(WeaponAffix affix, Target target, String category) {
        register(affix, target, category, "");
    }

    private static void register(WeaponAffix affix, Target target, String category, String synergyHint) {
        AFFIXES.put(affix.id(), affix);
        TARGETS.put(affix.id(), target);
        CATEGORIES.put(affix.id(), category);
        if (synergyHint != null && !synergyHint.isBlank()) {
            SYNERGY_HINTS.put(affix.id(), synergyHint);
        }
    }

    private static SimpleAffix percent(String id, String displayName, double min, double max, LoreAppender loreAppender) {
        return new SimpleAffix(id, displayName, true, min, max, false, loreAppender) {
            @Override
            public double strengthen(double currentValue, int useCount, Random random) {
                double value = strengthenNumber(currentValue, random);
                return id.endsWith("_chance") ? Math.min(1.0, value) : value;
            }

            @Override
            public String format(double value) {
                if (id.endsWith("_chance")) value = chance(value);
                return WeaponManager.format(value * 100, 1) + "%";
            }
        };
    }

    private static SimpleAffix number(String id, String displayName, double min, double max, LoreAppender loreAppender) {
        return number(id, displayName, min, max, loreAppender, false);
    }

    private static SimpleAffix number(String id, String displayName, double min, double max, LoreAppender loreAppender, boolean integer) {
        return new SimpleAffix(id, displayName, true, min, max, integer, loreAppender);
    }

    private static SimpleAffix level(String id, String displayName, int min, int max, LoreAppender loreAppender) {
        return new SimpleAffix(id, displayName, true, min, max, true, loreAppender) {
            @Override
            public boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data) {
                double value = total(template, data, id, 0.0);
                return value > 0 && value < max;
            }

            @Override
            public double strengthen(double currentValue, int useCount, Random random) {
                return Math.min(max, currentValue + 1);
            }

            @Override
            public String format(double value) {
                return (int) value + "级";
            }
        };
    }

    private static SimpleAffix toggle(String id, String displayName, String loreLine, boolean strengthenable) {
        return new SimpleAffix(id, displayName, strengthenable, 1, 1, true, (lore, template, data) -> {
            if (total(template, data, id, 0.0) > 0) lore.add(Message.toComponent(loreLine));
        }) {
            @Override
            public String format(double value) {
                return value > 0 ? "已启用" : "未启用";
            }
        };
    }

    private static SimpleAffix neutral(String id, String displayName, String loreLine) {
        return toggle(id, displayName, loreLine, false);
    }

    private static double total(CustomWeapon template, WeaponInstanceData data, String id, double defaultValue) {
        return data.getTotalEffect(template, id, defaultValue);
    }

    private static double chance(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int damageStoreRequiredHits(CustomWeapon template, WeaponInstanceData data) {
        int reduction = (int) data.getTotalEffect(template, "damage_store_hit_reduction", 0.0);
        return Math.max(5, 20 - reduction);
    }

    public static double durabilityRestoreChance(int level) {
        return switch (Math.max(1, Math.min(5, level))) {
            case 1 -> 0.25;
            case 2 -> 0.30;
            case 3 -> 0.40;
            case 4 -> 0.45;
            default -> 0.50;
        };
    }

    private static double strengthenNumber(double currentValue, int useCount, Random random) {
        return strengthenNumber(currentValue, random);
    }

    public static double strengthenRawNumber(double currentValue, Random random) {
        return strengthenNumber(currentValue, random);
    }

    private static double strengthenNumber(double currentValue, Random random) {
        double multiplier = 1.04 + 0.12 * Math.pow(random.nextDouble(), 2.0);
        return currentValue * multiplier;
    }

    private static class SimpleAffix implements WeaponAffix {
        private final String id;
        private final String displayName;
        private final boolean strengthenable;
        private final double min;
        private final double max;
        private final boolean integer;
        private final LoreAppender loreAppender;

        SimpleAffix(String id, String displayName, boolean strengthenable, double min, double max, boolean integer, LoreAppender loreAppender) {
            this.id = id;
            this.displayName = displayName;
            this.strengthenable = strengthenable;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.loreAppender = loreAppender;
        }

        @Override
        public String id() { return id; }

        @Override
        public String displayName() { return displayName; }

        @Override
        public boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data) {
            return strengthenable && total(template, data, id, 0.0) != 0.0;
        }

        @Override
        public double generateBaseValue(Random random) {
            if (min == max) return min;
            if (integer) return (int) min + random.nextInt((int) (max - min + 1));
            return min + random.nextDouble() * (max - min);
        }

        @Override
        public double strengthen(double currentValue, int useCount, Random random) {
            return integer ? currentValue + 1 : strengthenNumber(currentValue, useCount, random);
        }

        @Override
        public String format(double value) {
            return integer ? String.valueOf((int) value) : WeaponManager.format(value, 2);
        }

        @Override
        public void appendLore(List<Component> lore, CustomWeapon template, WeaponInstanceData data) {
            if (loreAppender != null) loreAppender.append(lore, template, data);
        }
    }

    @FunctionalInterface
    private interface LoreAppender {
        void append(List<Component> lore, CustomWeapon template, WeaponInstanceData data);
    }
}
