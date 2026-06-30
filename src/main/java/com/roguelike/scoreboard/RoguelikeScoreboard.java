package com.roguelike.scoreboard;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.level.LevelManager;
import com.roguelike.weapon.WeaponAbilityManager;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class RoguelikeScoreboard {
    private static RoguelikePlugin plugin;
    private static int taskId = -1;

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
        Objective objective = board.registerNewObjective("roguelike", "dummy", "§6§lRoguelike");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerData data = PlayerDataManager.get(player);
        java.util.List<String> abilityLines = WeaponAbilityManager.getSidebarLines(player);
        long levelRequiredExp = LevelManager.expForLevel(data.getLevel() + 1);
        int score = 8 + abilityLines.size();
        setLine(objective, "§6统计", score--);
        setLine(objective, "§f玩家: §e" + player.getName(), score--);
        setLine(objective, "§f等级: §e" + data.getLevel(), score--);
        setLine(objective, "§f经验: §a" + data.getExpForCurrentLevel() + "§7/§a" + levelRequiredExp, score--);
        setLine(objective, "§f击杀: §c" + data.getKills(), score--);
        setLine(objective, "§f死亡: §4" + data.getDeaths(), score--);
        if (!abilityLines.isEmpty()) {
            setLine(objective, "§2", score--);
            for (String line : abilityLines) {
                setLine(objective, line, score--);
            }
        }
        setLine(objective, "§1", score--);
        setLine(objective, "§7-----", score);

        player.setScoreboard(board);
    }

    private static void setLine(Objective objective, String text, int score) {
        String line = text.length() > 40 ? text.substring(0, 40) : text;
        objective.getScore(line).setScore(score);
    }

    private static String weaponName(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomWeapon weapon = WeaponManager.getTemplate(hand);
        WeaponInstanceData data = WeaponManager.getData(hand);
        if (weapon == null || data == null) return "无";
        String name = data.getCustomName() != null ? ChatColor.stripColor(data.getCustomName()) : weapon.getName();
        return name.length() > 12 ? name.substring(0, 12) : name;
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
