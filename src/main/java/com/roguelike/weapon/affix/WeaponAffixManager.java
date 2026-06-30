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

    private enum Target { WEAPON, TOOL, ALL }

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
            if (chainTargets > 0) lore.add(Message.toComponent("§d🌀 连锁: §f" + chainTargets + "目标 (" + WeaponManager.format(chainRange, 1) + "格)"));
        }, true));
        register(number("chain_range", "连锁范围", 2.0, 4.0, null));
        register(percent("chain_damage_percent", "连锁伤害", 0.30, 0.50, null));
        register(percent("crit_chance", "暴击率", 0.05, 0.15, (lore, template, data) -> {
            double critChance = chance(total(template, data, "crit_chance", 0.0));
            double critDamage = total(template, data, "crit_damage", 1.5);
            if (critChance > 0) lore.add(Message.toComponent("§c✦ 暴击: §f" + WeaponManager.format(critChance * 100, 0) + "% (" + WeaponManager.format(critDamage, 1) + "x)"));
        }));
        register(number("crit_damage", "暴击倍率", 1.5, 2.0, null));
        register(number("fire_damage", "火焰伤害", 2.0, 5.0, (lore, template, data) -> {
            double fireDamage = total(template, data, "fire_damage", 0.0);
            double fireDuration = total(template, data, "fire_duration", 0.0);
            if (fireDamage > 0) lore.add(Message.toComponent("§c🔥 火焰: §f" + WeaponManager.format(fireDamage, 1) + "伤害 (" + WeaponManager.format(fireDuration, 1) + "s)"));
        }));
        register(number("fire_duration", "燃烧时长", 2.0, 5.0, null));
        register(percent("lightning_chance", "雷电概率", 0.05, 0.15, (lore, template, data) -> {
            double value = chance(total(template, data, "lightning_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§9⚡ 雷电: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }));
        register(number("slow_duration", "减速时长", 1.0, 3.0, (lore, template, data) -> {
            double slowDuration = total(template, data, "slow_duration", 0.0);
            int slowLevel = (int) total(template, data, "slow_level", 0.0);
            if (slowDuration > 0) lore.add(Message.toComponent("§9❄ 减速: §f" + (slowLevel + 1) + "级 (" + WeaponManager.format(slowDuration, 1) + "s)"));
        }));
        register(number("slow_level", "减速等级", 1, 2, null, true));
        register(percent("damage_store_percent", "伤害存储率", 0.10, 0.25, (lore, template, data) -> {
            double storePercent = total(template, data, "damage_store_percent", 0.0);
            if (storePercent > 0) lore.add(Message.toComponent("§6⚡ 伤害储存: §f" + WeaponManager.format(storePercent * 100, 0) + "% / " + damageStoreRequiredHits(template, data) + "次"));
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
            if (value > 0) lore.add(Message.toComponent("§c🔥 燃烧增伤: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }));
        register(percent("poisoned_target_damage_percent", "中毒目标增伤", 0.15, 0.40, (lore, template, data) -> {
            double value = total(template, data, "poisoned_target_damage_percent", 0.0);
            if (value > 0) lore.add(Message.toComponent("§2☠ 中毒增伤: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }));
        register(percent("poison_chance", "中毒概率", 0.10, 0.30, (lore, template, data) -> {
            double value = chance(total(template, data, "poison_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§2☠ 中毒概率: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }));
        register(percent("explosion_chance", "爆炸概率", 0.05, 0.15, (lore, template, data) -> {
            double value = chance(total(template, data, "explosion_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§6✹ 爆炸概率: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }));
        register(percent("big_explosion_chance", "大爆炸概率", 0.02, 0.08, (lore, template, data) -> {
            double value = chance(total(template, data, "big_explosion_chance", 0.0));
            if (value > 0) lore.add(Message.toComponent("§4✹ 大爆炸概率: §f" + WeaponManager.format(value * 100, 0) + "%"));
        }));
        register(toggle("smash", "猛击", "§6✦ 猛击: §f3倍伤害，力量效果翻倍，使用后冷却7秒", false));
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
        register(toggle("dash", "Dash！", "§b➤ Dash！: §f向面朝方向突进，5秒冷却，2次充能", false));
        register(level("durability_restore", "用不坏", 1, 5, (lore, template, data) -> {
            int level = (int) total(template, data, "durability_restore", 0.0);
            if (level > 0) lore.add(Message.toComponent("§a✦ 用不坏: §f" + level + "级 (" + WeaponManager.format(durabilityRestoreChance(level) * 100, 0) + "%返还3耐久)"));
        }), Target.ALL);
        register(toggle("area_mining", "范围挖掘", "§6⬚ 范围挖掘: §f按面朝方向挖掘3x3方块", false), Target.TOOL);
        register(toggle("ore_highlight", "高亮矿物", "§e✦ 高亮矿物: §f挖掘时10%概率高亮附近矿物1秒", false), Target.TOOL);
        register(level("efficiency", "效率", 1, 5, (lore, template, data) -> {
            int level = (int) total(template, data, "efficiency", 0.0);
            if (level > 0) lore.add(Message.toComponent("§b⛏ 效率: §f" + level + "级"));
        }), Target.TOOL);
        register(level("fortune", "时运", 1, 5, (lore, template, data) -> {
            int level = (int) total(template, data, "fortune", 0.0);
            if (level > 0) lore.add(Message.toComponent("§e✦ 时运: §f" + level + "级"));
        }), Target.TOOL);
    }

    public static List<String> effectIds() {
        return List.copyOf(AFFIXES.keySet());
    }

    public static WeaponAffix get(String id) {
        return AFFIXES.get(id);
    }

    public static String displayName(String id) {
        WeaponAffix affix = get(id);
        return affix == null ? id : affix.displayName();
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
        return template.getEffect(id, 0.0) == 0.0 && data.getEffectBonus(id) == 0.0;
    }

    public static boolean isAvailable(CustomWeapon template, WeaponInstanceData data, String id, Material material) {
        return isApplicable(id, material) && isAvailable(template, data, id);
    }

    public static boolean isApplicable(String id, Material material) {
        return isApplicable(id, EquipmentTypeResolver.resolve(material));
    }

    public static boolean isApplicable(String id, EquipmentKind kind) {
        Target target = TARGETS.getOrDefault(id, Target.WEAPON);
        return target == Target.ALL || target == Target.WEAPON || kind == EquipmentKind.TOOL;
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
        AFFIXES.put(affix.id(), affix);
        TARGETS.put(affix.id(), target);
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
        double multiplier = 1.1 + 1.4 * Math.pow(random.nextDouble(), 2.0);
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
