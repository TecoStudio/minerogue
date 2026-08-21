package com.roguelike.boss;

import com.roguelike.mob.internal.ScriptedInternalMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 累计对 Boss 造成的伤害（按玩家），并记录哪些玩家正在查看 Boss 伤害侧边栏。
 * <p>
 * Boss 识别同时覆盖周期事件 Boss（活动区域 bossEntityUuid）和 /rw boss spawn 手动召唤
 * （PDC roguelike:internal_mob 属于已配置 Boss 的 mobId）。
 */
public final class BossDamageTracker {
    private static final Map<UUID, Map<UUID, Double>> DAMAGE = new ConcurrentHashMap<>();
    private static final Map<UUID, DamageSummary> SUMMARY = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> VIEWING = new ConcurrentHashMap<>();

    private BossDamageTracker() {
    }

    static void clearAll() {
        DAMAGE.clear();
        SUMMARY.clear();
        VIEWING.clear();
    }

    /** 玩家对某 Boss 实体造成伤害。返回该 Boss 的显示名（用于首次切入侧边栏），无 Boss 上下文返回 null。 */
    static void recordDamage(UUID bossUuid, UUID playerUuid, double amount, String bossDisplayName) {
        if (bossUuid == null || playerUuid == null || amount <= 0) return;
        DAMAGE.computeIfAbsent(bossUuid, k -> new ConcurrentHashMap<>())
                .merge(playerUuid, amount, Double::sum);
        SUMMARY.computeIfAbsent(bossUuid, k -> new DamageSummary(bossDisplayName)).add(amount);
    }

    static boolean isBossEntity(LivingEntity entity) {
        if (entity == null) return false;
        ActiveBossArena arena = BossEventManager.activeArena();
        if (arena != null && arena.bossEntityUuid() != null
                && arena.bossEntityUuid().equals(entity.getUniqueId())) {
            return true;
        }
        String mobId = ScriptedInternalMob.mobIdOf(entity);
        if (mobId == null) return false;
        return BossEventManager.configuredBossMobIds().contains(mobId);
    }

    static String bossDisplayNameFor(UUID bossUuid, LivingEntity entity) {
        DamageSummary summary = SUMMARY.get(bossUuid);
        if (summary != null && summary.displayName != null) return summary.displayName;
        if (entity != null) {
            String mobId = ScriptedInternalMob.mobIdOf(entity);
            if (mobId != null) {
                try {
                    return com.roguelike.config.ConfigManager.getScriptedMobConfig(mobId).name();
                } catch (Exception ignored) {
                }
            }
        }
        return "Boss";
    }

    static List<Map.Entry<UUID, Double>> sortedEntries(UUID bossUuid) {
        Map<UUID, Double> map = DAMAGE.get(bossUuid);
        if (map == null || map.isEmpty()) return List.of();
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<UUID, Double> e) -> e.getValue()).reversed());
        return Collections.unmodifiableList(entries);
    }

    static double totalDamage(UUID bossUuid) {
        DamageSummary summary = SUMMARY.get(bossUuid);
        return summary == null ? 0.0 : summary.total;
    }

    static double playerDamage(UUID bossUuid, UUID playerUuid) {
        Map<UUID, Double> map = DAMAGE.get(bossUuid);
        return map == null ? 0.0 : map.getOrDefault(playerUuid, 0.0);
    }

    static void clear(UUID bossUuid) {
        if (bossUuid == null) return;
        DAMAGE.remove(bossUuid);
        SUMMARY.remove(bossUuid);
        VIEWING.values().removeIf(bossUuid::equals);
    }

    static void setViewing(UUID playerUuid, UUID bossUuid) {
        VIEWING.put(playerUuid, bossUuid);
    }

    static void clearViewing(UUID playerUuid) {
        VIEWING.remove(playerUuid);
    }

    static UUID bossForPlayer(UUID playerUuid) {
        return VIEWING.get(playerUuid);
    }

    static List<UUID> viewersOf(UUID bossUuid) {
        List<UUID> viewers = new ArrayList<>();
        for (var entry : VIEWING.entrySet()) {
            if (bossUuid.equals(entry.getValue())) viewers.add(entry.getKey());
        }
        return viewers;
    }

    /** 离线/退出时移除该玩家，并恢复其正常侧边栏。 */
    static void onPlayerQuit(Player player) {
        VIEWING.remove(player.getUniqueId());
    }

    private static final class DamageSummary {
        private final String displayName;
        private volatile double total;

        DamageSummary(String displayName) {
            this.displayName = displayName;
        }

        void add(double amount) {
            total += amount;
        }
    }
}
