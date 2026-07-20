package com.roguelike.listener;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.ArmorSetManager;
import com.roguelike.combat.CombatHandler;
import com.roguelike.config.ConfigManager;
import com.roguelike.config.MobExperienceConfig;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.integration.IntegrationManager;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomItemStackFactory;
import com.roguelike.level.LevelManager;
import com.roguelike.mob.MobManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.ticket.TicketManager;
import com.roguelike.ticket.TicketType;
import com.roguelike.util.DevLog;
import com.roguelike.util.Message;
import com.roguelike.equipment.EquipmentTypeResolver;
import com.roguelike.forge.ForgeTableManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.mana.ManaManager;
import com.roguelike.weapon.ToolAbilityManager;
import com.roguelike.weapon.BowAbilityManager;
import com.roguelike.weapon.WeaponAbilityManager;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class EventListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager.get(player);
        LevelManager.updateExpBar(player);
        ManaManager.track(player);
        player.getServer().getScheduler().runTaskLater(RoguelikePlugin.getInstance(), () -> {
            WeaponManager.refreshHeldWeapon(player);
            ArmorSetManager.applyPassiveEffects(player);
            RoguelikeScoreboard.updatePlayer(player);
            ManaManager.track(player);
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerDataManager.unload(event.getPlayer());
        RoguelikeScoreboard.clearPlayer(event.getPlayer());
        WeaponManager.clearAttributes(event.getPlayer());
        ManaManager.untrack(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onExperienceChange(PlayerExpChangeEvent event) {
        event.setAmount(0);
        ManaManager.track(event.getPlayer());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        // 延迟一点确保主手已切换
        player.getServer().getScheduler().runTaskLater(RoguelikePlugin.getInstance(), () -> {
            WeaponManager.refreshHeldWeapon(player);
        }, 1L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (action == Action.RIGHT_CLICK_BLOCK && ForgeTableManager.isForgeTable(event.getClickedBlock())) {
            event.setCancelled(true);
            ForgeTableManager.open(player);
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (applyDirectUseCustomItem(player, main)) {
            event.setCancelled(true);
            return;
        }

        TicketType mainTicket = TicketManager.getTicketType(main);
        TicketType offTicket = TicketManager.getTicketType(off);

        // 主手持券，副手持目标物品。开发券允许目标是任意非空气物品。
        if (mainTicket != null) {
            if (canTargetAnyItem(mainTicket) || canTargetEquipment(off)) {
                event.setCancelled(true);
                TicketManager.applyTicket(player, main, off);
            }
        }
        // 副手持券，主手持目标物品。开发券允许目标是任意非空气物品。
        else if (offTicket != null) {
            if (canTargetAnyItem(offTicket) || canTargetEquipment(main)) {
                denyMainHandUseIfNeeded(event, player, main);
                event.setCancelled(true);
                TicketManager.applyTicket(player, off, main);
            }
        }
    }

    private void denyMainHandUseIfNeeded(PlayerInteractEvent event, Player player, ItemStack main) {
        if (!shouldDenyMainHandUseForOffhandTicket(main)) return;
        event.setUseItemInHand(Event.Result.DENY);
        player.clearActiveItem();
        player.getServer().getScheduler().runTaskLater(RoguelikePlugin.getInstance(), player::clearActiveItem, 1L);
    }

    static boolean shouldDenyMainHandUseForOffhandTicket(ItemStack main) {
        return main != null && shouldDenyMainHandUseForOffhandTicket(main.getType());
    }

    static boolean shouldDenyMainHandUseForOffhandTicket(Material material) {
        return material == Material.TRIDENT;
    }

    private boolean canTargetEquipment(ItemStack stack) {
        return WeaponManager.getTemplate(stack) != null || (stack != null && EquipmentTypeResolver.isWearable(stack.getType()));
    }

    private boolean canTargetAnyItem(TicketType ticket) {
        return ticket == TicketType.TICKET_B;
    }

    private boolean applyDirectUseCustomItem(Player player, ItemStack stack) {
        String id = CustomItemStackFactory.getCustomItemId(stack);
        if (id == null) return false;
        CustomItem item = ConfigManager.getItem(id);
        if (item == null || !"food".equalsIgnoreCase(item.getItemType())) return false;

        double healPercent = item.getEffect("heal_percent", 0.0);
        double healAmount = item.getEffect("heal_amount", 0.0);
        var maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double maximum = maxHealth == null ? player.getHealth() : maxHealth.getValue();
        double heal = healPercent > 0.0 ? maximum * healPercent : healAmount;
        if (heal > 0.0) {
            player.setHealth(healedHealth(player.getHealth(), maximum, heal));
        }
        if (item.getEffect("full_saturation", 0.0) > 0.0) {
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
        consumeOne(stack);
        Message.send(player, "&a你食用了 " + item.getName() + "。");
        return true;
    }

    private void consumeOne(ItemStack stack) {
        if (stack.getAmount() <= 1) {
            stack.setAmount(0);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        WeaponAbilityManager.handleSneak(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (BowAbilityManager.handleArrowDamage(event)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (CombatHandler.isInternalDamage()) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (WeaponAbilityManager.hasEffect(hand, "smash") && player.hasCooldown(hand.getType())) {
            event.setCancelled(true);
            Message.send(player, "&c武器暂时无法使用。");
            return;
        }
        if (WeaponManager.getTemplate(player.getInventory().getItemInMainHand()) != null) {
            double damage = CombatHandler.processAttack(player, target, event.getDamage(), event.isCritical());
            event.setDamage(damage);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (CombatHandler.cancelLightningDamageForImmunePlayer(event)) return;
        if (event.getEntity() instanceof Player player && !CombatHandler.isInternalDamage()) {
            event.setDamage(CombatHandler.applyIncomingNeutralDamage(player, event.getDamage()));
        }
        WeaponAbilityManager.cancelGiftHeal(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        ArmorSetManager.handlePlayerDamaged(event);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        ArmorSetManager.applyPassiveEffects(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            player.getServer().getScheduler().runTaskLater(RoguelikePlugin.getInstance(), () -> ArmorSetManager.applyPassiveEffects(player), 1L);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() != null) {
            Player player = entity.getKiller();
            String type = entity.getType().name().toLowerCase();
            long exp = scaledExperience(MobExperienceConfig.getMobExp(type), ConfigManager.getExpMultiplier());
            if (exp > 0) {
                LevelManager.addExperience(player, exp);
            }
            DevLog.debug(player.getName() + " 击杀了 " + entityName(entity));
            PlayerDataManager.get(player).addKill();
            WeaponAbilityManager.applyGiftKill(player);
            applyVictimExplosion(player, entity);
            RoguelikeScoreboard.updatePlayer(player);
            MobManager.handleDrop(entity);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerDataManager.get(player).addDeath();
        LevelManager.applyDeathPenalty(player);
        RoguelikeScoreboard.updatePlayer(player);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (IntegrationManager.isMythicMobsEnabled()) return;
        MobManager.applyToMob(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDamage(EntityDamageByEntityEvent event) {
        MobManager.handleDamage(event);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        // 切出手持时更新属性
        WeaponManager.clearAttributes(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ToolAbilityManager.handleItemDamage(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        BowAbilityManager.handleShoot(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ToolAbilityManager.handleBlockBreak(event);
        if (!countsForMiningExperience(event.getBlock().getType())) return;
        PlayerData data = PlayerDataManager.get(event.getPlayer());
        long count = data.incrementMinedBlocks();
        DevLog.debug(event.getPlayer().getName() + " 破坏了 " + materialName(event.getBlock().getType()) + " 方块 (" + count + "/" + nextProgressTarget(count) + ")");
        long exp = progressionExperience(count);
        if (exp > 0) {
            LevelManager.addExperience(event.getPlayer(), scaledExperience(exp, ConfigManager.getProgressionExpMultiplier()));
            PlayerDataManager.save(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (applyCustomItemConsume(event.getPlayer(), event.getItem())) return;
        if (!countsForFoodExperience(event.getItem().getType())) return;
        PlayerData data = PlayerDataManager.get(event.getPlayer());
        long count = data.incrementEatenItems();
        DevLog.debug(event.getPlayer().getName() + " 吃了 " + materialName(event.getItem().getType()) + " (" + count + "/" + nextProgressTarget(count) + ")");
        long exp = progressionExperience(count);
        if (exp > 0) {
            LevelManager.addExperience(event.getPlayer(), scaledExperience(exp, ConfigManager.getProgressionExpMultiplier()));
            PlayerDataManager.save(event.getPlayer());
        }
    }

    private boolean applyCustomItemConsume(Player player, ItemStack stack) {
        String id = CustomItemStackFactory.getCustomItemId(stack);
        if (id == null) return false;
        CustomItem item = ConfigManager.getItem(id);
        if (item == null) return false;

        double heal = item.getEffect("heal_amount", 0.0);
        if (heal > 0.0) {
            var maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double maximum = maxHealth == null ? player.getHealth() : maxHealth.getValue();
            player.setHealth(healedHealth(player.getHealth(), maximum, heal));
        }
        return true;
    }

    static double healedHealth(double current, double maximum, double amount) {
        if (amount <= 0.0) return current;
        return Math.min(maximum, current + amount);
    }

    private void applyVictimExplosion(Player player, LivingEntity victim) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomWeapon template = WeaponManager.getTemplate(hand);
        WeaponInstanceData data = WeaponManager.getData(hand);
        if (template == null || data == null) return;
        double chance = data.getTotalEffect(template, "victim_explosion_chance", 0.0);
        if (!shouldTriggerChance(chance, ThreadLocalRandom.current().nextDouble())) return;
        victim.getWorld().createExplosion(victim.getLocation(), 2.0f, false, false, player);
    }

    static boolean shouldTriggerChance(double chance, double roll) {
        double clamped = Math.max(0.0, Math.min(1.0, chance));
        return roll < clamped;
    }

    static double damageWithBleedingBonus(double damage, boolean bleeding, double bonus) {
        if (!bleeding || bonus <= 0.0) return damage;
        return damage * (1.0 + bonus);
    }

    private boolean countsForMiningExperience(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 NETHER_QUARTZ_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 ANCIENT_DEBRIS -> true;
            default -> false;
        };
    }

    private boolean countsForFoodExperience(Material material) {
        return switch (material) {
            case ENCHANTED_GOLDEN_APPLE,
                 GOLDEN_APPLE, GOLDEN_CARROT,
                 COOKED_BEEF, COOKED_PORKCHOP, COOKED_MUTTON,
                 COOKED_CHICKEN, COOKED_COD, COOKED_SALMON,
                 RABBIT_STEW, MUSHROOM_STEW, BEETROOT_SOUP,
                 SUSPICIOUS_STEW, PUMPKIN_PIE,
                 APPLE, BREAD, CARROT, POTATO, BAKED_POTATO,
                 BEETROOT, MELON_SLICE, SWEET_BERRIES, GLOW_BERRIES,
                 DRIED_KELP, COOKIE, HONEY_BOTTLE,
                 BEEF, PORKCHOP, MUTTON, CHICKEN, COD, SALMON,
                 RABBIT, TROPICAL_FISH -> true;
            default -> false;
        };
    }

    private long progressionExperience(long count) {
        if (count == 10) return 100;
        if (count == 20) return 200;
        if (count == 50) return 300;
        if (count >= 100 && count % 100 == 0) return 500;
        return 0;
    }

    private long scaledExperience(long base, double multiplier) {
        if (base <= 0 || multiplier <= 0.0) return 0;
        return Math.max(1L, Math.round(base * multiplier));
    }

    private long nextProgressTarget(long count) {
        if (count <= 50) return 50;
        if (count % 100 == 0) return count;
        return ((count / 100) + 1) * 100;
    }

    private String materialName(Material material) {
        return material.name().toLowerCase();
    }

    private String entityName(LivingEntity entity) {
        String customName = entity.getCustomName();
        if (customName != null && !customName.isBlank()) return customName;
        return entity.getType().name().toLowerCase();
    }
}
