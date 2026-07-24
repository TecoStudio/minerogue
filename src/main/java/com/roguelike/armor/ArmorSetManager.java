package com.roguelike.armor;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.affix.ArmorAffixManager;
import com.roguelike.combat.CombatHandler;
import com.roguelike.config.ArmorDefinition;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomItem;
import com.roguelike.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ArmorSetManager {
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static NamespacedKey SET_KEY;
    private static NamespacedKey PIECE_KEY;

    private ArmorSetManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        SET_KEY = new NamespacedKey(plugin, "armor_set");
        PIECE_KEY = new NamespacedKey(plugin, "armor_set_piece");
    }

    public static ItemStack createSetItem(CustomItem item) {
        return createSetItem(item.getId(), item.getName(), item.getDescription());
    }

    public static ItemStack createSetItem(String id) {
        ArmorDefinition definition = armorDefinitions().get(id.toLowerCase(Locale.ROOT));
        if (definition == null) return null;
        return createSetItem(id, definition.name(), definition.description());
    }

    private static ItemStack createSetItem(String id, String name, String description) {
        ArmorDefinition definition = armorDefinitions().get(id.toLowerCase(Locale.ROOT));
        if (definition == null) return null;
        String set = armorSet(id, definition);
        String piece = armorPiece(id, definition);
        if (set.isBlank() || piece.isBlank()) return null;

        ItemStack stack = new ItemStack(resolveMaterial(definition, set, piece));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(Message.toComponent(setColor(set) + name));
        List<Component> lore = new ArrayList<>();
        lore.add(Message.toComponent("§7" + description));
        lore.add(Message.toComponent("§7─────────────────"));
        String affix = armorAffix(set, definition);
        int affixLevel = Math.max(0, definition.affixLevel());
        if (!affix.isBlank() && affixLevel > 0) {
            lore.add(Message.toComponent("§7自带词条: " + setColor(set) + ArmorAffixManager.displayName(affix) + " §8(" + affix + ")"));
        }
        for (String line : armorLore(set, definition)) {
            lore.add(Message.toComponent(line));
        }
        lore.add(Message.toComponent("§7─────────────────"));
        lore.add(Message.toComponent("§8套装: " + set + " / 部位: " + piece));
        meta.lore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(SET_KEY, PersistentDataType.STRING, set);
        meta.getPersistentDataContainer().set(PIECE_KEY, PersistentDataType.STRING, piece);
        applyBaseEnchantments(meta, set, definition);
        stack.setItemMeta(meta);
        if (!affix.isBlank() && affixLevel > 0) {
            ArmorAffixManager.applyEnchant(stack, affix, affixLevel);
        }
        return stack;
    }

    public static boolean isSetItemId(String id) {
        ArmorDefinition definition = id == null ? null : armorDefinitions().get(id.toLowerCase(Locale.ROOT));
        return definition != null && !armorSet(id, definition).isBlank() && !armorPiece(id, definition).isBlank();
    }

    public static void applyPassiveEffects(Player player) {
        int swift = swiftPieces(player);
        if (swift > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, Math.min(3, swift - 1), true, false, true));
        }
    }

    public static void handlePlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        int thorns = ArmorAffixManager.thornsPieces(player);
        if (thorns > 0) {
            double reflected = event.getDamage() * thornsReflectPercent(thorns);
            CombatHandler.applyInternalDamage(attacker, reflected, player);
            attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, attacker.getLocation().add(0, 1, 0), 8, 0.25, 0.35, 0.25);
            if (thorns >= 4) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 1, true, true, true));
            }
        }

        int explosive = ArmorAffixManager.explosivePieces(player);
        if (explosive > 0 && RANDOM.nextDouble() < explosiveExplosionChance(explosive)) {
            applyExplosiveSetBlast(player, explosive);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            if (explosive >= 4) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 2, true, true, true));
            }
        }
    }

    public static int swiftPieces(Player player) {
        return ArmorAffixManager.swiftPieces(player);
    }

    private static int countPieces(Player player, String set) {
        PlayerInventory inventory = player.getInventory();
        int count = 0;
        if (isPiece(inventory.getHelmet(), set)) count++;
        if (isPiece(inventory.getChestplate(), set)) count++;
        if (isPiece(inventory.getLeggings(), set)) count++;
        if (isPiece(inventory.getBoots(), set)) count++;
        return count;
    }

    private static boolean isPiece(ItemStack stack, String set) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        String id = meta.getPersistentDataContainer().get(SET_KEY, PersistentDataType.STRING);
        return set.equals(id);
    }

    private static double thornsReflectPercent(int pieces) {
        return switch (pieces) {
            case 1 -> 0.35;
            case 2 -> 0.45;
            case 3 -> 0.55;
            default -> 0.70;
        };
    }

    private static double explosiveExplosionChance(int pieces) {
        return switch (pieces) {
            case 1 -> 0.25;
            case 2 -> 0.45;
            case 3 -> 0.75;
            default -> 1.0;
        };
    }

    private static void applyExplosiveSetBlast(Player player, int pieces) {
        Location origin = player.getLocation();
        player.getWorld().spawnParticle(Particle.EXPLOSION, origin, 1);
        double radius = switch (pieces) {
            case 1 -> 2.4;
            case 2 -> 2.8;
            case 3 -> 3.2;
            default -> 3.6;
        };
        double maxDamage = switch (pieces) {
            case 1 -> 4.8;
            case 2 -> 6.6;
            case 3 -> 8.4;
            default -> 10.2;
        };
        for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(origin, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || target.equals(player)) continue;
            double distance = Math.max(0.75, target.getLocation().distance(origin));
            double falloff = Math.max(0.0, 1.0 - distance / radius);
            double damage = maxDamage * (0.35 + falloff * 0.65);
            if (damage > 0.25) {
                CombatHandler.applyInternalDamage(target, damage, player);
            }
        }
    }

    private static void applyBaseEnchantments(ItemMeta meta, String set, ArmorDefinition definition) {
        for (ArmorDefinition.ArmorEnchantmentDefinition enchantment : definition.enchantments()) {
            Enchantment resolved = resolveEnchantment(enchantment.id());
            if (resolved != null) addEnchantIfHigher(meta, resolved, enchantment.level());
        }
        if ("swift".equals(set)) {
            addEnchantIfHigher(meta, Enchantment.UNBREAKING, 4);
        } else if ("explosive".equals(set)) {
            addEnchantIfHigher(meta, Enchantment.BLAST_PROTECTION, 4);
        }
    }

    private static Enchantment resolveEnchantment(String id) {
        if (id == null) return null;
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "unbreaking", "durability" -> Enchantment.UNBREAKING;
            case "blast_protection", "blast-protection" -> Enchantment.BLAST_PROTECTION;
            case "protection" -> Enchantment.PROTECTION;
            case "fire_protection", "fire-protection" -> Enchantment.FIRE_PROTECTION;
            case "projectile_protection", "projectile-protection" -> Enchantment.PROJECTILE_PROTECTION;
            case "feather_falling", "feather-falling" -> Enchantment.FEATHER_FALLING;
            default -> null;
        };
    }

    private static void addEnchantIfHigher(ItemMeta meta, Enchantment enchantment, int level) {
        if (level <= meta.getEnchantLevel(enchantment)) return;
        meta.addEnchant(enchantment, level, true);
    }

    public static Map<String, String> setItemDefinitions() {
        Map<String, String> values = new LinkedHashMap<>();
        armorDefinitions().forEach((id, definition) -> values.put(id, definition.name() + "|" + definition.description()));
        return values;
    }

    public static Map<String, ArmorDefinition> armorDefinitions() {
        return ConfigManager.getArmorDefinitions();
    }

    private static Material resolveMaterial(ArmorDefinition definition, String set, String piece) {
        Material configured = Material.matchMaterial(normalizeMaterial(definition.material()));
        if (configured != null) return configured;
        String prefix = switch (set) {
            case "thorns" -> "CHAINMAIL";
            case "swift" -> "GOLDEN";
            case "explosive" -> "COPPER";
            case "guardian" -> "IRON";
            case "vampire" -> "NETHERITE";
            case "storm" -> "DIAMOND";
            default -> "LEATHER";
        };
        Material material = Material.matchMaterial(prefix + "_" + materialSuffix(piece));
        if (material != null) return material;
        if ("explosive".equals(set)) return Material.matchMaterial("LEATHER_" + materialSuffix(piece));
        return Material.LEATHER_CHESTPLATE;
    }

    private static String normalizeMaterial(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) normalized = normalized.substring("MINECRAFT:".length());
        return normalized;
    }

    private static String materialSuffix(String piece) {
        return switch (piece) {
            case "helmet" -> "HELMET";
            case "chestplate" -> "CHESTPLATE";
            case "leggings" -> "LEGGINGS";
            case "boots" -> "BOOTS";
            default -> "CHESTPLATE";
        };
    }

    private static String armorSet(String id, ArmorDefinition definition) {
        if (definition.set() != null && !definition.set().isBlank()) return definition.set().toLowerCase(Locale.ROOT);
        int underscore = id == null ? -1 : id.indexOf('_');
        return underscore <= 0 ? "" : id.substring(0, underscore).toLowerCase(Locale.ROOT);
    }

    private static String armorPiece(String id, ArmorDefinition definition) {
        if (definition.piece() != null && !definition.piece().isBlank()) return definition.piece().toLowerCase(Locale.ROOT);
        String lower = id == null ? "" : id.toLowerCase(Locale.ROOT);
        for (String suffix : List.of("helmet", "chestplate", "leggings", "boots")) {
            if (lower.endsWith("_" + suffix)) return suffix;
        }
        return "";
    }

    private static String armorAffix(String set, ArmorDefinition definition) {
        if (definition.affix() != null && !definition.affix().isBlank()) return definition.affix().toLowerCase(Locale.ROOT);
        return set;
    }

    private static List<String> armorLore(String set, ArmorDefinition definition) {
        if (!definition.lore().isEmpty()) return definition.lore();
        return switch (set) {
            case "thorns" -> List.of(
                    "§f荆棘: §f受击反弹基于敌人攻击力的伤害",
                    "§71/2/3/4件: §f35% / 45% / 55% / 70% 反伤",
                    "§74件: §f受击获得亢奋与力量 II"
            );
            case "swift" -> List.of(
                    "§9神速: §f每件提供速度等级 +1",
                    "§7原版自带: §f用不坏 IV",
                    "§74件: §fDash 变为3充能，冷却4秒"
            );
            case "explosive" -> List.of(
                    "§5炸药: §f受击概率触发TNT爆炸",
                    "§71/2/3/4件: §f25% / 45% / 75% / 100%",
                    "§74件: §f爆炸后获得力量 III",
                    "§7原版自带: §f爆炸保护 IV"
            );
            default -> List.of("§7" + definition.description());
        };
    }

    private static String setColor(String set) {
        return switch (set) {
            case "swift" -> "§9";
            case "explosive" -> "§5";
            case "guardian" -> "§a";
            case "vampire" -> "§4";
            case "storm" -> "§b";
            default -> "§f";
        };
    }
}
