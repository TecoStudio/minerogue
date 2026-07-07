package com.roguelike.command;

import com.roguelike.util.Message;
import org.bukkit.command.CommandSender;

class AdminCommandHandler {
    private final AdminInfoCommands infoCommands = new AdminInfoCommands();
    private final ExportCommand exportCommand = new ExportCommand();
    private final DebugCommand debugCommand = new DebugCommand();
    private final AffixesCommand affixesCommand = new AffixesCommand();
    private final GiveCommand giveCommand = new GiveCommand();
    private final StatsCommand statsCommand = new StatsCommand();

    boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("roguelike.admin")) {
            Message.send(sender, "&c你没有权限。");
            return true;
        }
        if (args.length == 0) {
            infoCommands.showAdminHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> infoCommands.handleReload(sender);
            case "backup" -> infoCommands.handleBackup(sender);
            case "export" -> {
                return exportCommand.handleExport(sender);
            }
            case "debug" -> {
                return debugCommand.handleDebug(sender, args);
            }
            case "affixes" -> {
                return affixesCommand.handleAffixes(sender, args);
            }
            case "give" -> {
                if (args.length < 3) {
                    Message.send(sender, "&c用法: /roguelike give <weapon|item|ticket> <id> [玩家] [数量]");
                    return true;
                }
                return giveCommand.handleGive(sender, args);
            }
            case "exp" -> infoCommands.handleExp(sender, args);
            case "list" -> infoCommands.handleList(sender, args);
            case "stats" -> {
                return statsCommand.handleStats(sender, args);
            }
            case "reset" -> infoCommands.handleReset(sender, args);
            case "monster" -> infoCommands.handleMonster(sender, args);
            case "fixhand" -> infoCommands.handleFixhand(sender);
            case "help", "?" -> infoCommands.showAdminHelp(sender);
            default -> Message.send(sender, "&c未知命令。使用 /rw help");
        }
        return true;
    }
}
