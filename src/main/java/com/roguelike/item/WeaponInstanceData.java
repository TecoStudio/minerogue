package com.roguelike.item;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguelike.RoguelikePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WeaponInstanceData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static NamespacedKey KEY;

    private String instanceId;
    private String baseWeaponId;
    private String customName;
    private int gearLevel;
    private String quality;
    private String legendaryAffix;
    private double damageBonus;
    private double attackSpeedBonus;
    private double storedDamage;
    private int storedDamageHits;
    private final Map<String, Double> effectBonuses;
    private final List<String> appliedModifiers;
    private int ticketAUses;
    private int ticketAFailStreak;
    private double ticketAFailBonus;
    private int ticketBUses;
    private int ticketCUses;

    public WeaponInstanceData(String baseWeaponId) {
        this.instanceId = UUID.randomUUID().toString();
        this.baseWeaponId = baseWeaponId;
        this.customName = null;
        this.gearLevel = 1;
        this.quality = "base";
        this.legendaryAffix = "";
        this.damageBonus = 0;
        this.attackSpeedBonus = 0;
        this.storedDamage = 0;
        this.storedDamageHits = 0;
        this.effectBonuses = new HashMap<>();
        this.appliedModifiers = new ArrayList<>();
        this.ticketAUses = 0;
        this.ticketAFailStreak = 0;
        this.ticketAFailBonus = 0;
        this.ticketBUses = 0;
        this.ticketCUses = 0;
    }

    public static void init(RoguelikePlugin plugin) {
        KEY = new NamespacedKey(plugin, "roguelike_weapon");
    }

    public String getInstanceId() {
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = UUID.randomUUID().toString();
        }
        return instanceId;
    }

    public String getBaseWeaponId() { return baseWeaponId; }
    public void setBaseWeaponId(String id) { this.baseWeaponId = id; }

    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public int getGearLevel() { return Math.max(1, gearLevel); }
    public void setGearLevel(int gearLevel) { this.gearLevel = Math.max(1, gearLevel); }

    public String getQuality() { return normalizeQuality(quality); }
    public void setQuality(String quality) { this.quality = normalizeQuality(quality); }

    public String getLegendaryAffix() { return legendaryAffix == null ? "" : legendaryAffix; }
    public void setLegendaryAffix(String legendaryAffix) { this.legendaryAffix = legendaryAffix == null ? "" : legendaryAffix; }

    public int getQualityPowerBonus() {
        return switch (getQuality()) {
            case "plus" -> 2;
            case "plusplus" -> 4;
            case "s" -> 6;
            case "legendary" -> 3;
            default -> 1;
        };
    }

    public int getGearPower() {
        return getGearLevel() + getQualityPowerBonus();
    }

    public int getRandomAffixSlotLimit(CustomWeapon template) {
        int baseSlots = switch (getQuality()) {
            case "plus" -> 2;
            case "plusplus" -> 3;
            case "s" -> 4;
            case "legendary" -> 3;
            default -> 1;
        };
        int bonus = template == null ? 0 : template.getBonusAffixSlots();
        return Math.max(0, baseSlots + bonus);
    }

    public int getRandomAffixCount() {
        int count = 0;
        for (double value : effectBonuses.values()) {
            if (value != 0.0) count++;
        }
        return count;
    }

    public boolean hasOpenRandomAffixSlot(CustomWeapon template) {
        return true;
    }

    public boolean isOverflowingRandomAffixSlots(CustomWeapon template) {
        return getRandomAffixCount() > getRandomAffixSlotLimit(template);
    }

    public double getDamageBonus() { return damageBonus; }
    public void addDamageBonus(double amount) { this.damageBonus += amount; }
    public void setDamageBonus(double damageBonus) { this.damageBonus = damageBonus; }

    public double getAttackSpeedBonus() { return attackSpeedBonus; }
    public void addAttackSpeedBonus(double amount) { this.attackSpeedBonus += amount; }
    public void setAttackSpeedBonus(double attackSpeedBonus) { this.attackSpeedBonus = attackSpeedBonus; }

    public double getStoredDamage() { return storedDamage; }
    public void setStoredDamage(double storedDamage) { this.storedDamage = storedDamage; }
    public void addStoredDamage(double amount) { this.storedDamage += amount; }

    public int getStoredDamageHits() { return storedDamageHits; }
    public void setStoredDamageHits(int storedDamageHits) { this.storedDamageHits = Math.max(0, storedDamageHits); }
    public void incrementStoredDamageHits() { this.storedDamageHits++; }

    public Map<String, Double> getEffectBonuses() {
        return new HashMap<>(effectBonuses);
    }

    public double getEffectBonus(String key) {
        return effectBonuses.getOrDefault(canonicalEffectId(key), 0.0);
    }

    public double getEffectBonus(String key, double defaultValue) {
        return effectBonuses.getOrDefault(canonicalEffectId(key), defaultValue);
    }

    public void addEffectBonus(String key, double amount) {
        String canonicalKey = canonicalEffectId(key);
        effectBonuses.put(canonicalKey, effectBonuses.getOrDefault(canonicalKey, 0.0) + amount);
    }

    public void setEffectBonus(String key, double value) {
        effectBonuses.put(canonicalEffectId(key), value);
    }

    public void removeEffectBonus(String key) {
        effectBonuses.remove(canonicalEffectId(key));
    }

    public List<String> getAppliedModifiers() {
        return new ArrayList<>(appliedModifiers);
    }

    public void addModifier(String id) {
        appliedModifiers.add(id);
    }

    public int getTicketAUses() { return ticketAUses; }
    public void incrementTicketAUses() { this.ticketAUses++; }

    public int getTicketAFailStreak() { return ticketAFailStreak; }
    public void incrementTicketAFailStreak() { this.ticketAFailStreak++; }
    public void resetTicketAFailStreak() {
        this.ticketAFailStreak = 0;
        this.ticketAFailBonus = 0;
    }

    public double getTicketAFailBonus() { return ticketAFailBonus; }
    public void addTicketAFailBonus(double amount) { this.ticketAFailBonus += Math.max(0, amount); }

    public int getTicketBUses() { return ticketBUses; }
    public void incrementTicketBUses() { this.ticketBUses++; }

    public int getTicketCUses() { return ticketCUses; }
    public void incrementTicketCUses() { this.ticketCUses++; }

    public double getScaledBaseDamage(CustomWeapon base) {
        return base.getBaseDamage() * (1.0 + getGearPower() * 0.08);
    }

    public double getTotalDamage(CustomWeapon base) {
        return getScaledBaseDamage(base) + damageBonus;
    }

    public double getTotalAttackSpeed(CustomWeapon base) {
        return base.getAttackSpeed() + attackSpeedBonus;
    }

    public double getTotalEffect(CustomWeapon base, String key) {
        return base.getEffect(key, 0.0) + getEffectBonus(key, 0.0);
    }

    public double getTotalEffect(CustomWeapon base, String key, double defaultValue) {
        return base.getEffect(key, defaultValue) + getEffectBonus(key, 0.0);
    }

    public void saveToItemStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return;
        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, GSON.toJson(this));
        });
    }

    public static WeaponInstanceData fromItemStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(KEY, PersistentDataType.STRING)) return null;
        String json = pdc.get(KEY, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return null;
        try {
            WeaponInstanceData data = GSON.fromJson(json, WeaponInstanceData.class);
            if (data != null) data.normalizeNewFields();
            if (data != null && (data.instanceId == null || data.instanceId.isBlank())) {
                // Older items did not store an instance id. Assign one lazily so GUI
                // confirmations can verify the exact item instead of only its template.
                data.instanceId = UUID.randomUUID().toString();
                data.saveToItemStack(stack);
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRoguelikeWeapon(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY, PersistentDataType.STRING);
    }

    public static void removeFromItemStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(KEY);
        stack.setItemMeta(meta);
    }

    private void normalizeNewFields() {
        if (gearLevel <= 0) gearLevel = 1;
        quality = normalizeQuality(quality);
        if (legendaryAffix == null) legendaryAffix = "";
        migrateLegacyEffectIds();
    }

    private void migrateLegacyEffectIds() {
        Map<String, Double> migrated = new HashMap<>();
        for (Map.Entry<String, Double> entry : effectBonuses.entrySet()) {
            migrated.merge(canonicalEffectId(entry.getKey()), entry.getValue(), Double::sum);
        }
        effectBonuses.clear();
        effectBonuses.putAll(migrated);
    }

    private static String canonicalEffectId(String key) {
        if (key == null) return null;
        return switch (key) {
            case "neutral_damage_200" -> "contract_damage_200";
            case "neutral_speed_200" -> "contract_speed_200";
            case "neutral_attack_speed_200" -> "contract_attack_speed_200";
            case "neutral_range_200" -> "contract_range_200";
            case "neutral_crit_chance_100" -> "contract_crit_chance_100";
            case "neutral_crit_damage_300" -> "contract_crit_damage_300";
            case "neutral_lifesteal_100" -> "contract_lifesteal_100";
            case "neutral_thunder_100" -> "contract_thunder_100";
            case "neutral_explosion_100" -> "contract_explosion_100";
            case "neutral_berserk_self_harm" -> "contract_berserk_self_harm";
            default -> key;
        };
    }

    private static String normalizeQuality(String quality) {
        if (quality == null || quality.isBlank()) return "base";
        String normalized = quality.trim().toLowerCase();
        return switch (normalized) {
            case "+", "plus" -> "plus";
            case "++", "plusplus" -> "plusplus";
            case "s" -> "s";
            case "l", "legendary" -> "legendary";
            default -> "base";
        };
    }
}
