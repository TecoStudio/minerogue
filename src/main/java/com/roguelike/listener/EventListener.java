package com.roguelike.listener;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.ArmorSetManager;
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
import com.roguelike.equipment.EquipmentTypeResolver;
import com.roguelike.forge.ForgeTableManager;
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
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager.get(player);
        LevelManager.updateExpBar(player);
        player.getServer().getScheduler().runTaskLater(RoguelikePlugin.getInstance(), () -> {
            WeaponManager.refreshHeldWeapon(player);
            ArmorSetManager.applyPassiveEffects(player);
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
        if (action == Action.RIGHT_CLICK_BLOCK && ForgeTableManager.isForgeTable(event.getClickedBlock())) {
            event.setCancelled(true);
            ForgeTableManager.open(player);
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

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
                event.setCancelled(true);
                TicketManager.applyTicket(player, off, main);
            }
        }
    }

    private boolean canTargetEquipment(ItemStack stack) {
        return WeaponManager.getTemplate(stack) != null || (stack != null && EquipmentTypeResolver.isWearable(stack.getType()));
    }

    private boolean canTargetAnyItem(TicketType ticket) {
        return ticket == TicketType.TICKET_B || ticket == TicketType.WEAPON_DEVELOPMENT;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
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
