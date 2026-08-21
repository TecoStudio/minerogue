package com.roguelike.boss;

import com.roguelike.RoguelikePlugin;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Boss 伤害实时侧边栏：为正在查看某 Boss 的玩家重建伤害排行榜。
 * 每玩家仅一个侧边栏，与 RoguelikeScoreboard 互斥（通过 BossDamageTracker 的 viewing 状态协调）。
 */
public final class BossDamageScoreboard {
    private static final ChatColor[] UNIQUE_LINE_SUFFIXES = ChatColor.values();
    private static final NumberFormat INT_FORMAT = NumberFormat.getIntegerInstance(Locale.ROOT);
    private static final int DEFAULT_TOP = 10;

    private static RoguelikePlugin plugin;
    private static int taskId = -1;
    private static int updateInterval = 10;
    private static int topDisplay = DEFAULT_TOP;
    private static boolean enabled = true;

    private BossDamageScoreboard() {
    }

    public static void init(RoguelikePlugin plugin) {
        BossDamageScoreboard.plugin = plugin;
        loadConfig();
        restartTask();
    }

    static void loadConfig() {
        if (plugin == null) return;
        enabled = plugin.getConfig().getBoolean("boss-damage-scoreboard.enabled", true);
        updateInterval = Math.max(5, plugin.getConfig().getInt("boss-damage-scoreboard.update-interval-ticks", 10));
        topDisplay = Math.max(1, plugin.getConfig().getInt("boss-damage-scoreboard.top-display", DEFAULT_TOP));
        restartTask();
    }

    public static boolean isEnabled() {
        return enabled && plugin != null;
    }

    private static void restartTask() {
        stopTask();
        if (!isEnabled()) return;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, BossDamageScoreboard::updateAll, 5L, updateInterval);
    }

    private static void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public static void shutdown() {
        stopTask();
        restoreAll();
    }

    public static boolean isViewing(Player player) {
        return player != null && BossDamageTracker.bossForPlayer(player.getUniqueId()) != null;
    }

    /** 玩家首次对 Boss 造成伤害时切入 Boss 侧边栏。 */
    static void show(Player player, UUID bossUuid, String bossDisplayName) {
        if (!isEnabled() || player == null || bossUuid == null) return;
        BossDamageTracker.setViewing(player.getUniqueId(), bossUuid);
        updatePlayer(player, bossUuid, bossDisplayName);
    }

    static void updateAll() {
        if (!isEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID bossUuid = BossDamageTracker.bossForPlayer(player.getUniqueId());
            if (bossUuid == null) continue;
            updatePlayer(player, bossUuid, null);
        }
    }

    private static void updatePlayer(Player player, UUID bossUuid, String hintName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        String bossName = hintName != null ? hintName : BossDamageTracker.bossDisplayNameFor(bossUuid, null);

        List<Map.Entry<UUID, Double>> entries = BossDamageTracker.sortedEntries(bossUuid);
        double total = BossDamageTracker.totalDamage(bossUuid);
        UUID myId = player.getUniqueId();

        List<String> lines = new ArrayList<>();
        boolean inTop = false;
        int shown = 0;
        for (Map.Entry<UUID, Double> entry : entries) {
            if (shown >= topDisplay) break;
            String name = nameOf(entry.getKey());
            if (entry.getKey().equals(myId)) inTop = true;
            lines.add(formatLine(shown + 1, name, entry.getValue(), entry.getKey().equals(myId)));
            shown++;
        }
        if (!inTop && !entries.isEmpty() && entries.size() > topDisplay) {
            double mine = BossDamageTracker.playerDamage(bossUuid, myId);
            if (mine > 0) {
                int rank = rankOf(entries, myId);
                lines.add("&7" + rank + ". " + nameOf(myId) + " &e" + INT_FORMAT.format((long) mine));
            }
        }
        lines.add("&8─────────────────");
        lines.add("&7总伤害: &e" + INT_FORMAT.format((long) total));

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("boss_damage", "dummy", Message.toComponent("&c" + bossName + " 伤害榜"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        int score = lines.size();
        int index = 0;
        for (String line : lines) {
            setLine(objective, color(line), score--, index++);
        }
        player.setScoreboard(board);
    }

    /** 恢复某玩家到正常 Roguelike 侧边栏。 */
    static void restore(Player player) {
        if (player == null) return;
        BossDamageTracker.clearViewing(player.getUniqueId());
        if (RoguelikeScoreboard.isEnabled()) {
            RoguelikeScoreboard.updatePlayer(player);
        } else {
            RoguelikeScoreboard.clearPlayer(player);
        }
    }

    static void restoreAll() {
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (BossDamageTracker.bossForPlayer(player.getUniqueId()) != null) {
                restore(player);
            }
        }
    }

    /** Boss 死亡时广播伤害总榜到聊天。 */
    static void broadcastLeaderboard(UUID bossUuid, String bossDisplayName) {
        List<Map.Entry<UUID, Double>> entries = BossDamageTracker.sortedEntries(bossUuid);
        double total = BossDamageTracker.totalDamage(bossUuid);
        String name = bossDisplayName != null ? bossDisplayName : BossDamageTracker.bossDisplayNameFor(bossUuid, null);
        Bukkit.broadcast(Message.toComponent("&6&l═══ " + name + " 伤害排行 ═══"));
        if (entries.isEmpty()) {
            Bukkit.broadcast(Message.toComponent("&7无伤害记录。"));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Double> entry : entries) {
                if (rank > 10) break;
                Bukkit.broadcast(Message.toComponent(
                        (rank == 1 ? "&e1" : "&7" + rank) + ". &f" + nameOf(entry.getKey()) + " &e" + INT_FORMAT.format((long) (double) entry.getValue())));
                rank++;
            }
        }
        Bukkit.broadcast(Message.toComponent("&7总伤害: &e" + INT_FORMAT.format((long) total)));
    }

    private static String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null ? name : "???";
    }

    private static int rankOf(List<Map.Entry<UUID, Double>> entries, UUID uuid) {
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : entries) {
            if (entry.getKey().equals(uuid)) return rank;
            rank++;
        }
        return entries.size() + 1;
    }

    private static String formatLine(int rank, String name, double damage, boolean self) {
        String prefix = self ? "&a" + rank : "&7" + rank;
        return prefix + ". &f" + name + " &e" + INT_FORMAT.format((long) damage);
    }

    private static void setLine(Objective objective, String text, int score, int index) {
        String suffix = UNIQUE_LINE_SUFFIXES[index % UNIQUE_LINE_SUFFIXES.length].toString();
        int maxTextLength = Math.max(0, 40 - suffix.length());
        String line = text.length() > maxTextLength ? text.substring(0, maxTextLength) : text;
        line = line + suffix;
        objective.getScore(line).setScore(score);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
