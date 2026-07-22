package com.roguelike.boss;

import com.roguelike.mob.MobManager;
import com.roguelike.util.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BossEventCommand {
    public boolean handle(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("spawn")) {
            return handleDirectSpawn(sender, args);
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("event")) {
            sendUsage(sender);
            return true;
        }
        if (args.length < 3 || args[2].equalsIgnoreCase("status")) {
            BossEventManager.statusLines().forEach(line -> Message.send(sender, line));
            return true;
        }
        switch (args[2].toLowerCase()) {
            case "force" -> handleForce(sender);
            case "clear" -> handleClear(sender);
            case "next" -> handleNext(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private boolean handleDirectSpawn(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Message.send(sender, "&c用法: /rw boss spawn <" + String.join("|", BossEventManager.configuredBossIds()) + ">");
            return true;
        }
        if (!BossEventManager.isConfiguredBossId(args[2])) {
            Message.send(sender, "&c无效的 Boss。可用: " + String.join(", ", BossEventManager.configuredBossIds()));
            return true;
        }
        if (!MobManager.isAcceptedMobId(args[2])) {
            Message.send(sender, "&cBoss 已在事件配置中，但当前内置怪物未加载: " + args[2]);
            return true;
        }
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&c只有玩家可以直接召唤 Boss。");
            return true;
        }
        var entity = MobManager.spawnInternalMob(args[2], player.getLocation());
        if (entity == null) {
            Message.send(sender, "&c召唤 Boss 失败，请检查当前位置和世界状态。");
            return true;
        }
        Message.send(sender, "&a已直接召唤 Boss " + args[2] + "。");
        return true;
    }

    private void handleForce(CommandSender sender) {
        if (BossEventManager.hasActiveArena()) {
            Message.send(sender, "&c已有活动 Boss 事件，请先使用 /rw boss event clear 清理。");
            return;
        }
        if (BossEventManager.forceSpawn()) {
            Message.send(sender, "&a已强制触发周期 Boss 事件。");
        } else {
            Message.send(sender, "&c触发失败：请确认 world 存在、至少一名玩家在线且内置 Boss 可生成。");
        }
    }

    private void handleClear(CommandSender sender) {
        if (!BossEventManager.hasActiveArena()) {
            Message.send(sender, "&c当前没有活动 Boss 事件。");
            return;
        }
        BossEventManager.clearActiveArena(true);
        Message.send(sender, "&a已清理当前 Boss 事件，区域保护已解除。");
    }

    private void handleNext(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Message.send(sender, "&c用法: /rw boss event next <hours>");
            return;
        }
        double hours;
        try {
            hours = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            Message.send(sender, "&c小时数必须是数字，例如 48 或 0.1。");
            return;
        }
        if (hours < 0.0) {
            Message.send(sender, "&c小时数不能为负数。");
            return;
        }
        BossEventManager.setNextSpawnHours(hours);
        Message.send(sender, "&a已调整下一次 Boss 事件时间。");
    }

    private void sendUsage(CommandSender sender) {
        Message.send(sender, usage());
    }

    static String usage() {
        return "&c用法: /rw boss <spawn|event> ...，例如 /rw boss spawn blood-zombie、/rw boss spawn vagrant 或 /rw boss event status";
    }
}
