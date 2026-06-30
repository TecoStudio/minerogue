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
        return effectBonuses.getOrDefault(key, 0.0);
    }

    public double getEffectBonus(String key, double defaultValue) {
        return effectBonuses.getOrDefault(key, defaultValue);
    }

    public void addEffectBonus(String key, double amount) {
        effectBonuses.put(key, effectBonuses.getOrDefault(key, 0.0) + amount);
    }

    public void setEffectBonus(String key, double value) {
        effectBonuses.put(key, value);
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

    public double getTotalDamage(CustomWeapon base) {
        return base.getBaseDamage() + damageBonus;
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
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY, PersistentDataType.STRING, GSON.toJson(this));
        stack.setItemMeta(meta);
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
}
