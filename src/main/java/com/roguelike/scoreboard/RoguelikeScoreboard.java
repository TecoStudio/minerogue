package com.roguelike.scoreboard;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.level.LevelManager;
import com.roguelike.weapon.BowAbilityManager;
import com.roguelike.weapon.WeaponAbilityManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public class RoguelikeScoreboard {
    private static RoguelikePlugin plugin;
    private static int taskId = -1;
    private static final ChatColor[] UNIQUE_LINE_SUFFIXES = ChatColor.values();

    public static void init(RoguelikePlugin plugin) {
        RoguelikeScoreboard.plugin = plugin;
        restartTask();
    }

    public static void restartTask() {
        stopTask();
        if (!isEnabled()) return;
        int interval = Math.max(10, plugin.getConfig().getInt("scoreboard.update-interval", 20));
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, RoguelikeScoreboard::updateAll, 1L, interval);
    }

    public static void shutdown() {
        stopTask();
        clearAll();
    }

    private static void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public static boolean isEnabled() {
        return plugin != null && ConfigManager.getPluginConfig().getBoolean("scoreboard.enabled", true);
    }

    public static void updateAll() {
        if (!isEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public static void updatePlayer(Player player) {
        if (!isEnabled() || player == null || !player.isOnline()) return;
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("roguelike", "dummy", color(ConfigManager.getSidebarTitle()));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerData data = PlayerDataManager.get(player);
        List<String> lines = buildLines(player, data);
        int score = lines.size();
        int index = 0;
        for (String line : lines) {
            setLine(objective, line, score--, index++);
        }

        player.setScoreboard(board);
    }

    private static List<String> buildLines(Player player, PlayerData data) {
        List<String> lines = new ArrayList<>();
        List<String> abilityLines = WeaponAbilityManager.getSidebarLines(player);
        abilityLines = new ArrayList<>(abilityLines);
        abilityLines.addAll(BowAbilityManager.getSidebarLines(player));
        long levelRequiredExp = LevelManager.expForLevel(data.getLevel() + 1);
        for (String configured : ConfigManager.getSidebarLines()) {
            if (configured.contains("%ability_cooldowns%")) {
                for (String abilityLine : abilityLines) {
                    lines.add(color(configured.replace("%ability_cooldowns%", abilityLine)));
                }
                continue;
            }
            lines.add(color(configured
                    .replace("%player%", player.getName())
                    .replace("%level%", Integer.toString(data.getLevel()))
                    .replace("%exp%", Long.toString(data.getExpForCurrentLevel()))
                    .replace("%exp_next%", Long.toString(levelRequiredExp))
                    .replace("%kills%", Integer.toString(data.getKills()))
                    .replace("%deaths%", Integer.toString(data.getDeaths()))));
        }
        return lines;
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

    public static void clearPlayer(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    public static void clearAll() {
        Bukkit.getOnlinePlayers().forEach(RoguelikeScoreboard::clearPlayer);
    }
}
