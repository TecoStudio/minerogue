package com.roguelike.weapon;

import com.roguelike.combat.CombatHandler;
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

import java.util.UUID;

public final class BowAbilityManager {
    private static final String BOW_DATA = "roguelike_bow_data";
    private static org.bukkit.plugin.Plugin plugin;

    private BowAbilityManager() {
    }

    public static void init(org.bukkit.plugin.Plugin plugin) {
        BowAbilityManager.plugin = plugin;
    }

    public static void shutdown() {
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
        double chargeMultiplier = chargeMultiplier(event.getForce(), charge);
        arrow.setMetadata(BOW_DATA, new FixedMetadataValue(plugin,
                new BowShotData(player.getUniqueId(), template.getId(), data.getInstanceId(), chargeMultiplier)));
        if (chargeMultiplier > 1.05) {
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getEyeLocation(), 12, 0.35, 0.35, 0.35);
        }
        if (scatter > 0) spawnScatter(player, arrow, scatter, template.getId(), data.getInstanceId(), chargeMultiplier);
        if (rapid > 0) scheduleRapid(player, bow.clone(), rapid, template.getId(), data.getInstanceId(), chargeMultiplier);
    }

    public static boolean handleArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return false;
        if (!(event.getEntity() instanceof LivingEntity target)) return false;
        BowShotData shot = shotData(arrow);
        if (shot == null) return false;
        Player player = Bukkit.getPlayer(shot.playerId);
        if (player == null || !player.isOnline()) return false;
        if (shot.chargeMultiplier > 1.0) {
            event.setDamage(event.getDamage() * shot.chargeMultiplier);
        }
        if (WeaponManager.getTemplate(player.getInventory().getItemInMainHand()) != null) {
            event.setDamage(CombatHandler.processAttack(player, target, event.getDamage()));
        }
        return true;
    }

    static double chargeMultiplier(float force, int level) {
        if (level <= 0 || force < 0.95f) return 1.0;
        return 1.0 + Math.min(1.5, level * 0.15 + force * level * 0.05);
    }

    private static void spawnScatter(Player player, AbstractArrow source, int level, String templateId, String instanceId, double chargeMultiplier) {
        Vector direction = source.getVelocity().clone();
        if (direction.lengthSquared() <= 0.001) return;
        double speed = direction.length();
        direction.normalize();
        int arrows = Math.min(5, level);
        for (int i = 0; i < arrows; i++) {
            double angle = Math.toRadians((i - (arrows - 1) / 2.0) * 7.5);
            Vector scattered = rotateY(direction, angle).multiply(speed * 0.92);
            Arrow arrow = player.launchProjectile(Arrow.class, scattered);
            arrow.setShooter(player);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setMetadata(BOW_DATA, new FixedMetadataValue(plugin,
                    new BowShotData(player.getUniqueId(), templateId, instanceId, chargeMultiplier * 0.65)));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.35f);
    }

    private static void scheduleRapid(Player player, ItemStack bow, int level, String templateId, String instanceId, double chargeMultiplier) {
        int arrows = Math.min(5, level);
        for (int i = 1; i <= arrows; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || player.isDead()) return;
                CustomWeapon currentTemplate = WeaponManager.getTemplate(bow);
                if (currentTemplate == null) return;
                Location eye = player.getEyeLocation();
                Arrow arrow = player.launchProjectile(Arrow.class, eye.getDirection().normalize().multiply(2.8));
                arrow.setShooter(player);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setMetadata(BOW_DATA, new FixedMetadataValue(plugin,
                        new BowShotData(player.getUniqueId(), templateId, instanceId, chargeMultiplier * 0.75)));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.45f, 1.6f);
            }, i * 4L);
        }
    }

    private static Vector rotateY(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z).normalize();
    }

    private static BowShotData shotData(AbstractArrow arrow) {
        for (MetadataValue value : arrow.getMetadata(BOW_DATA)) {
            Object raw = value.value();
            if (raw instanceof BowShotData data) return data;
        }
        return null;
    }

    private record BowShotData(UUID playerId, String templateId, String instanceId, double chargeMultiplier) {
    }
}
