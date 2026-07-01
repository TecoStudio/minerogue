package com.roguelike.armor.affix;

import com.roguelike.equipment.EquipmentTypeResolver;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ArmorAffixManager {
    private static final Map<String, ArmorAffix> AFFIXES = new LinkedHashMap<>();

    static {
        register(enchantment("thorns", "荆棘", Enchantment.THORNS, 3, ArmorSlot.ARMOR));
        register(enchantment("swift", "疾步", Enchantment.SWIFT_SNEAK, 3, ArmorSlot.ARMOR));
        register(enchantment("self_explosion", "自爆", Enchantment.BLAST_PROTECTION, 4, ArmorSlot.ARMOR));
        register(enchantment("mending", "经验修补", Enchantment.MENDING, 1, ArmorSlot.ARMOR));
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
        meta.addEnchant(affix.enchantment(), Math.min(level, affix.maxLevel()), true);
        stack.setItemMeta(meta);
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
