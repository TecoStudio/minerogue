package com.roguelike.armor;

import com.roguelike.RoguelikePlugin;
import com.roguelike.combat.CombatHandler;
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
import org.bukkit.inventory.EntityEquipment;
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
        ArmorSet set = ArmorSet.fromItemId(id);
        ArmorPiece piece = ArmorPiece.fromItemId(id);
        if (set == null || piece == null) return null;

        ItemStack stack = new ItemStack(set.material(piece));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(Message.toComponent(set.color + name));
        List<Component> lore = new ArrayList<>();
        lore.add(Message.toComponent("§7" + description));
        lore.add(Message.toComponent("§7─────────────────"));
        for (String line : set.lore) {
            lore.add(Message.toComponent(line));
        }
        lore.add(Message.toComponent("§7─────────────────"));
        lore.add(Message.toComponent("§8套装: " + set.id + " / 部位: " + piece.id));
        meta.lore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(SET_KEY, PersistentDataType.STRING, set.id);
        meta.getPersistentDataContainer().set(PIECE_KEY, PersistentDataType.STRING, piece.id);
        applyBaseEnchantments(meta, set);
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isSetItemId(String id) {
        return ArmorSet.fromItemId(id) != null && ArmorPiece.fromItemId(id) != null;
    }

    public static void applyPassiveEffects(Player player) {
        int swift = countPieces(player, ArmorSet.SWIFT);
        if (swift > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, Math.min(3, swift - 1), true, false, true));
        }
    }

    public static void handlePlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        int thorns = countPieces(player, ArmorSet.THORNS);
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

        int explosive = countPieces(player, ArmorSet.EXPLOSIVE);
        if (explosive > 0 && RANDOM.nextDouble() < explosiveExplosionChance(explosive)) {
            applyExplosiveSetBlast(player, explosive);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            if (explosive >= 4) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 2, true, true, true));
            }
        }
    }

    public static int swiftPieces(Player player) {
        return countPieces(player, ArmorSet.SWIFT);
    }

    private static int countPieces(Player player, ArmorSet set) {
        PlayerInventory inventory = player.getInventory();
        int count = 0;
        if (isPiece(inventory.getHelmet(), set)) count++;
        if (isPiece(inventory.getChestplate(), set)) count++;
        if (isPiece(inventory.getLeggings(), set)) count++;
        if (isPiece(inventory.getBoots(), set)) count++;
        return count;
    }

    private static boolean isPiece(ItemStack stack, ArmorSet set) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        String id = meta.getPersistentDataContainer().get(SET_KEY, PersistentDataType.STRING);
        return set.id.equals(id);
    }

    private static double thornsReflectPercent(int pieces) {
        return switch (pieces) {
            case 1 -> 0.25;
            case 2 -> 0.30;
            case 3 -> 0.35;
            default -> 0.45;
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

    private static void applyBaseEnchantments(ItemMeta meta, ArmorSet set) {
        if (set == ArmorSet.SWIFT) {
            addEnchantIfHigher(meta, Enchantment.UNBREAKING, 4);
        } else if (set == ArmorSet.EXPLOSIVE) {
            addEnchantIfHigher(meta, Enchantment.BLAST_PROTECTION, 4);
        }
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
        Map<String, ArmorDefinition> values = new LinkedHashMap<>();
        values.put("thorns_helmet", new ArmorDefinition("荆棘头盔", "锁链荆棘套部件", "common"));
        values.put("thorns_chestplate", new ArmorDefinition("荆棘胸甲", "锁链荆棘套部件", "common"));
        values.put("thorns_leggings", new ArmorDefinition("荆棘护腿", "锁链荆棘套部件", "common"));
        values.put("thorns_boots", new ArmorDefinition("荆棘靴子", "锁链荆棘套部件", "common"));
        values.put("swift_helmet", new ArmorDefinition("神速头盔", "黄金神速套部件", "rare"));
        values.put("swift_chestplate", new ArmorDefinition("神速胸甲", "黄金神速套部件", "rare"));
        values.put("swift_leggings", new ArmorDefinition("神速护腿", "黄金神速套部件", "rare"));
        values.put("swift_boots", new ArmorDefinition("神速靴子", "黄金神速套部件", "rare"));
        values.put("explosive_helmet", new ArmorDefinition("炸药头盔", "铜质炸药套部件", "epic"));
        values.put("explosive_chestplate", new ArmorDefinition("炸药胸甲", "铜质炸药套部件", "epic"));
        values.put("explosive_leggings", new ArmorDefinition("炸药护腿", "铜质炸药套部件", "epic"));
        values.put("explosive_boots", new ArmorDefinition("炸药靴子", "铜质炸药套部件", "epic"));
        return values;
    }

    public record ArmorDefinition(String name, String description, String rarity) {
    }

    private enum ArmorSet {
        THORNS("thorns", "§f", List.of(
                "§f荆棘套: §f受击反弹基于敌人攻击力的伤害",
                "§72/3/4件: §f30% / 35% / 45% 反伤",
                "§74件: §f受击获得亢奋与力量 II"
        )),
        SWIFT("swift", "§9", List.of(
                "§9神速套: §f每件提供速度等级 +1",
                "§7自带: §f用不坏 IV",
                "§74件: §fDash 变为3充能，冷却4秒"
        )),
        EXPLOSIVE("explosive", "§5", List.of(
                "§5炸药套: §f受击概率触发TNT爆炸",
                "§71/2/3/4件: §f25% / 45% / 75% / 100%",
                "§7自带: §f爆炸保护 IV；4件爆炸后获得力量 III"
        ));

        private final String id;
        private final String color;
        private final List<String> lore;

        ArmorSet(String id, String color, List<String> lore) {
            this.id = id;
            this.color = color;
            this.lore = lore;
        }

        Material material(ArmorPiece piece) {
            String prefix = switch (this) {
                case THORNS -> "CHAINMAIL";
                case SWIFT -> "GOLDEN";
                case EXPLOSIVE -> "COPPER";
            };
            Material material = Material.matchMaterial(prefix + "_" + piece.materialSuffix);
            if (material != null) return material;
            if (this == EXPLOSIVE) return Material.matchMaterial("LEATHER_" + piece.materialSuffix);
            return Material.LEATHER_CHESTPLATE;
        }

        static ArmorSet fromItemId(String itemId) {
            if (itemId == null) return null;
            String normalized = itemId.toLowerCase(Locale.ROOT);
            for (ArmorSet set : values()) {
                if (normalized.startsWith(set.id + "_")) return set;
            }
            return null;
        }
    }

    private enum ArmorPiece {
        HELMET("helmet", "HELMET"),
        CHESTPLATE("chestplate", "CHESTPLATE"),
        LEGGINGS("leggings", "LEGGINGS"),
        BOOTS("boots", "BOOTS");

        private final String id;
        private final String materialSuffix;

        ArmorPiece(String id, String materialSuffix) {
            this.id = id;
            this.materialSuffix = materialSuffix;
        }

        static ArmorPiece fromItemId(String itemId) {
            if (itemId == null) return null;
            String normalized = itemId.toLowerCase(Locale.ROOT);
            for (ArmorPiece piece : values()) {
                if (normalized.endsWith("_" + piece.id)) return piece;
            }
            return null;
        }
    }
}
