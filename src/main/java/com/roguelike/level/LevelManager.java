package com.roguelike.level;

import com.roguelike.RoguelikePlugin;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.ticket.TicketManager;
import com.roguelike.util.Message;
import org.bukkit.entity.Player;

public class LevelManager {
    private static final long[] EXP_TO_NEXT_BY_LEVEL = {
            0, 10, 20, 50, 80, 100, 150, 200, 300, 400, 500, 600
    };
    private static RoguelikePlugin plugin;

    public static void init(RoguelikePlugin plugin) {
        LevelManager.plugin = plugin;
    }

    public static int calculateLevel(long totalExp) {
        int level = 1;
        long remaining = Math.max(0, totalExp);
        while (remaining >= expToNextFromLevel(level)) {
            remaining -= expToNextFromLevel(level);
            level++;
        }
        return level;
    }

    public static long totalExpForLevel(int level) {
        if (level <= 1) return 0;
        long total = 0;
        for (int current = 1; current < level; current++) {
            total += expToNextFromLevel(current);
        }
        return total;
    }

    public static long expForLevel(int level) {
        if (level <= 1) return 0;
        return expToNextFromLevel(level - 1);
    }

    public static long expToNextFromLevel(int level) {
        if (level <= 0) return 0;
        if (level < EXP_TO_NEXT_BY_LEVEL.length) return EXP_TO_NEXT_BY_LEVEL[level];
        return EXP_TO_NEXT_BY_LEVEL[EXP_TO_NEXT_BY_LEVEL.length - 1] + (level - EXP_TO_NEXT_BY_LEVEL.length + 1L) * 100L;
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
            final int strengthenTickets = gained;
            final int developmentTickets = countDevelopmentTickets(oldLevel, newLevel);
            final int removeTickets = countRemoveTickets(oldLevel, newLevel);
            Message.send(player, "&6&l═══════ LEVEL UP! ═══════");
            Message.send(player, "  &e你升到了等级 " + newLevel + "!");
            if (gained > 1) {
                Message.send(player, "  &6(+" + gained + " 等级!)");
            }
            Message.send(player, "  &7升级奖励:");
            Message.send(player, "    &c强化券 &fx" + strengthenTickets);
            if (developmentTickets > 0) Message.send(player, "    &a开发券 &fx" + developmentTickets);
            if (removeTickets > 0) Message.send(player, "    &9移除券 &fx" + removeTickets);

            plugin.getServer().getScheduler().runTask(plugin, () -> TicketManager.giveLevelUpTickets(player, oldLevel, newLevel));
            PlayerDataManager.save(player);
        }
    }

    public static void applyDeathPenalty(Player player) {
        PlayerData data = PlayerDataManager.get(player);
        int oldLevel = data.getLevel();
        if (oldLevel <= 4) {
            updateExpBar(player);
            Message.send(player, "&a等级保护: &7当前等级 " + oldLevel + "，死亡不会掉级。");
            return;
        }

        int levelsLost = oldLevel <= 10 ? 1 : Math.max(1, oldLevel / 3);
        int newLevel = Math.max(1, oldLevel - levelsLost);
        data.setTotalExp(totalExpForLevel(newLevel));
        PlayerDataManager.save(player);
        updateExpBar(player);
        Message.send(player, "&c死亡惩罚: &7失去 " + (oldLevel - newLevel) + " 级，当前等级 " + newLevel + "。");
    }

    private static int countDevelopmentTickets(int oldLevel, int newLevel) {
        int count = 0;
        for (int level = oldLevel + 1; level <= newLevel; level++) {
            if (level == 2 || level % 3 == 0) count++;
        }
        return count;
    }

    private static int countRemoveTickets(int oldLevel, int newLevel) {
        int count = 0;
        for (int level = oldLevel + 1; level <= newLevel; level++) {
            if (level == 2 || level % 5 == 0) count++;
        }
        return count;
    }
}
