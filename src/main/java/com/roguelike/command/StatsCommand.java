package com.roguelike.command;

import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class StatsCommand {
    boolean handleStats(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("top")) {
            return handleStatsTop(sender, args);
        }
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            Message.send(sender, "&c找不到玩家。");
            return true;
        }
        PlayerData data = PlayerDataManager.get(target);
        Message.send(sender, "&6&l═══ " + target.getName() + " 的状态 ═══");
        Message.send(sender, "&6等级: &e" + data.getLevel());
        Message.send(sender, "&6经验: &e" + data.getExpForCurrentLevel() + "/" + data.getExpToNextLevel());
        Message.send(sender, "&6击杀: &c" + data.getKills());
        Message.send(sender, "&6死亡: &4" + data.getDeaths());
        return true;
    }

    private boolean handleStatsTop(CommandSender sender, String[] args) {
        String type = args.length >= 3 ? args[2].toLowerCase() : "level";
        int limit = 10;
        if (args.length >= 4) {
            try {
                limit = Math.max(1, Math.min(20, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) {
                limit = 10;
            }
        }

        Comparator<Player> comparator = switch (type) {
            case "kills" -> Comparator.comparingInt(player -> PlayerDataManager.get(player).getKills());
            case "deaths" -> Comparator.comparingInt(player -> PlayerDataManager.get(player).getDeaths());
            case "level" -> Comparator.comparingInt(player -> PlayerDataManager.get(player).getLevel());
            default -> null;
        };
        if (comparator == null) {
            Message.send(sender, "&c用法: /rw stats top <level|kills|deaths> [数量]");
            return true;
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(comparator.reversed());
        if (players.size() > limit) {
            players = players.subList(0, limit);
        }
        Message.send(sender, "&6&l═══ 在线排行榜: " + statDisplayName(type) + " ═══");
        if (players.isEmpty()) {
            Message.send(sender, "&7当前没有在线玩家。");
            return true;
        }
        int rank = 1;
        for (Player player : players) {
            PlayerData data = PlayerDataManager.get(player);
            Message.send(sender, "&e#" + rank++ + " &f" + player.getName() + " &7- &a" + statValue(type, data));
        }
        return true;
    }

    private String statDisplayName(String type) {
        return switch (type) {
            case "kills" -> "击杀";
            case "deaths" -> "死亡";
            default -> "等级";
        };
    }

    private String statValue(String type, PlayerData data) {
        return switch (type) {
            case "kills" -> Integer.toString(data.getKills());
            case "deaths" -> Integer.toString(data.getDeaths());
            default -> Integer.toString(data.getLevel());
        };
    }
}
