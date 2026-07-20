package com.roguelike.weapon;

import com.roguelike.equipment.EquipmentTypeResolver;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BowAbilityManager {
    private static final String BOW_DATA = "roguelike_bow_data";
    private static final int VANILLA_FULL_DRAW_TICKS = 20;
    private static final int OVERCHARGE_TICKS = 20;
    private static final Map<UUID, ChargeState> chargeStates = new HashMap<>();

    private static org.bukkit.plugin.Plugin plugin;
    private static int taskId = -1;

    private BowAbilityManager() {
    }

    public static void init(org.bukkit.plugin.Plugin plugin) {
        shutdown();
        BowAbilityManager.plugin = plugin;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, BowAbilityManager::tick, 1L, 1L);
    }

    public static void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        chargeStates.clear();
        plugin = null;
    }

    public static void handleShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        ItemStack bow = event.getBow();
        CustomWeapon template = WeaponManager.getTemplate(bow);
        WeaponInstanceData data = WeaponManager.getData(bow);
        if (template == null || data == null || plugin == null) return;

        int scatter = (int) data.getTotalEffect(template, "scatter_shot", 0.0);
        int rapid = (int) data.getTotalEffect(template, "rapid_shot", 0.0);
        int charge = (int) data.getTotalEffect(template, "charge_power", 0.0);
        double progress = event.getForce() >= 0.99f ? shotOverchargeProgress(player, data.getInstanceId()) : 0.0;
        double chargeMultiplier = chargeMultiplier(charge, progress);
        ArrowSnapshot snapshot = ArrowSnapshot.from(arrow);

        arrow.setMetadata(BOW_DATA, new FixedMetadataValue(plugin,
                new BowShotData(player.getUniqueId(), template.getId(), data.getInstanceId(), chargeMultiplier)));
        if (chargeMultiplier > 1.05) {
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getEyeLocation(), 12, 0.35, 0.35, 0.35);
        }
        chargeStates.remove(player.getUniqueId());

        int extraScatter = extraScatterProjectiles(scatter);
        if (extraScatter > 0) spawnScatter(player, arrow, snapshot, extraScatter, template.getId(), data.getInstanceId(), chargeMultiplier);
        if (rapid > 0) scheduleRapid(player, bow.clone(), rapid, template.getId(), data.getInstanceId(), chargeMultiplier, snapshot);
    }

    public static boolean handleArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return false;
        if (!(event.getEntity() instanceof LivingEntity)) return false;
        BowShotData shot = shotData(arrow);
        if (shot == null) return false;

        // Bow shots intentionally keep Bukkit/Paper's vanilla arrow damage as the base.
        // Do not call CombatHandler.processAttack here: melee weapon damage, crits,
        // stored damage, lifesteal, etc. are a separate system from bows.
        event.setDamage(applyChargeMultiplier(event.getDamage(), shot.chargeMultiplier));
        return true;
    }

    public static List<String> getSidebarLines(Player player) {
        ChargeState state = updateChargeState(player);
        if (state == null || state.progress <= 0.0) return List.of();
        int percent = (int) Math.round(state.progress * 100.0);
        return List.of("§d蓄能: §f" + percent + "%");
    }

    static double overchargeProgress(int activeTicks) {
        if (activeTicks <= VANILLA_FULL_DRAW_TICKS) return 0.0;
        return Math.min(1.0, (activeTicks - VANILLA_FULL_DRAW_TICKS) / (double) OVERCHARGE_TICKS);
    }

    static double chargeMultiplier(int level, double progress) {
        if (level <= 0 || progress <= 0.0) return 1.0;
        double clampedProgress = Math.min(1.0, progress);
        return 1.0 + Math.min(1.5, level * 0.20 * clampedProgress);
    }

    static int extraScatterProjectiles(int configuredArrowCount) {
        return Math.max(0, Math.min(5, configuredArrowCount) - 1);
    }

    static double applyChargeMultiplier(double damage, double chargeMultiplier) {
        return chargeMultiplier > 1.0 ? damage * chargeMultiplier : damage;
    }

    static Vector scatterVelocity(Vector sourceVelocity, int extraIndex) {
        if (sourceVelocity == null) return new Vector();
        double speed = sourceVelocity.length();
        if (speed <= 0.001) return sourceVelocity.clone();
        Vector direction = sourceVelocity.clone().normalize();
        return rotateY(direction, scatterAngle(extraIndex)).multiply(speed);
    }

    private static void spawnScatter(Player player, AbstractArrow source, ArrowSnapshot snapshot, int arrows,
                                     String templateId, String instanceId, double chargeMultiplier) {
        if (source.getVelocity().lengthSquared() <= 0.001) return;
        for (int i = 0; i < arrows; i++) {
            Arrow arrow = player.launchProjectile(Arrow.class, scatterVelocity(source.getVelocity(), i));
            arrow.setShooter(player);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            snapshot.applyTo(arrow);
            arrow.setMetadata(BOW_DATA, new FixedMetadataValue(plugin,
                    new BowShotData(player.getUniqueId(), templateId, instanceId, chargeMultiplier * 0.65)));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.35f);
    }

    private static void scheduleRapid(Player player, ItemStack bow, int level, String templateId, String instanceId,
                                      double chargeMultiplier, ArrowSnapshot snapshot) {
        int arrows = Math.min(5, level);
        for (int i = 1; i <= arrows; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || player.isDead()) return;
                CustomWeapon currentTemplate = WeaponManager.getTemplate(bow);
                if (currentTemplate == null) return;
                Location eye = player.getEyeLocation();
                Arrow arrow = player.launchProjectile(Arrow.class, eye.getDirection().normalize().multiply(snapshot.speed));
                arrow.setShooter(player);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                snapshot.applyTo(arrow);
                arrow.setMetadata(BOW_DATA, new FixedMetadataValue(plugin,
                        new BowShotData(player.getUniqueId(), templateId, instanceId, chargeMultiplier * 0.75)));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.45f, 1.6f);
            }, i * 4L);
        }
    }

    private static void tick() {
        chargeStates.keySet().removeIf(id -> {
            Player player = Bukkit.getPlayer(id);
            return player == null || !player.isOnline();
        });
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateChargeState(player);
        }
    }

    private static ChargeState updateChargeState(Player player) {
        if (player == null || !player.isOnline()) return null;
        UUID playerId = player.getUniqueId();
        if (!player.hasActiveItem()) {
            chargeStates.remove(playerId);
            return null;
        }

        ItemStack active = player.getActiveItem();
        if (active == null || !EquipmentTypeResolver.isBow(active.getType())) {
            chargeStates.remove(playerId);
            return null;
        }
        CustomWeapon template = WeaponManager.getTemplate(active);
        WeaponInstanceData data = WeaponManager.getData(active);
        if (template == null || data == null || data.getTotalEffect(template, "charge_power", 0.0) <= 0.0) {
            chargeStates.remove(playerId);
            return null;
        }

        ChargeState state = chargeStates.computeIfAbsent(playerId, ignored -> new ChargeState());
        state.instanceId = data.getInstanceId();
        state.activeTicks = Math.max(0, player.getActiveItemUsedTime());
        state.progress = overchargeProgress(state.activeTicks);
        return state;
    }

    private static double shotOverchargeProgress(Player player, String instanceId) {
        ChargeState state = chargeStates.get(player.getUniqueId());
        if (state != null && instanceId != null && instanceId.equals(state.instanceId)) {
            return state.progress;
        }
        return overchargeProgress(player.getActiveItemUsedTime());
    }

    private static double scatterAngle(int extraIndex) {
        int slot = Math.max(0, extraIndex) + 1;
        int ring = (slot + 1) / 2;
        int side = slot % 2 == 1 ? -1 : 1;
        return Math.toRadians(side * ring * 7.5);
    }

    private static Vector rotateY(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }

    private static BowShotData shotData(AbstractArrow arrow) {
        for (MetadataValue value : arrow.getMetadata(BOW_DATA)) {
            Object raw = value.value();
            if (raw instanceof BowShotData data) return data;
        }
        return null;
    }

    private static final class ChargeState {
        String instanceId;
        int activeTicks;
        double progress;
    }

    private record BowShotData(UUID playerId, String templateId, String instanceId, double chargeMultiplier) {
    }

    private record ArrowSnapshot(double damage, boolean critical, int knockback, int pierce, int fireTicks,
                                 boolean shotFromCrossbow, ItemStack weapon, ItemStack itemStack, double speed) {
        @SuppressWarnings("removal")
        static ArrowSnapshot from(AbstractArrow arrow) {
            ItemStack weapon = arrow.getWeapon();
            ItemStack itemStack = arrow.getItemStack();
            double speed = Math.max(0.001, arrow.getVelocity().length());
            return new ArrowSnapshot(arrow.getDamage(), arrow.isCritical(), arrow.getKnockbackStrength(),
                    arrow.getPierceLevel(), arrow.getFireTicks(), arrow.isShotFromCrossbow(),
                    weapon == null ? null : weapon.clone(), itemStack == null ? null : itemStack.clone(), speed);
        }

        @SuppressWarnings("removal")
        void applyTo(AbstractArrow arrow) {
            arrow.setDamage(damage);
            arrow.setCritical(critical);
            arrow.setKnockbackStrength(knockback);
            arrow.setPierceLevel(pierce);
            arrow.setFireTicks(fireTicks);
            arrow.setShotFromCrossbow(shotFromCrossbow);
            if (weapon != null) arrow.setWeapon(weapon.clone());
            if (itemStack != null) arrow.setItemStack(itemStack.clone());
        }
    }
}