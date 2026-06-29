package com.roguelike.level;

import com.roguelike.RoguelikePlugin;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.ticket.TicketManager;
import com.roguelike.util.Message;
import org.bukkit.entity.Player;

public class LevelManager {
    private static final int BASE_EXP = 50;
    private static RoguelikePlugin plugin;

    public static void init(RoguelikePlugin plugin) {
        LevelManager.plugin = plugin;
    }

    public static int calculateLevel(long totalExp) {
        if (totalExp < BASE_EXP) return 1;
        double ratio = totalExp / (double) BASE_EXP + 1.0;
        int level = (int) (Math.log(ratio) / Math.log(2)) + 1;
        return Math.max(1, level);
    }

    public static long totalExpForLevel(int level) {
        if (level <= 1) return 0;
        return (long) (BASE_EXP * (Math.pow(2, level - 1) - 1));
    }

    public static long expForLevel(int level) {
        if (level <= 1) return 0;
        return (long) (BASE_EXP * Math.pow(2, level - 2));
    }

    public static long expToNextLevel(long totalExp) {
        int current = calculateLevel(totalExp);
        long needed = totalExpForLevel(current + 1);
        return Math.max(0, needed - totalExp);
    }

    public static void updateExpBar(Player player) {
        RoguelikeScoreboard.updatePlayer(player);
    }

    public static void addExperience(Player player, long amount) {
        if (amount <= 0) return;
        PlayerData data = PlayerDataManager.get(player);
        int oldLevel = data.getLevel();
        data.addExp(amount);
        int newLevel = data.getLevel();

        updateExpBar(player);

        if (newLevel > oldLevel) {
            final int gained = newLevel - oldLevel;
            Message.send(player, "&6&l═══════ LEVEL UP! ═══════");
            Message.send(player, "  &e你升到了等级 " + newLevel + "!");
            if (gained > 1) {
                Message.send(player, "  &6(+" + gained + " 等级!)");
            }
            Message.send(player, "  &7升级奖励:");
            Message.send(player, "    &c强化券 &fx" + gained);
            Message.send(player, "    &a开发券 &fx" + gained);
            Message.send(player, "    &9重置券 &fx" + gained);

            plugin.getServer().getScheduler().runTask(plugin, () -> TicketManager.giveLevelUpTickets(player, gained));
            PlayerDataManager.save(player);
        }
    }
}
