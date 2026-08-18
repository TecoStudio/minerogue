package com.roguelike.combat;

import com.roguelike.RoguelikePlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

public final class DamageTestDummyManager {
    private static NamespacedKey markerKey;

    private DamageTestDummyManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        markerKey = new NamespacedKey(plugin, "damage_test_dummy");
    }

    public static boolean isWool(Material material) {
        return switch (material) {
            case WHITE_WOOL, ORANGE_WOOL, MAGENTA_WOOL, LIGHT_BLUE_WOOL,
                 YELLOW_WOOL, LIME_WOOL, PINK_WOOL, GRAY_WOOL,
                 LIGHT_GRAY_WOOL, CYAN_WOOL, PURPLE_WOOL, BLUE_WOOL,
                 BROWN_WOOL, GREEN_WOOL, RED_WOOL, BLACK_WOOL -> true;
            default -> false;
        };
    }

    public static void markArmorStandAbove(Block woolBlock) {
        if (!isWool(woolBlock.getType())) return;
        for (Entity entity : woolBlock.getWorld().getNearbyEntities(
                woolBlock.getLocation().add(0.5, 1.0, 0.5), 0.75, 1.1, 0.75)) {
            if (!(entity instanceof ArmorStand stand)) continue;
            if (stand.getLocation().getBlockX() != woolBlock.getX()
                    || stand.getLocation().getBlockY() != woolBlock.getY() + 1
                    || stand.getLocation().getBlockZ() != woolBlock.getZ()) {
                continue;
            }
            stand.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    public static boolean isTestDummy(Entity entity) {
        return entity instanceof ArmorStand stand
                && markerKey != null
                && stand.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public static boolean isProtected(Entity entity) {
        if (!isTestDummy(entity)) return false;
        Block below = entity.getLocation().getBlock().getRelative(0, -1, 0);
        return isWool(below.getType());
    }
}
