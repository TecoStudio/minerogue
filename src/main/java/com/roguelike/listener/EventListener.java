package com.roguelike.listener;

import com.roguelike.RoguelikePlugin;
import com.roguelike.combat.CombatHandler;
import com.roguelike.config.MobExperienceConfig;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.integration.IntegrationManager;
import com.roguelike.level.LevelManager;
import com.roguelike.mob.MobManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.ticket.TicketManager;
import com.roguelike.ticket.TicketType;
import com.roguelike.util.Message;
import com.roguelike.weapon.ToolAbilityManager;
import com.roguelike.weapon.WeaponAbilityManager;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class EventListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager.get(player);
        LevelManager.updateExpBar(player);
        player.getServer().getScheduler().runTaskLater(RoguelikePlugin.getInstance(), () -> {
            WeaponManager.refreshHeldWeapon(player);
            RoguelikeScoreboard.updatePlayer(player);
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerDataManager.unload(event.getPlayer());
        RoguelikeScoreboard.clearPlayer(event.getPlayer());
        WeaponManager.clearAttributes(event.getPlayer());
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

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        TicketType mainTicket = TicketManager.getTicketType(main);
        TicketType offTicket = TicketManager.getTicketType(off);

        // 主手持券，副手持目标物品。开发券允许目标是任意非空气物品。
        if (mainTicket != null) {
            if (canTargetAnyItem(mainTicket) || WeaponManager.getTemplate(off) != null) {
                event.setCancelled(true);
                if (TicketManager.applyTicket(player, main, off)) {
                    main.setAmount(main.getAmount() - 1);
                }
            }
        }
        // 副手持券，主手持目标物品。开发券允许目标是任意非空气物品。
        else if (offTicket != null) {
            if (canTargetAnyItem(offTicket) || WeaponManager.getTemplate(main) != null) {
                event.setCancelled(true);
                if (TicketManager.applyTicket(player, off, main)) {
                    off.setAmount(off.getAmount() - 1);
                }
            }
        }
    }

    private boolean canTargetAnyItem(TicketType ticket) {
        return ticket == TicketType.TICKET_B || ticket == TicketType.WEAPON_DEVELOPMENT;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (WeaponManager.getTemplate(event.getItem()) == null) return;
        event.setCancelled(true);
        Message.send(event.getEnchanter(), "&cRoguelike 武器不能使用附魔台附魔。");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (WeaponManager.getTemplate(event.getItem()) == null) return;
        event.setCancelled(true);
        Message.send(event.getEnchanter(), "&cRoguelike 武器不能使用附魔台附魔。");
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if (WeaponManager.getTemplate(first) == null) return;
        if (!hasAnyEnchant(second) && !hasAnyEnchant(event.getResult())) return;
        event.setResult(null);
    }

    private boolean hasAnyEnchant(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (!stack.getEnchantments().isEmpty()) return true;
        ItemMeta meta = stack.getItemMeta();
        return meta instanceof EnchantmentStorageMeta storageMeta && storageMeta.hasStoredEnchants();
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        WeaponAbilityManager.handleSneak(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (CombatHandler.isInternalDamage()) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (WeaponManager.getTemplate(hand) != null && player.hasCooldown(hand.getType())) {
            event.setCancelled(true);
            Message.send(player, "&c武器暂时无法使用。");
            return;
        }
        if (WeaponManager.getTemplate(player.getInventory().getItemInMainHand()) != null) {
            double damage = CombatHandler.processAttack(player, target, event.getDamage());
            event.setDamage(damage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        WeaponAbilityManager.cancelGiftHeal(event);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() != null) {
            Player player = entity.getKiller();
            String type = entity.getType().name().toLowerCase();
            int exp = MobExperienceConfig.getMobExp(type);
            if (exp > 0) {
                LevelManager.addExperience(player, exp);
            }
            PlayerDataManager.get(player).addKill();
            WeaponAbilityManager.applyGiftKill(player);
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
    public void onBlockBreak(BlockBreakEvent event) {
        ToolAbilityManager.handleBlockBreak(event);
    }
}
