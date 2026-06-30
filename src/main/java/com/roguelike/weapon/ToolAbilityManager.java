package com.roguelike.weapon;

import com.roguelike.RoguelikePlugin;
import com.roguelike.equipment.EquipmentTypeResolver;
import com.roguelike.equipment.affix.AffixManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class ToolAbilityManager {
    private static RoguelikePlugin plugin;
    private static final Set<Location> INTERNAL_BREAKS = new HashSet<>();

    public static void init(RoguelikePlugin plugin) {
        ToolAbilityManager.plugin = plugin;
    }

    public static void handleItemDamage(PlayerItemDamageEvent event) {
        ItemStack stack = event.getItem();
        CustomWeapon template = WeaponManager.getTemplate(stack);
        WeaponInstanceData data = WeaponManager.getData(stack);
        if (template == null || data == null) return;

        int level = (int) data.getTotalEffect(template, "durability_restore", 0.0);
        if (level <= 0 || ThreadLocalRandom.current().nextDouble() >= AffixManager.durabilityRestoreChance(level)) return;

        int repair = 3 - event.getDamage();
        event.setDamage(0);
        if (repair <= 0) return;

        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(Math.max(0, damageable.getDamage() - repair));
            stack.setItemMeta(meta);
        }
    }

    public static void handleBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (INTERNAL_BREAKS.remove(location)) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        CustomWeapon template = WeaponManager.getTemplate(tool);
        WeaponInstanceData data = WeaponManager.getData(tool);
        if (template == null || data == null) return;

        if (data.getTotalEffect(template, "ore_highlight", 0.0) > 0 && ThreadLocalRandom.current().nextDouble() < 0.10) {
            highlightNearbyOres(player, event.getBlock().getLocation());
        }
        if (data.getTotalEffect(template, "area_mining", 0.0) <= 0 || !EquipmentTypeResolver.isTool(tool.getType())) return;

        for (Block block : areaBlocks(event.getBlock(), player)) {
            if (block.equals(event.getBlock()) || block.getType().isAir() || block.isLiquid()) continue;
            if (!EquipmentTypeResolver.canBreakWithTool(block.getType(), tool.getType())) continue;
            INTERNAL_BREAKS.add(block.getLocation());
            block.breakNaturally(tool, true, true);
        }
    }

    private static Set<Block> areaBlocks(Block center, Player player) {
        Set<Block> blocks = new HashSet<>();
        BlockFace face = dominantFace(player.getLocation().getDirection());
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                blocks.add(switch (face) {
                    case EAST, WEST -> center.getRelative(0, a, b);
                    case UP, DOWN -> center.getRelative(a, 0, b);
                    default -> center.getRelative(a, b, 0);
                });
            }
        }
        return blocks;
    }

    private static BlockFace dominantFace(Vector direction) {
        double x = Math.abs(direction.getX());
        double y = Math.abs(direction.getY());
        double z = Math.abs(direction.getZ());
        if (y >= x && y >= z) return direction.getY() >= 0 ? BlockFace.UP : BlockFace.DOWN;
        if (x >= z) return direction.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST;
        return direction.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private static void highlightNearbyOres(Player player, Location origin) {
        World world = origin.getWorld();
        if (world == null) return;
        Material marker = Material.GLOWSTONE;
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    Block block = world.getBlockAt(origin.getBlockX() + x, origin.getBlockY() + y, origin.getBlockZ() + z);
                    if (!isOre(block.getType())) continue;
                    BlockData real = block.getBlockData();
                    player.sendBlockChange(block.getLocation(), marker.createBlockData());
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.sendBlockChange(block.getLocation(), real), 20L);
                }
            }
        }
    }

    private static boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
    }
}
