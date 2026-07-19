package com.roguelike.item;

import com.roguelike.util.Message;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CustomItemStackFactory {
    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("roguelike", "custom_item_id");
    private static final String BURGER_TEXTURE = "https://textures.minecraft.net/texture/6ae9c9bc898c7cfe9a3b11b6deaed1757817ad02511ab5a8b306ab92eeb3e2d";

    private CustomItemStackFactory() {
    }

    public static ItemStack createItemStack(CustomItem item) {
        ItemStack stack = new ItemStack(resolveMaterial(item));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, item.getId());
        meta.displayName(Message.toComponent(rarityColor(item.getRarity()) + item.getName()));
        List<Component> lore = new ArrayList<>();
        if (!item.getDescription().isBlank()) lore.add(Message.toComponent("&7" + item.getDescription()));
        lore.add(Message.toComponent("&7品质: " + rarityColor(item.getRarity()) + rarityName(item.getRarity())));
        for (String line : effectLore(item)) {
            lore.add(Message.toComponent("&7- &f" + line));
        }
        meta.lore(lore);

        if (meta instanceof PotionMeta potionMeta) {
            applyPotionMeta(item, potionMeta);
        } else if (meta instanceof SkullMeta skullMeta) {
            applySkullMeta(item, skullMeta);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    public static String getCustomItemId(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public static String materialName(CustomItem item) {
        String raw = item.getItem();
        if (raw == null || raw.isBlank()) return defaultMaterialName(item);
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) normalized = normalized.substring("MINECRAFT:".length());
        return normalized;
    }

    public static List<String> effectLore(CustomItem item) {
        List<String> lore = new ArrayList<>();
        double heal = item.getEffect("heal_amount", 0.0);
        if (heal > 0) lore.add("恢复生命: " + format(heal));
        double speed = item.getEffect("speed_level", 0.0);
        if (speed > 0) lore.add("速度等级: " + format(speed));
        double resistance = item.getEffect("resistance_level", 0.0);
        if (resistance > 0) lore.add("抗性等级: " + format(resistance));
        double duration = item.getEffect("duration_seconds", 0.0);
        if (duration > 0) lore.add("持续时间: " + format(duration) + "秒");
        double healPercent = item.getEffect("heal_percent", 0.0);
        if (healPercent > 0) lore.add("恢复生命: " + format(healPercent * 100.0) + "%");
        if (item.getEffect("full_saturation", 0.0) > 0) lore.add("补满饱食度");
        return lore;
    }

    private static Material resolveMaterial(CustomItem item) {
        Material material = Material.matchMaterial(materialName(item));
        if (material != null && !material.isAir()) return material;
        return Material.matchMaterial(defaultMaterialName(item));
    }

    private static String defaultMaterialName(CustomItem item) {
        String type = item.getItemType();
        if ("potion".equalsIgnoreCase(type) || "tonic".equalsIgnoreCase(type)) return "POTION";
        return "PAPER";
    }

    private static void applyPotionMeta(CustomItem item, PotionMeta meta) {
        double heal = item.getEffect("heal_amount", 0.0);
        double speed = item.getEffect("speed_level", 0.0);
        double resistance = item.getEffect("resistance_level", 0.0);
        int durationTicks = Math.max(1, (int) Math.round(item.getEffect("duration_seconds", 0.0) * 20.0));

        if (speed > 0) {
            meta.setColor(Color.AQUA);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, levelToAmplifier(speed)), true);
        } else if (resistance > 0) {
            meta.setColor(Color.GRAY);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, levelToAmplifier(resistance)), true);
        } else if (heal > 0) {
            meta.setColor(Color.RED);
        }
    }

    private static void applySkullMeta(CustomItem item, SkullMeta meta) {
        if (!"burger".equalsIgnoreCase(item.getId())) return;
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.fromString("0df25a12-1c45-4e24-9b98-4b0f7f7a4a1b"), "RoguelikeBurger");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(BURGER_TEXTURE));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (IllegalArgumentException | MalformedURLException ignored) {
            // Keep the item usable even if the decorative texture cannot be applied.
        }
    }

    private static String rarityColor(String rarity) {
        return switch (rarity == null ? "" : rarity.toLowerCase(Locale.ROOT)) {
            case "rare" -> "&9";
            case "epic" -> "&5";
            case "legendary" -> "&6";
            case "special" -> "&a";
            default -> "&f";
        };
    }

    private static String rarityName(String rarity) {
        return switch (rarity == null ? "" : rarity.toLowerCase(Locale.ROOT)) {
            case "rare" -> "稀有";
            case "epic" -> "史诗";
            case "legendary" -> "传奇";
            case "special" -> "特殊";
            default -> "常见";
        };
    }

    private static int levelToAmplifier(double level) {
        return Math.max(0, (int) Math.round(level) - 1);
    }

    private static String format(double value) {
        if (Math.rint(value) == value) return Long.toString(Math.round(value));
        return Double.toString(value);
    }
}
