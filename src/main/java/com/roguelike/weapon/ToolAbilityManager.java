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
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;
public class ToolAbilityManager {
    private static RoguelikePlugin plugin;
    private static BukkitTask task;
    private static final BlockFace[] VISIBLE_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public static void init(RoguelikePlugin plugin) {
        shutdown();
        ToolAbilityManager.plugin = plugin;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, ToolAbilityManager::tick, 1L, 1L);
    }

    public static void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        plugin = null;
    }

    private static void tick() {
        if (plugin == null) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyCrazyMinerHaste(player);
        }
    }

    private static void applyCrazyMinerHaste(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!hasCrazyMiner(tool)) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 10, 2, true, false, false));
    }

    private static boolean hasCrazyMiner(ItemStack stack) {
        CustomWeapon template = WeaponManager.getTemplate(stack);
        WeaponInstanceData data = WeaponManager.getData(stack);
        return template != null && data != null
                && EquipmentTypeResolver.isPickaxe(stack.getType())
                && data.getTotalEffect(template, "crazy_miner", 0.0) > 0.0;
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
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        CustomWeapon template = WeaponManager.getTemplate(tool);
        WeaponInstanceData data = WeaponManager.getData(tool);
        if (template == null || data == null) return;

        if (hasCrazyMiner(tool) && ThreadLocalRandom.current().nextDouble() < 0.12) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 5, 0, true, false, false));
        }

        if (data.getTotalEffect(template, "ore_highlight", 0.0) > 0 && ThreadLocalRandom.current().nextDouble() < 0.10) {
            highlightNearbyOres(player, event.getBlock().getLocation());
        }
    }

    private static void highlightNearbyOres(Player player, Location origin) {
        World world = origin.getWorld();
        if (world == null) return;
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    Block block = world.getBlockAt(origin.getBlockX() + x, origin.getBlockY() + y, origin.getBlockZ() + z);
                    if (!isOre(block.getType())) continue;
                    if (isExposed(block)) continue;
                    highlightOreForPlayer(player, block);
                }
            }
        }
    }

    private static void highlightOreForPlayer(Player player, Block block) {
        BlockData real = block.getBlockData();
        Location location = block.getLocation();
        BlockDisplay display = block.getWorld().spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(real);
            entity.setGlowing(true);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setBrightness(new Display.Brightness(15, 15));
        });

        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.hideEntity(plugin, display);
            }
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, 20L);
    }

    private static boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
    }

    private static boolean isExposed(Block block) {
        for (BlockFace face : VISIBLE_FACES) {
            Block neighbor = block.getRelative(face);
            if (!neighbor.getType().isOccluding()) return true;
        }
        return false;
    }
}
