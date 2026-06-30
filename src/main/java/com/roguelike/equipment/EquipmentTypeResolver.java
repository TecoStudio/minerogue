package com.roguelike.equipment;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Locale;

public final class EquipmentTypeResolver {
    private EquipmentTypeResolver() {
    }

    public static boolean isTool(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE");
    }

    public static boolean isArmor(Material material) {
        return armorSlot(material) != null;
    }

    public static EquipmentKind resolve(Material material) {
        if (isArmor(material)) return EquipmentKind.ARMOR;
        if (isTool(material)) return EquipmentKind.TOOL;
        return EquipmentKind.WEAPON;
    }

    public static boolean canBreakWithTool(Material block, Material tool) {
        if (block == null || tool == null) return false;
        String toolName = tool.name();
        if (toolName.endsWith("_PICKAXE")) return Tag.MINEABLE_PICKAXE.isTagged(block);
        if (toolName.endsWith("_AXE")) return Tag.MINEABLE_AXE.isTagged(block);
        return false;
    }

    public static ArmorSlot armorSlot(Material material) {
        if (material == null) return null;
        String name = material.name().toLowerCase(Locale.ROOT);
        if (name.endsWith("_helmet") || name.equals("turtle_helmet")) return ArmorSlot.HELMET;
        if (name.endsWith("_chestplate")) return ArmorSlot.CHESTPLATE;
        if (name.endsWith("_leggings")) return ArmorSlot.LEGGINGS;
        if (name.endsWith("_boots")) return ArmorSlot.BOOTS;
        return null;
    }

    public enum ArmorSlot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }
}
