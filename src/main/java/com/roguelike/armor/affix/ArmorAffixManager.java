package com.roguelike.armor.affix;

import com.roguelike.RoguelikePlugin;
import com.roguelike.equipment.EquipmentTypeResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ArmorAffixManager {
    private static final Map<String, ArmorAffix> AFFIXES = new LinkedHashMap<>();
    private static NamespacedKey AFFIX_LEVELS_KEY;
    private static NamespacedKey VANILLA_LEVELS_KEY;

    static {
    }

    public static void init(RoguelikePlugin plugin) {
        AFFIX_LEVELS_KEY = new NamespacedKey(plugin, "armor_affix_levels");
        VANILLA_LEVELS_KEY = new NamespacedKey(plugin, "armor_affix_vanilla_levels");
    }

    public static List<String> effectIds() {
        return List.copyOf(AFFIXES.keySet());
    }

    public static List<String> effectIds(Material material) {
        List<String> ids = new ArrayList<>();
        for (ArmorAffix affix : AFFIXES.values()) {
            if (affix.isApplicable(material)) ids.add(affix.id());
        }
        return ids;
    }

    public static ArmorAffix get(String id) {
        return AFFIXES.get(id);
    }

    public static String displayName(String id) {
        ArmorAffix affix = get(id);
        return affix == null ? id : affix.displayName();
    }

    public static String format(String id, int level) {
        ArmorAffix affix = get(id);
        return affix == null ? level + "级" : affix.format(level);
    }

    public static int generateBaseLevel(String id, Random random) {
        ArmorAffix affix = get(id);
        return affix == null ? 1 : affix.generateBaseLevel(random);
    }

    public static int strengthen(String id, int currentLevel) {
        ArmorAffix affix = get(id);
        return affix == null ? currentLevel + 1 : affix.strengthen(currentLevel);
    }

    public static boolean isApplicable(String id, Material material) {
        ArmorAffix affix = get(id);
        return affix != null && affix.isApplicable(material);
    }

    public static boolean isArmor(Material material) {
        return EquipmentTypeResolver.isWearable(material);
    }

    public static void applyEnchant(ItemStack stack, String id, int level) {
        if (stack == null || stack.getType().isAir()) return;
        ArmorAffix affix = get(id);
        if (affix == null || level <= 0 || !affix.isApplicable(stack.getType())) return;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        Map<String, Integer> levels = readLevels(meta, AFFIX_LEVELS_KEY);
        Map<String, Integer> vanillaLevels = readLevels(meta, VANILLA_LEVELS_KEY);
        vanillaLevels.putIfAbsent(id, meta.getEnchantLevel(affix.enchantment()));

        int affixLevel = Math.min(level, affix.maxLevel());
        int visibleLevel = Math.max(vanillaLevels.getOrDefault(id, 0), affixLevel);
        levels.put(id, affixLevel);
        writeLevels(meta, AFFIX_LEVELS_KEY, levels);
        writeLevels(meta, VANILLA_LEVELS_KEY, vanillaLevels);
        if (visibleLevel > 0) {
            meta.addEnchant(affix.enchantment(), visibleLevel, true);
        }
        stack.setItemMeta(meta);
    }

    public static int getAppliedLevel(ItemStack stack, String id) {
        if (stack == null || stack.getType().isAir()) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        return readLevels(meta, AFFIX_LEVELS_KEY).getOrDefault(id, 0);
    }

    public static boolean hasAppliedAffix(ItemStack stack, String id) {
        return getAppliedLevel(stack, id) > 0;
    }

    public static void removeAppliedAffix(ItemStack stack, String id) {
        if (stack == null || stack.getType().isAir()) return;
        ArmorAffix affix = get(id);
        if (affix == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        Map<String, Integer> levels = readLevels(meta, AFFIX_LEVELS_KEY);
        if (levels.remove(id) == null) return;
        Map<String, Integer> vanillaLevels = readLevels(meta, VANILLA_LEVELS_KEY);
        int vanillaLevel = vanillaLevels.getOrDefault(id, 0);
        vanillaLevels.remove(id);
        writeLevels(meta, AFFIX_LEVELS_KEY, levels);
        writeLevels(meta, VANILLA_LEVELS_KEY, vanillaLevels);

        if (vanillaLevel > 0) {
            meta.addEnchant(affix.enchantment(), vanillaLevel, true);
        } else {
            meta.removeEnchant(affix.enchantment());
        }
        stack.setItemMeta(meta);
    }

    private static Map<String, Integer> readLevels(ItemMeta meta, NamespacedKey key) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        if (key == null) return levels;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String raw = pdc.get(key, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return levels;
        for (String entry : raw.split(";")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank()) continue;
            try {
                levels.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return levels;
    }

    private static void writeLevels(ItemMeta meta, NamespacedKey key, Map<String, Integer> levels) {
        if (key == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (levels.isEmpty()) {
            pdc.remove(key);
            return;
        }
        StringBuilder builder = new StringBuilder();
        levels.forEach((id, level) -> {
            if (level <= 0) return;
            if (!builder.isEmpty()) builder.append(';');
            builder.append(id).append(':').append(level);
        });
        if (builder.isEmpty()) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.STRING, builder.toString());
        }
    }

    private static void register(ArmorAffix affix) {
        AFFIXES.put(affix.id(), affix);
    }

    private static ArmorAffix enchantment(String id, String displayName, Enchantment enchantment, int maxLevel, ArmorSlot slot) {
        return new SimpleArmorAffix(id, displayName, enchantment, maxLevel, slot);
    }

    private enum ArmorSlot {
        HELMET(EquipmentTypeResolver.ArmorSlot.HELMET),
        CHESTPLATE(EquipmentTypeResolver.ArmorSlot.CHESTPLATE),
        LEGGINGS(EquipmentTypeResolver.ArmorSlot.LEGGINGS),
        BOOTS(EquipmentTypeResolver.ArmorSlot.BOOTS),
        ARMOR;

        private final EquipmentTypeResolver.ArmorSlot slot;

        ArmorSlot() {
            this.slot = null;
        }

        ArmorSlot(EquipmentTypeResolver.ArmorSlot slot) {
            this.slot = slot;
        }

        boolean matches(Material material) {
            EquipmentTypeResolver.ArmorSlot actual = EquipmentTypeResolver.armorSlot(material);
            return this == ARMOR ? EquipmentTypeResolver.isWearable(material) : slot == actual;
        }
    }

    private static class SimpleArmorAffix implements ArmorAffix {
        private final String id;
        private final String displayName;
        private final Enchantment enchantment;
        private final int maxLevel;
        private final ArmorSlot slot;

        SimpleArmorAffix(String id, String displayName, Enchantment enchantment, int maxLevel, ArmorSlot slot) {
            this.id = id;
            this.displayName = displayName;
            this.enchantment = enchantment;
            this.maxLevel = maxLevel;
            this.slot = slot;
        }

        @Override
        public String id() { return id; }

        @Override
        public String displayName() { return displayName; }

        @Override
        public Enchantment enchantment() { return enchantment; }

        @Override
        public int maxLevel() { return maxLevel; }

        @Override
        public boolean isApplicable(Material material) { return slot.matches(material); }

        @Override
        public int generateBaseLevel(Random random) {
            return maxLevel <= 1 ? 1 : 1 + random.nextInt(maxLevel);
        }

        @Override
        public int strengthen(int currentLevel) {
            return Math.min(maxLevel, currentLevel + 1);
        }

        @Override
        public String format(int level) {
            return Math.max(1, Math.min(maxLevel, level)) + "级";
        }
    }
}
