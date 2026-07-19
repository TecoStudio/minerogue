package com.roguelike.command;

import com.roguelike.armor.ArmorSetManager;
import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.forge.ForgeRecipeManager;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import com.roguelike.level.LevelManager;
import com.roguelike.mob.MobManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.util.DevLog;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

class AdminInfoCommands {
    void handleReload(CommandSender sender) {
        ConfigManager.getPlugin().reloadConfig();
        DevLog.init(ConfigManager.getPlugin());
        ConfigManager.reload();
        ForgeRecipeManager.reload();
        PlayerDataManager.reloadSettings();
        RoguelikeScoreboard.restartTask();
        RoguelikeScoreboard.updateAll();
        Message.send(sender, "&a配置已重载。");
    }

    void handleBackup(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(ConfigManager.getPlugin(), PlayerDataManager::createBackup);
        Message.send(sender, "&a已开始异步备份玩家数据，请查看控制台日志确认结果。");
    }

    void handleExp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Message.send(sender, "&c用法: /roguelike exp <数量> [玩家]");
            return;
        }
        Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            Message.send(sender, "&c找不到玩家。");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            Message.send(sender, "&c经验数量必须是数字。");
            return;
        }
        LevelManager.addExperience(target, amount);
        Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 经验。");
    }

    void handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Message.send(sender, "&c用法: /rw list <weapons|items|armor>");
            return;
        }
        if (args[1].equalsIgnoreCase("weapons")) {
            Message.send(sender, "&6可用武器:");
            for (CustomWeapon w : ConfigManager.getWeapons()) {
                sender.sendMessage("§7- " + WeaponManager.getRarityColor(w.getRarity()) + w.getName()
                        + " §7[" + WeaponManager.getRarityColor(w.getRarity()) + WeaponManager.getRarityDisplayName(w.getRarity()) + "§7] (" + w.getId() + ")");
            }
        } else if (args[1].equalsIgnoreCase("items")) {
            Message.send(sender, "&6可用物品:");
            for (CustomItem i : ConfigManager.getItems()) {
                sender.sendMessage("§7- " + i.getName() + " (" + i.getId() + ")");
            }
        } else if (args[1].equalsIgnoreCase("armor")) {
            Message.send(sender, "&6可用防具:");
            ArmorSetManager.armorDefinitions().forEach((armorId, armor) ->
                    sender.sendMessage("§7- " + armor.name() + " §7[" + armor.rarity() + "§7] (" + armorId + ")"));
        }
    }

    void handleReset(CommandSender sender, String[] args) {
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            Message.send(sender, "&c找不到玩家。");
            return;
        }
        PlayerDataManager.reset(target);
        LevelManager.updateExpBar(target);
        Message.send(sender, "&a已重置 " + target.getName() + " 的 Roguelike 数据。");
    }

    void handleMonster(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
            Message.send(sender, "&c用法: /rw monster spawn <" + String.join("|", MobManager.getSpawnableMobIds()) + ">");
            return;
        }
        if (!MobManager.isAcceptedMobId(args[2])) {
            Message.send(sender, "&c无效的自定义怪物。可用: " + String.join(", ", MobManager.getAcceptedMobIds()));
            return;
        }
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&c只有玩家可以生成怪物。");
            return;
        }
        var entity = MobManager.spawnInternalMob(args[2], player.getLocation());
        if (entity == null) {
            Message.send(sender, "&c生成怪物失败，请检查当前位置和世界状态。");
            return;
        }
        Message.send(sender, "&a已生成 " + args[2] + "。");
    }

    void handleFixhand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Message.send(sender, "&c只有玩家可以刷新手持武器。");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!WeaponManager.refreshWeapon(hand)) {
            Message.send(sender, "&c你手上没有 Roguelike 武器。");
            return;
        }
        WeaponManager.refreshHeldWeapon(player);
        Message.send(sender, "&a已刷新手持武器属性。");
    }

    void showAdminHelp(CommandSender sender) {
        Message.send(sender, "&6&l═══ Roguelike 管理员帮助 ═══");
        Message.send(sender, "&e/rw export &7- 导出可选 YAML/TAB/MythicMobs 示例");
        Message.send(sender, "&e/rw backup &7- 立即异步备份玩家数据");
        Message.send(sender, "&e/rw debug <on|off|status> &7- 控制开发日志输出");
        Message.send(sender, "&e/rw affixes [held] &7- 查看可用词条/手持武器词条");
        Message.send(sender, "&e/rw reload &7- 重载配置");
        Message.send(sender, "&e/rw give &7- 打开给予 GUI");
        Message.send(sender, "&e/rw give <weapon|item|armor|ticket> <id> [玩家] [数量] &7- 命令给予，ticket 包括工具开发券");
        Message.send(sender, "&e/rw give armor <id> [玩家] [数量] &7- 给予防具");
        Message.send(sender, "&e/rw exp <数量> [玩家] &7- 给予经验");
        Message.send(sender, "&e/rw list <weapons|items|armor> &7- 列出模板");
        Message.send(sender, "&e/rw stats [玩家] &7- 查看玩家状态");
        Message.send(sender, "&e/rw stats top <level|kills|deaths> [数量] &7- 查看在线排行榜");
        Message.send(sender, "&e/rw reset [玩家] &7- 重置玩家数据");
        Message.send(sender, "&e/rw monster spawn <自定义怪物> &7- 生成插件自定义怪物");
        Message.send(sender, "&e/rw fixhand &7- 刷新手持武器属性");
    }
}
