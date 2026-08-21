package com.roguelike.debug;

import com.roguelike.RoguelikePlugin;
import com.roguelike.item.WeaponInstanceData;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Small, immutable and payload-safe summary of weapon instance state for diagnostics. */
public record WeaponDebugContext(String baseWeaponId, String instanceId, String material,
                                 List<String> effectKeys, double storedDamage, int bonusCount,
                                 boolean missing, boolean invalid, String errorType) {
    public WeaponDebugContext {
        baseWeaponId = safe(baseWeaponId);
        instanceId = safe(instanceId);
        material = safe(material);
        effectKeys = List.copyOf(effectKeys == null ? List.of() : new ArrayList<>(effectKeys));
        storedDamage = Double.isFinite(storedDamage) ? storedDamage : 0.0;
        bonusCount = Math.max(0, bonusCount);
        errorType = safe(errorType);
    }

    public static WeaponDebugContext from(Material material, WeaponInstanceData data) {
        if (data == null) return missing(material);
        try {
            return new WeaponDebugContext(data.getBaseWeaponId(), data.getInstanceId(), materialName(material),
                    data.getEffectBonuses().keySet().stream().filter(key -> key != null && !key.isBlank()).sorted().toList(),
                    data.getStoredDamage(), data.getRandomAffixCount(), false, false, "");
        } catch (RuntimeException exception) {
            return invalid(material, exception.getClass());
        }
    }

    public static WeaponDebugContext missing(Material material) {
        return new WeaponDebugContext("", "", materialName(material), List.of(), 0.0, 0, true, false, "");
    }

    public static WeaponDebugContext invalid(Material material, Class<?> exceptionType) {
        return new WeaponDebugContext("", "", materialName(material), List.of(), 0.0, 0, false, true,
                exceptionType == null ? "unknown" : exceptionType.getSimpleName());
    }

    public Map<String, Object> fields() {
        return Map.of("baseWeaponId", baseWeaponId, "instanceId", instanceId, "material", material,
                "effectKeys", String.join(",", effectKeys), "storedDamage", storedDamage,
                "bonusCount", bonusCount, "missing", missing, "invalid", invalid,
                "errorType", errorType);
    }

    public static void trace(String event, String phase, Material material, WeaponInstanceData data, Map<String, ?> values) {
        try {
            RoguelikePlugin plugin = RoguelikePlugin.getInstance();
            DebugService service = plugin == null ? null : plugin.getDebugService();
            if (service == null || !service.isEnabled()) return;
            trace(service, event, phase, from(material, data), values);
        } catch (RuntimeException ignored) {
            // Diagnostics must never affect gameplay.
        }
    }

    public static void trace(DebugService service, String event, String phase, WeaponDebugContext context, Map<String, ?> values) {
        try {
            if (service == null || !service.isEnabled()) return;
            java.util.LinkedHashMap<String, Object> fields = new java.util.LinkedHashMap<>();
            if (context != null) fields.putAll(context.fields());
            if (values != null) fields.putAll(values);
            DebugEventSummary.record(service, event, phase, fields);
        } catch (RuntimeException ignored) {
            // Diagnostics must never affect gameplay.
        }
    }

    public static void trace(String event, String phase, WeaponDebugContext context, Map<String, ?> values) {
        try {
            RoguelikePlugin plugin = RoguelikePlugin.getInstance();
            DebugService service = plugin == null ? null : plugin.getDebugService();
            trace(service, event, phase, context, values);
        } catch (RuntimeException ignored) {
            // Diagnostics must never affect gameplay.
        }
    }

    private static String materialName(Material material) {
        return material == null ? "UNKNOWN" : material.name();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
