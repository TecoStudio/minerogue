package com.roguelike.equipment.affix;

import com.roguelike.armor.affix.ArmorAffixManager;
import com.roguelike.equipment.EquipmentKind;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.weapon.affix.WeaponAffixManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AffixManager {
    private AffixManager() {
    }

    public static List<String> effectIds(EquipmentKind kind) {
        return switch (kind) {
            case WEAPON, TOOL -> weaponEffectIds(kind);
            case ARMOR -> ArmorAffixManager.effectIds();
        };
    }

    public static List<String> weaponEffectIds() {
        return WeaponAffixManager.effectIds();
    }

    public static List<String> weaponEffectIds(EquipmentKind kind) {
        List<String> ids = new ArrayList<>();
        for (String id : WeaponAffixManager.effectIds()) {
            if (WeaponAffixManager.isApplicable(id, kind)) ids.add(id);
        }
        return ids;
    }

    public static List<String> armorEffectIds() {
        return ArmorAffixManager.effectIds();
    }

    public static String displayName(EquipmentKind kind, String id) {
        return kind == EquipmentKind.ARMOR ? ArmorAffixManager.displayName(id) : WeaponAffixManager.displayName(id);
    }

    public static String formatWeapon(String id, double value) {
        return WeaponAffixManager.format(id, value);
    }

    public static String formatArmor(String id, int level) {
        return ArmorAffixManager.format(id, level);
    }

    public static double generateWeaponBaseValue(String id, Random random) {
        return WeaponAffixManager.generateBaseValue(id, random);
    }

    public static int generateArmorBaseLevel(String id, Random random) {
        return ArmorAffixManager.generateBaseLevel(id, random);
    }

    public static boolean isWeaponAffixAvailable(CustomWeapon template, WeaponInstanceData data, String id, Material material) {
        return WeaponAffixManager.isAvailable(template, data, id, material);
    }

    public static boolean isWeaponAffixStrengthenable(CustomWeapon template, WeaponInstanceData data, String id, Material material) {
        return WeaponAffixManager.isStrengthenable(template, data, id, material);
    }

    public static double strengthenWeapon(String id, double currentValue, int useCount, Random random) {
        return WeaponAffixManager.strengthen(id, currentValue, useCount, random);
    }

    public static double strengthenRawNumber(double currentValue, Random random) {
        return WeaponAffixManager.strengthenRawNumber(currentValue, random);
    }

    public static int strengthenArmor(String id, int currentLevel) {
        return ArmorAffixManager.strengthen(id, currentLevel);
    }

    public static void appendWeaponLore(List<Component> lore, CustomWeapon template, WeaponInstanceData data) {
        WeaponAffixManager.appendLore(lore, template, data);
    }

    public static double durabilityRestoreChance(int level) {
        return WeaponAffixManager.durabilityRestoreChance(level);
    }

    public static boolean isArmorAffixApplicable(String id, Material material) {
        return ArmorAffixManager.isApplicable(id, material);
    }

    public static void applyArmorEnchant(ItemStack stack, String id, int level) {
        ArmorAffixManager.applyEnchant(stack, id, level);
    }
}
