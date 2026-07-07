package com.roguelike.command;

import com.roguelike.config.ConfigManager;
import com.roguelike.util.DevLog;
import com.roguelike.util.Message;
import org.bukkit.command.CommandSender;

class DebugCommand {
    boolean handleDebug(CommandSender sender, String[] args) {
        var plugin = ConfigManager.getPlugin();
        if (args.length < 2) {
            Message.send(sender, "&6Debug 日志: " + (DevLog.isEnabled() ? "&a开启" : "&c关闭"));
            Message.send(sender, "&7用法: /rw debug <on|off|status>");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "on", "true", "enable" -> {
                plugin.getConfig().set("debug.enabled", true);
                plugin.saveConfig();
                DevLog.info("Debug logging enabled by " + sender.getName());
                Message.send(sender, "&aDebug 日志已开启。");
            }
            case "off", "false", "disable" -> {
                plugin.getConfig().set("debug.enabled", false);
                plugin.saveConfig();
                DevLog.info("Debug logging disabled by " + sender.getName());
                Message.send(sender, "&cDebug 日志已关闭。");
            }
            case "status" -> Message.send(sender, "&6Debug 日志: " + (DevLog.isEnabled() ? "&a开启" : "&c关闭"));
            default -> Message.send(sender, "&c用法: /rw debug <on|off|status>");
        }
        return true;
    }
}
