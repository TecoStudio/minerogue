package com.roguelike.weapon;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.ArmorSetManager;
import com.roguelike.armor.affix.ArmorAffixManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WeaponAbilityManager {
    private static int taskId = -1;
    private static final Map<UUID, Long> bombCooldowns = new HashMap<>();
    private static final Map<UUID, DashState> dashStates = new HashMap<>();
    private static final Map<UUID, GiftHeal> giftHeals = new HashMap<>();
    private static final List<TrackedTnt> trackedTnt = new ArrayList<>();

    public static void init(RoguelikePlugin plugin) {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, WeaponAbilityManager::tick, 1L, 1L);
    }

    public static void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        bombCooldowns.clear();
        dashStates.clear();
        giftHeals.clear();
        trackedTnt.clear();
    }

    public static boolean hasEffect(ItemStack stack, String effect) {
        CustomWeapon template = WeaponManager.getTemplate(stack);
        WeaponInstanceData data = WeaponManager.getData(stack);
        return template != null && data != null && data.getTotalEffect(template, effect, 0.0) > 0.0;
    }

    public static double applySmash(Player player, ItemStack weapon, CustomWeapon template, WeaponInstanceData data, double damage) {
        if (data.getTotalEffect(template, "smash", 0.0) <= 0.0) return damage;
        player.setCooldown(weapon.getType(), 140);
        double multiplier = 3.0;
        PotionEffect strength = player.getPotionEffect(PotionEffectType.STRENGTH);
        if (strength != null) {
            multiplier += 3.0 * (strength.getAmplifier() + 1);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.8f, 1.2f);
        Message.send(player, "&6&l猛击！ &f伤害倍率 x" + WeaponManager.format(multiplier, 1));
        return damage * multiplier;
    }

    public static void applyHyper(Player player, CustomWeapon template, WeaponInstanceData data, boolean crit) {
        if (!crit) return;
        int level = (int) Math.max(0, data.getTotalEffect(template, "hyper", 0.0));
        if (level <= 0) return;
        int amplifier = Math.min(2, level - 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, amplifier, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, amplifier, true, true));
    }

    public static void applyGiftKill(Player player) {
        if (!hasEffect(player.getInventory().getItemInMainHand(), "gift")) return;
        double maxHealth = maxHealth(player);
        giftHeals.put(player.getUniqueId(), new GiftHeal(player.getUniqueId(), maxHealth * 0.5, 140));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, true));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4);
        Message.send(player, "&d馈赠触发，7秒内回复最大生命值的50%。");
    }

    public static void cancelGiftHeal(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && giftHeals.remove(player.getUniqueId()) != null) {
            Message.send(player, "&7馈赠回复已因受到伤害停止。");
        }
    }

    public static void handleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hasEffect(hand, "bomb")) throwBomb(player);
        if (hasDash(player)) dash(player);
    }

    public static List<String> getSidebarLines(Player player) {
        List<String> lines = new ArrayList<>();
        long now = System.currentTimeMillis();
        Long bombUntil = bombCooldowns.get(player.getUniqueId());
        if (bombUntil != null && bombUntil > now) {
            lines.add("§6炸弹: §f" + secondsLeft(bombUntil, now) + "s");
        }
        if (!hasDash(player)) return lines;
        DashState dash = dashStates.computeIfAbsent(player.getUniqueId(), id -> new DashState());
        int maxCharges = maxDashCharges(player);
        refillDash(dash, now, maxCharges, dashCooldownMillis(player));
        if (dash.charges < maxCharges) {
            lines.add(dash.charges > 0 ? "§bDash: §f" + dash.charges + "/" + maxCharges : "§bDash: §f" + secondsLeft(dash.nextChargeAt, now) + "s");
        }
        return lines;
    }

    private static void throwBomb(Player player) {
        long now = System.currentTimeMillis();
        long until = bombCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (until > now) {
            Message.send(player, "&c小心炸弹！冷却中: &f" + secondsLeft(until, now) + "秒");
            return;
        }
        Location spawn = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(0.8));
        TNTPrimed tnt = player.getWorld().spawn(spawn, TNTPrimed.class, entity -> {
            entity.setSource(player);
            entity.setFuseTicks(60);
            entity.setYield(4.0f);
            entity.setIsIncendiary(true);
            entity.setVelocity(player.getLocation().getDirection().normalize().multiply(2.1).setY(0.55));
        });
        trackedTnt.add(new TrackedTnt(tnt, spawn.clone(), player.getUniqueId()));
        bombCooldowns.put(player.getUniqueId(), now + 30_000L);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1.3f);
        Message.send(player, "&6小心炸弹！");
    }

    private static void dash(Player player) {
        if (player.isOnGround() || player.getGameMode() == GameMode.SPECTATOR) return;
        long now = System.currentTimeMillis();
        DashState state = dashStates.computeIfAbsent(player.getUniqueId(), id -> new DashState());
        int maxCharges = maxDashCharges(player);
        long cooldown = dashCooldownMillis(player);
        refillDash(state, now, maxCharges, cooldown);
        if (state.charges <= 0) {
            Message.send(player, "&cDash 冷却中: &f" + secondsLeft(state.nextChargeAt, now) + "秒");
            return;
        }
        state.charges--;
        if (state.charges < maxCharges && state.nextChargeAt <= now) state.nextChargeAt = now + cooldown;
        Vector velocity = player.getLocation().getDirection().normalize().multiply(1.15);
        velocity.setY(Math.max(velocity.getY(), 0.15));
        player.setVelocity(player.getVelocity().add(velocity));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 1.8f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.2, 0.2, 0.2, 0.02);
    }

    private static void tick() {
        tickGiftHeals();
        tickTnt();
    }

    private static void tickGiftHeals() {
        Iterator<GiftHeal> iterator = giftHeals.values().iterator();
        while (iterator.hasNext()) {
            GiftHeal heal = iterator.next();
            Player player = Bukkit.getPlayer(heal.playerId);
            if (player == null || !player.isOnline() || player.isDead()) {
                iterator.remove();
                continue;
            }
            player.setHealth(Math.min(maxHealth(player), player.getHealth() + heal.totalHeal / heal.totalTicks));
            heal.ticksLeft--;
            if (heal.ticksLeft <= 0) iterator.remove();
        }
    }

    private static void tickTnt() {
        Iterator<TrackedTnt> iterator = trackedTnt.iterator();
        while (iterator.hasNext()) {
            TrackedTnt tracked = iterator.next();
            TNTPrimed tnt = tracked.tnt;
            if (tnt == null || tnt.isDead() || !tnt.isValid()) {
                iterator.remove();
                continue;
            }
            if (tnt.getLocation().distanceSquared(tracked.start) >= 400.0) {
                Location location = tnt.getLocation();
                tnt.remove();
                Player source = Bukkit.getPlayer(tracked.ownerId);
                location.getWorld().createExplosion(location, 4.0f, true, true, source);
                iterator.remove();
            }
        }
    }

    private static void refillDash(DashState state, long now, int maxCharges, long cooldown) {
        state.charges = Math.min(state.charges, maxCharges);
        while (state.charges < maxCharges && state.nextChargeAt > 0 && state.nextChargeAt <= now) {
            state.charges++;
            state.nextChargeAt = state.charges < maxCharges ? state.nextChargeAt + cooldown : 0L;
        }
    }

    private static boolean hasDash(Player player) {
        return ArmorAffixManager.hasEquippedAffix(player, "dash");
    }

    private static int maxDashCharges(Player player) {
        return ArmorSetManager.swiftPieces(player) >= 4 ? 3 : 2;
    }

    private static long dashCooldownMillis(Player player) {
        return ArmorSetManager.swiftPieces(player) >= 4 ? 4_000L : 5_000L;
    }

    private static int secondsLeft(long until, long now) {
        return (int) Math.max(1, Math.ceil((until - now) / 1000.0));
    }

    private static double maxHealth(Player player) {
        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : player.getHealth();
    }

    private static class DashState {
        int charges = 2;
        long nextChargeAt = 0L;
    }

    private static class GiftHeal {
        final UUID playerId;
        final double totalHeal;
        final int totalTicks;
        int ticksLeft;

        GiftHeal(UUID playerId, double totalHeal, int totalTicks) {
            this.playerId = playerId;
            this.totalHeal = totalHeal;
            this.totalTicks = totalTicks;
            this.ticksLeft = totalTicks;
        }
    }

    private record TrackedTnt(TNTPrimed tnt, Location start, UUID ownerId) {
    }
}
