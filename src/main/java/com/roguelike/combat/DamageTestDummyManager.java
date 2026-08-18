package com.roguelike.combat;

import com.roguelike.RoguelikePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.attribute.Attribute;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.UUID;

public final class DamageTestDummyManager {
    public static final double TEST_DUMMY_HEALTH = 1000.0;
    private static NamespacedKey markerKey;
    private static NamespacedKey anchorWorldKey;
    private static NamespacedKey anchorXKey;
    private static NamespacedKey anchorYKey;
    private static NamespacedKey anchorZKey;
    private static BukkitTask restoreTask;

    private DamageTestDummyManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        markerKey = new NamespacedKey(plugin, "damage_test_dummy");
        anchorWorldKey = new NamespacedKey(plugin, "damage_test_dummy_world");
        anchorXKey = new NamespacedKey(plugin, "damage_test_dummy_x");
        anchorYKey = new NamespacedKey(plugin, "damage_test_dummy_y");
        anchorZKey = new NamespacedKey(plugin, "damage_test_dummy_z");
        restoreTask = Bukkit.getScheduler().runTaskTimer(plugin, DamageTestDummyManager::restoreAll, 5L, 5L);
    }

    public static void shutdown() {
        if (restoreTask != null) {
            restoreTask.cancel();
            restoreTask = null;
        }
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

    public static Location returnLocationAbove(Location woolLocation) {
        return woolLocation.clone().add(0.5, 1.0, 0.5);
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
            PersistentDataContainer data = stand.getPersistentDataContainer();
            data.set(markerKey, PersistentDataType.BYTE, (byte) 1);
            data.set(anchorWorldKey, PersistentDataType.STRING, woolBlock.getWorld().getUID().toString());
            data.set(anchorXKey, PersistentDataType.INTEGER, woolBlock.getX());
            data.set(anchorYKey, PersistentDataType.INTEGER, woolBlock.getY());
            data.set(anchorZKey, PersistentDataType.INTEGER, woolBlock.getZ());
            setTestDummyHealth(stand);
        }
    }

    private static void setTestDummyHealth(ArmorStand stand) {
        var maxHealth = stand.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(TEST_DUMMY_HEALTH);
            stand.setHealth(TEST_DUMMY_HEALTH);
        }
    }

    private static void restoreAll() {
        if (markerKey == null) return;
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (isTestDummy(stand)) restore(stand);
            }
        }
    }

    private static void restore(ArmorStand stand) {
        Location anchor = getAnchor(stand);
        if (anchor == null || !isWool(anchor.getBlock().getType())) return;
        Location target = returnLocationAbove(anchor);
        if (stand.getLocation().distanceSquared(target) <= 0.01) return;
        stand.teleport(target);
        stand.setVelocity(new Vector());
        setTestDummyHealth(stand);
    }

    private static Location getAnchor(Entity entity) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        String worldId = data.get(anchorWorldKey, PersistentDataType.STRING);
        Integer x = data.get(anchorXKey, PersistentDataType.INTEGER);
        Integer y = data.get(anchorYKey, PersistentDataType.INTEGER);
        Integer z = data.get(anchorZKey, PersistentDataType.INTEGER);
        if (worldId == null || x == null || y == null || z == null) return null;
        try {
            World world = Bukkit.getWorld(UUID.fromString(worldId));
            return world == null ? null : new Location(world, x, y, z);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isTestDummy(Entity entity) {
        return entity instanceof ArmorStand stand
                && markerKey != null
                && stand.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public static boolean isProtected(Entity entity) {
        if (!isTestDummy(entity)) return false;
        Location anchor = getAnchor(entity);
        return anchor != null && isWool(anchor.getBlock().getType());
    }
}
