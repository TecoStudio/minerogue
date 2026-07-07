package com.roguelike.command;

import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.util.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class PlayerCommandHandler {
    boolean handlePlayer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令。");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showStatus(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "tickets" -> showTickets(player);
            case "trade" -> showTrade(player);
            case "help", "?" -> showHelp(player);
            default -> Message.send(player, "&c未知命令。使用 /rl help");
        }
        return true;
    }

    private void showStatus(Player player) {
        PlayerData data = PlayerDataManager.get(player);
        Message.send(player, "&6&l═══ 状态 ═══");
        Message.send(player, "&6等级: &e" + data.getLevel());
        Message.send(player, "&6经验: &e" + data.getExpForCurrentLevel() + "/" + data.getExpToNextLevel());
        Message.send(player, "&6击杀: &c" + data.getKills());
        Message.send(player, "&6死亡: &4" + data.getDeaths());
    }

    private void showTickets(Player player) {
        PlayerData data = PlayerDataManager.get(player);
        Message.send(player, "&6&l═══ 已使用武器券 ═══");
        Message.send(player, "&c强化券: &f" + data.getTicketAUses());
        Message.send(player, "&f超级强化券: &f" + data.getSuperTicketAUses());
        Message.send(player, "&a开发券: &f" + data.getTicketBUses());
        Message.send(player, "&9移除券: &f" + data.getTicketCUses());
    }

    private void showTrade(Player player) {
        Message.send(player, "&6&l═══ 自由交易 ═══");
        Message.send(player, "&7本服不使用金币、余额、商店或价格系统。");
        Message.send(player, "&7武器、强化券和掉落物都可以由玩家自行协商交换。");
        Message.send(player, "&e建议使用原版容器、投掷物品或其他服务器已有安全交易插件完成交换。");
    }

    private void showHelp(Player player) {
        Message.send(player, "&6&l═══ Roguelike 帮助 ═══");
        Message.send(player, "&e/rl &7- 查看状态");
        Message.send(player, "&e/rl tickets &7- 查看已使用武器券数量");
        Message.send(player, "&e/rl trade &7- 查看自由交易说明");
    }
}
