package com.roguelike.scoreboard;

import com.roguelike.RoguelikePlugin;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.integration.IntegrationManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    private static RoguelikePlugin plugin;
    private static final Map<UUID, org.bukkit.scoreboard.Scoreboard> boards = new HashMap<>();
    private static boolean enabled = true;

    public static void init(RoguelikePlugin plugin) {
        ScoreboardManager.plugin = plugin;
        enabled = plugin.getConfig().getBoolean("scoreboard.enabled", false);
        long interval = Math.max(10L, plugin.getConfig().getLong("scoreboard.update-interval", 20L));

        if (enabled) {
            Bukkit.getScheduler().runTaskTimer(plugin, ScoreboardManager::updateAll, interval, interval);
        }
    }

    public static void updatePlayer(Player player) {
        if (!enabled) {
            clear(player);
            return;
        }
        org.bukkit.scoreboard.Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), k -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective sidebar = board.getObjective("rl_sidebar");
        if (sidebar == null) {
            sidebar = board.registerNewObjective("rl_sidebar", Criteria.DUMMY, Component.text("§6§l═══ 玩家统计 ═══"));
            sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        PlayerData data = PlayerDataManager.get(player);

        setLine(board, sidebar, "§r", 6);
        setLine(board, sidebar, "§6等级: §e" + data.getLevel(), 5);
        setLine(board, sidebar, "§6经验: §e" + data.getExpForCurrentLevel() + "/" + data.getExpToNextLevel(), 4);
        setLine(board, sidebar, "§6击杀: §c" + data.getKills(), 3);
        setLine(board, sidebar, "§6死亡: §4" + data.getDeaths(), 2);
        setLine(board, sidebar, "§r ", 1);
        setLine(board, sidebar, "§7§o按 Tab 查看全服排行", 0);
        board.resetScores("§7");

        updateTabList(player);
        player.setScoreboard(board);
    }

    private static void setLine(org.bukkit.scoreboard.Scoreboard board, Objective objective, String text, int score) {
        Team team = board.getTeam("line_" + score);
        if (team == null) {
            team = board.registerNewTeam("line_" + score);
        }
        String entry = "§" + Integer.toHexString(score);
        team.addEntry(entry);
        team.prefix(Component.text(text));
        objective.getScore(entry).setScore(score);
    }

    public static void updateTabList(Player target) {
        if (!enabled) return;
        PlayerData data = PlayerDataManager.get(target);
        String listName = "§7K§c" + data.getKills() + " §7D§4" + data.getDeaths() + " §f" + target.getName();
        target.playerListName(Component.text(listName));
    }

    public static void updateAll() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public static void remove(Player player) {
        clear(player);
        boards.remove(player.getUniqueId());
    }

    public static void clear(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.playerListName(Component.text(player.getName()));
    }
}
