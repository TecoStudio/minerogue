package com.roguelike.combat;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.affix.ArmorAffixManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.util.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import com.roguelike.weapon.WeaponAbilityManager;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CombatHandler {
    private static final String BLEEDING_METADATA = "roguelike_bleeding_until";
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static final Map<UUID, Long> lightningImmuneUntil = new HashMap<>();
    private static boolean internalDamage = false;
    private static RoguelikePlugin plugin;

    public static void init(RoguelikePlugin plugin) {
        CombatHandler.plugin = plugin;
    }

    public static boolean isInternalDamage() {
        return internalDamage;
    }

    public static boolean shouldProcessAttack(boolean roguelikeWeapon, boolean protectedDummy) {
        return roguelikeWeapon || protectedDummy;
    }

    public static double processAttack(Player player, LivingEntity target, double baseDamage) {
        CustomWeapon template = WeaponManager.getTemplate(player.getInventory().getItemInMainHand());
        WeaponInstanceData data = WeaponInstanceData.fromItemStack(player.getInventory().getItemInMainHand());
        if (template == null || data == null) {
            if (DamageTestDummyManager.isProtected(target)) {
                sendBaseDamageFormula(player, baseDamage);
            } else {
                player.sendActionBar(Message.toComponent(formatDamageActionBar(baseDamage)));
            }
            return baseDamage;
        }

        double weaponDamage = data.getTotalDamage(template);
        double vanillaBonus = vanillaBonus(baseDamage, weaponDamage, false);
        double damage = weaponDamage + vanillaBonus;
        double totalSpeed = WeaponManager.getTotalAttackSpeed(template, data);
        List<String> damageParts = new ArrayList<>();
        List<FormulaPart> formulaParts = new ArrayList<>();
        List<String> extraParts = new ArrayList<>();
        damageParts.add("基础 " + WeaponManager.format(weaponDamage, 1));
        formulaParts.add(FormulaPart.add("§a", weaponDamage));
        if (vanillaBonus > 0.05) {
            damageParts.add("原版跳劈/附魔 +" + WeaponManager.format(vanillaBonus, 1));
            formulaParts.add(FormulaPart.add("§b", vanillaBonus));
        }
        double speedMultiplier = attackSpeedOverflowMultiplier(totalSpeed);
        if (speedMultiplier > 1.0) {
            double before = damage;
            damage *= speedMultiplier;
            damageParts.add("攻速溢出 x" + WeaponManager.format(speedMultiplier, 2)
                    + "：" + WeaponManager.format(before, 1) + " -> " + WeaponManager.format(damage, 1));
            formulaParts.add(FormulaPart.multiply("§b", speedMultiplier));
        }

        List<String> neutralSources = new ArrayList<>();
        double neutralMultiplier = 1.0;
        if (data.getTotalEffect(template, "contract_damage_200", 0.0) > 0) {
            neutralMultiplier *= 2.0;
            neutralSources.add("狂战契约 x2.0");
        }
        if (data.getTotalEffect(template, "contract_berserk_self_harm", 0.0) > 0) {
            neutralMultiplier *= 3.0;
            neutralSources.add("血怒契约 x3.0");
            double selfDamage = maxHealth(player) * 0.10;
            if (selfDamage > 0) applyInternalDamage(player, selfDamage, player);
        }
        if (neutralMultiplier > 1.0) {
            double before = damage;
            damage *= neutralMultiplier;
            damageParts.add("契约增伤 x" + WeaponManager.format(neutralMultiplier, 1)
                    + "：" + WeaponManager.format(before, 1) + " -> " + WeaponManager.format(damage, 1)
                    + "，来源 " + String.join("、", neutralSources));
            formulaParts.add(FormulaPart.multiply("§6", neutralMultiplier));
        }

        double beforeSmash = damage;
        damage = WeaponAbilityManager.applySmash(player, player.getInventory().getItemInMainHand(), template, data, damage);
        if (damage > beforeSmash + 0.05) {
            double multiplier = damage / beforeSmash;
            damageParts.add("猛击 x" + WeaponManager.format(multiplier, 1)
                    + "：" + WeaponManager.format(beforeSmash, 1) + " -> " + WeaponManager.format(damage, 1));
            formulaParts.add(FormulaPart.multiply("§c", multiplier));
        }

        boolean wasBurning = target.getFireTicks() > 0;
        boolean wasPoisoned = target.hasPotionEffect(PotionEffectType.POISON);
        double burningBonus = data.getTotalEffect(template, "burning_target_damage_percent", 0.0);
        double poisonedBonus = data.getTotalEffect(template, "poisoned_target_damage_percent", 0.0);
        if (wasBurning && burningBonus > 0) {
            double before = damage;
            damage *= 1 + burningBonus;
            double multiplier = 1 + burningBonus;
            damageParts.add("燃烧目标增伤 x" + WeaponManager.format(multiplier, 2)
                    + "：" + WeaponManager.format(before, 1) + " -> " + WeaponManager.format(damage, 1));
            formulaParts.add(FormulaPart.multiply("§6", multiplier));
        }
        if (wasPoisoned && poisonedBonus > 0) {
            double before = damage;
            damage *= 1 + poisonedBonus;
            double multiplier = 1 + poisonedBonus;
            damageParts.add("中毒目标增伤 x" + WeaponManager.format(multiplier, 2)
                    + "：" + WeaponManager.format(before, 1) + " -> " + WeaponManager.format(damage, 1));
            formulaParts.add(FormulaPart.multiply("§2", multiplier));
        }

        // 暴击
        double critChance = chance(data.getTotalEffect(template, "crit_chance", 0.0) + contractBonus(template, data, "contract_crit_chance_100", 1.0));
        double critDamage = data.getTotalEffect(template, "crit_damage", 1.5) + contractBonus(template, data, "contract_crit_damage_300", 1.5);
        boolean crit = critChance > 0 && RANDOM.nextDouble() < critChance;
        if (crit) {
            double before = damage;
            damage *= critDamage;
            WeaponAbilityManager.applyHyper(player, template, data, true);
            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            player.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 10, 0.3, 0.3, 0.3);
            damageParts.add("插件暴击 x" + WeaponManager.format(critDamage, 1)
                    + "：" + WeaponManager.format(before, 1) + " -> " + WeaponManager.format(damage, 1));
            formulaParts.add(FormulaPart.multiply("§d", critDamage));
        }

        // 伤害存储爆发：按攻击次数触发，默认 20 下，最低 5 下。
        double storePercent = data.getTotalEffect(template, "damage_store_percent", 0.0);
        int storeHitReduction = (int) data.getTotalEffect(template, "damage_store_hit_reduction", 0.0);
        int requiredHits = Math.max(5, 20 - storeHitReduction);
        if (storePercent > 0) {
            data.addStoredDamage(damage * storePercent);
            data.incrementStoredDamageHits();
            if (data.getStoredDamageHits() >= requiredHits) {
                double burst = data.getStoredDamage();
                damage += burst;
                data.setStoredDamage(0);
                data.setStoredDamageHits(0);
                damageParts.add("爆发储存 +" + WeaponManager.format(burst, 1));
                formulaParts.add(FormulaPart.add("§e", burst));
                Message.send(player, "&6&l爆发！ 额外 " + WeaponManager.format(burst, 1) + " 伤害");
                player.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
            }
            data.saveToItemStack(player.getInventory().getItemInMainHand());
        }

        // 吸血
        double lifePercent = data.getTotalEffect(template, "lifesteal_percent", 0.0) + contractBonus(template, data, "contract_lifesteal_100", 1.0);
        double lifeFlat = data.getTotalEffect(template, "lifesteal_flat", 0.0);
        double heal = damage * lifePercent + lifeFlat;
        if (heal > 0) {
            var maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : player.getHealth();
            player.setHealth(Math.min(player.getHealth() + heal, maxHealth));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), (int) Math.min(5, heal + 1));
        }

        // 减速
        double slowDuration = data.getTotalEffect(template, "slow_duration", 0.0);
        int slowLevel = (int) data.getTotalEffect(template, "slow_level", 0.0);
        if (slowDuration > 0) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (slowDuration * 20), slowLevel));
        }

        // 火焰
        double fireDamage = data.getTotalEffect(template, "fire_damage", 0.0);
        double fireDuration = data.getTotalEffect(template, "fire_duration", 0.0);
        if (fireDuration > 0) {
            target.setFireTicks((int) (fireDuration * 20));
            if (fireDamage > 0) {
                applyInternalDamage(target, fireDamage, player);
                if (fireDamage > 3.0) {
                    extraParts.add("火焰 " + WeaponManager.format(fireDamage, 1));
                }
            }
        }

        // 中毒
        double poisonChance = chance(data.getTotalEffect(template, "poison_chance", 0.0));
        if (poisonChance > 0 && RANDOM.nextDouble() < poisonChance) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            extraParts.add("中毒触发");
        }

        // 雷电
        int stormPieces = ArmorAffixManager.stormPieces(player);
        double lightning = chance(data.getTotalEffect(template, "lightning_chance", 0.0)
                + contractBonus(template, data, "contract_thunder_100", 1.0)
                + stormLightningChance(stormPieces));
        if (lightning > 0 && RANDOM.nextDouble() < lightning) {
            protectFromOwnLightning(player);
            target.getWorld().strikeLightning(target.getLocation());
            extraParts.add(stormPieces > 0 ? "雷暴触发" : "雷击触发");
        }

        // 爆炸
        double explosionChance = chance(data.getTotalEffect(template, "explosion_chance", 0.0) + contractBonus(template, data, "contract_explosion_100", 1.0));
        if (explosionChance > 0 && RANDOM.nextDouble() < explosionChance) {
            target.getWorld().createExplosion(target.getLocation(), 2.0f, false, false, player);
            extraParts.add("爆炸触发");
        }

        // 大爆炸：TNT 级别爆炸，会点火并破坏方块。
        double bigExplosionChance = chance(data.getTotalEffect(template, "big_explosion_chance", 0.0));
        if (bigExplosionChance > 0 && RANDOM.nextDouble() < bigExplosionChance) {
            target.getWorld().createExplosion(target.getLocation(), 4.0f, true, true, player);
            extraParts.add("大爆炸触发");
        }

        // 连锁伤害
        int chainTargets = (int) data.getTotalEffect(template, "chain_targets", 0.0);
        double chainRange = data.getTotalEffect(template, "chain_range", 0.0);
        double chainPercent = data.getTotalEffect(template, "chain_damage_percent", 0.0);
        if (chainTargets > 0 && chainRange > 0 && chainPercent > 0) {
            applyChainDamage(player, target, damage, chainTargets, chainRange, chainPercent);
            double chainDamage = damage * chainPercent;
            if (chainDamage > 3.0) {
                extraParts.add("连锁每目标 " + WeaponManager.format(chainDamage, 1));
            } else {
                extraParts.add("连锁触发");
            }
        }

        sendDamageFormula(player, target, damage, formulaParts, damageParts, extraParts);
        return damage;
    }

    private static double stormLightningChance(int pieces) {
        return switch (pieces) {
            case 1 -> 0.08;
            case 2 -> 0.16;
            case 3 -> 0.24;
            case 4 -> 0.36;
            default -> 0.0;
        };
    }

    static double vanillaBonus(double eventDamage, double weaponDamage, boolean vanillaCritical) {
        double existingBonus = Math.max(0.0, eventDamage - weaponDamage);
        double criticalBonus = vanillaCritical ? Math.max(0.0, weaponDamage * 0.5) : 0.0;
        return existingBonus >= criticalBonus ? existingBonus : existingBonus + criticalBonus;
    }

    static double resolveWeaponBaseDamage(double eventDamage, double weaponDamage, boolean vanillaCritical) {
        return weaponDamage + vanillaBonus(eventDamage, weaponDamage, vanillaCritical);
    }

    static double attackSpeedOverflowMultiplier(double totalSpeed) {
        return totalSpeed > WeaponManager.MAX_ATTACK_SPEED
                ? totalSpeed / WeaponManager.MAX_ATTACK_SPEED
                : 1.0;
    }

    private static void sendBaseDamageFormula(Player player, double damage) {
        Component message = Message.toComponent("&7伤害: &a" + WeaponManager.format(damage, 1)
                + " &8= &f" + WeaponManager.format(damage, 1));
        String hover = "§f" + WeaponManager.format(damage, 1) + " §7(基础伤害 "
                + WeaponManager.format(damage, 1) + ")";
        player.sendMessage(message.hoverEvent(HoverEvent.showText(Message.toComponent(hover))));
    }

    private static void sendDamageFormula(Player player, LivingEntity target, double damage,
                                          List<FormulaPart> formulaParts, List<String> damageParts,
                                          List<String> extraParts) {
        Component message = Message.toComponent("&7伤害: ");
        StringBuilder formula = new StringBuilder();
        for (int i = 0; i < formulaParts.size(); i++) {
            FormulaPart part = formulaParts.get(i);
            if (i > 0) {
                message = message.append(Message.toComponent(part.operator));
                formula.append(part.operator);
            }
            String value = part.color + WeaponManager.format(part.value, part.decimals);
            message = message.append(Message.toComponent(value));
            formula.append(value);
        }
        message = message.append(Message.toComponent(" &8= &f" + WeaponManager.format(damage, 1)));
        if (DamageTestDummyManager.isProtected(target)) {
            String hover = "§f" + WeaponManager.format(damage, 1) + " §7(" + String.join("§7, ", damageParts) + "§7)";
            if (!extraParts.isEmpty()) {
                hover += "\n§7额外: §e" + String.join("§7, §e", extraParts);
            }
            player.sendMessage(message.hoverEvent(HoverEvent.showText(Message.toComponent(hover))));
        } else {
            player.sendActionBar(Message.toComponent(formatDamageActionBar(damage)));
        }
    }

    static String formatDamageChatText(String formula, double damage) {
        return "§7伤害: " + formula + " §8= §f" + WeaponManager.format(damage, 1);
    }

    static String formatDamageActionBar(double damage) {
        return "§7伤害: §f" + WeaponManager.format(damage, 1);
    }

    private static double contractBonus(CustomWeapon template, WeaponInstanceData data, String id, double value) {
        return data.getTotalEffect(template, id, 0.0) > 0 ? value : 0.0;
    }

    public static double applyIncomingContractDamage(Player player, double damage) {
        CustomWeapon template = WeaponManager.getTemplate(player.getInventory().getItemInMainHand());
        WeaponInstanceData data = WeaponManager.getData(player.getInventory().getItemInMainHand());
        if (template == null || data == null) return damage;
        if (hasAnyIncomingDamageDouble(template, data)) return damage * 2.0;
        return damage;
    }

    public static boolean cancelLightningDamageForImmunePlayer(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return false;
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.LIGHTNING) return false;
        Long until = lightningImmuneUntil.get(player.getUniqueId());
        if (until == null || until < System.currentTimeMillis()) return false;
        event.setCancelled(true);
        return true;
    }

    private static void protectFromOwnLightning(Player player) {
        lightningImmuneUntil.put(player.getUniqueId(), System.currentTimeMillis() + 1500L);
        if (plugin != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> lightningImmuneUntil.remove(player.getUniqueId()), 40L);
        }
    }

    private static boolean hasAnyIncomingDamageDouble(CustomWeapon template, WeaponInstanceData data) {
        return data.getTotalEffect(template, "contract_damage_200", 0.0) > 0
                || data.getTotalEffect(template, "chain_targets", 0.0) > 0
                || data.getTotalEffect(template, "chain_damage_percent", 0.0) > 0
                || data.getTotalEffect(template, "crit_chance", 0.0) > 0
                || data.getTotalEffect(template, "crit_damage", 1.5) > 1.5
                || data.getTotalEffect(template, "fire_damage", 0.0) > 0
                || data.getTotalEffect(template, "lightning_chance", 0.0) > 0
                || data.getTotalEffect(template, "damage_store_percent", 0.0) > 0
                || data.getTotalEffect(template, "burning_target_damage_percent", 0.0) > 0
                || data.getTotalEffect(template, "poisoned_target_damage_percent", 0.0) > 0
                || data.getTotalEffect(template, "explosion_chance", 0.0) > 0
                || data.getTotalEffect(template, "big_explosion_chance", 0.0) > 0
                || data.getTotalEffect(template, "smash", 0.0) > 0
                || data.getTotalEffect(template, "contract_speed_200", 0.0) > 0
                || data.getTotalEffect(template, "contract_attack_speed_200", 0.0) > 0
                || data.getTotalEffect(template, "contract_range_200", 0.0) > 0
                || data.getTotalEffect(template, "contract_crit_chance_100", 0.0) > 0
                || data.getTotalEffect(template, "contract_crit_damage_300", 0.0) > 0
                || data.getTotalEffect(template, "contract_lifesteal_100", 0.0) > 0
                || data.getTotalEffect(template, "contract_thunder_100", 0.0) > 0
                || data.getTotalEffect(template, "contract_explosion_100", 0.0) > 0;
    }

    private static void sendStoreProgress(Player player, int hits, int requiredHits) {
        int percent = requiredHits <= 0 ? 0 : (int) Math.min(100, Math.round(hits / (double) requiredHits * 100));
        int filled = Math.min(10, Math.max(0, percent / 10));
        String bar = "§6[" + "#".repeat(filled) + "§7" + "-".repeat(10 - filled) + "§6]§f" + percent + "%";
        player.sendActionBar(Message.toComponent("§e伤害储存 " + bar + " §7" + hits + "/" + requiredHits));
    }

    private static double chance(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double maxHealth(Player player) {
        var maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        return maxHealthAttr != null ? maxHealthAttr.getValue() : player.getHealth();
    }

    private static void applyChainDamage(Player player, LivingEntity primary, double baseDamage, int maxTargets, double range, double percent) {
        List<LivingEntity> nearby = new ArrayList<>();
        for (org.bukkit.entity.Entity entity : primary.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity living && !entity.equals(primary) && !entity.equals(player)) {
                nearby.add(living);
                if (nearby.size() >= maxTargets) break;
            }
        }
        double chainDamage = baseDamage * percent;
        for (LivingEntity entity : nearby) {
            applyInternalDamage(entity, chainDamage, player);
            drawParticleLine(primary.getLocation().add(0, 1, 0), entity.getLocation().add(0, 1, 0));
        }
    }

    public static void applyInternalDamage(LivingEntity target, double damage, Player player) {
        internalDamage = true;
        try {
            target.damage(damage, player);
        } finally {
            internalDamage = false;
        }
    }

    private static void drawParticleLine(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length == 0) return;
        direction.normalize();
        for (double d = 0; d < length; d += 0.3) {
            Location point = from.clone().add(direction.clone().multiply(d));
            from.getWorld().spawnParticle(Particle.ENCHANTED_HIT, point, 1);
        }
    }

    private record FormulaPart(String operator, String color, double value, int decimals) {
        private static FormulaPart add(String color, double value) {
            return new FormulaPart(" &8+ ", color, value, 1);
        }

        private static FormulaPart multiply(String color, double value) {
            return new FormulaPart(" &8x ", color, value, 2);
        }
    }

}
