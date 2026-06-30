package com.roguelike.command;

import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.level.LevelManager;
import com.roguelike.mob.MobManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.ticket.TicketManager;
import com.roguelike.ticket.TicketType;
import com.roguelike.util.DevLog;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RoguelikeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("rl")) {
            return handlePlayer(sender, args);
        } else if (cmd.equals("rw") || cmd.equals("roguelike")) {
            return handleAdmin(sender, args);
        }
        return false;
    }

    private boolean handlePlayer(CommandSender sender, String[] args) {
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

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("roguelike.admin")) {
            Message.send(sender, "&c你没有权限。");
            return true;
        }
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                ConfigManager.reload();
                RoguelikeScoreboard.restartTask();
                RoguelikeScoreboard.updateAll();
                Message.send(sender, "&a配置已重载。");
            }
            case "export" -> {
                return handleExport(sender);
            }
            case "debug" -> {
                return handleDebug(sender, args);
            }
            case "affixes" -> {
                return handleAffixes(sender, args);
            }
            case "give" -> {
                if (args.length < 3) {
                    Message.send(sender, "&c用法: /roguelike give <weapon|item|ticket> <id> [玩家] [数量]");
                    return true;
                }
                return handleGive(sender, args);
            }
            case "exp" -> {
                if (args.length < 2) {
                    Message.send(sender, "&c用法: /roguelike exp <数量> [玩家]");
                    return true;
                }
                Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    Message.send(sender, "&c找不到玩家。");
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    Message.send(sender, "&c经验数量必须是数字。");
                    return true;
                }
                LevelManager.addExperience(target, amount);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 经验。");
            }
            case "list" -> {
                if (args.length < 2) {
                    Message.send(sender, "&c用法: /rw list <weapons|items>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("weapons")) {
                    Message.send(sender, "&6可用武器:");
                    for (CustomWeapon w : ConfigManager.getWeapons()) {
                        sender.sendMessage("§7- " + WeaponManager.getRarityColor(w.getRarity()) + w.getName() + " §7(" + w.getId() + ")");
                    }
                } else if (args[1].equalsIgnoreCase("items")) {
                    Message.send(sender, "&6可用物品:");
                    for (CustomItem i : ConfigManager.getItems()) {
                        sender.sendMessage("§7- " + i.getName() + " (" + i.getId() + ")");
                    }
                }
            }
            case "stats" -> {
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
            }
            case "reset" -> {
                Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    Message.send(sender, "&c找不到玩家。");
                    return true;
                }
                PlayerDataManager.unload(target);
                PlayerDataManager.get(target);
                LevelManager.updateExpBar(target);
                Message.send(sender, "&a已重置 " + target.getName() + " 的 Roguelike 数据。");
            }
            case "monster" -> {
                if (!(sender instanceof Player player)) {
                    Message.send(sender, "&c只有玩家可以生成怪物。");
                    return true;
                }
                if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
                    Message.send(sender, "&c用法: /rw monster spawn <" + String.join("|", MobManager.getSpawnableMobIds()) + ">");
                    return true;
                }
                var entity = MobManager.spawnInternalMob(args[2], player.getLocation());
                if (entity == null) {
                    Message.send(sender, "&c无效的自定义怪物。可用: " + String.join(", ", MobManager.getSpawnableMobIds()));
                    return true;
                }
                Message.send(sender, "&a已生成 " + args[2] + "。");
            }
            case "fixhand" -> {
                if (!(sender instanceof Player player)) {
                    Message.send(sender, "&c只有玩家可以刷新手持武器。");
                    return true;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (!WeaponManager.refreshWeapon(hand)) {
                    Message.send(sender, "&c你手上没有 Roguelike 武器。");
                    return true;
                }
                WeaponManager.refreshHeldWeapon(player);
                Message.send(sender, "&a已刷新手持武器属性。");
            }
            case "help", "?" -> showAdminHelp(sender);
            default -> Message.send(sender, "&c未知命令。使用 /rw help");
        }
        return true;
    }

    private boolean handleExport(CommandSender sender) {
        var plugin = ConfigManager.getPlugin();
        Path examples = plugin.getDataFolder().toPath().resolve("examples");

        try {
            ConfigManager.exportEditableYaml();
            writeFile(examples.resolve("weapons.yml"), "weapons:\n" +
                    "  flame_sword:\n" +
                    "    item: minecraft:diamond_sword\n" +
                    "    name: 烈焰之剑\n" +
                    "    description: 燃烧敌人的剑\n" +
                    "    base-damage: 10\n" +
                    "    attack-speed: 1.4\n" +
                    "    durability: 800\n" +
                    "    rarity: epic\n" +
                    "    effects:\n" +
                    "      attack_range: 3.2\n" +
                    "      fire_damage: 4.0\n" +
                    "      fire_duration: 3.0\n" +
                    "      crit_chance: 0.1\n" +
                    "      crit_damage: 1.75\n" +
                    "  excited_stone_sword:\n" +
                    "    item: minecraft:stone_sword\n" +
                    "    name: 石剑\n" +
                    "    description: 僵尸精英使用的亢奋石剑\n" +
                    "    base-damage: 5\n" +
                    "    attack-speed: 1.6\n" +
                    "    durability: 131\n" +
                    "    rarity: common\n" +
                    "    effects:\n" +
                    "      attack_range: 3.0\n" +
                    "      crit_chance: 0.10\n" +
                    "      hyper: 1.0\n");
            writeFile(examples.resolve("items.yml"), "items:\n" +
                    "  healing_potion:\n" +
                    "    name: 治疗药水\n" +
                    "    description: 恢复生命值\n" +
                    "    item-type: potion\n" +
                    "    rarity: common\n" +
                    "    effects:\n" +
                    "      heal_amount: 10\n");
            writeFile(examples.resolve("mobs.yml"), "internal:\n" +
                    "  enabled: true\n" +
                    "  skeleton-elite:\n" +
                    "    enabled: true\n" +
                    "    spawn-chance: 0.12\n" +
                    "    name: '&c骷髅精英'\n" +
                    "  zombie-elite:\n" +
                    "    enabled: true\n" +
                    "    spawn-chance: 0.12\n" +
                    "    name: '&2僵尸精英'\n" +
                    "    weapon-template: excited_stone_sword\n" +
                    "default-experience: 10\n" +
                    "experience:\n" +
                    "  zombie: 15\n" +
                    "  skeleton: 15\n" +
                    "modifiers: {}\n");
            writeFile(examples.resolve("tab-scoreboard.yml"), "scoreboards:\n" +
                    "  roguelike:\n" +
                    "    title: '&6&lRoguelike'\n" +
                    "    lines:\n" +
                    "      - '&7等级: &e%roguelike_level%'\n" +
                    "      - '&7经验: &e%roguelike_exp%/%roguelike_exp_next%'\n" +
                    "      - '&7击杀: &c%roguelike_kills%'\n" +
                    "      - '&7死亡: &4%roguelike_deaths%'\n" +
                    "      - '&7武器: &f%roguelike_weapon_name%'\n");
            writeFile(examples.resolve("mythicmobs.yml"), "RoguelikeSkeletonKnight:\n" +
                    "  Type: SKELETON\n" +
                    "  Display: '&6Skeletal Knight'\n" +
                    "  Health: 40\n" +
                    "  Damage: 6\n" +
                    "  Options:\n" +
                    "    PreventOtherDrops: false\n");
        } catch (IOException e) {
            Message.send(sender, "&c导出示例失败: " + e.getMessage());
            return true;
        }

        Message.send(sender, "&a已导出 YAML 配置到 plugins/Roguelike，并导出示例到 plugins/Roguelike/examples。");
        Message.send(sender, "&7修改 weapons.yml、items.yml、mobs.yml 后执行 /rw reload 生效。");
        return true;
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        String type = args[1].toLowerCase();
        String id = args[2];
        Player target = args.length >= 4 ? Bukkit.getPlayer(args[3]) : (sender instanceof Player p ? p : null);
        int amount = 1;
        if (args.length >= 5) {
            try { amount = Integer.parseInt(args[4]); } catch (NumberFormatException e) { amount = 1; }
        }
        if (target == null) {
            Message.send(sender, "&c找不到玩家。");
            return true;
        }

        switch (type) {
            case "weapon" -> {
                CustomWeapon weapon = ConfigManager.getWeapon(id);
                if (weapon == null) {
                    Message.send(sender, "&c找不到武器: " + id);
                    return true;
                }
                ItemStack stack = WeaponManager.createWeaponStack(weapon, null);
                stack.setAmount(1);
                for (int i = 0; i < amount; i++) {
                    target.getInventory().addItem(stack.clone());
                }
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + weapon.getName());
            }
            case "item" -> {
                CustomItem item = ConfigManager.getItem(id);
                if (item == null) {
                    Message.send(sender, "&c找不到物品: " + id);
                    return true;
                }
                // 简化为给予一个带名称的纸
                ItemStack stack = new ItemStack(Material.PAPER);
                var meta = stack.getItemMeta();
                if (meta != null) {
                    meta.displayName(Message.toComponent("&f" + item.getName()));
                    stack.setItemMeta(meta);
                }
                stack.setAmount(amount);
                target.getInventory().addItem(stack);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + item.getName());
            }
            case "ticket" -> {
                TicketType ticket = TicketType.fromId(id);
                if (ticket == null) {
                    Message.send(sender, "&c可用券类型: ticket_a, super_ticket_a, ticket_b, ticket_c");
                    return true;
                }
                ItemStack stack = TicketManager.createTicket(ticket);
                stack.setAmount(amount);
                target.getInventory().addItem(stack);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 张 " + ticket.getDisplayName());
            }
            default -> Message.send(sender, "&c用法: /rw give <weapon|item|ticket> <id> [玩家] [数量]");
        }
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
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

    private boolean handleAffixes(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Message.send(sender, "&c用法: /rw affixes");
            return true;
        }

        Message.send(sender, "&6&l═══ 可用武器词条 ═══");
        Message.send(sender, "&e基础属性:");
        for (String stat : TicketManager.getBaseStatKeys()) {
            sender.sendMessage("§7- §f" + TicketManager.getStatDisplayName(stat) + " §8(" + stat + ")");
        }
        Message.send(sender, "&e效果词条:");
        for (String stat : TicketManager.getEffectStatKeys()) {
            sender.sendMessage("§7- §f" + TicketManager.getStatDisplayName(stat) + " §8(" + stat + ")");
        }
        return true;
    }

    private void showHeldWeaponAffixes(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomWeapon template = WeaponManager.getTemplate(hand);
        WeaponInstanceData data = WeaponManager.getData(hand);
        if (template == null || data == null) {
            Message.send(player, "&c你手上没有 Roguelike 武器。");
            return;
        }

        Message.send(player, "&6&l═══ 当前武器词条 ═══");
        Message.send(player, "&7模板: &f" + template.getName() + " &8(" + template.getId() + ")");
        sendAffixLine(player, "damage", template.getBaseDamage(), data.getTotalDamage(template));
        sendAffixLine(player, "attack_speed", template.getAttackSpeed(), data.getTotalAttackSpeed(template));
        sendAffixLine(player, "attack_range", template.getEffect("attack_range", 3.0), data.getTotalEffect(template, "attack_range", 3.0));
        for (String stat : TicketManager.getEffectStatKeys()) {
            if (stat.equals("attack_range")) continue;
            double base = template.getEffect(stat, 0.0);
            double total = data.getTotalEffect(template, stat, 0.0);
            if (base != 0.0 || total != 0.0) {
                sendAffixLine(player, stat, base, total);
            }
        }
        Message.send(player, "&7强化券使用次数: &f" + data.getTicketAUses() + " &7连续失败: &f" + data.getTicketAFailStreak());
        Message.send(player, "&7开发券使用次数: &f" + data.getTicketBUses() + " &7重置券: &f" + data.getTicketCUses());
    }

    private void sendAffixLine(CommandSender sender, String stat, double base, double total) {
        String text = "§7- §f" + TicketManager.getStatDisplayName(stat)
                + " §8(" + stat + ") §7基础: §f" + TicketManager.formatStatValue(stat, base)
                + " §7当前: §e" + TicketManager.formatStatValue(stat, total);
        if (base != total) {
            String sign = total > base ? "+" : "";
            text += " §a(" + sign + TicketManager.formatStatValue(stat, total - base) + ")";
        }
        sender.sendMessage(text);
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
        Message.send(player, "&9重置券: &f" + data.getTicketCUses());
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

    private void showAdminHelp(CommandSender sender) {
        Message.send(sender, "&6&l═══ Roguelike 管理员帮助 ═══");
        Message.send(sender, "&e/rw export &7- 导出可选 YAML/TAB/MythicMobs 示例");
        Message.send(sender, "&e/rw debug <on|off|status> &7- 控制开发日志输出");
        Message.send(sender, "&e/rw affixes &7- 查看可用词条");
        Message.send(sender, "&e/rw reload &7- 重载配置");
        Message.send(sender, "&e/rw give weapon <id> [玩家] [数量] &7- 给予武器");
        Message.send(sender, "&e/rw exp <数量> [玩家] &7- 给予经验");
        Message.send(sender, "&e/rw list <weapons|items> &7- 列出模板");
        Message.send(sender, "&e/rw stats [玩家] &7- 查看玩家状态");
        Message.send(sender, "&e/rw reset [玩家] &7- 重置玩家数据");
        Message.send(sender, "&e/rw monster spawn <自定义怪物> &7- 生成插件自定义怪物");
        Message.send(sender, "&e/rw fixhand &7- 刷新手持武器属性");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        List<String> list = new ArrayList<>();
        if (cmd.equals("rl")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("status", "tickets", "trade", "help"));
            }
        } else if (cmd.equals("rw") || cmd.equals("roguelike")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("export", "debug", "affixes", "reload", "give", "exp", "list", "stats", "reset", "monster", "fixhand", "help"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                list.addAll(Arrays.asList("weapon", "item", "ticket"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
                list.addAll(Arrays.asList("on", "off", "status"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("monster")) {
                list.add("spawn");
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                switch (args[1].toLowerCase()) {
                    case "weapon" -> ConfigManager.getWeapons().forEach(w -> list.add(w.getId()));
                    case "item" -> ConfigManager.getItems().forEach(i -> list.add(i.getId()));
                    case "ticket" -> list.addAll(Arrays.asList("ticket_a", "super_ticket_a", "ticket_b", "ticket_c"));
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("monster") && args[1].equalsIgnoreCase("spawn")) {
                list.addAll(MobManager.getSpawnableMobIds());
            } else if ((args.length == 4 && args[0].equalsIgnoreCase("give")) ||
                       ((args.length == 2 && (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("reset"))))) {
                Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
                list.addAll(Arrays.asList("weapons", "items"));
            }
        }
        return list;
    }
}
