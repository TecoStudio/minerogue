package com.roguelike.mob.internal;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.mob.InternalMob;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ZombieEliteMob implements InternalMob {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static final String ID = "zombie_elite";

    private final NamespacedKey mobKey;

    public ZombieEliteMob(RoguelikePlugin plugin) {
        this.mobKey = new NamespacedKey(plugin, "internal_mob");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSpawn(LivingEntity entity) {
        ConfigManager.ZombieEliteConfig config = ConfigManager.getZombieEliteConfig();
        if (!config.enabled()) return;
        if (!(entity instanceof Zombie zombie)) return;
        if (zombie.getEntitySpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (RANDOM.nextDouble() >= config.spawnChance()) return;

        apply(zombie, config);
    }

    @Override
    public LivingEntity spawn(Location location) {
        Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        apply(zombie, ConfigManager.getZombieEliteConfig());
        return zombie;
    }

    private void apply(Zombie zombie, ConfigManager.ZombieEliteConfig config) {
        zombie.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, ID);
        zombie.customName(Message.toComponent(config.name()));
        zombie.setCustomNameVisible(false);

        var health = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(config.health());
            zombie.setHealth(config.health());
        }
        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(config.damage());
        }

        EntityEquipment equipment = zombie.getEquipment();
        if (equipment == null) return;
        equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
        equipment.setItemInMainHand(stoneSword(config.weaponTemplate()));
        equipment.setHelmetDropChance(0.02f);
        equipment.setItemInMainHandDropChance(0.10f);
    }

    private ItemStack stoneSword(String weaponTemplate) {
        CustomWeapon template = ConfigManager.getWeapon(weaponTemplate);
        ItemStack sword = template != null
                ? WeaponManager.createWeaponStack(template, Material.STONE_SWORD)
                : new ItemStack(Material.STONE_SWORD);

        Enchantment sharpness = Enchantment.getByKey(NamespacedKey.minecraft("sharpness"));
        if (sharpness != null) {
            sword.addUnsafeEnchantment(sharpness, 1);
        }
        return sword;
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        LivingEntity attacker = event.getDamager() instanceof LivingEntity living ? living : null;
        if (attacker == null || !isMob(attacker)) return;
        event.setDamage(ConfigManager.getZombieEliteConfig().damage());
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isMob(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        return ID.equals(value);
    }
}
