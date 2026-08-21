package com.roguelike.command;

import com.roguelike.RoguelikePlugin;
import com.roguelike.debug.DebugRecord;
import com.roguelike.debug.DebugService;
import com.roguelike.util.Message;
import org.bukkit.command.CommandSender;

class DebugCommand {
    private static final int DEFAULT_TAIL_COUNT = 10;
    private static final int MIN_TAIL_COUNT = 1;
    private static final int MAX_TAIL_COUNT = 50;

    boolean handleDebug(CommandSender sender, String[] args) {
        RoguelikePlugin plugin = RoguelikePlugin.getInstance();
        DebugService service = plugin == null ? null : plugin.getDebugService();
        if (service == null) {
            Message.send(sender, "&cDebug 服务尚未初始化。");
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, service);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "on", "true", "enable" -> setEnabled(sender, plugin, service, true);
            case "off", "false", "disable" -> setEnabled(sender, plugin, service, false);
            case "status" -> Message.send(sender, "&6Debug: &f" + service.status());
            case "reload" -> {
                service.reload();
                Message.send(sender, "&aDebug 配置已重载。");
            }
            case "tail" -> showTail(sender, service, args);
            case "stats" -> service.statsLines().forEach(line -> Message.send(sender, "&7" + line));
            case "clear" -> {
                service.clear();
                Message.send(sender, "&aDebug 内存记录和统计已清空。");
            }
            case "flush" -> {
                if (service.flush()) {
                    Message.send(sender, "&aDebug 日志已刷新到文件。");
                } else {
                    Message.send(sender, "&cDebug 日志刷新失败或超时，部分记录可能未写入文件。");
                }
            }
            default -> sendUsage(sender, service);
        }
        return true;
    }

    private void setEnabled(CommandSender sender, RoguelikePlugin plugin, DebugService service, boolean enabled) {
        plugin.getConfig().set("debug.enabled", enabled);
        plugin.saveConfig();
        service.setEnabled(enabled);
        Message.send(sender, enabled ? "&aDebug 日志已开启。" : "&cDebug 日志已关闭。");
    }

    private void showTail(CommandSender sender, DebugService service, String[] args) {
        int count = DEFAULT_TAIL_COUNT;
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                Message.send(sender, "&c数量必须是 1-50 的整数。");
                return;
            }
        }
        if (count < MIN_TAIL_COUNT || count > MAX_TAIL_COUNT) {
            Message.send(sender, "&c数量必须是 1-50。");
            return;
        }
        var records = service.tail(count);
        Message.send(sender, "&6最近 " + records.size() + " 条 Debug 记录:");
        for (DebugRecord record : records) Message.send(sender, "&7" + record.format());
    }

    private void sendUsage(CommandSender sender, DebugService service) {
        Message.send(sender, "&6Debug: &f" + service.status());
        Message.send(sender, "&7用法: /rw debug <status|on|off|reload|tail [1-50]|stats|clear|flush>");
        Message.send(sender, "&7开关别名: true/false/enable/disable");
    }
}
