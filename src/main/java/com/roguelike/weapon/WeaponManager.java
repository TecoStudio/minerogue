package com.roguelike.weapon;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class WeaponManager {
    private static RoguelikePlugin plugin;
    private static final UUID LEGACY_ATTACK_SPEED_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID LEGACY_ATTACK_DAMAGE_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    private static final UUID ITEM_ATTACK_SPEED_UUID = UUID.fromString("D9F7AA2E-1B55-4A51-9C5B-63C029C2F2D1");
    private static final UUID ATTACK_RANGE_UUID = UUID.fromString("A3D7E2B1-9C4F-4A2E-8B1D-6E5F3C2A1D0E");

    private static final String[] EFFECT_KEYS = {
            "attack_range", "lifesteal_percent", "lifesteal_flat", "slow_duration", "slow_level",
            "chain_targets", "chain_range", "chain_damage_percent", "damage_store_percent", "damage_store_max",
            "crit_chance", "crit_damage", "bleed_chance", "bleed_damage", "bleed_duration",
            "fire_damage", "fire_duration", "lightning_chance", "stun_duration"
    };

    public static void init(RoguelikePlugin plugin) {
        WeaponManager.plugin = plugin;
        WeaponInstanceData.init(plugin);
    }

    public static String[] getEffectKeys() {
        return EFFECT_KEYS.clone();
    }

    private static Material inferMaterial(CustomWeapon template) {
        Material configured = parseMaterial(template.getItem());
        if (configured != null && !configured.isAir()) return configured;

        String id = template.getId().toUpperCase(Locale.ROOT);
        Material exact = Material.matchMaterial(id);
        if (exact != null && !exact.isAir()) return exact;
        if (id.contains("WOOD") || id.contains("WOODEN")) return Material.WOODEN_SWORD;
        if (id.contains("STONE")) return Material.STONE_SWORD;
        if (id.contains("GOLD") || id.contains("GOLDEN")) return Material.GOLDEN_SWORD;
        if (id.contains("DIAMOND")) return Material.DIAMOND_SWORD;
        if (id.contains("NETHERITE")) return Material.NETHERITE_SWORD;
        if (id.contains("AXE")) return Material.IRON_AXE;
        if (id.contains("DAGGER")) return Material.IRON_SWORD;
        if (id.contains("BOW")) return Material.BOW;
        return Material.WOODEN_SWORD;
    }

    private static Material parseMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        String normalized = itemId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return Material.matchMaterial(normalized);
    }

    public static ItemStack createWeaponStack(CustomWeapon template, Material material) {
        if (material == null || material.isAir()) material = inferMaterial(template);
        ItemStack stack = new ItemStack(material);
        WeaponInstanceData data = new WeaponInstanceData(template.getId());
        data.saveToItemStack(stack);
        updateLore(stack, template, data);
        return stack;
    }

    public static void makeWeapon(ItemStack stack, CustomWeapon template) {
        if (stack == null || stack.getType().isAir()) return;
        WeaponInstanceData data = new WeaponInstanceData(template.getId());
        if ("special".equalsIgnoreCase(template.getRarity())) {
            data.setCustomName(getRarityColor(template.getRarity()) + "§l特殊 " + formatMaterialName(stack.getType()));
        }
        data.saveToItemStack(stack);
        updateLore(stack, template, data);
    }

    private static String formatMaterialName(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? material.name() : builder.toString();
    }

    public static void updateLore(ItemStack stack, CustomWeapon template, WeaponInstanceData data) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) meta = plugin.getServer().getItemFactory().getItemMeta(stack.getType());
        if (meta == null) return;

        String rarityColor = getRarityColor(template.getRarity());
        String name = data.getCustomName() != null ? data.getCustomName() : rarityColor + "§l" + template.getName();
        meta.displayName(Message.toComponent(name));

        List<Component> lore = new ArrayList<>();
        lore.add(Message.toComponent("§7========== §6武器属性 §7=========="));
        if (!template.getDescription().isEmpty()) {
            lore.add(Message.toComponent("§7" + template.getDescription()));
        }
        lore.add(Message.toComponent("§7─────────────────"));

        double totalDamage = data.getTotalDamage(template);
        double totalSpeed = data.getTotalAttackSpeed(template);
        double totalRange = data.getTotalEffect(template, "attack_range", 3.0);

        lore.add(Message.toComponent("§a⚔ 基础伤害: §f" + format(totalDamage, 1)));
        lore.add(Message.toComponent("§b⚡ 攻击速度: §f" + format(totalSpeed, 2)));
        lore.add(Message.toComponent("§e⬛ 攻击距离: §f" + format(totalRange, 1) + "格"));
        lore.add(Message.toComponent("§7─────────────────"));

        appendEffectLore(lore, template, data);

        if (data.getStoredDamage() > 0) {
            lore.add(Message.toComponent("§6⚡ 爆发存储: §f" + format(data.getStoredDamage(), 1) + "伤害"));
        }

        if (!data.getAppliedModifiers().isEmpty()) {
            lore.add(Message.toComponent("§7─────────────────"));
            lore.add(Message.toComponent("§7已应用强化: §e" + data.getAppliedModifiers().size()));
        }

        int a = data.getTicketAUses();
        int b = data.getTicketBUses();
        int c = data.getTicketCUses();
        if (a > 0 || b > 0 || c > 0) {
            lore.add(Message.toComponent("§7─────────────────"));
            StringBuilder sb = new StringBuilder("§7强化券: ");
            if (a > 0) sb.append("§cA").append(a).append(" ");
            if (b > 0) sb.append("§aB").append(b).append(" ");
            if (c > 0) sb.append("§9C").append(c);
            lore.add(Message.toComponent(sb.toString()));
        }

        lore.add(Message.toComponent("§7========== " + rarityColor + "品质: " + template.getRarity().toUpperCase() + " §7=========="));

        meta.lore(lore);
        applyVanillaItemAttributes(meta, stack.getType(), totalDamage, totalSpeed);
        stack.setItemMeta(meta);
    }

    private static void applyVanillaItemAttributes(ItemMeta meta, Material material, double damage, double speed) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
        double damageBonus = damage - 1.0;
        if (damageBonus != 0) {
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(LEGACY_ATTACK_DAMAGE_UUID, "generic.attack_damage", damageBonus,
                            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        }
        double vanillaSpeed = getVanillaMainHandAttackSpeed(material);
        meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(ITEM_ATTACK_SPEED_UUID, "generic.attack_speed", speed - vanillaSpeed,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
    }

    private static void appendEffectLore(List<Component> lore, CustomWeapon template, WeaponInstanceData data) {
        double lifePercent = data.getTotalEffect(template, "lifesteal_percent", 0.0);
        double lifeFlat = data.getTotalEffect(template, "lifesteal_flat", 0.0);
        if (lifePercent > 0 || lifeFlat > 0) {
            String text = "§c❤ 吸血: §f" + format(lifePercent * 100, 0) + "%";
            if (lifeFlat > 0) text += " +" + format(lifeFlat, 1);
            lore.add(Message.toComponent(text));
        }

        double slowDuration = data.getTotalEffect(template, "slow_duration", 0.0);
        int slowLevel = (int) data.getTotalEffect(template, "slow_level", 0.0);
        if (slowDuration > 0) {
            lore.add(Message.toComponent("§9❄ 减速: §f" + (slowLevel + 1) + "级 (" + format(slowDuration, 1) + "s)"));
        }

        int chainTargets = (int) data.getTotalEffect(template, "chain_targets", 0.0);
        double chainRange = data.getTotalEffect(template, "chain_range", 0.0);
        if (chainTargets > 0) {
            lore.add(Message.toComponent("§d🌀 连锁: §f" + chainTargets + "目标 (" + format(chainRange, 1) + "格)"));
        }

        double critChance = data.getTotalEffect(template, "crit_chance", 0.0);
        double critDamage = data.getTotalEffect(template, "crit_damage", 1.5);
        if (critChance > 0) {
            lore.add(Message.toComponent("§c✦ 暴击: §f" + format(critChance * 100, 0) + "% (" + format(critDamage, 1) + "x)"));
        }

        double bleedChance = data.getTotalEffect(template, "bleed_chance", 0.0);
        double bleedDamage = data.getTotalEffect(template, "bleed_damage", 0.0);
        double bleedDuration = data.getTotalEffect(template, "bleed_duration", 0.0);
        if (bleedChance > 0) {
            lore.add(Message.toComponent("§4🩸 流血: §f" + format(bleedChance * 100, 0) + "% (" + format(bleedDamage, 1) + "伤害/s, " + format(bleedDuration, 1) + "s)"));
        }

        double fireDamage = data.getTotalEffect(template, "fire_damage", 0.0);
        double fireDuration = data.getTotalEffect(template, "fire_duration", 0.0);
        if (fireDamage > 0) {
            lore.add(Message.toComponent("§c🔥 火焰: §f" + format(fireDamage, 1) + "伤害 (" + format(fireDuration, 1) + "s)"));
        }

        double lightning = data.getTotalEffect(template, "lightning_chance", 0.0);
        if (lightning > 0) {
            lore.add(Message.toComponent("§9⚡ 雷电: §f" + format(lightning * 100, 0) + "%"));
        }

        double stun = data.getTotalEffect(template, "stun_duration", 0.0);
        if (stun > 0) {
            lore.add(Message.toComponent("§5💫 眩晕: §f" + format(stun, 1) + "s"));
        }
    }

    public static void updateHeldAttributes(Player player) {
        clearAttributes(player);
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomWeapon template = getTemplate(hand);
        WeaponInstanceData data = getData(hand);
        if (template == null || data == null) return;

        double totalRange = data.getTotalEffect(template, "attack_range", 3.0);

        applyRangeAttribute(player, totalRange);
    }

    private static void applyRangeAttribute(Player player, double range) {
        Attribute rangeAttr = getRangeAttribute();
        if (rangeAttr == null) return;
        var attr = player.getAttribute(rangeAttr);
        if (attr == null) return;
        double vanilla = attr.getBaseValue();
        double bonus = range - vanilla;
        if (bonus != 0) {
            attr.addModifier(new AttributeModifier(ATTACK_RANGE_UUID, "generic.attack_range", bonus,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        }
    }

    public static void clearAttributes(Player player) {
        var dmgAttr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.removeModifier(LEGACY_ATTACK_DAMAGE_UUID);

        var spdAttr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (spdAttr != null) spdAttr.removeModifier(LEGACY_ATTACK_SPEED_UUID);

        Attribute rangeAttr = getRangeAttribute();
        if (rangeAttr != null) {
            var rangeInst = player.getAttribute(rangeAttr);
            if (rangeInst != null) rangeInst.removeModifier(ATTACK_RANGE_UUID);
        }
    }

    private static Attribute getRangeAttribute() {
        try {
            return Attribute.ENTITY_INTERACTION_RANGE;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static double getVanillaMainHandAttackSpeed(Material material) {
        String name = material.name();
        if (name.endsWith("_SWORD")) return 1.6;
        if (name.endsWith("_SHOVEL")) return 1.0;
        if (name.endsWith("_PICKAXE")) return 1.2;
        if (name.endsWith("_AXE")) {
            if (name.startsWith("WOODEN_") || name.startsWith("STONE_")) return 0.8;
            if (name.startsWith("IRON_") || name.startsWith("NETHERITE_")) return 0.9;
            if (name.startsWith("DIAMOND_")) return 1.0;
            if (name.startsWith("GOLDEN_")) return 1.0;
            return 0.9;
        }
        if (name.endsWith("_HOE")) {
            if (name.startsWith("WOODEN_")) return 1.0;
            if (name.startsWith("STONE_")) return 2.0;
            if (name.startsWith("IRON_")) return 3.0;
            if (name.startsWith("DIAMOND_") || name.startsWith("NETHERITE_")) return 4.0;
            if (name.startsWith("GOLDEN_")) return 1.0;
        }
        return 4.0;
    }

    public static double getVanillaMainHandDamage(Material material) {
        String name = material.name();
        if (name.endsWith("_SWORD")) {
            if (name.startsWith("WOODEN_") || name.startsWith("GOLDEN_")) return 4.0;
            if (name.startsWith("STONE_")) return 5.0;
            if (name.startsWith("IRON_")) return 6.0;
            if (name.startsWith("DIAMOND_")) return 7.0;
            if (name.startsWith("NETHERITE_")) return 8.0;
        }
        if (name.endsWith("_AXE")) {
            if (name.startsWith("WOODEN_") || name.startsWith("GOLDEN_")) return 7.0;
            if (name.startsWith("STONE_") || name.startsWith("IRON_") || name.startsWith("DIAMOND_")) return 9.0;
            if (name.startsWith("NETHERITE_")) return 10.0;
        }
        if (name.endsWith("_PICKAXE")) {
            if (name.startsWith("WOODEN_") || name.startsWith("GOLDEN_")) return 2.0;
            if (name.startsWith("STONE_")) return 3.0;
            if (name.startsWith("IRON_")) return 4.0;
            if (name.startsWith("DIAMOND_")) return 5.0;
            if (name.startsWith("NETHERITE_")) return 6.0;
        }
        if (name.endsWith("_SHOVEL")) {
            if (name.startsWith("WOODEN_") || name.startsWith("GOLDEN_")) return 2.5;
            if (name.startsWith("STONE_")) return 3.5;
            if (name.startsWith("IRON_")) return 4.5;
            if (name.startsWith("DIAMOND_")) return 5.5;
            if (name.startsWith("NETHERITE_")) return 6.5;
        }
        if (name.endsWith("_HOE")) {
            if (name.startsWith("WOODEN_") || name.startsWith("GOLDEN_")) return 1.0;
            if (name.startsWith("STONE_")) return 1.0;
            if (name.startsWith("IRON_")) return 1.0;
            if (name.startsWith("DIAMOND_")) return 1.0;
            if (name.startsWith("NETHERITE_")) return 1.0;
        }
        return 1.0;
    }

    public static CustomWeapon getTemplate(ItemStack stack) {
        WeaponInstanceData data = WeaponInstanceData.fromItemStack(stack);
        if (data == null) return null;
        return ConfigManager.getWeapon(data.getBaseWeaponId());
    }

    public static WeaponInstanceData getData(ItemStack stack) {
        return WeaponInstanceData.fromItemStack(stack);
    }

    public static boolean refreshWeapon(ItemStack stack) {
        CustomWeapon template = getTemplate(stack);
        WeaponInstanceData data = getData(stack);
        if (template == null || data == null) return false;
        updateLore(stack, template, data);
        return true;
    }

    public static void refreshHeldWeapon(Player player) {
        updateHeldAttributes(player);
        refreshWeapon(player.getInventory().getItemInMainHand());
    }

    public static String getRarityColor(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "legendary" -> "§6";
            case "epic" -> "§5";
            case "rare" -> "§9";
            case "uncommon" -> "§a";
            case "special" -> "§d";
            default -> "§7";
        };
    }

    public static String format(double value, int decimals) {
        return String.format("%." + decimals + "f", value);
    }

    public static List<String> getModifiableStats(CustomWeapon template, WeaponInstanceData data) {
        List<String> list = new ArrayList<>();
        list.add("damage");
        list.add("attack_speed");
        list.add("attack_range");
        for (String key : EFFECT_KEYS) {
            double total = data.getTotalEffect(template, key, 0.0);
            if (total != 0) list.add(key);
        }
        return list;
    }
}
